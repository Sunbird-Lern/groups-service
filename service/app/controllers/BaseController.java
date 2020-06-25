package controllers;

import static controllers.ResponseHandler.handleResponse;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.ActorServiceException;
import org.sunbird.Application;
import org.sunbird.BaseException;
import org.sunbird.request.Request;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;
import validators.IRequestValidator;

/**
 * This controller we can use for writing some common method to handel api request.
 * CompletableFuture: A Future that may be explicitly completed (setting its value and status), and
 * may be used as a CompletionStage, supporting dependent functions and actions that trigger upon
 * its completion. CompletionStage: A stage of a possibly asynchronous computation, that performs an
 * action or computes a value when another CompletionStage completes
 */
public class BaseController extends Controller {
  private static final int WAIT_TIME_VALUE = 30;
  protected ObjectMapper mapper = new ObjectMapper();

  public int getTimeout(Request request) {
    int timeout = WAIT_TIME_VALUE;
    if (request != null && request.getTimeout() != null) {
      timeout = request.getTimeout();
    }
    return timeout;
  }

  protected ActorRef getActorRef(String operation) {
    return Application.getInstance().getActorRef(operation);
  }

  protected void validate(Request request, IRequestValidator validator) throws BaseException {
    // All controllers can validate this.
  }

  /**
   * this method will take org.sunbird.Request and a validation function and lastly operation(Actor
   * operation) this method is validating the request and , this method is used to handle all the
   * request type which has requestBody
   *
   * @param request
   * @return
   */
  public CompletionStage<Result> handleRequest(
      Request request, IRequestValidator iRequestValidator) {
    try {
      validate(request, iRequestValidator);
      return invoke(request);
    } catch (Exception ex) {
      return CompletableFuture.supplyAsync(() -> StringUtils.EMPTY)
          .thenApply(result -> ResponseHandler.handleFailureResponse(ex, request));
    }
  }

  /**
   * Responsible to handle the request and ask from actor
   *
   * @param request
   * @return CompletionStage<Result>
   * @throws BaseException
   */
  public CompletionStage<Result> invoke(Request request) throws BaseException {
    if (request == null) {
      handleResponse(new ActorServiceException.InvalidRequestData(), request);
    }

    Function<Object, Result> fn =
        new Function<Object, Result>() {
          @Override
          public Result apply(Object object) {
            return handleResponse(object, request);
          }
        };
    Timeout timeout = new Timeout(getTimeout(request), TimeUnit.SECONDS);

    ActorRef actorRef = getActorRef(request.getOperation());
    if (actorRef != null) {
      Future<Object> future = Patterns.ask(actorRef, request, timeout);
      return FutureConverters.toJava(future).thenApplyAsync(fn);
    } else {
      return CompletableFuture.supplyAsync(
          () -> handleResponse(new ActorServiceException.InvalidOperationName(null), request));
    }
  }

  public Request createSBRequest(play.mvc.Http.Request httpReq) {
    // Copy body
    JsonNode requestData = httpReq.body().asJson();
    if (requestData == null || requestData.isMissingNode()) {
      requestData = JsonNodeFactory.instance.objectNode();
    }

    // Copy headers
    ObjectNode headerData = Json.mapper().valueToTree(httpReq.getHeaders().toMap());
    ((ObjectNode) requestData).set("headers", headerData);

    Request request = Json.fromJson(requestData, Request.class);
    request.setPath(httpReq.path());

    return request;
  }
}
