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

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.ClusterSerializable;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class BasicAuthHandlerTest extends AuthHandlerTestBase {

  @Test
  public void testLoginDefaultRealm() throws Exception {
    doLogin(BasicAuthHandler.DEFAULT_REALM);
  }

  @Test
  public void testLoginNonDefaultRealm() throws Exception {
    doLogin("aardvarks");
  }

  private void doLogin(String realm) throws Exception {

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

    // Now try again with credentials
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcw=="), resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNull(wwwAuth);
    }, 200, "OK", "Welcome to the protected resource!");

  }

  @Test
  public void testWithSessions() throws Exception {
    router.route().handler(BodyHandler.create());
    SessionStore store = new SerializingSessionStore();

    AuthenticationProvider authProvider = PropertyFileAuthentication.create(vertx, "login/loginusers.properties");
    router.route().handler(SessionHandler.create(store));
    router.route("/protected/*").handler(BasicAuthHandler.create(authProvider));

    AtomicReference<String> sessionID = new AtomicReference<>();
    AtomicInteger count = new AtomicInteger();

    Handler<RoutingContext> handler = rc -> {
      int c = count.incrementAndGet();
      assertNotNull(rc.session());
      String sessID = sessionID.get();
      if (sessID != null) {
        assertEquals(sessID, rc.session().id());
      }
      assertNotNull(rc.user().get());
      assertEquals("tim", rc.user().get().principal().getString("username"));
      if (c == 7) {
        rc.user().clear();
      }
      rc.response().end("Welcome to the protected resource!");
    };

    router.route("/protected/somepage").handler(handler);

    AtomicReference<String> sessionCookie = new AtomicReference<>();

    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNotNull(wwwAuth);
      assertEquals("Basic realm=\"" + BasicAuthHandler.DEFAULT_REALM + "\"", wwwAuth);
      String setCookie = resp.headers().get("set-cookie");
      // auth failed you should not get a session cookie!!!
      assertNull(setCookie);
    }, 401, "Unauthorized", null);

    // Now try again with credentials
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcw=="), resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNull(wwwAuth);
      // auth is success, we should get a cookie!!!
      String setCookie = resp.headers().get("set-cookie");
      assertNotNull(setCookie);
      sessionCookie.set(setCookie);
    }, 200, "OK", "Welcome to the protected resource!");

    // And try again a few times we should be logged in with user stored in the session
    for (int i = 0; i < 5; i++) {
      testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("cookie", sessionCookie.get()), resp -> {
        String wwwAuth = resp.headers().get("WWW-Authenticate");
        assertNull(wwwAuth);
      }, 200, "OK", "Welcome to the protected resource!");
    }

    // Now set the user to null, this effectively logs him out

    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNotNull(wwwAuth);
      assertEquals("Basic realm=\"" + BasicAuthHandler.DEFAULT_REALM + "\"", wwwAuth);
    }, 401, "Unauthorized", null);

    // And login again
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcw=="), resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNull(wwwAuth);
    }, 200, "OK", "Welcome to the protected resource!");


  }

  @Test
  public void testLoginFail() throws Exception {

    String realm = "vertx-web";

    Handler<RoutingContext> handler = rc -> {
      fail("should not get here");
      rc.response().end("Welcome to the protected resource!");
    };

    AuthenticationProvider authProvider = PropertyFileAuthentication.create(vertx, "login/loginusers.properties");
    router.route("/protected/*").handler(BasicAuthHandler.create(authProvider));

    router.route("/protected/somepage").handler(handler);

    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNotNull(wwwAuth);
      assertEquals("Basic realm=\"" + realm + "\"", wwwAuth);
    }, 401, "Unauthorized", null);

    // Now try again with bad credentials
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Basic dGltOn5hdXdhZ2Vz"), resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNotNull(wwwAuth);
      assertEquals("Basic realm=\"" + realm + "\"", wwwAuth);

    }, 401, "Unauthorized", null);

  }

  @Test
  public void testLoginFailWithBadBase64() throws Exception {

    String realm = "vertx-web";

    Handler<RoutingContext> handler = rc -> {
      fail("should not get here");
      rc.response().end("Welcome to the protected resource!");
    };

    AuthenticationProvider authProvider = PropertyFileAuthentication.create(vertx, "login/loginusers.properties");
    router.route("/protected/*").handler(BasicAuthHandler.create(authProvider));

    router.route("/protected/somepage").handler(handler);

    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      String wwwAuth = resp.headers().get("WWW-Authenticate");
      assertNotNull(wwwAuth);
      assertEquals("Basic realm=\"" + realm + "\"", wwwAuth);
    }, 401, "Unauthorized", null);

    // Now try again with bad credentials
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Basic dGltOn5hdXdhZ2Vz="), resp -> {

    }, 400, "Bad Request", null);

  }

  @Override
  protected WebAuthenticationHandler createAuthHandler(AuthenticationProvider authProvider) {
    return BasicAuthHandler.create(authProvider);
  }

  private class SerializingSessionStore implements SessionStore {

    private final Map<String, Buffer> sessions = new ConcurrentHashMap<>();
    private final VertxContextPRNG prng = VertxContextPRNG.current(vertx);

    @Override
    public SessionStore init(Vertx vertx, JsonObject options) {
      return this;
    }

    @Override
    public long retryTimeout() {
      return 0L;
    }

    @Override
    public Session createSession(long timeout) {
      return new SharedDataSessionImpl(prng, timeout, DEFAULT_SESSIONID_LENGTH);
    }

    @Override
    public Session createSession(long timeout, int length) {
      return new SharedDataSessionImpl(prng, timeout, length);
    }

    @Override
    public Future<@Nullable Session> get(String id) {
      final Promise<Session> promise = ((VertxInternal) vertx).promise();

      Buffer buff = sessions.get(id);
      SharedDataSessionImpl sess;
      if (buff != null) {
        sess = new SharedDataSessionImpl(prng);
        sess.readFromBuffer(0, buff);
      } else {
        sess = null;
      }
      vertx.runOnContext(v -> promise.complete(sess));
      return promise.future();
    }

    @Override
    public Future<Void> delete(String id) {
      final Promise<Void> promise = ((VertxInternal) vertx).promise();
      sessions.remove(id);
      vertx.runOnContext(v -> promise.complete());
      return promise.future();
    }

    @Override
    public Future<Void> put(Session session) {
      final Promise<Void> promise = ((VertxInternal) vertx).promise();
      ClusterSerializable cs = (ClusterSerializable)session;
      Buffer buff = Buffer.buffer();
      cs.writeToBuffer(buff);
      sessions.put(session.id(), buff);
      vertx.runOnContext(v -> promise.complete());
      return promise.future();
    }

    @Override
    public Future<Void> clear() {
      final Promise<Void> promise = ((VertxInternal) vertx).promise();
      sessions.clear();
      vertx.runOnContext(v -> promise.complete());
      return promise.future();
    }

    @Override
    public Future<Integer> size() {
      final Promise<Integer> promise = ((VertxInternal) vertx).promise();
      vertx.runOnContext(v -> promise.complete(sessions.size()));
      return promise.future();
    }

    @Override
    public void close() {
      sessions.clear();
    }
  }

  @Test
  public void testSecurityBypass() throws Exception {

    Handler<RoutingContext> handler = rc -> {
      fail("should not get here");
      rc.response().end("Welcome to the protected resource!");
    };

    AuthenticationProvider authProvider = PropertyFileAuthentication.create(vertx, "login/loginusers.properties");
    router.route().pathRegex("/api/.*").handler(BasicAuthHandler.create(authProvider));

    router.route("/api/v1/standard-job-profiles").handler(handler);

    testRequest(HttpMethod.GET, "//api/v1/standard-job-profiles", 401, "Unauthorized");
  }
}
