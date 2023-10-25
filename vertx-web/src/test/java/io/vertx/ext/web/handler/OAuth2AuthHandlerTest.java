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
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.impl.jose.JWK;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.WebTestBase;
import io.vertx.ext.web.sstore.SessionStore;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Paulo Lopes
 */
public class OAuth2AuthHandlerTest extends WebTestBase {

  private static final JsonObject fixture = new JsonObject(
    "{" +
      "  \"access_token\": \"4adc339e0\"," +
      "  \"refresh_token\": \"ec1a59d298\"," +
      "  \"token_type\": \"bearer\"," +
      "  \"scope\": \"read write\"," +
      "  \"expires_in\": 7200" +
      "}");

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  private String redirectURL = null;

  @Test
  public void testAuthCodeFlow() throws Exception {

    // lets mock a oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().putHeader("Content-Type", "application/json").end(fixture.encode()));
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    // create a oauth2 handler on our domain to the callback: "http://localhost:8080/callback"
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler.create(vertx, oauth2, "http://localhost:8080/callback");

    // setup the callback handler for receiving the callback
    oauth2Handler.setupCallback(router.route("/callback"));

    // protect everything under /protected
    router.route("/protected/*").handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      // in this case we should get a redirect
      redirectURL = resp.getHeader("Location");
      assertNotNull(redirectURL);
    }, 302, "Found", null);

    // fake the redirect
    testRequest(HttpMethod.GET, "/callback?state=/protected/somepage&code=1", null, resp -> {
    }, 200, "OK", "Welcome to the protected resource!");

    server.close();
  }

  @Test
  public void testAuthCodeFlowWithScopes() throws Exception {

    // lets mock an oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().putHeader("Content-Type", "application/json").end(fixture.encode()));
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    // create a oauth2 handler on our domain to the callback: "http://localhost:8080/callback"
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler
      .create(vertx, oauth2, "http://localhost:8080/callback")
      // require "read" scope
      .withScope("read");

    // setup the callback handler for receiving the callback
    oauth2Handler.setupCallback(router.route("/callback"));

    // protect everything under /protected
    router.route("/protected/*").handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      // in this case we should get a redirect
      redirectURL = resp.getHeader("Location");
      assertNotNull(redirectURL);
    }, 302, "Found", null);

    // fake the redirect
    testRequest(HttpMethod.GET, "/callback?state=/protected/somepage&code=1", null, resp -> {
    }, 200, "OK", "Welcome to the protected resource!");

    server.close();
  }

  @Test
  public void testAuthCodeFlowWithScopesInvalid() throws Exception {

    // lets mock an oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().putHeader("Content-Type", "application/json").end(fixture.encode()));
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    // create a oauth2 handler on our domain to the callback: "http://localhost:8080/callback"
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler
      .create(vertx, oauth2, "http://localhost:8080/callback")
      // require "rea" scope (will fail)
      .withScope("rea");

    // setup the callback handler for receiving the callback
    oauth2Handler.setupCallback(router.route("/callback"));

    // protect everything under /protected
    router.route("/protected/*").handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      // in this case we should get a redirect
      redirectURL = resp.getHeader("Location");
      assertNotNull(redirectURL);
    }, 302, "Found", null);

    // fake the redirect
    testRequest(HttpMethod.GET, "/callback?state=/protected/somepage&code=1", 403, "Forbidden");

    server.close();
  }

  @Test
  public void testAuthCodeFlowWithScopesFromMetadata() throws Exception {

    // lets mock an oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().putHeader("Content-Type", "application/json").end(fixture.encode()));
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    // create a oauth2 handler on our domain to the callback: "http://localhost:8080/callback"
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler
      .create(vertx, oauth2, "http://localhost:8080/callback");

    // setup the callback handler for receiving the callback
    oauth2Handler.setupCallback(router.route("/callback"));

    // protect everything under /protected
    router.route("/protected/*")
      .putMetadata("scopes", "read")
      .handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      // in this case we should get a redirect
      redirectURL = resp.getHeader("Location");
      assertNotNull(redirectURL);
    }, 302, "Found", null);

    // fake the redirect
    testRequest(HttpMethod.GET, "/callback?state=/protected/somepage&code=1", null, resp -> {
    }, 200, "OK", "Welcome to the protected resource!");

    server.close();
  }

  @Test
  public void testAuthCodeFlowWithScopesFromMetadataNoMatch() throws Exception {

    // lets mock an oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().putHeader("Content-Type", "application/json").end(fixture.encode()));
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    // create a oauth2 handler on our domain to the callback: "http://localhost:8080/callback"
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler
      .create(vertx, oauth2, "http://localhost:8080/callback");

    // setup the callback handler for receiving the callback
    oauth2Handler.setupCallback(router.route("/callback"));

    // protect everything under /protected
    router.route("/protected/*")
      .putMetadata("scopes", "delete")
      .handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      // in this case we should get a redirect
      redirectURL = resp.getHeader("Location");
      assertNotNull(redirectURL);
    }, 302, "Found", null);

    // fake the redirect
    testRequest(HttpMethod.GET, "/callback?state=/protected/somepage&code=1", null, resp -> {
    }, 403, "Forbidden", null);

    server.close();
  }

  @Test
  public void testAuthCodeFlowBypass() throws Exception {

    // lets mock a oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer ->
          req.response()
            .setStatusCode(400)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
              .put("error", 400)
              .put("error_description", "invalid code").encode()));
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    // create a oauth2 handler on our domain to the callback: "http://localhost:8080/callback"
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler.create(vertx, oauth2, "http://localhost:8080/callback");

    // setup the callback handler for receiving the callback
    oauth2Handler.setupCallback(router.route("/callback"));

    // protect everything under /protected
    router.route("/protected/*").handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });

    // fake the redirect
    testRequest(HttpMethod.GET, "/callback?state=/protected/somepage&code=1", 500, "Internal Server Error");

    server.close();
  }

  @Ignore("does not pass for now")
  @Test
  public void testAuthPKCECodeFlow() throws Exception {

    final AtomicReference<String> verifier = new AtomicReference<>();

    // lets mock a oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true)
          .bodyHandler(buffer -> {
            String[] parts = buffer.toString().split("&");
            int matches = 0;
            for (String part : parts) {
              if (part.equals("code=1")) {
                matches++;
              }
              if (part.startsWith("code_verifier=")) {
                MessageDigest md;
                try {
                  md = MessageDigest.getInstance("SHA-256");
                  md.update(part.substring(14).getBytes(StandardCharsets.US_ASCII));
                  if (verifier.get().equals(Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest()))) {
                    matches++;
                  }
                } catch (NoSuchAlgorithmException e) {
                  e.printStackTrace();
                }
              }
              if (part.equals("grant_type=authorization_code")) {
                matches++;
              }
            }
            assertEquals(3, matches);
            req.response().putHeader("Content-Type", "application/json").end(fixture.encode());
          });
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    // ensure that there's a session
    router.route().handler(SessionHandler.create(SessionStore.create(vertx)));

    // create a oauth2 handler on our domain to the callback: "http://localhost:8080/callback"
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler
      .create(vertx, oauth2, "http://localhost:8080/callback")
      .pkceVerifierLength(64);
    // setup the callback handler for receiving the callback
    oauth2Handler.setupCallback(router.route("/callback"));
    // protect everything under /protected
    router.route("/protected/*").handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });

    final AtomicReference<String> cookie = new AtomicReference<>();
    final AtomicReference<String> state = new AtomicReference<>();

    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      // in this case we should get a redirect
      redirectURL = resp.getHeader("Location");
      assertNotNull(redirectURL);
      assertTrue(redirectURL.contains("&code_challenge="));
      assertTrue(redirectURL.contains("&code_challenge_method="));
      // save the params
      String[] parts = redirectURL.substring(redirectURL.indexOf('?') + 1).split("&");
      String codeChallenge = null;
      String codeChallengeMethod = null;
      String nonce = null;
      for (String part : parts) {
        if (part.startsWith("code_challenge=")) {
          codeChallenge = part.substring(15);
        }
        if (part.startsWith("code_challenge_method=")) {
          codeChallengeMethod = part.substring(22);
        }
        if (part.startsWith("state=")) {
          nonce = part.substring(6);
        }
      }
      assertNotNull(nonce);
      assertNotNull(codeChallenge);
      assertNotNull(codeChallengeMethod);
      assertTrue(codeChallenge.length() >= 43 && codeChallenge.length() <= 128);
      assertEquals("S256", codeChallengeMethod);

      // save state
      verifier.set(codeChallenge);
      state.set(nonce);
      cookie.set(resp.getHeader("set-cookie"));
    }, 302, "Found", null);

    // fake the redirect from IdP
    testRequest(
      HttpMethod.GET,
      "/callback?state=" + state.get() + "&code=1",
      req -> {
        // add the session cookie back to ensure the session gets updated
        req.putHeader("cookie", cookie.get());
      },
      resp -> {
      },
      302,
      "Found",
      "Redirecting to /protected/somepage.");

    server.close();
  }

  @Test
  public void testAuthCodeFlowBadSetup() throws Exception {

    // lets mock a oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().putHeader("Content-Type", "application/json").end(fixture.encode()));
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    // protect everything. This has the bad sideffect that it will also shade the callback route which is computed
    // after this handler, HOWEVER, the handler implements the OrderListener, so the setup will be fixed OOTB
    router.route()
      .handler(
        OAuth2AuthHandler
          .create(vertx, oauth2, "http://localhost:8080/callback")
          .setupCallback(router.route("/callback")));

    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      // in this case we should get a redirect
      redirectURL = resp.getHeader("Location");
      assertNotNull(redirectURL);
    }, 302, "Found", null);

    // fake the redirect (Given that the setup is fixed by the OrderListener), this will succeed
    testRequest(HttpMethod.GET, "/callback?state=/protected/somepage&code=1", null, resp -> {
    }, 200, "OK", "Welcome to the protected resource!");

    // second attempt with proper config
    router.clear();

    // protect everything.
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler
      .create(vertx, oauth2, "http://localhost:8080/callback")
      .setupCallback(router.route("/callback"));

    // now the callback is registered before as it should
    router.route().handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", null, resp -> {
      // in this case we should get a redirect
      redirectURL = resp.getHeader("Location");
      assertNotNull(redirectURL);
    }, 302, "Found", null);

    // fake the redirect
    testRequest(HttpMethod.GET, "/callback?state=/protected/somepage&code=1", null, resp -> {
    }, 200, "OK", "Welcome to the protected resource!");

    server.close();
  }

  @Test
  public void testPasswordFlow() throws Exception {

    // lets mock a oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> {
          final String queryString = buffer.toString();
          assertTrue(queryString.contains("username=paulo"));
          assertTrue(queryString.contains("password=bananas"));
          assertTrue(queryString.contains("grant_type=password"));

          req.response().putHeader("Content-Type", "application/json").end(fixture.encode());
        });
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    WebAuthenticationHandler oauth2Handler = BasicAuthHandler.create(oauth2);

    // protect everything under /protected
    router.route("/protected/*").handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("paulo:bananas".getBytes(StandardCharsets.UTF_8))), res -> {
      // in this case we should get the resource
    }, 200, "OK", "Welcome to the protected resource!");

    testRequest(HttpMethod.GET, "/protected/somepage", 401, "Unauthorized");

    server.close();
  }

  @Test
  public void testBearerOnly() throws Exception {

    // lets mock a oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options().setClientId("client-id"));
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler.create(vertx, oauth2);

    // protect everything under /protected
    router.route("/protected/*").handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", 401, "Unauthorized");
    // Now try again with fake credentials
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Bearer 4adc339e0"), 401, "Unauthorized", "Unauthorized");
  }

  @Test
  public void testBearerOnlyWithJWT() throws Exception {

    OAuth2Auth oauth = OAuth2Auth
      .create(
        vertx,
        new OAuth2Options()
          // the client id must match the audience of the tokens
          .setClientId("s6BhdRkqt3")
          .addPubSecKey(new PubSecKeyOptions()
            .setAlgorithm("RS256")
            .setBuffer(
              "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmuIC9Qvwoe/3tUpHkcUp\n" +
                "vWmzQqnZtz3HBKbxzc/jBTxUHefJDs88Xjw5nNXhl4tXkHzFRAZHtDnwX074/2oc\n" +
                "PRSWaBjHYXB771af91UPrc9fb4lh3W1a8hmQU6sgKlQVwDnUuePDkCmwKCsuyX0M\n" +
                "wxuwOwEUo4r15NBh/H7FvuHVPnqWK1/kliYtQukF3svQkpZT6/puQ0bEOefROLB+\n" +
                "EAPM0OAaDyknjxCZJenk9FIyC6skOKVaxW7CcE54lIUjS1GKFQc44/+T+u0VKSmh\n" +
                "rRdBNcAhXmdpwjLoDTy/I8z+uqkKitdEVczCdleNqeb6b1kjPWS3VbLXxY/LIYlz\n" +
                "uQIDAQAB\n" +
                "-----END PUBLIC KEY-----")
          )
      );

    assertNotNull(oauth);

    JWT jwt = new JWT().addJWK(
      new JWK(new PubSecKeyOptions()
        .setAlgorithm("RS256")
        .setBuffer(
          "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCa4gL1C/Ch7/e1\n" +
            "SkeRxSm9abNCqdm3PccEpvHNz+MFPFQd58kOzzxePDmc1eGXi1eQfMVEBke0OfBf\n" +
            "Tvj/ahw9FJZoGMdhcHvvVp/3VQ+tz19viWHdbVryGZBTqyAqVBXAOdS548OQKbAo\n" +
            "Ky7JfQzDG7A7ARSjivXk0GH8fsW+4dU+epYrX+SWJi1C6QXey9CSllPr+m5DRsQ5\n" +
            "59E4sH4QA8zQ4BoPKSePEJkl6eT0UjILqyQ4pVrFbsJwTniUhSNLUYoVBzjj/5P6\n" +
            "7RUpKaGtF0E1wCFeZ2nCMugNPL8jzP66qQqK10RVzMJ2V42p5vpvWSM9ZLdVstfF\n" +
            "j8shiXO5AgMBAAECggEAIriwOQcoNuV4/qdcTA2LQe9ERJmXOUEcMKrMYntMRYw0\n" +
            "v0+K/0ruGaIeuE4qeLLAOp/+CTXvNTQX8wXdREUhd3/6B/QmHm39GrasveHP1gM7\n" +
            "PeHqkp1FWijo9hjS6SpYhfNxAQtSeCsgVqD3qCvkhIjchR3E5rTsUxN0JAq3ggb9\n" +
            "WCJ2LUxOOTHAWL4cv7FIKfwU/bwjBdHbSLuh7em4IE8tzcFgh49281APprGb4a3d\n" +
            "CPlIZC+CQmTFKPGzT0WDNc3EbPPKcx8ECRf1Zo94Tqnzv7FLgCmr0o4O9e6E3yss\n" +
            "Uwp7EKPUQyAwBkc+pHwqUmOPqHB+z28JUOwqoD0vQQKBgQDNiXSydWh9BUWAleQU\n" +
            "fgSF0bjlt38HVcyMKGC1xQhi8VeAfLJxGCGbdxsPFNCtMPDLRRyd4xHBmsCmPPli\n" +
            "CFHD1UbfNuKma6azl6A86geuTolgrHoxp57tZwoBpG9JHoTA53pfBPxb8q39YXKh\n" +
            "DSXsJVldxsHwzFAklj3ZqzWq3QKBgQDA6M/VW3SXEt1NWwMI+WGa/QKHDjLDhZzF\n" +
            "F3iQTtzDDmA4louAzX1cykNo6Y7SpORi0ralml65iwT2HZtE8w9vbw4LNmBiHmlX\n" +
            "AvpZSHT6/7nQeiFtxZu9cyw4GGpNSaeqp4Cq6TGYmfbq4nIdryzUU2AgsqSZyrra\n" +
            "xh7K+2I4jQKBgGjC8xQy+7sdgLt1qvc29B8xMkkEKl8WwFeADSsY7plf4fW/mURD\n" +
            "xH11S/l35pUgKNuysk9Xealws1kIIyRwkRx8DM+hLg0dOa64Thg+QQP7S9JWl0HP\n" +
            "6hWfO15y7bYbNBcO5TShWe+T1lMb5E1qYjXnI5HEyP1vZjn/yi60MXqRAoGAe6F4\n" +
            "+QLIwL1dSOMoGctBS4QU55so23e41fNJ2CpCf1uqPPn2Y9DOI/aYpxbv6n20xMTI\n" +
            "O2+of37h6h1lUhX38XGZ7YOm15sn5ZTJ/whZuDbFzh9HZ0N6oTq7vyOelPO8WblJ\n" +
            "077pgyRBQ51mhzGqKFVayPnUVZ/Ais7oEyxycU0CgYEAzEUhmN22ykywh0My83z/\n" +
            "7yl2tyrlv2hcZbaP7+9eHdUafGG8jMTVD7jxhzAbiSo2UeyHUnAItDnLetLh89K6\n" +
            "0oF3/rZLqugtb+f48dgRE/SDF4Itgp5fDqWHLhEW7ZhWCFlFgZ3sq0XryIxzFof0\n" +
            "O/Fd1NnotirzTnob5ReblIM=\n" +
            "-----END PRIVATE KEY-----\n")));

    assertNotNull(jwt);


    // lets mock a oauth2 server using code auth code flow
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler.create(vertx, oauth);

    // protect everything under /protected
    router.route("/protected/*").handler(oauth2Handler);
    // mount some handler under the protected zone
    router.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/protected/somepage", 401, "Unauthorized");
    // Now try again with fake credentials
    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Bearer 4adc339e0"), 401, "Unauthorized", "Unauthorized");
    // Now try again with real credentials
    String accessToken = jwt.sign(
      new JsonObject(
        "{\n" +
          "      \"iss\": \"https://server.example.com\",\n" +
          "      \"aud\": \"s6BhdRkqt3\",\n" +
          "      \"jti\": \"a-123\",\n" +
          "      \"exp\": 999999999999,\n" +
          "      \"iat\": 1311280970,\n" +
          "      \"sub\": \"24400320\",\n" +
          "      \"upn\": \"jdoe@server.example.com\",\n" +
          "      \"groups\": [\"red-group\", \"green-group\", \"admin-group\", \"admin\"]\n" +
          "}"), new JWTOptions().setAlgorithm("RS256"));

    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Bearer " + accessToken), 200, "OK", "Welcome to the protected resource!");

    // Now try again with expired credentials
    String accessTokenExp = jwt.sign(
      new JsonObject(
        "{\n" +
          "      \"iss\": \"https://server.example.com\",\n" +
          "      \"aud\": \"s6BhdRkqt3\",\n" +
          "      \"jti\": \"a-123\",\n" +
          "      \"exp\": 1311280970,\n" +
          "      \"iat\": 1311280970,\n" +
          "      \"sub\": \"24400320\",\n" +
          "      \"upn\": \"jdoe@server.example.com\",\n" +
          "      \"groups\": [\"red-group\", \"green-group\", \"admin-group\", \"admin\"]\n" +
          "}"), new JWTOptions().setAlgorithm("RS256"));

    testRequest(HttpMethod.GET, "/protected/somepage", req -> req.putHeader("Authorization", "Bearer " + accessTokenExp), 401, "Unauthorized", "Unauthorized");
  }

  @Test
  public void testAuthCodeFlowSubRouter() throws Exception {

    // lets mock a oauth2 server using code auth code flow
    OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000"));

    final CountDownLatch latch = new CountDownLatch(1);

    HttpServer server = vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.POST && "/oauth/token".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().putHeader("Content-Type", "application/json").end(fixture.encode()));
      } else if (req.method() == HttpMethod.POST && "/oauth/revoke".equals(req.path())) {
        req.setExpectMultipart(true).bodyHandler(buffer -> req.response().end());
      } else {
        req.response().setStatusCode(400).end();
      }
    });

    server.listen(10000).onComplete(ready -> {
      if (ready.failed()) {
        throw new RuntimeException(ready.cause());
      }
      // ready
      latch.countDown();
    });

    latch.await();

    Router subRouter = Router.router(vertx);

    router.route("/secret*").subRouter(subRouter);

    // create a oauth2 handler on our domain to the callback: "http://localhost:8080/secret/callback"
    OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler.create(vertx, oauth2, "http://localhost:8080/secret/callback");

    // setup the callback handler for receiving the callback
    oauth2Handler.setupCallback(subRouter.route("/callback"));

    // protect everything under /protected
    subRouter.route("/protected/*").handler(oauth2Handler);
    // mount some handler under the protected zone
    subRouter.route("/protected/somepage").handler(rc -> {
      assertNotNull(rc.user().get());
      rc.response().end("Welcome to the protected resource!");
    });


    testRequest(HttpMethod.GET, "/secret/protected/somepage", null, resp -> {
      // in this case we should get a redirect
      redirectURL = resp.getHeader("Location");
      assertNotNull(redirectURL);
    }, 302, "Found", null);

    // fake the redirect
    testRequest(HttpMethod.GET, "/secret/callback?state=/secret/protected/somepage&code=1", null, resp -> {
    }, 200, "OK", "Welcome to the protected resource!");

    server.close();
  }

  @Test
  public void testSharing() throws Exception {
    OAuth2AuthHandler oauth2 = OAuth2AuthHandler.create(vertx, OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("client-id")
      .setClientSecret("client-secret")
      .setSite("http://localhost:10000")), "http://localhost:8080/secret/callback");

    router.route("/protected/*").handler(oauth2.setupCallback(router.route("/callback")));
    router.route("/protected/userinfo").handler(oauth2);

    assertEquals("/callback", router.getRoutes().get(0).getPath());
    assertEquals("/protected/", router.getRoutes().get(1).getPath());
    assertEquals("/protected/userinfo", router.getRoutes().get(2).getPath());
  }
}
