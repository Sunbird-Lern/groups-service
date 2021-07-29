package org.sunbird.notifications;

import org.apache.commons.collections4.MapUtils;
import org.sunbird.common.request.Request;
import org.sunbird.common.util.JsonKey;
import org.sunbird.common.util.Notification;
import org.sunbird.common.util.NotificationType;
import org.sunbird.models.MemberResponse;
import org.sunbird.util.LoggerUtil;

import java.util.*;

public class GroupDeleteNotificationHandler implements INotificationHandler{

    private LoggerUtil logger = new LoggerUtil(GroupDeleteNotificationHandler.class);

    @Override
    public List<Notification> getNotificationObj(Request request, Map<String,Object> updatedBy) {
           //Get Group Details
        Map<String,Object> groupDetails = (Map<String, Object>) request.getRequest().get(JsonKey.GROUP);
        if(MapUtils.isNotEmpty(groupDetails)) {
            Map<String,Object> actionData = new HashMap<>();
            actionData.put(JsonKey.TYPE, NotificationType.GROUP_DELETE);
            actionData.put(JsonKey.CATEGORY,JsonKey.GROUP);

            Map<String,Object> templates = getTemplateObj(groupDetails, updatedBy);
            List<MemberResponse> membersInDB = (List<MemberResponse>) request.getRequest().get(JsonKey.MEMBERS);
            MemberResponse memberDetail = membersInDB.stream().filter(x -> x.getUserId().equals(updatedBy.get(JsonKey.ID))).findAny().orElse(null);
            Map<String, Object> additionalInfo = getAdditionalInfo(updatedBy, groupDetails, memberDetail);
            actionData.put(JsonKey.TEMPLATE,templates);
            actionData.put(JsonKey.CREATED_BY,updatedBy);
            actionData.put(JsonKey.ADDITIONAL_INFO,additionalInfo);
            Notification notification = new Notification();
            List<String> userIds = new ArrayList<>();
            for (MemberResponse member: membersInDB) {
                //No need to send delete notifications to the user who deleted the group
                if(!member.getUserId().equals((String)updatedBy.get(JsonKey.ID))){
                    userIds.add(member.getUserId());
                }
            }

            notification.setIds(userIds);
            notification.setPriority(1);
            notification.setType(JsonKey.FEED);
            notification.setAction(actionData);
            return Arrays.asList(notification);
        }
        return null;
    }

    private Map<String, Object> getAdditionalInfo(Map<String, Object> updatedBy, Map<String, Object> groupDetails, MemberResponse memberDetail) {
        Map<String, Object> additionalInfo = new HashMap<>();

        if (null != memberDetail) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put(JsonKey.ID, updatedBy.get(JsonKey.ID));
            userInfo.put(JsonKey.NAME, updatedBy.get(JsonKey.NAME));
            userInfo.put(JsonKey.ROLE, memberDetail.getRole());
            additionalInfo.put(JsonKey.USER, userInfo);
        }
        Map<String,Object> group= new HashMap<>();
        group.put(JsonKey.ID, groupDetails.get(JsonKey.ID));
        group.put(JsonKey.NAME, groupDetails.get(JsonKey.NAME));
        additionalInfo.put(JsonKey.GROUP, group);
        return additionalInfo;
    }

    private Map<String,Object> getTemplateObj(Map<String, Object> groupDetails, Map<String,Object> updatedBy) {
        Map<String,Object> template = new HashMap<>();
        template.put(JsonKey.TYPE, "JSON");
        Map<String,Object> props = new HashMap<>();
        props.put(JsonKey.PROP1, groupDetails.get(JsonKey.NAME));
        props.put(JsonKey.PROP2, updatedBy.get(JsonKey.NAME));
        template.put(JsonKey.PROPS,props);
        return template;
    }
}
