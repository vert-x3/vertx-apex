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

package io.vertx.ext.web;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.test.core.VertxTestBase;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class WebTestBase extends VertxTestBase {

  protected static Set<HttpMethod> METHODS = new HashSet<>(Arrays.asList(HttpMethod.DELETE, HttpMethod.GET,
    HttpMethod.HEAD, HttpMethod.PATCH, HttpMethod.OPTIONS, HttpMethod.TRACE, HttpMethod.POST, HttpMethod.PUT));

  protected HttpServer server;
  protected HttpClient client;
  protected Router router;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    router = Router.router(vertx);
    server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost"));
    client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
    CountDownLatch latch = new CountDownLatch(1);
    server.requestHandler(router::accept).listen(onSuccess(res -> {
      latch.countDown();
    }));
    awaitLatch(latch);
  }

  @Override
  public void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
    if (server != null) {
      CountDownLatch latch = new CountDownLatch(1);
      server.close((asyncResult) -> {
        assertTrue(asyncResult.succeeded());
        latch.countDown();
      });
      awaitLatch(latch);
    }
    super.tearDown();
  }

  protected void testRequest(HttpMethod method, String path, int statusCode, String statusMessage) throws Exception {
    testRequest(method, path, null, statusCode, statusMessage, null);
  }

  protected void testRequests(Collection<HttpMethod> methods, String path, int statusCode, String statusMessage) throws Exception {
    testRequests(methods, path, null, statusCode, statusMessage, null);
  }

  protected void testRequest(HttpMethod method, String path, int statusCode, String statusMessage,
                             String responseBody) throws Exception {
    testRequest(method, path, null, statusCode, statusMessage, responseBody);
  }

  protected void testRequests(Collection<HttpMethod> methods, String path, int statusCode, String statusMessage,
                             String responseBody) throws Exception {
    testRequests(methods, path, null, statusCode, statusMessage, responseBody);
  }

  protected void testRequest(HttpMethod method, String path, int statusCode, String statusMessage,
                             Buffer responseBody) throws Exception {
    testRequestBuffer(method, path, null, null, statusCode, statusMessage, responseBody);
  }

  protected void testRequests(Collection<HttpMethod> methods, String path, int statusCode, String statusMessage,
                             Buffer responseBody) throws Exception {
    testRequestsBuffer(methods, path, null, null, statusCode, statusMessage, responseBody);
  }


  protected void testRequestWithContentType(HttpMethod method, String path, String contentType, int statusCode, String statusMessage) throws Exception {
    testRequest(method, path, req -> req.putHeader("content-type", contentType), statusCode, statusMessage, null);
  }

  protected void testRequestWithAccepts(HttpMethod method, String path, String accepts, int statusCode, String statusMessage) throws Exception {
    testRequest(method, path, req -> req.putHeader("accept", accepts), statusCode, statusMessage, null);
  }

  protected void testRequestWithCookies(HttpMethod method, String path, String cookieHeader, int statusCode, String statusMessage) throws Exception {
    testRequest(method, path, req -> req.putHeader("cookie", cookieHeader), statusCode, statusMessage, null);
  }

  protected void testRequest(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction,
                             int statusCode, String statusMessage,
                             String responseBody) throws Exception {
    testRequest(method, path, requestAction, null, statusCode, statusMessage, responseBody);
  }

  protected void testRequests(Collection<HttpMethod> methods, String path, Consumer<HttpClientRequest> requestAction,
                             int statusCode, String statusMessage,
                             String responseBody) throws Exception {
    testRequests(methods, path, requestAction, null, statusCode, statusMessage, responseBody);
  }

  protected void testRequest(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction, Consumer<HttpClientResponse> responseAction,
                             int statusCode, String statusMessage,
                             String responseBody) throws Exception {
    testRequestBuffer(method, path, requestAction, responseAction, statusCode, statusMessage, responseBody != null ? Buffer.buffer(responseBody) : null);
  }

  protected void testRequests(Collection<HttpMethod> methods, String path, Consumer<HttpClientRequest> requestAction, Consumer<HttpClientResponse> responseAction,
                             int statusCode, String statusMessage,
                             String responseBody) throws Exception {
    testRequestsBuffer(methods, path, requestAction, responseAction, statusCode, statusMessage, responseBody != null ? Buffer.buffer(responseBody) : null);
  }


  protected void testRequestBuffer(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction, Consumer<HttpClientResponse> responseAction,
                                   int statusCode, String statusMessage,
                                   Buffer responseBodyBuffer) throws Exception {
    testRequestsBuffer(client, Collections.singleton(method), 8080, path, requestAction, responseAction, statusCode, statusMessage, responseBodyBuffer);
  }

  protected void testRequestsBuffer(Collection<HttpMethod> methods, String path, Consumer<HttpClientRequest> requestAction, Consumer<HttpClientResponse> responseAction,
                                   int statusCode, String statusMessage,
                                   Buffer responseBodyBuffer) throws Exception {
    testRequestsBuffer(client, methods, 8080, path, requestAction, responseAction, statusCode, statusMessage, responseBodyBuffer);
  }

  protected void testRequestBuffer(HttpClient client, HttpMethod method, int port, String path, Consumer<HttpClientRequest> requestAction, Consumer<HttpClientResponse> responseAction,
                                   int statusCode, String statusMessage,
                                   Buffer responseBodyBuffer) throws Exception {
    testRequestsBuffer(client, Collections.singleton(method), port, path, requestAction, responseAction, statusCode, statusMessage, responseBodyBuffer);
  }

  protected void testRequestsBuffer(HttpClient client, Collection<HttpMethod> methods, int port, String path, Consumer<HttpClientRequest> requestAction, Consumer<HttpClientResponse> responseAction,
                                    int statusCode, String statusMessage,
                                    Buffer responseBodyBuffer) throws Exception {
    CountDownLatch latch = new CountDownLatch(methods.size());
    methods.forEach(method -> {
      HttpClientRequest req = client.request(method, port, "localhost", path, resp -> {
        assertEquals(statusCode, resp.statusCode());
        assertEquals(statusMessage, resp.statusMessage());
        if (responseAction != null) {
          responseAction.accept(resp);
        }
        if (responseBodyBuffer == null) {
          latch.countDown();
        } else {
          resp.bodyHandler(buff -> {
            assertEquals(responseBodyBuffer, buff);
            latch.countDown();
          });
        }
      });
      if (requestAction != null) {
        requestAction.accept(req);
      }
      req.end();
    });
    awaitLatch(latch);
  }
}
