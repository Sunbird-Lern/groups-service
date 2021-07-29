package org.sunbird.notifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections4.CollectionUtils;
import org.sunbird.common.request.Request;
import org.sunbird.common.util.JsonKey;
import org.sunbird.common.util.Notification;
import org.sunbird.models.MemberResponse;
import org.sunbird.service.MemberService;
import org.sunbird.service.MemberServiceImpl;
import org.sunbird.util.ActivityConfigReader;
import org.sunbird.util.GroupUtil;
import org.sunbird.util.LoggerUtil;
import org.sunbird.util.SearchServiceUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ActivityUpdateNotificationHandler implements INotificationHandler{
    private LoggerUtil logger = new LoggerUtil(ActivityUpdateNotificationHandler.class);

    @Override
    public List<Notification> getNotificationObj(Request request, Map<String,Object> updatedBy) {
        List<Notification> notifications = new ArrayList<>();
        Map<String,Object> reqObj = (Map<String,Object>)request.getRequest().get(JsonKey.REQUEST);
        Map<String,Object> groupDetails = (Map<String, Object>) request.getRequest().get(JsonKey.GROUP);
        if(null != groupDetails){
            Map<String, Object> activityOperationMap = (Map<String, Object>) reqObj.get(JsonKey.ACTIVITIES);
            List<MemberResponse> membersInDB = (List<MemberResponse>) request.getRequest().get(JsonKey.MEMBERS);
            if (CollectionUtils.isNotEmpty(
                    (List<Map<String, Object>>) activityOperationMap.get(JsonKey.ADD))) {
                notifications.addAll(handleActivityAddNotifications(activityOperationMap,groupDetails,updatedBy,membersInDB, request.getContext()));
            }
            if (CollectionUtils.isNotEmpty(
                    (List<Map<String, Object>>) activityOperationMap.get(JsonKey.REMOVE))) {
                notifications.addAll(handleActivityRemoveNotifications(activityOperationMap,groupDetails,updatedBy,membersInDB, request.getContext()));
            }
            return notifications;
        }
        return null;
    }

    /**
     *  Handle Activity Remove Notifications
     * @param activityOperationMap
     * @param groupDetails
     * @param updatedBy
     * @param membersInDB
     * @param reqContext
     * @return
     */
    private List<Notification> handleActivityRemoveNotifications(Map<String, Object> activityOperationMap, Map<String, Object> groupDetails, Map<String, Object> updatedBy, List<MemberResponse> membersInDB, Map<String, Object> reqContext) {
        List<Notification> notifications = new ArrayList<>();
        List<String> activityList = (List<String>) activityOperationMap.get(JsonKey.REMOVE);
        List<Map<String,Object>> activities = new ArrayList<>();
        for(String activityId : activityList){
          Map<String,Object> activity =   ((List<Map<String,Object>>)groupDetails.
                    get(JsonKey.ACTIVITIES)).stream().
                    filter(x->activityId.equals(x.get(JsonKey.ID))).findAny().orElse(null);
          if(null != activity){
              activities.add(activity);
          }
        }
        Map<SearchServiceUtil, Map<String, String>> idClassTypeMap =
                GroupUtil.groupActivityIdsBySearchUtilClass(activities,reqContext);
        getActivityDetails(reqContext, activities);
        //Each activity add will be sent as different notifications

        for (Map<String,Object> activity: activities) {
            List<MemberResponse> adminMembers = membersInDB.stream().
                    filter(x -> x.getRole().equals(JsonKey.ADMIN)).
                    collect(Collectors.toList());
            //Create separate notification call for Admins and Members
            notifications.add(getNotificationObj(JsonKey.ADMIN,"activity-remove",groupDetails, updatedBy, activity,membersInDB.stream().
                    filter(x -> x.getRole().equals(JsonKey.ADMIN)).
                    collect(Collectors.toList())) );
            notifications.add(getNotificationObj(JsonKey.MEMBER,"activity-remove", groupDetails, updatedBy, activity,membersInDB.stream().
                    filter(x -> x.getRole().equals(JsonKey.MEMBER)).
                    collect(Collectors.toList())));


        }
        return notifications;
    }

    /**
     *  Handle Activity Add Notifications
     * @param activityOperationMap
     * @param groupDetails
     * @param updatedBy
     * @param membersInDB
     * @param reqContext
     * @return
     */
    private List<Notification> handleActivityAddNotifications(Map<String, Object> activityOperationMap, Map<String, Object> groupDetails,
                                                                 Map<String, Object> updatedBy, List<MemberResponse> membersInDB,
                                                                   Map<String,Object> reqContext) {
        List<Notification> notifications = new ArrayList<>();
        List<Map<String,Object>> activityList = (List<Map<String, Object>>) activityOperationMap.get(JsonKey.ADD);
        getActivityDetails(reqContext, activityList);
        //Each activity add will be sent as different notifications

        for (Map<String,Object> activity: activityList) {
               List<MemberResponse> adminMembers = membersInDB.stream().
                                           filter(x -> x.getRole().equals(JsonKey.ADMIN)).
                                           collect(Collectors.toList());
                //Create separate notification call for Admins and Members
                notifications.add(getNotificationObj(JsonKey.ADMIN,"activity-add",groupDetails, updatedBy, activity,membersInDB.stream().
                        filter(x -> x.getRole().equals(JsonKey.ADMIN)).
                        collect(Collectors.toList())) );
                notifications.add(getNotificationObj(JsonKey.MEMBER,"activity-add", groupDetails, updatedBy, activity,membersInDB.stream().
                        filter(x -> x.getRole().equals(JsonKey.MEMBER)).
                        collect(Collectors.toList())));


        }
        return notifications;

    }

    /**
     * Get Activity Details from content Service
     * @param reqContext
     * @param activityList
     */
    private void getActivityDetails(Map<String, Object> reqContext, List<Map<String, Object>> activityList) {
        Map<SearchServiceUtil, Map<String, String>> idClassTypeMap =
                              GroupUtil.groupActivityIdsBySearchUtilClass(activityList, reqContext);
        for (Map.Entry<SearchServiceUtil, Map<String, String>> itr : idClassTypeMap.entrySet()) {
            try {
                SearchServiceUtil searchServiceUtil = itr.getKey();
                List<String> fields = ActivityConfigReader.getFieldsLists(searchServiceUtil);
                Map<String, Map<String, Object>> activityInfoMap =
                        searchServiceUtil.searchContent(itr.getValue(), fields, reqContext);
                for (Map<String, Object> activity : activityList) {
                    String activityKey = (String) activity.get(JsonKey.TYPE) + activity.get(JsonKey.ID);
                    if (activityInfoMap.containsKey(activityKey)) {
                        activity.put(JsonKey.NAME, activityInfoMap.get(activityKey).get(JsonKey.NAME));
                    }
                }
            } catch (JsonProcessingException e) {
               // logger.error(reqContext,"No Service Class Configured");
            }
        }
    }

    /**
     *  Create Notification Object
     * @param role
     * @param activityOp
     * @param groupDetails
     * @param updatedBy
     * @param activity
     * @param members
     * @return
     */
    private Notification getNotificationObj(String role, String activityOp, Map<String, Object> groupDetails, Map<String, Object> updatedBy,
                                           Map<String, Object> activity, List<MemberResponse> members) {
        Notification notification = new Notification();
        notification.setPriority(1);
        notification.setType(JsonKey.FEED);
        Map<String,Object> actionData = new HashMap<>();
        Map<String,Object> template = getAddActivityTemplateObj(groupDetails, updatedBy, activity);
        actionData.put(JsonKey.TEMPLATE,template);
        actionData.put(JsonKey.TYPE,activityOp);
        actionData.put(JsonKey.CREATED_BY, updatedBy);
        actionData.put(JsonKey.ADDITIONAL_INFO,getAdditionalInfo(groupDetails,role, activity));
        notification.setAction(actionData);
        List<String> userIds = new ArrayList<>();
        for (MemberResponse memberResponse : members){
            if(role.equals(memberResponse.getRole())){
                userIds.add(memberResponse.getUserId());
            }
        }
        notification.setIds(userIds);
        return notification;
    }

    private Map<String, Object> getAddActivityTemplateObj(Map<String, Object> groupDetails, Map<String, Object> updatedBy
                                                        ,Map<String,Object> activity) {
        Map<String,Object> template = new HashMap<>();
        template.put(JsonKey.TYPE, "JSON");
        Map<String,Object> props = new HashMap<>();
        props.put(JsonKey.PROP1, activity.get(JsonKey.DESC));
        props.put(JsonKey.PROP2, groupDetails.get(JsonKey.NAME));
        props.put(JsonKey.PROP3, updatedBy.get(JsonKey.NAME));
        template.put(JsonKey.PROPS,props);
        return template;
    }

    private Map<String, Object> getAdditionalInfo(Map<String, Object> groupDetails,
                                                  String role, Map<String,Object> activityInfo ) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(JsonKey.ROLE,role);
        Map<String,Object> group= new HashMap<>();
        group.put(JsonKey.ID, groupDetails.get(JsonKey.ID));
        group.put(JsonKey.NAME, groupDetails.get(JsonKey.NAME));
        additionalInfo.put(JsonKey.GROUP, group);
        return additionalInfo;
    }
}
