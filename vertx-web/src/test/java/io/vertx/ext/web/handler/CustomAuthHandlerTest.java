/*
 * Copyright 2018 Bosch Software Innovations GmbH.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.web.handler;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class CustomAuthHandlerTest extends AuthHandlerTestBase {

  @Override
  protected WebAuthenticationHandler createAuthHandler(AuthenticationProvider authProvider) {
    return newAuthHandler(authProvider, null);
  }

  private WebAuthenticationHandler newAuthHandler(AuthenticationProvider authProvider, Handler<Throwable> exceptionProcessor) {

    return SimpleAuthenticationHandler.create()
      .authenticate(ctx -> {
        final Promise<User> promise = Promise.promise();

        authProvider.authenticate(new UsernamePasswordCredentials("user", "pass")).onComplete(authn -> {
          if (authn.failed()) {
            if (exceptionProcessor != null) {
              exceptionProcessor.handle(authn.cause());
            }
            promise.fail(authn.cause());
          } else {
            promise.complete(authn.result());
          }
        });

        return promise.future();
      });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCredentialsValidationErrorPropagation() throws Exception {

    Handler<RoutingContext> handler = rc -> {
      fail("should not get here");
      rc.response().end("Welcome to the protected resource!");
    };

    Throwable rootCause = new IllegalArgumentException("validation of credentials failed");
    AuthenticationProvider authProvider = mock(AuthenticationProvider.class);
    doAnswer(invocation -> Future.failedFuture(rootCause)).when(authProvider).authenticate(any(Credentials.class));

    router.route("/protected/*").handler(newAuthHandler(authProvider, exception -> {
      assertTrue(exception instanceof IllegalArgumentException);
      assertEquals(rootCause, exception);
    }));

    router.route("/protected/somepage").handler(handler);

    testRequest(HttpMethod.GET, "/protected/somepage", 401, "Unauthorized");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testHttpStatusExceptionFailurePropagation() throws Exception {

    Handler<RoutingContext> handler = rc -> {
      fail("should not get here");
      rc.response().end("Welcome to the protected resource!");
    };

    Throwable rootCause = new HttpException(499, "bla");
    AuthenticationProvider authProvider = mock(AuthenticationProvider.class);
    doAnswer(invocation -> Future.failedFuture(rootCause)).when(authProvider).authenticate(any(Credentials.class));

    router.route("/protected/*").handler(newAuthHandler(authProvider, exception -> {
      assertTrue(exception instanceof HttpException);
      assertEquals(rootCause, exception);
    }));

    router.route("/protected/somepage").handler(handler);

    router.errorHandler(499, rc -> rc
      .response()
      .setStatusCode(((HttpException) rc.failure()).getStatusCode())
      .setStatusMessage(((HttpException) rc.failure()).getPayload())
      .end()
    );

    testRequest(HttpMethod.GET, "/protected/somepage", 499, "bla");
  }

  @Test
  public void testAnonymousAuthentication() throws Exception {

    router.route("/protected")
      .handler(SimpleAuthenticationHandler.create().authenticate(ctx -> Future.succeededFuture()))
      .handler(ctx -> {
        // validation
        assertNull(ctx.user().get());
        ctx.next();
      })
      .handler(ctx -> ctx.response().end("Welcome to the protected resource!"));

    testRequest(HttpMethod.GET, "/protected", 200, "OK", "Welcome to the protected resource!");
  }
}
