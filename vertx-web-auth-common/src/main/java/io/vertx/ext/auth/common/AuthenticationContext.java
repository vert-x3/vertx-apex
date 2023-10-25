package io.vertx.ext.auth.common;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * Context that is being accepted by various authentication handlers.
 * <p>
 * The context allows access to the HTTP request and response to verify provided authentication information 
 * <p>
 * The {@link UserContext} provides access to the authenticated user.
 * 
 */
public interface AuthenticationContext {

  /**
   * @return the HTTP request object
   */
  HttpServerRequest request();

  /**
   * @return the HTTP response object
   */
  HttpServerResponse response();

  /**
   * Control the user associated with this request. The user context allows accessing the security user object as well as perform authentication refreshes,
   * logout and other operations.
   * 
   * @return the user context
   */
  UserContext user();

  default void onContinue() {
    // NOOP
  }

  Session session();

}
