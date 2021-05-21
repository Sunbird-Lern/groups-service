package org.sunbird.exception;

import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;

import java.text.MessageFormat;
import java.util.Locale;

public class ValidationException {

  private static Localizer localizer = Localizer.getInstance();

  public static class InvalidRequestData extends BaseException {

    public InvalidRequestData() {
      super(
          IResponseMessage.INVALID_REQUESTED_DATA,
          Localizer.getInstance().getMessage(IResponseMessage.INVALID_REQUESTED_DATA, null),
          400);
    }
  }

  public static class MandatoryParamMissing extends BaseException {
    public MandatoryParamMissing(String param, String parentKey, ResponseCode responseCode) {
      super(
              responseCode.getErrorCode(),
          MessageFormat.format(
              ValidationException.getLocalizedMessage(
                  responseCode.getErrorMessage(), null),
              parentKey,
              param),
          400);
    }
    public MandatoryParamMissing(String param, String parentKey) {
      super(
              IResponseMessage.MISSING_MANDATORY_PARAMS,
              MessageFormat.format(
                      ValidationException.getLocalizedMessage(
                              IResponseMessage.MISSING_MANDATORY_PARAMS, null),
                      parentKey,
                      param),
              400);
    }
  }

  public static class ParamDataTypeError extends BaseException {
    public ParamDataTypeError(String param, String type) {
      super(
          IResponseMessage.INVALID_REQUESTED_DATA,
          MessageFormat.format(
              ValidationException.getLocalizedMessage(IResponseMessage.DATA_TYPE_ERROR, null),
              param,
              type),
          400);
    }
  }

  public static class InvalidParamValue extends BaseException {
    public InvalidParamValue(String paramValue, String paramName) {
      super(
          IResponseMessage.INVALID_PARAMETER_VALUE,
          MessageFormat.format(
              ValidationException.getLocalizedMessage(
                  IResponseMessage.INVALID_PARAMETER_VALUE, null),
              paramValue,
              paramName),
          400);
    }
  }

  public static class GroupNotFound extends BaseException {
    public GroupNotFound(String paramValue) {
      super(
          IResponseMessage.GROUP_NOT_FOUND,
          MessageFormat.format(
              ValidationException.getLocalizedMessage(IResponseMessage.GROUP_NOT_FOUND, null),
              paramValue),
          400);
    }
  }

  public static class GroupNotActive extends BaseException {
    public GroupNotActive(String paramValue) {
      super(
              ResponseCode.GS_UDT08.getErrorCode(),
              MessageFormat.format(
                      ValidationException.getLocalizedMessage(IResponseMessage.GROUP_NOT_ACTIVE, null),
                      paramValue),400);
    }
  }

  private static String getLocalizedMessage(String key, Locale locale) {
    return localizer.getMessage(key, locale);
  }
}
