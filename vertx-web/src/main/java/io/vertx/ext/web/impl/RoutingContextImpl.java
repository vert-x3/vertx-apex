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

package io.vertx.ext.web.impl;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.impl.ContextInternal;
import io.vertx.ext.auth.common.UserContext;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.UserHolder;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.stream.Collectors;

import static io.vertx.ext.web.handler.impl.SessionHandlerImpl.SESSION_USER_HOLDER_KEY;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class RoutingContextImpl extends RoutingContextImplBase {

  private static final AtomicIntegerFieldUpdater<RoutingContextImpl> HANDLER_SEQ =
    AtomicIntegerFieldUpdater.newUpdater(RoutingContextImpl.class, "handlerSeq");

  private final RouterImpl router;
  private final HttpServerRequest request;
  private final RequestBodyImpl body;

  private volatile int handlerSeq;

  private Map<String, Object> data;
  private Map<String, String> pathParams;
  private MultiMap queryParams;
  private SparseArray<Handler<Void>> headersEndHandlers;
  private SparseArray<Handler<Void>> bodyEndHandlers;
  // clean up handlers
  private SparseArray<Handler<AsyncResult<Void>>> endHandlers;

  private Throwable failure;
  private int statusCode = -1;
  private String normalizedPath;
  private String acceptableContentType;
  private ParsableHeaderValuesContainer parsedHeaders;

  private final AtomicBoolean cleanup = new AtomicBoolean(false);
  private List<FileUpload> fileUploads;
  private Session session;
  private UserContext identity;

  private volatile boolean isSessionAccessed = false;
  private volatile boolean endHandlerCalled = false;

  public RoutingContextImpl(String mountPoint, RouterImpl router, HttpServerRequest request, Set<RouteImpl> routes) {
    super(mountPoint, routes, router);
    this.router = router;
    this.request = new HttpServerRequestWrapper(request, router.getAllowForward());
    this.body = new RequestBodyImpl(this);

    final String path = request.path();

    if (path == null || path.length() == 0) {
      // HTTP paths must start with a '/'
      fail(400);
    } else if (path.charAt(0) != '/') {
      // For compatiblity we return `Not Found` when a path does not start with `/`
      fail(404);
    }
  }

  private String ensureNotNull(String string){
    return string == null ? "" : string;
  }

  private void fillParsedHeaders(HttpServerRequest request) {
    String accept = request.getHeader(HttpHeaders.ACCEPT);
    String acceptCharset = request.getHeader (HttpHeaders.ACCEPT_CHARSET);
    String acceptEncoding = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
    String acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
    String contentType = ensureNotNull(request.getHeader(HttpHeaders.CONTENT_TYPE));

    parsedHeaders = new ParsableHeaderValuesContainer(
        HeaderParser.sort(HeaderParser.convertToParsedHeaderValues(accept, ParsableMIMEValue::new)),
        HeaderParser.sort(HeaderParser.convertToParsedHeaderValues(acceptCharset, ParsableHeaderValue::new)),
        HeaderParser.sort(HeaderParser.convertToParsedHeaderValues(acceptEncoding, ParsableHeaderValue::new)),
        HeaderParser.sort(HeaderParser.convertToParsedHeaderValues(acceptLanguage, ParsableLanguageValue::new)),
        new ParsableMIMEValue(contentType)
    );
  }

  @Override
  public HttpServerRequest request() {
    return request;
  }

  @Override
  public HttpServerResponse response() {
    return request.response();
  }

  @Override
  public Throwable failure() {
    return failure;
  }

  @Override
  public int statusCode() {
    return statusCode;
  }

  @Override
  public boolean failed() {
    return failure != null || statusCode != -1;
  }

  @Override
  public void next() {
    if (!iterateNext()) {
      checkHandleNoMatch();
    }
  }

  @Override
  public void onContinue() {
    next();
  }

  private void checkHandleNoMatch() {
    // Next called but no more matching routes
    if (failed()) {
      // Send back FAILURE
      unhandledFailure(statusCode, failure, router);
    } else {
      Handler<RoutingContext> handler = router.getErrorHandlerByStatusCode(this.matchFailure);
      this.statusCode = this.matchFailure;
      if (handler == null) { // Default 404 handling
        // Send back empty default response with status code
        this.response().setStatusCode(matchFailure);
        if (this.request().method() != HttpMethod.HEAD && matchFailure == 404) {
          // If it's a 404 let's send a body too
          this.response()
            .putHeader(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8")
            .end(DEFAULT_404);
        } else if (this.request().method() != HttpMethod.HEAD && matchFailure == 405) {
          // If it's a 405 let's send a body too
          this.response()
            .putHeader(HttpHeaderNames.ALLOW, allowedMethods.stream().map(HttpMethod::name).collect(Collectors.joining(","))).end();
        } else {
          this.response().end();
        }
      } else {
        handler.handle(this);
      }
    }
  }

  @Override
  public void fail(int statusCode) {
    this.statusCode = statusCode;
    doFail();
  }

  @Override
  public void fail(Throwable t) {
    if (t instanceof HttpException) {
      this.fail(((HttpException) t).getStatusCode(), t);
    } else {
      this.fail(500, t);
    }
  }

  @Override
  public void fail(int statusCode, Throwable throwable) {
    this.statusCode = statusCode;
    this.failure = throwable == null ? new NullPointerException() : throwable;
    if (LOG.isDebugEnabled()) {
      LOG.debug("RoutingContext failure (" + statusCode + ")", failure);
    }
    doFail();
  }

  @Override
  public RoutingContext put(String key, Object obj) {
    getData().put(key, obj);
    return this;
  }

  @Override
  public Vertx vertx() {
    return router.vertx();
  }

  @Override
  public @Nullable RoutingContextInternal parent() {
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    if (data == null) {
      return null;
    } else {
      return (T) getData().get(key);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String key, T defaultValue) {
    if (data == null) {
      return defaultValue;
    } else {
      Map<String, ?> data = getData();
      if (data.containsKey(key)) {
        return (T) data.get(key);
      } else {
        return defaultValue;
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T remove(String key) {
    if (data == null) {
      return null;
    } else {
      return (T) getData().remove(key);
    }
  }

  @Override
  public Map<String, Object> data() {
    return getData();
  }

  @Override
  public String normalizedPath() {
    if (normalizedPath == null) {
      String path = request.path();
      if (path == null) {
        normalizedPath = "/";
      } else {
        normalizedPath = HttpUtils.normalizePath(path);
      }
    }
    return normalizedPath;
  }

  @Override
  public RequestBody body() {
    return body;
  }

  @Override
  public void setBody(Buffer body) {
    this.body.setBuffer(body);
  }

  @Override
  public List<FileUpload> fileUploads() {
    if (fileUploads == null) {
      fileUploads = new ArrayList<>();
    }
    return fileUploads;
  }

  /**
   * Cancel all unfinished file upload in progress and delete all uploaded files.
   */
  public void cancelAndCleanupFileUploads() {
    if (cleanup.compareAndSet(false, true)) {
      for (FileUpload fileUpload : fileUploads()) {
        if (!fileUpload.cancel()) {
          fileUpload.delete();
        }
      }
    }
  }

  @Override
  public void setSession(Session session) {
    this.session = session;
    // attempt to load the user from the session if one exists
    UserHolder holder = session.get(SESSION_USER_HOLDER_KEY);
    if (holder != null) {
      holder.refresh(this);
    }
  }

  @Override
  public Session session() {
    this.isSessionAccessed = true;
    return session;
  }

  @Override
  public boolean isSessionAccessed(){
    return isSessionAccessed;
  }

  @Override
  public UserContext user() {
    if (identity == null) {
      identity = new UserContextImpl(this);
    }
    return identity;
  }

  @Override
  public String getAcceptableContentType() {
    return acceptableContentType;
  }

  @Override
  public void setAcceptableContentType(String contentType) {
    this.acceptableContentType = contentType;
  }

  @Override
  public ParsableHeaderValuesContainer parsedHeaders() {
    if (parsedHeaders == null) {
      fillParsedHeaders(request);
    }
    return parsedHeaders;
  }

  @Override
  public int addHeadersEndHandler(Handler<Void> handler) {
    int seq = nextHandlerSeq();
    getHeadersEndHandlers().put(seq, handler);
    return seq;
  }

  @Override
  public boolean removeHeadersEndHandler(int handlerID) {
    return getHeadersEndHandlers().remove(handlerID) != null;
  }

  @Override
  public int addBodyEndHandler(Handler<Void> handler) {
    int seq = nextHandlerSeq();
    getBodyEndHandlers().put(seq, handler);
    return seq;
  }

  @Override
  public boolean removeBodyEndHandler(int handlerID) {
    return getBodyEndHandlers().remove(handlerID) != null;
  }

  @Override
  public int addEndHandler(Handler<AsyncResult<Void>> handler) {
    int seq = nextHandlerSeq();
    getEndHandlers().put(seq, handler);
    return seq;
  }

  @Override
  public boolean removeEndHandler(int handlerID) {
    return getEndHandlers().remove(handlerID) != null;
  }

  @Override
  public void reroute(HttpMethod method, String path) {
    if (path.charAt(0) != '/') {
      throw new IllegalArgumentException("path must start with '/'");
    }
    // change the method and path of the request
    ((HttpServerRequestWrapper) request).changeTo(method, path);
    // we need to reset the normalized path
    normalizedPath = null;
    // we also need to reset any previous status
    statusCode = -1;
    // we need to reset any response headers
    response().headers().clear();
    // reset the end handlers
    if (headersEndHandlers != null) {
      headersEndHandlers.clear();
    }
    if (bodyEndHandlers != null) {
      bodyEndHandlers.clear();
    }

    failure = null;
    restart();
  }

  @Override
  public Map<String, String> pathParams() {
    return getPathParams();
  }

  @Override
  public @Nullable String pathParam(String name) {
    return getPathParams().get(name);
  }

  @Override
  public MultiMap queryParams() {
    return getQueryParams(null);
  }

  @Override
  public MultiMap queryParams(Charset charset) {
    return getQueryParams(charset);
  }

  @Override
  public @Nullable List<String> queryParam(String query) {
    return queryParams().getAll(query);
  }

  private MultiMap getQueryParams(Charset charset) {
    // Check if query params are already parsed
    if (charset != null || queryParams == null) {
      try {
        // Decode query parameters and put inside context.queryParams
        if (charset == null) {
          queryParams = MultiMap.caseInsensitiveMultiMap();
          Map<String, List<String>> decodedParams = new QueryStringDecoder(request.uri()).parameters();
          for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
            queryParams.add(entry.getKey(), entry.getValue());
          }
        } else {
          MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();
          Map<String, List<String>> decodedParams = new QueryStringDecoder(request.uri(), charset).parameters();
          for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
            queryParams.add(entry.getKey(), entry.getValue());
          }
          return queryParams;
        }
      } catch (IllegalArgumentException e) {
        throw new HttpException(400, "Error while decoding query params", e);
      }
    }
    return queryParams;
  }

  private Map<String, String> getPathParams() {
    if (pathParams == null) {
      pathParams = new HashMap<>();
    }
    return pathParams;
  }

  private SparseArray<Handler<Void>> getHeadersEndHandlers() {
    if (headersEndHandlers == null) {
      headersEndHandlers = new SparseArray<>();
      // order is important we we should traverse backwards
      response().headersEndHandler(v -> headersEndHandlers.forEachInReverseOrder(handler -> handler.handle(null)));
    }
    return headersEndHandlers;
  }

  private SparseArray<Handler<Void>> getBodyEndHandlers() {
    if (bodyEndHandlers == null) {
      bodyEndHandlers = new SparseArray<>();
      // order is important we we should traverse backwards
      response().bodyEndHandler(v -> bodyEndHandlers.forEachInReverseOrder(handler -> handler.handle(null)));
    }
    return bodyEndHandlers;
  }

  private SparseArray<Handler<AsyncResult<Void>>> getEndHandlers() {
    if (endHandlers == null) {
      // order is important as we should traverse backwards
      endHandlers = new SparseArray<>();
      final ContextInternal ctx = (ContextInternal) vertx().getOrCreateContext();

      final Handler<Void> endHandler = v -> {
        if (!endHandlerCalled) {
          endHandlerCalled = true;
          endHandlers.forEachInReverseOrder(handler -> handler.handle(ctx.succeededFuture()));
        }
      };

      final Handler<Throwable> exceptionHandler = cause -> {
        if (!endHandlerCalled) {
          endHandlerCalled = true;
          endHandlers.forEachInReverseOrder(handler -> handler.handle(ctx.failedFuture(cause)));
        }
      };

      final Handler<Void> closeHandler = cause -> {
        if (!endHandlerCalled) {
          endHandlerCalled = true;
          endHandlers.forEachInReverseOrder(handler -> handler.handle(ctx.failedFuture("Connection closed")));
        }
      };

      response()
        .endHandler(endHandler)
        .exceptionHandler(exceptionHandler)
        .closeHandler(closeHandler);
    }

    return endHandlers;
  }

  private void doFail() {
    this.iter = router.iterator();
    currentRoute = null;
    next();
  }

  private Map<String, Object> getData() {
    if (data == null) {
      data = new HashMap<>();
    }
    return data;
  }

  private int nextHandlerSeq() {
    int seq = HANDLER_SEQ.incrementAndGet(this);
    if (seq == Integer.MAX_VALUE) {
      throw new IllegalStateException("Too many header/body end handlers!");
    }
    return seq;
  }

  private static final String DEFAULT_404 =
    "<html><body><h1>Resource not found</h1></body></html>";

}
