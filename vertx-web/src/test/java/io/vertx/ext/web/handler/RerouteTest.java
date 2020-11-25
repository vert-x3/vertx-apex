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
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.WebTestBase;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Paulo Lopes
 */
public class RerouteTest extends WebTestBase {

  @AfterClass
  public static void oneTimeTearDown() throws IOException {
    cleanupFileUploadDir();
  }

  @Test
  public void testReroute() throws Exception {
    router.get("/users/:name").handler(ctx -> ctx.response().end("/users/:name"));
    router.get("/me").handler(ctx -> ctx.reroute("/users/paulo"));

    testRequest(HttpMethod.GET, "/me", 200, "OK", "/users/:name");
  }

  @Test
  public void testRerouteReparse() throws Exception {
    router.get("/users/:name").handler(ctx -> ctx.response().end(ctx.request().getParam("name")));
    router.get("/me").handler(ctx -> ctx.reroute("/users/paulo"));

    testRequest(HttpMethod.GET, "/me", 200, "OK", "paulo");
  }

  @Test
  public void testRerouteMethod() throws Exception {
    router.post("/me").handler(ctx -> ctx.response().end("POST"));
    router.get("/me").handler(ctx -> ctx.reroute(HttpMethod.POST, "/me"));

    testRequest(HttpMethod.GET, "/me", 200, "OK", "POST");
  }

  @Test
  public void testRerouteWithBody() throws Exception {
    router.route("/test/*").handler(BodyHandler.create());
    router.route("/test/v1").handler(ctx -> ctx.reroute("/test/v2"));
    router.route("/test/v2").handler(ctx -> ctx.response().end());

    testRequest(HttpMethod.POST, "/test/v1", req -> {
      req.setChunked(true);
      req.write("Test HTTP Body");
    }, 200, "OK", null);
  }

  @Test
  public void testRerouteFailure() throws Exception {
    router.get("/error/400").handler(ctx -> ctx.response()
            .setStatusCode(400)
            .end("/error/400"));
    router.get("/me").handler(ctx -> ctx.fail(400));
    router.get().failureHandler(ctx -> {
      if (ctx.statusCode() == 400) {
        ctx.reroute("/error/400");
      } else {
        ctx.next();
      }
    });

    testRequest(HttpMethod.GET, "/me", 400, "Bad Request", "/error/400");
  }

  @Test
  public void testRerouteClearHeader() throws Exception {
    router.get("/users/:name").handler(ctx -> ctx.response().end("/users/:name"));
    router.get("/me").handler(ctx -> {
      ctx.response().putHeader("X-woop", "durp");
      ctx.reroute("/users/paulo");
    });

    testRequest(HttpMethod.GET, "/me", null, res -> assertNull(res.getHeader("X-woop")), 200, "OK", "/users/:name");
  }

  @Test
  public void testRerouteClearHeader2() throws Exception {
    router.get("/users/:name").handler(ctx -> {
      ctx.response().putHeader("X-woop", "durp2");
      ctx.response().end("/users/:name");
    });
    router.get("/me").handler(ctx -> {
      ctx.response().putHeader("X-woop", "durp");
      ctx.reroute("/users/paulo");
    });

    testRequest(HttpMethod.GET, "/me", null, res -> assertEquals("durp2", res.getHeader("X-woop")), 200, "OK", "/users/:name");
  }

  @Test
  public void testRerouteClearHeader3() throws Exception {
    router.get("/users/:name").handler(ctx -> {
      ctx.response().putHeader("X-woop", "durp2");
      ctx.response().end("/users/:name");
    });
    router.get("/me").handler(ctx -> {
      ctx.response().putHeader("X-woop", "durp");
      ctx.reroute("/users/paulo");
    });

    testRequest(HttpMethod.GET, "/me", null, res -> {
      assertEquals("durp2", res.getHeader("X-woop"));
      assertNull(res.getHeader("Cookie"));
    }, 200, "OK", "/users/:name");
  }

  @Test
  public void testRerouteWithParams() throws Exception {
    router.get("/other").handler(ctx -> ctx.response().end("/other"));
    router.get("/base").handler(ctx -> ctx.reroute("/other?paramter1=p1&parameter2=p2"));

    testRequest(HttpMethod.GET, "/base", 200, "OK", "/other");
  }

