package controllers;

import java.util.concurrent.CompletionStage;
import org.sunbird.exception.BaseException;
import org.sunbird.models.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import validators.GroupSearchRequestValidator;
import validators.IRequestValidator;

public class SearchGroupController extends BaseController {
  @Override
  protected boolean validate(Request request) throws BaseException {
    IRequestValidator requestValidator = new GroupSearchRequestValidator();
    return requestValidator.validate(request);
  }

  public CompletionStage<Result> searchGroup(Http.Request req) {
    Request request = createSBRequest(req, ActorOperations.SEARCH_GROUP.getValue());
    return handleRequest(request);
  }
}
