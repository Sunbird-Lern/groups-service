package org.sunbird.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.dao.GroupDao;
import org.sunbird.dao.GroupDaoImpl;
import org.sunbird.dao.MemberDao;
import org.sunbird.dao.MemberDaoImpl;
import org.sunbird.exception.BaseException;
import org.sunbird.exception.ValidationException;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.ResponseCode;
import org.sunbird.models.Group;
import org.sunbird.models.GroupResponse;
import org.sunbird.models.Member;
import org.sunbird.models.MemberResponse;
import org.sunbird.response.Response;
import org.sunbird.util.ActivityConfigReader;
import org.sunbird.util.GroupUtil;
import org.sunbird.util.JsonKey;
import org.sunbird.util.JsonUtils;
import org.sunbird.util.SearchServiceUtil;

public class GroupServiceImpl implements GroupService {
  private static Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

  private static GroupDao groupDao = GroupDaoImpl.getInstance();
  private static MemberService memberService = new MemberServiceImpl();
  private static ObjectMapper objectMapper = new ObjectMapper();
  private static MemberDao memberDao = MemberDaoImpl.getInstance();

  @Override
  public String createGroup(Group groupObj) throws BaseException {
    String groupId = groupDao.createGroup(groupObj);
    return groupId;
  }

  public GroupResponse readGroupWithActivities(String groupId) throws Exception {
    Map<String, Object> dbResGroup = readGroup(groupId);
    logger.info("readGroupActivities started");
    readGroupActivities(dbResGroup);
    logger.info("readGroupActivities ended");
    return JsonUtils.convert(dbResGroup, GroupResponse.class);
  }

