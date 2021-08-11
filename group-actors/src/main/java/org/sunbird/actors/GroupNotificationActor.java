package org.sunbird.actors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.ActorConfig;
import org.sunbird.common.request.Request;
import org.sunbird.common.response.Response;
import org.sunbird.common.util.JsonKey;
import org.sunbird.common.util.Notification;
import org.sunbird.notifications.INotificationHandler;
import org.sunbird.notifications.NotificationFactory;
import org.sunbird.service.UserService;
import org.sunbird.service.UserServiceImpl;
import org.sunbird.util.GroupRequestHandler;
import org.sunbird.util.LoggerUtil;
import org.sunbird.util.NotificationService;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActorConfig(
        tasks = {"sendNotifications"},
        asyncTasks = {},
        dispatcher = "group-dispatcher"
)
public class GroupNotificationActor extends BaseActor{

    private LoggerUtil logger = new LoggerUtil(GroupNotificationActor.class);

    @Override
    public void onReceive(Request request) throws Throwable {

        logger.info(request.getContext(), MessageFormat.format("Notification Actor: sending notifications for ",request.getOperation()));
        String updatedBy = new GroupRequestHandler().getRequestedBy(request);
        Map<String, Object> user = new HashMap<>();
        if(null != updatedBy) {
            user = getUserDetails(request, updatedBy);
            user.put(JsonKey.TYPE,JsonKey.USER);
        }else{
            user.put(JsonKey.ID,JsonKey.GROUPS_SERVICE);
            user.put(JsonKey.TYPE,JsonKey.SYSTEM);
        }
        INotificationHandler notificationHandler = NotificationFactory.getNotificationHandler(request.getOperation());
        List<Notification> notifications = notificationHandler.getNotificationObj(request,user);
        if(CollectionUtils.isNotEmpty(notifications)){
            sendNotifications(notifications, request.getContext());
        }
        Response response = new Response();
        response.put(JsonKey.RESULT,"ok");
        sender().tell(response, self());
    }

    /**
     * Get User Details from user service
     * @param request
     * @param updatedBy
     * @return
     */

    private Map<String,Object> getUserDetails(Request request, String updatedBy) {
        Map<String,Object> user  = new HashMap<>();
        user.put(JsonKey.ID,updatedBy);
        UserService userService = UserServiceImpl.getInstance();
        Response response = userService.searchUserByIds(Arrays.asList(updatedBy), request.getContext());
        if (null != response && null != response.getResult()) {
            Map<String, Object> userRes =
                    (Map<String, Object>) response.getResult().get(JsonKey.RESPONSE);
            if (null != userRes) {
                List<Map<String, Object>> userDetails =
                        (List<Map<String, Object>>) userRes.get(JsonKey.CONTENT);
                Map<String, Object> userInfo =
                        userDetails
                                .stream()
                                .filter(x -> updatedBy.equals((String) x.get(JsonKey.ID)))
                                .findFirst()
                                .orElse(null);
                if (userInfo != null) {
                    String firstName =
                            StringUtils.isNotEmpty((String) userInfo.get(JsonKey.FIRSTNAME))
                                    ? (String) userInfo.get(JsonKey.FIRSTNAME)
                                    : "";

                    String lastName =
                            StringUtils.isNotEmpty((String) userInfo.get(JsonKey.LASTNAME))
                                    ? " " + (String) userInfo.get(JsonKey.LASTNAME)
                                    : "";
                    user.put(JsonKey.NAME,firstName + lastName);
                }
            }

        }
        return user;
    }

    private void sendNotifications(List<Notification> notifications, Map<String,Object> reqContext) {
        NotificationService notificationService = new NotificationService();
        notificationService.sendSyncNotification(notifications, reqContext);

    }
}
