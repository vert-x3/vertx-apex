package io.vertx.ext.web.handler.impl;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.common.handler.impl.HTTPAuthorizationHandler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

public abstract class WebHTTPAuthorizationHandler<T extends AuthenticationProvider> extends HTTPAuthorizationHandler<RoutingContext, T> {

  public WebHTTPAuthorizationHandler(T authProvider, Type type, String realm) {
    super(authProvider, type, realm);
  }

  // TODO remove duplicated code from WebAuthenticationHandlerImpl
  /**
   * This method is protected so custom auth handlers can override the default error handling
   */
  protected void processException(RoutingContext ctx, Throwable exception) {
    if (exception != null) {
      if (exception instanceof HttpException) {
        final int statusCode = ((HttpException) exception).getStatusCode();
        final String payload = ((HttpException) exception).getPayload();

        switch (statusCode) {
        case 302:
          ctx.response()
            .putHeader(HttpHeaders.LOCATION, payload)
            .setStatusCode(302)
            .end("Redirecting to " + payload + ".");
          return;
        case 401:
          if (!"XMLHttpRequest".equals(ctx.request().getHeader("X-Requested-With"))) {
            setAuthenticateHeader(ctx);
          }
          ctx.fail(401, exception);
          return;
        default:
          ctx.fail(statusCode, exception);
          return;
        }
      }
    }

    // fallback 500
    ctx.fail(exception);
  }


}
