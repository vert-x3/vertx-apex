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

package io.vertx.ext.web.tests.templ;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.tests.WebTestBase;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class TemplateTest extends WebTestBase {

  @Test
  public void testTemplateHandler() throws Exception {
    testRelativeToRoutePath(null);
  }

  @Test
  public void testTemplateHandler2() throws Exception {
    testRelativeToRoutePath("/");
  }

  @Test
  public void testRelativeToRoutePath() throws Exception {
    testRelativeToRoutePath("/pathprefix/");
  }

  private void testRelativeToRoutePath(String pathPrefix) throws Exception {
    TemplateEngine engine = new TestEngine(false);
    router.route().handler(context -> {
      context.put("foo", "badger");
      context.put("bar", "fox");
      context.next();
    });
    Route route = router.route();
    if (pathPrefix != null) {
      route.path(pathPrefix + "*");
    }
    route.handler(TemplateHandler.create(engine, "somedir", "text/html"));
    String expected =
      "<html>\n" +
        "<body>\n" +
        "<h1>Test template</h1>\n" +
        "foo is badger bar is fox<br>\n" +
        "</body>\n" +
        "</html>";
    testRequest(HttpMethod.GET, pathPrefix != null ? pathPrefix + "/test-template.html" : "/test-template.html", 200, "OK", expected);
  }

  @Test
  public void testTemplateEngineFail() throws Exception {
    TemplateEngine engine = new TestEngine(true);
    router.route().handler(TemplateHandler.create(engine, "somedir", "text/html"));
    router.errorHandler(500, ctx -> {
      Throwable t = ctx.failure();
      assertEquals("eek", t.getMessage());
      testComplete();
    });
    testRequest(HttpMethod.GET, "/foo.html", 500, "Internal Server Error");
    await();
  }

  @Test
  public void testTemplateEngineWithPathVariables() throws Exception {
    TemplateEngine engine = new TestEngine(false);
    router.route().handler(context -> {
      context.put("foo", "badger");
      context.put("bar", "fox");
      context.next();
    });
    router.route("/:project/*").handler(TemplateHandler.create(engine, "somedir", "text/html"));
    String expected =
      "<html>\n" +
        "<body>\n" +
        "<h1>Test template</h1>\n" +
        "foo is badger bar is fox<br>\n" +
        "</body>\n" +
        "</html>";
    testRequest(HttpMethod.GET, "/1/test-template.html", 200, "OK", expected);
  }

  @Test
  public void testRenderDirectly() throws Exception {
    TemplateEngine engine = new TestEngine(false);
    router.route().handler(context -> {
      context.put("foo", "badger");
      context.put("bar", "fox");
      engine.render(context.data(), "somedir/test-template.html").onComplete(res -> {
        if (res.succeeded()) {
          context.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html").end(res.result());
        } else {
          context.fail(res.cause());
        }
      });
    });
    String expected =
      "<html>\n" +
        "<body>\n" +
        "<h1>Test template</h1>\n" +
        "foo is badger bar is fox<br>\n" +
        "</body>\n" +
        "</html>";
    testRequest(HttpMethod.GET, "/", 200, "OK", expected);
  }

  @Test
  public void testRenderToBuffer() throws Exception {
    TemplateEngine engine = new TestEngine(false);
    String expected =
      "<html>\n" +
        "<body>\n" +
        "<h1>Test template</h1>\n" +
        "foo is badger bar is fox<br>\n" +
        "</body>\n" +
        "</html>";
    router.route().handler(context -> {
      context.put("foo", "badger");
      context.put("bar", "fox");
      engine.render(context.data(), "somedir/test-template.html").onComplete(onSuccess(res -> {
        String rendered = res.toString();
        final String actual = normalizeLineEndingsFor(res).toString();
        assertEquals(expected, actual);
        context.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
        context.response().end(rendered);
        testComplete();
      }));
    });

    testRequestBuffer(HttpMethod.GET, "/", null, null, 200, "OK", Buffer.buffer(expected), true);
    await();
  }

  // Just for testing - not for actual use
  class TestEngine implements TemplateEngine {

    boolean fail;

    TestEngine(boolean fail) {
      this.fail = fail;
    }

    @Override
    public Future<Buffer> render(Map<String, Object> context, String templateFileName) {
      if (fail) {
        return Future.failedFuture(new Exception("eek"));
      } else {
        String templ = vertx.fileSystem().readFileBlocking(templateFileName).toString(StandardCharsets.UTF_8);
        if (context.containsKey("foo")) {
          templ = templ.replace("{foo}", (String) context.get("foo"));
        }
        if (context.containsKey("bar")) {
          templ = templ.replace("{bar}", (String) context.get("bar"));
        }
        return Future.succeededFuture(Buffer.buffer(templ));
      }
    }

    @Override
    public void clearCache() {
      // no-op
    }
  }

  @Test
  public void testSubRouterBeforeTemplateHandler() throws Exception {

    String top =
      "<html lang=\"en\">\n" +
        "<body>\n" +
        "<h1>Top index!</h1>\n" +
        "</body>\n" +
        "</html>\n";

    String sub =
      "<html lang=\"en\">\n" +
        "<body>\n" +
        "<h1>Sub index!</h1>\n" +
        "</body>\n" +
        "</html>\n";

    router.clear();

    router.route("/sub*").subRouter(Router.router(vertx));

    router.getWithRegex(".+\\.ftl")
      .handler(TemplateHandler.create(new TestEngine(false)));

    testRequestBuffer(HttpMethod.GET, "/sub/index.ftl", null, null, 200, "OK", Buffer.buffer(sub), true);
    testRequestBuffer(HttpMethod.GET, "/index.ftl", null, null, 200, "OK", Buffer.buffer(top), true);
  }

}
