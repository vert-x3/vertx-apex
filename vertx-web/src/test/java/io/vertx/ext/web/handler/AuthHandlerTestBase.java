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

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.auth.properties.PropertyFileAuthorization;
import io.vertx.ext.web.WebTestBase;
import io.vertx.ext.auth.common.UserContextInternal;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class AuthHandlerTestBase extends WebTestBase {

  @AfterClass
  public static void oneTimeTearDown() throws IOException {
    cleanupFileUploadDir();
  }

  @Test
  public void testAuthAuthorities() throws Exception {
    testAuthorization("tim", false, PermissionBasedAuthorization.create("dance"));
  }

  @Test
  public void testAuthAuthoritiesFail() throws Exception {
    testAuthorization("tim", true, PermissionBasedAuthorization.create("knitter"));
  }

  protected abstract WebAuthenticationHandler createAuthHandler(AuthenticationProvider authProvider);

  protected boolean requiresSession() {
    return false;
  }

  protected SessionStore getSessionStore() {
    return LocalSessionStore.create(vertx);
  }

  protected void testAuthorization(String username, boolean fail, Authorization authority) throws Exception {
    if (requiresSession()) {
      router.route().handler(BodyHandler.create());
      SessionStore store = getSessionStore();
      router.route().handler(SessionHandler.create(store));
    }
    AuthenticationProvider authNProvider = PropertyFileAuthentication.create(vertx, "login/loginusers.properties");
    AuthorizationProvider authZProvider = PropertyFileAuthorization.create(vertx, "login/loginusers.properties");

    WebAuthenticationHandler authNHandler = createAuthHandler(authNProvider);
    router.route().handler(rc -> {
      // we need to be logged in
      if (!rc.user().authenticated()) {
        UsernamePasswordCredentials authInfo = new UsernamePasswordCredentials(username, "delicious:sausages");
        authNProvider.authenticate(authInfo).onComplete(res -> {
          if (res.succeeded()) {
            ((UserContextInternal) rc.user())
              .setUser(res.result());
            rc.next();
          } else {
            rc.fail(res.cause());
          }
        });
      }
    });
    router.route().handler(authNHandler);
    if (authority != null) {
      router.route().handler(AuthorizationHandler.create(authority).addAuthorizationProvider(authZProvider));
    }
    router.route().handler(rc -> rc.response().end());

    testRequest(HttpMethod.GET, "/", fail ? 403: 200, fail? "Forbidden": "OK");
  }
}