  @Test
  public void testRerouteAbsoluteURI() throws Exception {
    router.get("/other").handler(ctx -> {
      assertEquals("http://localhost:8080/other?paramter1=p1&parameter2=p2", ctx.request().absoluteURI());
      // assert the parameters have been parsed
      assertEquals("p1", ctx.queryParam("paramter1").get(0));
      assertEquals("p2", ctx.queryParam("parameter2").get(0));
      ctx.response().end("/other");
    });
    router.get("/base").handler(ctx -> {
      ctx.reroute("/other?paramter1=p1&parameter2=p2");
    });

    testRequest(HttpMethod.GET, "/base?p=1", 200, "OK", "/other");
  }


  @Test
  public void testRerouteChecksWithQuery() throws Exception {
    router.get("/other").handler(ctx -> {
      HttpServerRequest req = ctx.request();

      assertEquals(HttpMethod.GET, req.method());
      assertEquals("GET", req.method().name());
      assertEquals("/other", req.path());
      assertEquals("paramter1=p1&parameter2=p2", req.query());
      assertEquals("/other?paramter1=p1&parameter2=p2", req.uri());
      assertEquals("http://localhost:8080/other?paramter1=p1&parameter2=p2", req.absoluteURI());

      // assert the parameters have been parsed
      assertEquals("p1", ctx.queryParam("paramter1").get(0));
      assertEquals("p2", ctx.queryParam("parameter2").get(0));
      ctx.response().end("/other");
    });
    router.get("/base").handler(ctx -> {
      HttpServerRequest req = ctx.request();
      assertEquals(HttpMethod.GET, req.method());
      assertEquals("GET", req.method().name());
      assertEquals("/base", req.path());
      assertEquals("p=1", req.query());
      assertEquals("/base?p=1", req.uri());
      assertEquals("http://localhost:8080/base?p=1", req.absoluteURI());

      ctx.reroute("/other?paramter1=p1&parameter2=p2");
    });

    testRequest(HttpMethod.GET, "/base?p=1", 200, "OK", "/other");
  }

  @Test
  public void testRerouteChecksWithQueryAndFragment() throws Exception {
    router.get("/other").handler(ctx -> {
      HttpServerRequest req = ctx.request();

      assertEquals(HttpMethod.GET, req.method());
      assertEquals("GET", req.method().name());
      assertEquals("/other", req.path());
      assertEquals("paramter1=p1&parameter2=p2", req.query());
      assertEquals("/other?paramter1=p1&parameter2=p2#frag", req.uri());
      assertEquals("http://localhost:8080/other?paramter1=p1&parameter2=p2#frag", req.absoluteURI());

      // assert the parameters have been parsed
      assertEquals("p1", ctx.queryParam("paramter1").get(0));
      assertEquals("p2", ctx.queryParam("parameter2").get(0));
      ctx.response().end("/other");
    });
    router.get("/base").handler(ctx -> {
      HttpServerRequest req = ctx.request();
      assertEquals(HttpMethod.GET, req.method());
      assertEquals("GET", req.method().name());
      assertEquals("/base", req.path());
      assertEquals("p=1", req.query());
      assertEquals("/base?p=1", req.uri());
      assertEquals("http://localhost:8080/base?p=1", req.absoluteURI());

      ctx.reroute("/other?paramter1=p1&parameter2=p2#frag");
    });

    testRequest(HttpMethod.GET, "/base?p=1", 200, "OK", "/other");
  }

  @Test
  public void testRerouteChecksWithFragment() throws Exception {
    router.get("/other").handler(ctx -> {
      HttpServerRequest req = ctx.request();

      assertEquals(HttpMethod.GET, req.method());
      assertEquals("GET", req.method().name());
      assertEquals("/other", req.path());
      assertNull(req.query());
      assertEquals("/other#frag", req.uri());
      assertEquals("http://localhost:8080/other#frag", req.absoluteURI());
      ctx.response().end("/other");
    });
    router.get("/base").handler(ctx -> {
      HttpServerRequest req = ctx.request();
      assertEquals(HttpMethod.GET, req.method());
      assertEquals("GET", req.method().name());
      assertEquals("/base", req.path());
      assertEquals("p=1", req.query());
      assertEquals("/base?p=1", req.uri());
      assertEquals("http://localhost:8080/base?p=1", req.absoluteURI());

      ctx.reroute("/other#frag");
    });

    testRequest(HttpMethod.GET, "/base?p=1", 200, "OK", "/other");
  }

  @Test
  public void testRerouteWhenBaseRequestHasBadlyEncodedParams() throws Exception {
    router.get("/other").handler(ctx -> {
      assertEquals(true, ctx.request().params().isEmpty());
      ctx.response().end("/other");
    });
    router.get("/base").handler(ctx -> ctx.reroute("/other"));

    testRequest(HttpMethod.GET, "/base?parameter1=%%value1%%", 200, "OK", "/other");
  }
}
