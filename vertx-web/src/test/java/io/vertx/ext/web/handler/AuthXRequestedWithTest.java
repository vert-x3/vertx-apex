/*
 * Copyright 2014 Red Hat, Inc.
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

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.web.RoutingContext;
import org.junit.Test;

public class AuthXRequestedWithTest extends AuthHandlerTestBase {

  @Test
  public void testNoWwwAuthenticateForAjaxCalls() throws Exception {
    String realm = BasicAuthHandler.DEFAULT_REALM;
    Handler<RoutingContext> handler = rc -> {
      assertNotNull(rc.user().get());
      assertEquals("tim", rc.user().get().principal().getString("username"));
      rc.response().end("Welcome to the protected resource!");
    };

    AuthenticationProvider authProvider = PropertyFileAuthentication.create(vertx, "login/loginusers.properties");
    router.route("/protected/*").handler(BasicAuthHandler.create(authProvider, realm));

    router.route("/protected/somepage").handler(handler);

    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNotNull(wwwAuth);
      assertEquals("Basic realm=\"" + realm + "\"", wwwAuth);
    }, 401, "Unauthorized", null);

    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("X-Requested-With", "XMLHttpRequest"), resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNull(wwwAuth);
    }, 401, "Unauthorized", null);

    // Now try again with credentials
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcw=="), resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNull(wwwAuth);
    }, 200, "OK", "Welcome to the protected resource!");

  }

  @Override
  protected WebAuthenticationHandler createAuthHandler(AuthenticationProvider authProvider) {
    return BasicAuthHandler.create(authProvider);
  }

}