  public Map<String, Object> readGroup(String groupId) throws BaseException {
    Map<String, Object> dbResGroup;
    Response responseObj = groupDao.readGroup(groupId);
    if (null != responseObj && null != responseObj.getResult()) {
      List<Map<String, Object>> dbGroupDetails =
          (List<Map<String, Object>>) responseObj.getResult().get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(dbGroupDetails)
          && (JsonKey.ACTIVE.equals(dbGroupDetails.get(0).get(JsonKey.STATUS))
              || JsonKey.SUSPENDED.equals(dbGroupDetails.get(0).get(JsonKey.STATUS)))) {
        logger.info("Group details fetched for groupId :{}", groupId);
        dbResGroup = dbGroupDetails.get(0);
        // update createdOn, updatedOn format to utc "yyyy-MM-dd HH:mm:ss:SSSZ
        dbResGroup.put(
            JsonKey.CREATED_ON,
            dbResGroup.get(JsonKey.CREATED_ON) != null
                ? GroupUtil.convertDateToUTC((Date) dbResGroup.get(JsonKey.CREATED_ON))
                : dbResGroup.get(JsonKey.CREATED_ON));

        dbResGroup.put(
            JsonKey.UPDATED_ON,
            dbResGroup.get(JsonKey.UPDATED_ON) != null
                ? GroupUtil.convertDateToUTC((Date) dbResGroup.get(JsonKey.UPDATED_ON))
                : dbResGroup.get(JsonKey.UPDATED_ON));

        return dbResGroup;

      } else {
        throw new ValidationException.GroupNotFound(groupId);
      }
    } else {
      throw new ValidationException.GroupNotFound(groupId);
    }
  }

  @Override
  public void readGroupActivities(Map<String, Object> dbResGroup) {
    List<Map<String, Object>> dbResActivities =
        (List<Map<String, Object>>) dbResGroup.get(JsonKey.ACTIVITIES);
    if (dbResActivities != null && !dbResActivities.isEmpty()) {
      addActivityInfoDetails(dbResActivities);
    }
  }

  /**
   * Merge Activity Info to activities detail
   *
   * @param dbResActivities
   */
  private void addActivityInfoDetails(List<Map<String, Object>> dbResActivities) {
    logger.info("Fetching activityInfo for activity count: {}", dbResActivities.size());
    Map<SearchServiceUtil, Map<String, String>> idClassTypeMap =
        GroupUtil.groupActivityIdsBySearchUtilClass(dbResActivities);

    for (Map.Entry<SearchServiceUtil, Map<String, String>> itr : idClassTypeMap.entrySet()) {
      try {
        SearchServiceUtil searchServiceUtil = itr.getKey();
        List<String> fields = ActivityConfigReader.getFieldsLists(searchServiceUtil);
        Map<String, Map<String, Object>> activityInfoMap =
            searchServiceUtil.searchContent(itr.getValue(), fields);
        for (Map<String, Object> activity : dbResActivities) {
          String activityKey = (String) activity.get(JsonKey.TYPE) + activity.get(JsonKey.ID);
          if (activityInfoMap.containsKey(activityKey)) {
            activity.put(JsonKey.ACTIVITY_INFO, activityInfoMap.get(activityKey));
          }
        }
      } catch (JsonProcessingException e) {
        logger.error("No Service Class Configured");
      }
    }
  }

  /**
   * TODO: Extraction of specific fields needs to be added This method will search group details
   * based on filters and return all group details as success response or throw BaseException.
   *
   * @param searchFilter .
   * @return dbGroupDetails.
   */
  @Override
  public List<GroupResponse> searchGroup(Map<String, Object> searchFilter) throws BaseException {
    List<GroupResponse> groups = new ArrayList<>();
    String userId = (String) searchFilter.get(JsonKey.USER_ID);
    if (StringUtils.isNotBlank(userId)) {
      List<String> groupIds = fetchAllGroupIdsByUserId(userId);
      if (!groupIds.isEmpty()) {
        Map<String, String> groupRoleMap = memberService.fetchGroupRoleByUser(groupIds, userId);
        groups = readGroupDetailsByGroupIds(groupIds);
        GroupUtil.updateRoles(groups, groupRoleMap);
      }

    } else {
      logger.error("Bad Request UserId is Mandatory");
      throw new BaseException(
          IResponseMessage.INVALID_REQUESTED_DATA,
          IResponseMessage.MISSING_MANDATORY_PARAMS,
          ResponseCode.BAD_REQUEST.getCode());
    }
    return groups;
  }

  /**
   * Get all groupsIds By userIds
   *
   * @param userId
   * @return groupIdsList
   * @throws BaseException
   */
  private List<String> fetchAllGroupIdsByUserId(String userId) throws BaseException {
    Response groupIdsResponse = memberDao.readGroupIdsByUserId(userId);
    if (null != groupIdsResponse && null != groupIdsResponse.getResult()) {
      List<Map<String, Object>> dbResGroupIds =
          (List<Map<String, Object>>) groupIdsResponse.getResult().get(JsonKey.RESPONSE);
      if (null != dbResGroupIds && !dbResGroupIds.isEmpty()) {
        Set<String> groupIdsSet = (Set<String>) dbResGroupIds.get(0).get(JsonKey.GROUP_ID);
        logger.info("UserId {} has groupIds {}", userId, groupIdsSet.size());
        return new ArrayList<>(groupIdsSet);
      }
    }
    return new ArrayList<>();
  }

  /**
   * Filter Only Active Groups
   *
   * @param dbResGroupIds
   * @return
   */
  private List<Map<String, Object>> filterOutInActiveGroups(
      List<Map<String, Object>> dbResGroupIds) {
    List<Map<String, Object>> activeGroupLists = new ArrayList<>();
    dbResGroupIds.forEach(
        map -> {
          if (JsonKey.ACTIVE.equals(map.get(JsonKey.STATUS))) {
            activeGroupLists.add(map);
          }
        });
    return activeGroupLists;
  }

  /**
   * Read groups details based on group Ids
   *
   * @param groupIds
   * @return
   * @throws BaseException
   */
  private List<GroupResponse> readGroupDetailsByGroupIds(List<String> groupIds)
      throws BaseException {
    List<GroupResponse> groups = new ArrayList<>();
    Response response = groupDao.readGroups(groupIds);
    if (null != response && null != response.getResult()) {
      List<Map<String, Object>> dbGroupDetails =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      if (null != dbGroupDetails) {
        logger.info("Group details fetched - count : {} ", dbGroupDetails.size());
        dbGroupDetails.forEach(
            map -> {
              Group group = objectMapper.convertValue(map, Group.class);

              if (JsonKey.ACTIVE.equals(group.getStatus())
                  || JsonKey.SUSPENDED.equals(group.getStatus())) {
                GroupResponse groupResponse = createGroupResponseObj(group);
                groups.add(groupResponse);
              }
            });
      }
    }
    return groups;
  }

  @Override
  public Response updateGroup(Group groupObj) throws BaseException {
    return groupDao.updateGroup(groupObj);
  }

  @Override
  public Response deleteGroup(Group groupObj, List<MemberResponse> members) throws BaseException {
    Response responseObj = groupDao.deleteGroup(groupObj.getId());
    // Remove member mapping to the deleted group
    if (null != responseObj) {
      // Create member list
      List<String> memberIds = new ArrayList<>();
      List<Member> memberList = createDeleteMemberList(members, memberIds);
      List<Map<String, Object>> dbResGroupIds = memberService.getGroupIdsforUserIds(memberIds);
      memberService.removeGroupInUserGroup(memberList, dbResGroupIds);
      return responseObj;
    }

    logger.error("Error while deleting group {}", groupObj.getId());
    throw new BaseException(IResponseMessage.SERVER_ERROR, IResponseMessage.INTERNAL_ERROR);
  }

  private List<Member> createDeleteMemberList(
      List<MemberResponse> members, List<String> memberIds) {
    List<Member> memberList = new ArrayList<>();
    for (MemberResponse member : members) {
      Member memberObj = new Member();
      memberObj.setUserId(member.getUserId());
      memberObj.setGroupId(member.getGroupId());
      memberList.add(memberObj);
      memberIds.add(member.getUserId());
    }
    return memberList;
  }

  private GroupResponse createGroupResponseObj(Group group) {
    GroupResponse groupResponse = new GroupResponse();
    groupResponse.setId(group.getId());
    groupResponse.setDescription(group.getDescription());
    groupResponse.setName(group.getName());
    groupResponse.setStatus(group.getStatus());
    groupResponse.setMembershipType(group.getMembershipType());
    groupResponse.setActivities(group.getActivities());
    groupResponse.setCreatedBy(group.getCreatedBy());
    groupResponse.setUpdatedBy(group.getUpdatedBy());
    groupResponse.setCreatedOn(
        group.getCreatedOn() != null
            ? GroupUtil.convertTimestampToUTC(group.getCreatedOn().getTime())
            : null);
    groupResponse.setUpdatedOn(
        group.getUpdatedOn() != null
            ? GroupUtil.convertTimestampToUTC(group.getUpdatedOn().getTime())
            : null);
    return groupResponse;
  }

  // TODO should be private function : break into 2 functions add and remove
  @Override
  public List<Map<String, Object>> handleActivityOperations(
      String groupId, Map<String, Object> activityOperationMap) throws BaseException {
    List<Map<String, Object>> activityAddList =
        (List<Map<String, Object>>) activityOperationMap.get(JsonKey.ADD);
    List<String> activityRemoveList = (List<String>) activityOperationMap.get(JsonKey.REMOVE);

    // Fetching the activities from DB
    List<Map<String, Object>> dbActivityList = readActivityFromDb(groupId);

    if (CollectionUtils.isNotEmpty(activityAddList)) {
      // Check if activities in add request is already existing, if not append
      final List<Map<String, Object>> addDbActivityList = dbActivityList;
      if (CollectionUtils.isNotEmpty(addDbActivityList)) {
        List<Map<String, Object>> unavailable =
            activityAddList
                .stream()
                .filter(
                    e ->
                        (addDbActivityList
                                .stream()
                                .filter(d -> d.get("id").equals(e.get("id")))
                                .count())
                            < 1)
                .collect(Collectors.toList());
        dbActivityList.addAll(unavailable);
      } else {
        // If no activity in DB, assign the activities from request
        dbActivityList = new ArrayList<>(activityAddList);
      }
    }

    // Remove activities in request from finalist
    List<Map<String, Object>> finalList = dbActivityList;
    if (CollectionUtils.isNotEmpty(activityRemoveList)
        && CollectionUtils.isNotEmpty(dbActivityList)) {
      finalList =
          dbActivityList
              .stream()
              .filter(
                  e -> (activityRemoveList.stream().filter(d -> d.equals(e.get("id"))).count()) < 1)
              .collect(Collectors.toList());
    }
    return finalList;
  }

  private List<Map<String, Object>> readActivityFromDb(String groupId) throws BaseException {
    List<Map<String, Object>> dbActivityList = null;
    Response responseObj = groupDao.readGroup(groupId);
    if (null != responseObj && MapUtils.isNotEmpty(responseObj.getResult())) {
      List<Map<String, Object>> groupDetails =
          (List<Map<String, Object>>) responseObj.getResult().get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(groupDetails)) {
        Map<String, Object> dbResGroup = groupDetails.get(0);
        if (StringUtils.isNotBlank((String) dbResGroup.get(JsonKey.ID))
            && StringUtils.equals(groupId, (String) dbResGroup.get(JsonKey.ID)))
          dbActivityList = (List<Map<String, Object>>) dbResGroup.get(JsonKey.ACTIVITIES);
      } else {
        throw new ValidationException.GroupNotFound(groupId);
      }
    }
    return dbActivityList;
  }
}
