package org.sunbird.helper;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.BaseException;
import org.sunbird.common.message.IResponseMessage;
import org.sunbird.common.message.ResponseCode;
import org.sunbird.util.helper.PropertiesCache;
import org.sunbird.cache.util.Platform;

public class CassandraConnectionManagerImpl implements CassandraConnectionManager {
  private static Cluster cluster;
  private static Map<String, Session> cassandraSessionMap = new ConcurrentHashMap<>(2);
  private static Logger logger = LoggerFactory.getLogger(CassandraConnectionManagerImpl.class);

  static {
    registerShutDownHook();
  }

  @Override
  public void createConnection(String[] hosts) throws BaseException {
    createCassandraConnection(hosts);
  }

  @Override
  public Session getSession(String keyspace) {
    Session session = cassandraSessionMap.get(keyspace);
    if (null != session) {
      return session;
    } else {
      Session session2 = cluster.connect(keyspace);
      cassandraSessionMap.put(keyspace, session2);
      return session2;
    }
  }

  private void createCassandraConnection(String[] hosts) throws BaseException {
    try {
      /*PropertiesCache cache = PropertiesCache.getInstance();*/
      //PropertiesCache cache = PropertiesCache.getInstance();
      PoolingOptions poolingOptions = new PoolingOptions();
      poolingOptions.setCoreConnectionsPerHost(
          HostDistance.LOCAL,
          Platform.getInteger(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL, 0));
      poolingOptions.setMaxConnectionsPerHost(
          HostDistance.LOCAL,
              Platform.getInteger(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL, 0));
      poolingOptions.setCoreConnectionsPerHost(
          HostDistance.REMOTE,
              Platform.getInteger(Constants.MAX_CONNECTIONS_PER_HOST_FOR_REMOTE, 0));
      poolingOptions.setMaxConnectionsPerHost(
          HostDistance.REMOTE,
              Platform.getInteger(Constants.MAX_CONNECTIONS_PER_HOST_FOR_REMOTE, 0));
      poolingOptions.setMaxRequestsPerConnection(
          HostDistance.LOCAL,
              Platform.getInteger(Constants.MAX_REQUEST_PER_CONNECTION, 0));
      poolingOptions.setHeartbeatIntervalSeconds(
              Platform.getInteger(Constants.HEARTBEAT_INTERVAL, 0));
      poolingOptions.setPoolTimeoutMillis(
              Platform.getInteger(Constants.POOL_TIMEOUT, 0));

      //check for multi DC enabled or not from configuration file and send the value
      cluster = createCluster(hosts, poolingOptions,Platform.getBoolean(Constants.IS_MULTI_DC_ENABLED, false));

      final Metadata metadata = cluster.getMetadata();
      String msg = String.format("Connected to cluster: %s", metadata.getClusterName());
      logger.info(msg);

      for (final Host host : metadata.getAllHosts()) {
        msg =
            String.format(
                "Datacenter: %s; Host: %s; Rack: %s",
                host.getDatacenter(), host.getAddress(), host.getRack());
        logger.info(msg);
      }
    } catch (Exception e) {
      logger.info("Error occured while creating cassandra connection :", e);
      throw new BaseException(
          IResponseMessage.INTERNAL_ERROR, e.getMessage(), ResponseCode.SERVER_ERROR.getCode());
    }
  }

  private static Cluster createCluster(String[] hosts, PoolingOptions poolingOptions, boolean isMultiDCEnabled) {
    Cluster.Builder builder =
        Cluster.builder()
            .addContactPoints(hosts)
            .withProtocolVersion(ProtocolVersion.V3)
            .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
            .withTimestampGenerator(new AtomicMonotonicTimestampGenerator())
            .withPoolingOptions(poolingOptions);

    ConsistencyLevel consistencyLevel = getConsistencyLevel();
    logger.info(
        "CassandraConnectionManagerImpl:createCluster: Consistency level = " + consistencyLevel);

    if (consistencyLevel != null) {
      builder.withQueryOptions(new QueryOptions().setConsistencyLevel(consistencyLevel));
    }

    logger.info(
            "CassandraConnectionManagerImpl:createCluster: isMultiDCEnabled = " + isMultiDCEnabled);
    if (isMultiDCEnabled) {
      builder.withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().build());
    }

    return builder.build();
  }

  private static ConsistencyLevel getConsistencyLevel() {
    String consistency =
        PropertiesCache.getConfigValue(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL);

    logger.info("CassandraConnectionManagerImpl:getConsistencyLevel: level = " + consistency);

    if (StringUtils.isBlank(consistency)) return null;

    try {
      return ConsistencyLevel.valueOf(consistency.toUpperCase());
    } catch (IllegalArgumentException exception) {
      logger.error(
          "CassandraConnectionManagerImpl:getConsistencyLevel: Exception occurred with error message = "
              + exception.getMessage());
    }
    return null;
  }

  @Override
  public List<String> getTableList(String keyspacename) {
    Collection<TableMetadata> tables = cluster.getMetadata().getKeyspace(keyspacename).getTables();

    // to convert to list of the names
    return tables.stream().map(tm -> tm.getName()).collect(Collectors.toList());
  }

  /** Register the hook for resource clean up. this will be called when jvm shut down. */
  public static void registerShutDownHook() {
    Runtime runtime = Runtime.getRuntime();
    runtime.addShutdownHook(new ResourceCleanUp());
    logger.info("Cassandra ShutDownHook registered.");
  }

  /**
   * This class will be called by registerShutDownHook to register the call inside jvm , when jvm
   * terminate it will call the run method to clean up the resource.
   */
  static class ResourceCleanUp extends Thread {
    @Override
    public void run() {
      try {
        logger.info("started resource cleanup Cassandra.");
        for (Map.Entry<String, Session> entry : cassandraSessionMap.entrySet()) {
          cassandraSessionMap.get(entry.getKey()).close();
        }
        cluster.close();
        logger.info("completed resource cleanup Cassandra.");
      } catch (Exception ex) {
        logger.info("Error :", ex);
      }
    }
  }
}
