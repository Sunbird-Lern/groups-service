package org.sunbird.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.actors.GroupNotificationActor;
import org.sunbird.common.util.JsonKey;
import org.sunbird.common.util.Notification;
import org.sunbird.util.helper.PropertiesCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService {
    private LoggerUtil logger = new LoggerUtil(NotificationService.class);

    private static String notificationServiceUrl;
    private static String notificationServiceBaseUrl;
    private static String max_batch_limits;
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        notificationServiceBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.NOTIFICATION_SERVICE_BASE_URL);
        notificationServiceUrl = PropertiesCache.getInstance().getProperty(JsonKey.NOTIFICATION_SERVICE_API_URL);
        max_batch_limits = PropertiesCache.getInstance().getProperty(JsonKey.MAX_BATCH_LIMIT);
    }

    public void sendSyncNotification(Notification notification, Map<String,Object> reqContext){
         List<String> ids = notification.getIds();
         List<Notification> notifications = createNotificationsBatch(notification);
         for (Notification notificationReq: notifications) {
            Map<String, String> requestHeader = new HashMap<>();
            getUpdatedRequestHeader(requestHeader, reqContext);
            try {
                String notificationStrReq = objectMapper.writeValueAsString(notificationReq);
                logger.info(notificationStrReq);
                /*String response =
                        HttpClientUtil.post(
                                notificationServiceBaseUrl + notificationServiceUrl, notificationStrReq, requestHeader,reqContext);
*/
            } catch (JsonProcessingException ex) {
                // log the error
            }
        }
    }

    private List<Notification> createNotificationsBatch(Notification notification) {
        List<Notification> notifications = new ArrayList<>();
        int idSize = notification.getIds().size();
        int maxBatchLimit = Integer.parseInt(max_batch_limits);
        List<List<String>> idLists = new ArrayList<>();
        List<String> ids = notification.getIds();
        int index = 0;
        for (int i=0 ; i<ids.size(); i++){
            if(i % maxBatchLimit == 0){
                idLists.add(new ArrayList<>());
                index++;
            }
            idLists.get(index-1).add(ids.get(i));
        }
        for (List<String> idList : idLists) {
            notifications.add(createNotificationObj(notification, idList));
        }
        return notifications;
    }

    private Notification createNotificationObj(Notification notification, List<String> idList) {
            Notification newNotificationObj = new Notification();
            newNotificationObj.setIds(idList);
            newNotificationObj.setAction(notification.getAction());
            newNotificationObj.setPriority(notification.getPriority());
            newNotificationObj.setType(notification.getType());
            return newNotificationObj;
    }

    void getUpdatedRequestHeader(Map<String, String> header, Map<String, Object> reqContext) {
        if (null == header) {
            header = new HashMap<>();
        }
        header.put("Content-Type", "application/json");
        header.put("Authorization","Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiIyZThlNmU5MjA4YjI0MjJmOWFlM2EzNjdiODVmNWQzNiJ9.gvpNN7zEl28ZVaxXWgFmCL6n65UJfXZikUWOKSE8vJ8");
        header.put("x-authenticated-user-token","eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJsclI0MWpJNndlZmZoQldnaUpHSjJhNlowWDFHaE53a21IU3pzdzE0R0MwIn0.eyJqdGkiOiI2YTU5MWNhYS0yNmU3LTRiODgtOWYxMy1jNzFhYzQ5NDdkMTMiLCJleHAiOjE2Mjc1NDkwODYsIm5iZiI6MCwiaWF0IjoxNjI3NDYyNjg2LCJpc3MiOiJodHRwczovL2Rldi5zdW5iaXJkZWQub3JnL2F1dGgvcmVhbG1zL3N1bmJpcmQiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZjo1YThhM2YyYi0zNDA5LTQyZTAtOTAwMS1mOTEzYmMwZmRlMzE6YTEwZDUyMTYtNmI5Ni00MDRjLThkMWMtY2MxZjcyMGQ5MTBhIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoicHJvamVjdC1zdW5iaXJkLWRldi1jbGllbnQiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiI4MTg3ZWE2ZC05OTBlLTRkNTYtOGY2My1kMGRmMDA1ZTI2ODMiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHBzOi8vZGV2LnN1bmJpcmRlZC5vcmciXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6IiIsIm5hbWUiOiJzYXRpc2giLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzYXRpc2hfeDZjYyIsImdpdmVuX25hbWUiOiJzYXRpc2giLCJlbWFpbCI6ImFiKioqKipAeW9wbWFpbC5jb20ifQ.Yk9K-ODKEow0tohHkkbeHe_b2orBEGT1_dDJtB2t7EGqudKrBpU79xatHGDWrKmE64xYL00_rtY_BlQSzMWheLEXDO51-y8uUlEPzH-jrb3IQj7EcRK4r0LZqiyJDo-F8BGocwA-1_PRTrI5Endq5Vg4p-COklwHYGYf-ZqQWdWYeZpRQ_dxrST7IhVfPtb5iz4iFMFswXLmSC8pC9uP2OAnJwS1L_JT-co1T4JHRPTxI3-x-Wfd8segJGYXHSjbepVx-FqoJ-ogy0mcs6muWnhnYbVDXI97CAOPHxQ1EDaXWEDsomJbO9AjiSj991OMrTLPzMfye4JnqUeCjNgIDQ");
        setTraceIdInHeader(header, reqContext);
    }

    public static void setTraceIdInHeader(Map<String, String> header,  Map<String, Object> reqContext) {
        if (null != reqContext) {
            header.put(JsonKey.X_TRACE_ENABLED, (String) reqContext.get(JsonKey.X_TRACE_ENABLED));
            header.put(JsonKey.X_REQUEST_ID, (String) reqContext.get(JsonKey.X_REQUEST_ID));
        }
    }
}
