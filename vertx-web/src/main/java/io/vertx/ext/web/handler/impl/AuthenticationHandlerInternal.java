package io.vertx.ext.web.handler.impl;

import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;

public interface AuthenticationHandlerInternal extends AuthenticationHandler {

  /**
   * Parses the credentials from the request into a JsonObject. The implementation should
   * be able to extract the required info for the auth provider in the format the provider
   * expects.
   *
   * @param context the routing context
   * @return future user to be called once the information is available.
   */
  Future<User> authenticate(RoutingContext context);

  /**
   * Applies a {@code WWW-Authenticate} Response Header.
   *
   * If a server receives a request for an access-protected object, and an
   * acceptable Authorization header is not sent, the server responds with
   * a "401 Unauthorized" status code, and a WWW-Authenticate header.
   *
   * @param context the routing context
   * @return the {@code true} if a header was added.
   */
  default boolean setAuthenticateHeader(RoutingContext context) {
    return false;
  }

  /**
   * This method is called to perform any post authentication tasks, such as redirects or assertions.
   * Overrides must call {@link RoutingContext#next()} on success. Implementations must call this handler
   * at the end of the authentication process.
   *
   * @param ctx the routing context
   */
  default void postAuthentication(RoutingContext ctx) {
    ctx.next();
  }

  /**
   * Signal that this handler can perform an HTTP redirect during the authentication mechanism. In this case
   * this can be problematic in order to validate chains as it introduces a well known abort of the processing.
   * @return true if it is known that the authentication may perform a redirect.
   */
  default boolean performsRedirect() {
    return false;
  }
}
