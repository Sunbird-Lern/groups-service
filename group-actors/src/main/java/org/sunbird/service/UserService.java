package org.sunbird.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.exception.BaseException;
import org.sunbird.common.response.Response;
import org.sunbird.common.util.JsonKey;

public interface UserService {

  public Response searchUserByIds(List<String> userIds, Map<String, Object> reqContext)
      throws BaseException;

  public Response getSystemSettings() throws BaseException;

  public Response getOrganisationDetails(String orgId) throws BaseException;

  default void getUpdatedRequestHeader(Map<String, String> header, Map<String, Object> reqContext) {
    if (null == header) {
      header = new HashMap<>();
    }
    header.put("Content-Type", "application/json");
    header.put("Authorization","Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiIyZThlNmU5MjA4YjI0MjJmOWFlM2EzNjdiODVmNWQzNiJ9.gvpNN7zEl28ZVaxXWgFmCL6n65UJfXZikUWOKSE8vJ8");
    header.put("x-authenticated-user-token","eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJsclI0MWpJNndlZmZoQldnaUpHSjJhNlowWDFHaE53a21IU3pzdzE0R0MwIn0.eyJqdGkiOiI0OGQxMGQ1Ny0wYzI1LTRhMDEtOWUzMS01Njk0N2Y3NDlhNzYiLCJleHAiOjE2Mjc3MTU1NzYsIm5iZiI6MCwiaWF0IjoxNjI3NjI5MTc2LCJpc3MiOiJodHRwczovL2Rldi5zdW5iaXJkZWQub3JnL2F1dGgvcmVhbG1zL3N1bmJpcmQiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZjo1YThhM2YyYi0zNDA5LTQyZTAtOTAwMS1mOTEzYmMwZmRlMzE6YTEwZDUyMTYtNmI5Ni00MDRjLThkMWMtY2MxZjcyMGQ5MTBhIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoicHJvamVjdC1zdW5iaXJkLWRldi1jbGllbnQiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiI5ZDU4YTRkNC01MzliLTQ5NGQtOGFlMi0xZTgxNDI5M2NjYjYiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHBzOi8vZGV2LnN1bmJpcmRlZC5vcmciXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6IiIsIm5hbWUiOiJzYXRpc2giLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzYXRpc2hfeDZjYyIsImdpdmVuX25hbWUiOiJzYXRpc2giLCJlbWFpbCI6ImFiKioqKipAeW9wbWFpbC5jb20ifQ.WlciqopGwtHiTxyrQIy8INz6063HRcFuHFLhKHcA4khEe-VZWcqat35dyPEYZJiMIzPz0c0yXesSnZlT_CiVrrkoRXrTCWQHe81JG6nRuYZttkjaXRpWqYmljG3013cPVKbE1uGco9zq4JBc2hzVR9n-ccSFfbn51AJlhzKvHHMk0x7ccaiPQMk3tMtLu0VLhriozVZvP7RqHYzixQP11JrCxS8NnTFOFSJxntPTWfR9ADlb4ePInyQ9qK75_BWjI2GGixqYDpPNLIDloOV9ObK04Kxu7Q0ZtEMZpRFcSrGHAyTVlE5sq9A62A8zGJV1YUo64lpCrrLr6yGDBCH5uw");
    setTraceIdInHeader(header, reqContext);
  }

  public static void setTraceIdInHeader(Map<String, String> header,  Map<String, Object> reqContext) {
    if (null != reqContext) {
      header.put(JsonKey.X_TRACE_ENABLED, (String) reqContext.get(JsonKey.X_TRACE_ENABLED));
      header.put(JsonKey.X_REQUEST_ID, (String) reqContext.get(JsonKey.X_REQUEST_ID));
    }
  }
}
