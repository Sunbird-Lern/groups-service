package org.sunbird.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.dao.MemberDao;
import org.sunbird.dao.impl.MemberDaoImpl;
import org.sunbird.exception.BaseException;
import org.sunbird.models.Member;
import org.sunbird.models.MemberResponse;
import org.sunbird.response.Response;
import org.sunbird.service.MemberService;
import org.sunbird.service.UserService;
import org.sunbird.util.JsonKey;

public class MemberServiceImpl implements MemberService {

  private static MemberDao memberDao = MemberDaoImpl.getInstance();
  private static MemberService memberService = null;
  private static Logger logger = LoggerFactory.getLogger(MemberServiceImpl.class);
  private static ObjectMapper objectMapper = new ObjectMapper();
  private static UserService userService = UserServiceImpl.getInstance();

  public static MemberService getInstance() {
    if (memberService == null) {
      memberService = new MemberServiceImpl();
    }
    return memberService;
  }

  @Override
  public Response addMembers(List<Member> member) throws BaseException {
    member.forEach(
        m -> {
          m.setStatus(JsonKey.ACTIVE);
          m.setCreatedBy(""); // TODO - take from request
          m.setCreatedOn(new Timestamp(System.currentTimeMillis()));
        });
    Response response = memberDao.addMembers(member);
    return response;
  }

  @Override
  public Response editMembers(List<Member> member) throws BaseException {
    member.forEach(
        m -> {
          m.setUpdatedBy(""); // TODO - take from request
          m.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
        });
    Response response = memberDao.editMembers(member);
    return response;
  }

  @Override
  public Response removeMembers(List<Member> member) throws BaseException {
    member.forEach(
        m -> {
          m.setStatus(JsonKey.INACTIVE);
          m.setRemovedBy(""); // TODO - take from request
          m.setRemovedOn(new Timestamp(System.currentTimeMillis()));
        });
    Response response = memberDao.editMembers(member);
    return response;
  }

  public void handleMemberOperations(Map memberOperationMap, String groupId) throws BaseException {
    if (memberOperationMap != null && !memberOperationMap.isEmpty()) {
      List<Map<String, Object>> memberAddList =
          (List<Map<String, Object>>) memberOperationMap.get(JsonKey.MEMBER_ADD);
      if (CollectionUtils.isNotEmpty(memberAddList)) {
        Response addMemberRes = handleMemberAddition(memberAddList, groupId);
      }
      List<Map<String, Object>> memberEditList =
          (List<Map<String, Object>>) memberOperationMap.get(JsonKey.MEMBER_EDIT);
      if (CollectionUtils.isNotEmpty(memberEditList)) {
        List<Member> editMembers =
            memberEditList
                .stream()
                .map(data -> getMemberModel(data, groupId))
                .collect(Collectors.toList());
        if (!editMembers.isEmpty()) {
          Response editMemberRes = editMembers(editMembers);
        }
      }
      List<String> memberRemoveList = (List<String>) memberOperationMap.get(JsonKey.MEMBER_REMOVE);
      if (CollectionUtils.isNotEmpty(memberRemoveList)) {
        List<Member> removeMembers =
            memberRemoveList
                .stream()
                .map(data -> getMemberModelForRemove(data, groupId))
                .collect(Collectors.toList());
        if (!removeMembers.isEmpty()) {
          Response removeMemberRes = removeMembers(removeMembers);
        }
      }
    }
  }

  @Override
  public Response handleMemberAddition(List<Map<String, Object>> memberList, String groupId)
      throws BaseException {
    logger.info("Number of members to be added are: {}", memberList.size());
    Response addMemberRes = new Response();
    List<Member> members =
        memberList.stream().map(data -> getMemberModel(data, groupId)).collect(Collectors.toList());
    if (!members.isEmpty()) {
      addMemberRes = addMembers(members);
    }
    return addMemberRes;
  }

  // TODO: Fix me to get the Members Details with List<Member> includes username

  /**
   * Fetch Member Details based on Group
   *
   * @param groupIds
   * @param fields
   * @return
   * @throws BaseException
   */
  @Override
  public List<MemberResponse> fetchMembersByGroupIds(List<String> groupIds, List<String> fields)
      throws BaseException {
    Response response = memberDao.fetchMembersByGroupIds(groupIds, fields);
    List<MemberResponse> members = new ArrayList<>();

    if (null != response && null != response.getResult()) {
      List<Map<String, Object>> dbResMembers =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      if (null != dbResMembers) {
        dbResMembers.forEach(
            map -> {
              MemberResponse member = objectMapper.convertValue(map, MemberResponse.class);
              members.add(member);
            });
      }
    }
    if (!members.isEmpty()) {
      updateMemberDetails(members);
    }
    return members;
  }

  private void updateMemberDetails(List<MemberResponse> members) throws BaseException {
    List<String> memberIds = new ArrayList<>();
    members.forEach(
        member -> {
          memberIds.add(member.getUserId());
        });
    Response response = userService.searchUserByIds(memberIds);
    if (null != response && null != response.getResult()) {
      Map<String, Object> memberRes =
          (Map<String, Object>) response.getResult().get(JsonKey.RESPONSE);
      if (null != memberRes) {
        List<Map<String, Object>> userDetails =
            (List<Map<String, Object>>) memberRes.get(JsonKey.CONTENT);
        members.forEach(
            member -> {
              Map<String, Object> userInfo =
                  userDetails
                      .stream()
                      .filter(x -> member.getUserId().equals((String) x.get(JsonKey.ID)))
                      .findFirst()
                      .orElse(null);
              if (userInfo != null) {
                member.setUsername((String) userInfo.get(JsonKey.USERNAME));
              }
            });
      }
    }
  }

  @Override
  public Map<String, String> fetchGroupRoleByUser(List<String> groupIds, String userId)
      throws BaseException {
    Response response = memberDao.fetchGroupRoleByUser(groupIds, userId);
    Map<String, String> groupRoleMap = new HashMap<>();
    if (null != response && null != response.getResult()) {
      List<Map<String, Object>> dbResMembers =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      if (null != dbResMembers) {
        dbResMembers.forEach(
            map -> {
              groupRoleMap.put((String) map.get(JsonKey.GROUP_ID), (String) map.get(JsonKey.ROLE));
            });
      }
    }
    return groupRoleMap;
  }

  private Member getMemberModel(Map<String, Object> data, String groupId) {
    Member member = new Member();
    member.setGroupId(groupId);
    String role = (String) data.get(JsonKey.ROLE);
    if (StringUtils.isNotEmpty(role)) {
      member.setRole(role);
    } else {
      member.setRole(JsonKey.MEMBER);
    }
    member.setUserId((String) data.get(JsonKey.USER_ID));
    return member;
  }

  private Member getMemberModelForRemove(String userId, String groupId) {
    Member member = new Member();
    member.setUserId(userId);
    member.setGroupId(groupId);
    return member;
  }
}
