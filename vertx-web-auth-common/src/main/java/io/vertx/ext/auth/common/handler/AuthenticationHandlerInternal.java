package io.vertx.ext.auth.common.handler;

import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.common.AuthenticationContext;
import io.vertx.ext.auth.common.AuthenticationHandler;

public interface AuthenticationHandlerInternal<C extends AuthenticationContext> extends AuthenticationHandler<C> {

  /**
   * Parses the credentials from the request into a JsonObject. The implementation should
   * be able to extract the required info for the auth provider in the format the provider
   * expects.
   *
   * @param context the authentication context
   * @return future user to be called once the information is available.
   */
  Future<User> authenticate(C context);

  /**
   * Applies a {@code WWW-Authenticate} Response Header.
   *
   * If a server receives a request for an access-protected object, and an
   * acceptable Authorization header is not sent, the server responds with
   * a "401 Unauthorized" status code, and a WWW-Authenticate header.
   *
   * @param context the authentication context
   * @return the {@code true} if a header was added.
   */
  default boolean setAuthenticateHeader(C context) {
    return false;
  }

  /**
   * This method is called to perform any post authentication tasks, such as redirects or assertions.
   * Overrides must call {@link RoutingContext#next()} on success. Implementations must call this handler
   * at the end of the authentication process.
   *
   * @param ctx the authentication context
   * @param authenticated the authenticated user
   */
  default void postAuthentication(C ctx, User authenticated) {
    ctx.onContinue();
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
