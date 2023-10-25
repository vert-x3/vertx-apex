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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.common.AuthenticationHandler;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class RedirectAuthHandlerTest extends AuthHandlerTestBase {

  protected AtomicReference<String> sessionCookie = new AtomicReference<>();
  protected FormLoginHandler formLoginHandler;
  protected AuthenticationProvider authProvider;
  protected String usernameParam;
  protected String passwordParam;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    authProvider = PropertyFileAuthentication.create(vertx, "login/loginusers.properties");
    usernameParam = FormLoginHandler.DEFAULT_USERNAME_PARAM;
    passwordParam = FormLoginHandler.DEFAULT_PASSWORD_PARAM;
  }

  @Test
  public void testLogin() throws Exception {

    doLogin(rc -> {
      Session sess = rc.session();
      assertNotNull(sess);
      assertEquals(sessionCookie.get().substring(18, 50), sess.id());
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });
    // And request it again
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("cookie", sessionCookie.get()), resp -> {
    }, 200, "OK", "Welcome to the protected resource!");
    // Now logout
    router.route("/logout").handler(rc -> {
      rc.user().clear();
      rc.response().end("logged out");
    });
    testRequest(HttpMethod.GET, "/logout", req -> req.putHeader("cookie", sessionCookie.get()), resp -> {
    }, 200, "OK", "logged out");
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("cookie", sessionCookie.get()), resp -> {
      String location = resp.headers().get("location");
      assertNotNull(location);
      assertEquals("/loginpage", location);
    }, 302, "Found", null);
  }

  @Test
  public void testLoginChangeFormLoginHandlerParams() throws Exception {
    formLoginHandler = FormLoginHandler.create(authProvider);
    usernameParam = "username2";
    passwordParam ="password2";
    formLoginHandler.setUsernameParam(usernameParam).setPasswordParam(passwordParam);
    testLogin();
  }

  @Test
  public void testFormLoginHandlerDirectDefaultResponse() throws Exception {
    formLoginHandler = FormLoginHandler.create(authProvider);
    usernameParam = "username2";
    passwordParam ="password2";
    formLoginHandler.setUsernameParam(usernameParam).setPasswordParam(passwordParam);
    router.route().handler(LoggerHandler.create());
    router.route().handler(BodyHandler.create());
    router.route("/login").handler(formLoginHandler);
    testRequest(HttpMethod.POST, "/login", sendLoginRequestConsumer(), resp -> {
    }, 200, "OK", "<html><body><h1>Login successful</h1></body></html>");
  }

  @Test
  public void testFormLoginHandlerDirectSpecifyLoggedInURL() throws Exception {
    formLoginHandler = FormLoginHandler.create(authProvider);
    usernameParam = "username2";
    passwordParam ="password2";
    String loggedInDirectOKPage = "/youloggedinokpage.html";
    formLoginHandler.setUsernameParam(usernameParam).setPasswordParam(passwordParam).setDirectLoggedInOKURL(loggedInDirectOKPage);
    router.route().handler(LoggerHandler.create());
    router.route().handler(BodyHandler.create());
    router.route("/login").handler(formLoginHandler);
    testRequest(HttpMethod.POST, "/login", sendLoginRequestConsumer(), resp -> {
      String location = resp.headers().get("location");
      assertNotNull(location);
      assertEquals(loggedInDirectOKPage, location);
    }, 302, "Found", null);
  }

  private Consumer<HttpClientRequest> sendLoginRequestConsumer() {
    return req -> {
      String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
      Buffer buffer = Buffer.buffer();
      String str =
        "--" + boundary + "\r\n" +
          "Content-Disposition: form-data; name=\"" + usernameParam + "\"\r\n\r\ntim\r\n" +
          "--" + boundary + "\r\n" +
          "Content-Disposition: form-data; name=\"" + passwordParam + "\"\r\n\r\ndelicious:sausages\r\n" +
          "--" + boundary + "--\r\n";
      buffer.appendString(str);
      req.putHeader("content-length", String.valueOf(buffer.length()));
      req.putHeader("content-type", "multipart/form-data; boundary=" + boundary);
      if (sessionCookie.get() != null) {
        req.putHeader("cookie", sessionCookie.get());
      }
      req.write(buffer);
    };
  }

  @Test
  public void testLoginFailBadUser() throws Exception {
    testLoginFail(true);
  }

  @Test
  public void testLoginFailBadPassword() throws Exception {
    testLoginFail(false);
  }

  @Test
  public void testFormLoginFailures() throws Exception {
    router.route().handler(BodyHandler.create());
    SessionStore store = LocalSessionStore.create(vertx);
    router.route().handler(SessionHandler.create(store));
    FormLoginHandler loginHandler = FormLoginHandler.create(authProvider);
    router.route("/login").handler(loginHandler);
    // only POST is allowed
    testRequest(HttpMethod.GET, "/login", 405, "Method Not Allowed");
    // missing username in the form
    loginHandler.setUsernameParam("username-not-in-form");
    testRequest(HttpMethod.POST, "/login", sendLoginRequestConsumer(), 400, "Bad Request", null);
  }

  @Test
  public void testFormLoginWithoutBodyHandlerFailure() throws Exception {
    SessionStore store = LocalSessionStore.create(vertx);
    router.route().handler(SessionHandler.create(store));
    FormLoginHandler loginHandler = FormLoginHandler.create(authProvider);
    router.route("/login").handler(loginHandler);
    CountDownLatch latch = new CountDownLatch(1);
    router.errorHandler(500, ctx -> {
      Throwable cause = ctx.failure();
      assertNotNull(cause);
      assertEquals("BodyHandler is required to process POST requests", cause.getMessage());
      latch.countDown();
    });
    // not a multi-part form
    testRequest(HttpMethod.POST, "/login", 500, "Internal Server Error");
    latch.await();
  }

  @Test
  public void testRedirectWithParams() throws Exception {
    router.route().handler(BodyHandler.create());
    SessionStore store = LocalSessionStore.create(vertx);
    router.route().handler(SessionHandler.create(store));
    AuthenticationHandler authHandler = RedirectAuthHandler.create(authProvider);

    router.route("/protected/*").handler(authHandler);

    router.route("/protected/somepage").handler(ctx -> {
      assertEquals("1", ctx.request().getParam("param"));
      ctx.response().end("Welcome to the protected resource!");
    });

    router.route("/loginpage").handler(rc -> rc.response().putHeader("content-type", "text/html").end(createloginHTML()));

    router.route("/login").handler(FormLoginHandler.create(authProvider));

    // request protected resource, expect redirect to login
    testRequest(HttpMethod.GET, "/protected/somepage?param=1", null, resp -> {
      String location = resp.headers().get("location");
      assertNotNull(location);
      assertEquals("/loginpage", location);
      String setCookie = resp.headers().get("set-cookie");
      assertNotNull(setCookie);
      sessionCookie.set(setCookie);
    }, 302, "Found", null);

    // get login
    testRequest(HttpMethod.GET, "/loginpage", req -> req.putHeader("cookie", sessionCookie.get()), resp -> {
    }, 200, "OK", createloginHTML());

    // do post with credentials
    testRequest(HttpMethod.POST, "/login", sendLoginRequestConsumer(), resp -> {
      // session will be upgraded
      String setCookie = resp.headers().get("set-cookie");
      assertNotNull(setCookie);
      sessionCookie.set(setCookie);

      String location = resp.headers().get("location");
      assertNotNull(location);
      assertEquals("/protected/somepage?param=1", location);
    }, 302, "Found", null);

    // fetch the resource
    testRequest(HttpMethod.GET, "/protected/somepage?param=1", req -> req.putHeader("cookie", sessionCookie.get()), resp -> {
    }, 200, "OK", "Welcome to the protected resource!");
  }

  @Override
  protected WebAuthenticationHandler createAuthHandler(AuthenticationProvider authProvider) {
    return RedirectAuthHandler.create(authProvider);
  }

  @Override
  protected boolean requiresSession() {
    return true;
  }

  private void testLoginFail(boolean badUser) throws Exception {

    doLoginFail(badUser, rc -> {
      Session sess = rc.session();
      assertNotNull(sess);
      assertEquals(sessionCookie.get().substring(18, 54), sess.id());
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });
  }

  private void doLogin(Handler<RoutingContext> handler) throws Exception {
    doLoginCommon(handler);
    testRequest(HttpMethod.POST, "/login", sendLoginRequestConsumer(), resp -> {
      // session will be upgraded
      String setCookie = resp.headers().get("set-cookie");
      assertNotNull(setCookie);
      sessionCookie.set(setCookie);
      String location = resp.headers().get("location");
      assertNotNull(location);
      assertEquals("/protected/somepage", location);
    }, 302, "Found", null);
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("cookie", sessionCookie.get()), resp -> {
    }, 200, "OK", "Welcome to the protected resource!");
  }

  private void doLoginCommon(Handler<RoutingContext> handler) throws Exception {
    router.route().handler(BodyHandler.create());
    SessionStore store = LocalSessionStore.create(vertx);
    router.route().handler(SessionHandler.create(store));
    AuthenticationHandler authHandler = RedirectAuthHandler.create(authProvider);
    router.route("/protected/*").handler(authHandler);
    router.route("/protected/somepage").handler(handler);
    String loginHTML = createloginHTML();
    router.route("/loginpage").handler(rc -> rc.response().putHeader("content-type", "text/html").end(loginHTML));
    if (formLoginHandler == null) {
      formLoginHandler = FormLoginHandler.create(authProvider);
    }
    router.route("/login").handler(formLoginHandler);
    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      String location = resp.headers().get("location");
      assertNotNull(location);
      assertEquals("/loginpage", location);
      String setCookie = resp.headers().get("set-cookie");
      assertNotNull(setCookie);
      sessionCookie.set(setCookie);
    }, 302, "Found", null);
    testRequest(HttpMethod.GET, "/loginpage", req -> req.putHeader("cookie", sessionCookie.get()), resp -> {
    }, 200, "OK", loginHTML);
  }

  private void doLoginFail(boolean badUser, Handler<RoutingContext> handler) throws Exception {
    doLoginCommon(handler);
    testRequest(HttpMethod.POST, "/login", req -> {
      String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
      Buffer buffer = Buffer.buffer();
      String username = badUser ? "jim" : "tim";
      String password = badUser ? "delicious:sausages" : "fishfingers";
      String str =
        "--" + boundary + "\r\n" +
          "Content-Disposition: form-data; name=\"username\"\r\n\r\n" + username + "\r\n" +
          "--" + boundary + "\r\n" +
          "Content-Disposition: form-data; name=\"password\"\r\n\r\n" + password + "\r\n" +
          "--" + boundary + "--\r\n";
      buffer.appendString(str);
      req.putHeader("content-length", String.valueOf(buffer.length()));
      req.putHeader("content-type", "multipart/form-data; boundary=" + boundary);
      req.putHeader("cookie", sessionCookie.get());
      req.write(buffer);
    }, resp -> {
    }, 401, "Unauthorized", null);
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("cookie", sessionCookie.get()), resp -> {
      String location = resp.headers().get("location");
      assertNotNull(location);
      assertEquals("/loginpage", location);
    }, 302, "Found", null);
  }

  protected String createloginHTML() {
    return "<html>\n" +
      "<body>\n" +
      "<h2>Please login {{foo}}</h2><br>\n" +
      "<form action=\"/login\" method=\"post\">\n" +
      "  <div>\n" +
      "    <label>Username:</label>\n" +
      "    <input type=\"text\" name=\"" + usernameParam + "\"/>\n" +
      "  </div>\n" +
      "  <div>\n" +
      "    <label>Password:</label>\n" +
      "    <input type=\"password\" name=\"" + passwordParam + "\"/>\n" +
      "  </div>\n" +
      "  <div>\n" +
      "    <input type=\"submit\" value=\"Log In\"/>\n" +
      "  </div>\n" +
      "</form>\n" +
      "</body>\n" +
      "</html>";
  }
}
