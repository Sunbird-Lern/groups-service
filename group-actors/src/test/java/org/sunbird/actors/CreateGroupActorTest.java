package org.sunbird.actors;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.datastax.driver.core.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.CassandraUtil;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.common.message.IResponseMessage;
import org.sunbird.common.message.Localizer;
import org.sunbird.models.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.response.Response;
import org.sunbird.common.util.JsonKey;
import org.sunbird.util.SystemConfigUtil;
import org.sunbird.util.helper.PropertiesCache;
import org.sunbird.common.exception.BaseException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperation.class,
  CassandraOperationImpl.class,
  ServiceFactory.class,
  Localizer.class,
  SystemConfigUtil.class,
  PropertiesCache.class
})
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class CreateGroupActorTest extends BaseActorTest {

  private final Props props = Props.create(CreateGroupActor.class);
  private static Request reqObj;
  public static PropertiesCache propertiesCache;

  @BeforeClass
  public static void setUp() throws Exception {
    reqObj = createGroupReq();
    PowerMockito.mockStatic(Localizer.class);
    when(Localizer.getInstance()).thenReturn(null);

    CassandraMocker.setUpEmbeddedCassandra();

    // mock CassandraOperation
    CassandraOperationImpl cassandraOperation = CassandraMocker.mockCassandraOperation();

    // when inserting record to cassandra insert record in to EmbeddedCassandra
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(),Mockito.any()))
        .thenReturn(CassandraMocker.getCreateGroupResponse(reqObj));

    when(cassandraOperation.batchInsert(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyList(),Mockito.any()))
        .thenReturn(CassandraMocker.addMembersToGroup(reqObj));

    List<Map<String, Object>> members =
        (List<Map<String, Object>>) reqObj.getRequest().get(JsonKey.MEMBERS);

    when(cassandraOperation.upsertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(),Mockito.any()))
        .thenReturn(
            CassandraMocker.updateUserGroup(members, (String) reqObj.getRequest().get(JsonKey.ID)));

    // check record is inserted by querying EmbeddedCassandra
    ResultSet resultSet = EmbeddedCassandra.session.execute(EmbeddedCassandra.selectStatement);
    Response response = CassandraUtil.createResponse(resultSet);
    List<Map<String, Object>> groupList =
        (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    Assert.assertEquals(reqObj.get(JsonKey.NAME), groupList.get(0).get(JsonKey.NAME));

    // check members are inserted successfully or not
    ResultSet groupMember =
        EmbeddedCassandra.session.execute(
            EmbeddedCassandra.selectGroupMemberStatement.bind(
                members.get(0).get(JsonKey.USER_ID),
                members.get(0).get(JsonKey.ROLE),
                reqObj.getRequest().get(JsonKey.ID)));
    Response memberRes = CassandraUtil.createResponse(groupMember);
    List<Map<String, Object>> memberList =
        (List<Map<String, Object>>) memberRes.getResult().get(JsonKey.RESPONSE);
    Assert.assertEquals(
        members.get(0).get(JsonKey.USER_ID), memberList.get(0).get(JsonKey.USER_ID));

    PowerMockito.mockStatic(SystemConfigUtil.class);

    PowerMockito.mockStatic(PropertiesCache.class);
    propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_GROUP_MEMBERS_LIMIT))
        .thenReturn("4");
    when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_ACTIVITY_LIMIT)).thenReturn("2");
    when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_GROUP_LIMIT)).thenReturn("4");
  }

  @Test
  public void testCreateGroup() throws Exception {
    mockCacheActor();
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    System.out.println(res.getResult());
    Assert.assertTrue(null != res && res.getResponseCode() == 200);
    Assert.assertNotNull(res.getResult().get(JsonKey.GROUP_ID));
  }

  @Test
  public void testCreateGroupForMaxMemberAndMaxActivity() throws Exception {
    mockCacheActor();
    PowerMockito.mockStatic(Localizer.class);
    when(Localizer.getInstance()).thenReturn(null);
    PowerMockito.mockStatic(SystemConfigUtil.class);

    PowerMockito.mockStatic(PropertiesCache.class);
    propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_GROUP_MEMBERS_LIMIT))
        .thenReturn("4");
    when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_ACTIVITY_LIMIT)).thenReturn("2");
    when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_GROUP_LIMIT)).thenReturn("4");
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = createGroupReq();
    List<Map<String, Object>> members =
        (List<Map<String, Object>>) reqObj.getRequest().get(JsonKey.MEMBERS);
    List<Map<String, Object>> activities =
        (List<Map<String, Object>>) reqObj.getRequest().get(JsonKey.ACTIVITIES);
    Map<String, Object> member = new HashMap<>();
    member.put(JsonKey.USER_ID, "userID2");
    members.add(member);
    member = new HashMap<>();
    member.put(JsonKey.USER_ID, "userID3");
    members.add(member);
    member = new HashMap<>();
    member.put(JsonKey.USER_ID, "userID4");
    members.add(member);
    member = new HashMap<>();
    member.put(JsonKey.USER_ID, "userID5");
    members.add(member);
    member = new HashMap<>();
    member.put(JsonKey.USER_ID, "userID6");
    members.add(member);
    reqObj.getRequest().put(JsonKey.MEMBERS, members);
    Map<String, Object> activity = new HashMap<>();
    activity.put(JsonKey.TYPE, "COURSE");
    activity.put(JsonKey.ID, "courseId1");
    activities.add(activity);
    activity = new HashMap<>();
    activity.put(JsonKey.TYPE, "COURSE");
    activity.put(JsonKey.ID, "courseId12");
    activities.add(activity);
    reqObj.getRequest().put(JsonKey.ACTIVITIES, activities);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    Assert.assertTrue(null != res && res.getResponseCode() == 200);
    System.out.println("response " + res.getResult());
    Assert.assertNotNull(res.getResult().get(JsonKey.GROUP_ID));
    Map error = (Map) res.getResult().get(JsonKey.ERROR);
    System.out.println("error max limit" + error);
    List memberErrorList = (List) error.get(JsonKey.MEMBERS);
    List activityErrorList = (List) error.get(JsonKey.ACTIVITIES);
    Assert.assertEquals(
        ((Map) memberErrorList.get(0)).get(JsonKey.ERROR_CODE),
        IResponseMessage.Key.GS_CRT05);
    Assert.assertEquals(
        ((Map) activityErrorList.get(0)).get(JsonKey.ERROR_CODE),
        IResponseMessage.Key.GS_CRT06);
  }

  @Test
  public void testCreateGroupForMaxGroup() throws Exception {
    mockCacheActor();
    PowerMockito.mockStatic(Localizer.class);
    when(Localizer.getInstance()).thenReturn(null);
    PowerMockito.mockStatic(SystemConfigUtil.class);

    PowerMockito.mockStatic(PropertiesCache.class);
    propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_GROUP_MEMBERS_LIMIT))
            .thenReturn("4");
    when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_ACTIVITY_LIMIT)).thenReturn("1");
    when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_GROUP_LIMIT)).thenReturn("0");
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = createGroupReq();
    try {
      subject.tell(reqObj, probe.getRef());
    }catch (BaseException ex){
      Assert.assertTrue(true);
    }


  }

  private static Request createGroupReq() {
    Request reqObj = new Request();
    reqObj.setHeaders(headerMap);
    Map<String, Object> context = new HashMap<>();
    context.put(JsonKey.USER_ID, "user1");
    reqObj.setContext(context);
    reqObj.setOperation(ActorOperations.CREATE_GROUP.getValue());
    reqObj.getRequest().put(JsonKey.NAME, "TestGroup Name");
    reqObj.getRequest().put(JsonKey.DESC, "TestGroup Description");
    List<Map<String, Object>> members = new ArrayList<>();
    Map<String, Object> member = new HashMap<>();
    member.put(JsonKey.USER_ID, "userID");
    member.put(JsonKey.STATUS, "active");
    member.put(JsonKey.ROLE, "member");
    members.add(member);
    List<Map<String, Object>> activities = new ArrayList<>();
    Map<String, Object> activity = new HashMap<>();
    activity.put(JsonKey.TYPE, "COURSE");
    activity.put(JsonKey.ID, "courseId");
    activities.add(activity);
    reqObj.getRequest().put(JsonKey.ACTIVITIES, activities);
    reqObj.getRequest().put(JsonKey.MEMBERS, members);
    reqObj.getRequest().put(JsonKey.ID, UUID.randomUUID().toString());
    return reqObj;
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EmbeddedCassandra.close();
  }
}
