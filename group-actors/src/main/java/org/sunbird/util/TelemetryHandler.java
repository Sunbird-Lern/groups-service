package org.sunbird.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.ocsp.Req;
import org.sunbird.common.request.Request;
import org.sunbird.common.util.JsonKey;
import org.sunbird.models.Group;
import org.sunbird.telemetry.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TelemetryHandler {

    public static void logGroupCreateTelemetry(Request actorMessage, String groupId){
        String source =
                actorMessage.getContext().get(org.sunbird.common.util.JsonKey.REQUEST_SOURCE) != null
                        ? (String) actorMessage.getContext().get(org.sunbird.common.util.JsonKey.REQUEST_SOURCE)
                        : "";

        List<Map<String, Object>> correlatedObject = new ArrayList<>();
        if (StringUtils.isNotBlank(source)) {
            TelemetryUtil.generateCorrelatedObject(
                    source, StringUtils.capitalize(org.sunbird.common.util.JsonKey.REQUEST_SOURCE), null, correlatedObject);
        }
        Map<String, Object> targetObject = null;
        targetObject = groupId != null ?
                TelemetryUtil.generateTargetObject(groupId, TelemetryEnvKey.GROUP_CREATED,null, org.sunbird.common.util.JsonKey.ACTIVE, null,TelemetryEnvKey.GROUPS_LIST)
                : TelemetryUtil.generateTargetObject(groupId, TelemetryEnvKey.GROUP_ERROR, TelemetryEnvKey.CREATION,null, null,TelemetryEnvKey.GROUPS_LIST);

        // Add user information to Cdata
        TelemetryUtil.generateCorrelatedObject(
                (String) actorMessage.getContext().get(JsonKey.USER_ID),
                TelemetryEnvKey.USER,
                null,
                correlatedObject);
        // Add group info information to Cdata
        TelemetryUtil.generateCorrelatedObject(
                groupId,
                TelemetryEnvKey.GROUPID,
                null,
                correlatedObject);
        TelemetryUtil.telemetryProcessingCall(
                actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    }

    public static void logGroupDeleteTelemetry(Request actorMessage, String groupId, Map<String, Object> dbResGroup, boolean isDeleted) {
        Map<String, Object> targetObject = null;
        List<Map<String, Object>> correlatedObject = new ArrayList<>();
        if(isDeleted) {
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            groupId,
                            TelemetryEnvKey.DELETE_GROUP,
                            null,
                            null,
                            (String) dbResGroup.get(JsonKey.STATUS), TelemetryEnvKey.GROUP_DETAIL);
        }else{
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            groupId,
                            TelemetryEnvKey.GROUP_ERROR,
                            JsonKey.DELETE,
                            null,
                            null != dbResGroup ? (String) dbResGroup.get(JsonKey.STATUS):null, TelemetryEnvKey.GROUP_DETAIL);
        }
        TelemetryUtil.generateCorrelatedObject(
                (String) actorMessage.getContext().get(JsonKey.USER_ID),
                TelemetryEnvKey.USER,
                null,
                correlatedObject);
        // Add group info information to Cdata
        TelemetryUtil.generateCorrelatedObject(
                groupId,
                TelemetryEnvKey.GROUPID,
                null,
                correlatedObject);
        TelemetryUtil.telemetryProcessingCall(
                actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    }

    public static void logGroupUpdateTelemetry(Request actorMessage, Group group, Map<String, Object> dbResGroup, boolean isSuccess) {
        Map<String, Object> targetObject = null;
        List<Map<String, Object>> correlatedObject = new ArrayList<>();
        if (isSuccess) {
            if (null != dbResGroup && null != dbResGroup.get(JsonKey.STATUS)) {
                switch ((String) dbResGroup.get(JsonKey.STATUS)) {
                    case JsonKey.ACTIVE:
                        targetObject = generateTargetForActiveGroup(actorMessage, group, dbResGroup);
                        break;
                    case JsonKey.SUSPENDED:
                        targetObject =
                                TelemetryUtil.generateTargetObject(
                                        group.getId(),
                                        TelemetryEnvKey.ACTIVATE_GROUP,
                                        null,
                                        group.getStatus(),
                                        (String) dbResGroup.get(JsonKey.STATUS), TelemetryEnvKey.GROUP_DETAIL);
                        break;

                    default:
                        targetObject =
                                TelemetryUtil.generateTargetObject(
                                        group.getId(), TelemetryEnvKey.GROUP, null, null, null, TelemetryEnvKey.GROUP_DETAIL);
                }

            }
        }else{
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            group.getId(),
                            TelemetryEnvKey.GROUP_ERROR,
                            JsonKey.UPDATE,
                            null,
                            null != dbResGroup ?(String) dbResGroup.get(JsonKey.STATUS): null, TelemetryEnvKey.GROUP_DETAIL);
        }
        TelemetryUtil.generateCorrelatedObject(
                (String) actorMessage.getContext().get(JsonKey.USER_ID),
                TelemetryEnvKey.USER,
                null,
                correlatedObject);
        // Add group info information to Cdata
        TelemetryUtil.generateCorrelatedObject(
                group.getId(),
                TelemetryEnvKey.GROUPID,
                null,
                correlatedObject);
        TelemetryUtil.telemetryProcessingCall(
                actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    }

    public static void logGroupMembershipUpdateTelemetry(Request actorMessage, String userId, boolean isSuccess) {
        String source =
                actorMessage.getContext().get(JsonKey.REQUEST_SOURCE) != null
                        ? (String) actorMessage.getContext().get(JsonKey.REQUEST_SOURCE)
                        : "";

        List<Map<String, Object>> correlatedObject = new ArrayList<>();
        if (StringUtils.isNotBlank(source)) {
            TelemetryUtil.generateCorrelatedObject(
                    source, StringUtils.capitalize(JsonKey.REQUEST_SOURCE), null, correlatedObject);
        }
        List<Map<String,Object>> groups =(List<Map<String, Object>>) actorMessage.getRequest().get(JsonKey.GROUPS);
        for (Map<String,Object> group:groups) {
            // Add group info information to Cdata
            TelemetryUtil.generateCorrelatedObject(
                    (String) group.get(JsonKey.GROUP_ID),
                    TelemetryEnvKey.GROUPID,
                    null,
                    correlatedObject);
        }
        Map<String, Object> targetObject = null;
        if(isSuccess) {
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            userId, TelemetryEnvKey.MEMBER_UPDATE, null, null, null, TelemetryEnvKey.GROUP_DETAIL);
        }else{
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            userId, TelemetryEnvKey.GROUP_ERROR, TelemetryEnvKey.MEMBER_UPDATE, null, null, TelemetryEnvKey.GROUP_DETAIL);
        }
        TelemetryUtil.generateCorrelatedObject(
                (String) actorMessage.getContext().get(JsonKey.USER_ID),
                TelemetryEnvKey.USER,
                null,
                correlatedObject);
        TelemetryUtil.telemetryProcessingCall(
                actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    }

    private static Map<String, Object> generateTargetForActiveGroup(Request actorMessage, Group group, Map<String, Object> dbResGroup) {
        Map<String, Object> targetObject;
        if (!CollectionUtils.isEmpty(group.getActivities())) {
            targetObject = TelemetryUtil.generateTargetObject(
                    group.getId(),
                    TelemetryEnvKey.ACTIVITY,
                    null,
                    JsonKey.ACTIVE,
                    (String) dbResGroup.get(JsonKey.STATUS), TelemetryEnvKey.GROUP_DETAIL);
        } else if (!MapUtils.isEmpty((Map<String, Object>) actorMessage.getRequest().get(JsonKey.MEMBERS))) {
            targetObject = TelemetryUtil.generateTargetObject(
                    group.getId(),
                    TelemetryEnvKey.ADD_MEMBER,
                    null,
                    JsonKey.ACTIVE,
                    (String) dbResGroup.get(JsonKey.STATUS), TelemetryEnvKey.GROUP_DETAIL);
        } else if(JsonKey.SUSPENDED.equals(group.getStatus())){
            targetObject = TelemetryUtil.generateTargetObject(
                    group.getId(),
                    TelemetryEnvKey.DEACTIVATE_GROUP,
                    null,
                    JsonKey.ACTIVE,
                    (String) dbResGroup.get(JsonKey.STATUS), TelemetryEnvKey.GROUP_DETAIL);

        } else {
            targetObject = TelemetryUtil.generateTargetObject(
                    group.getId(),
                    TelemetryEnvKey.UPDATE_GROUP,
                    null,
                    JsonKey.ACTIVE,
                    (String) dbResGroup.get(JsonKey.STATUS), TelemetryEnvKey.GROUP_DETAIL);
        }
        return targetObject;
    }
}
