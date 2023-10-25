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

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.auth.common.UserContext;
import io.vertx.ext.web.*;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class RoutingContextWrapper extends RoutingContextImplBase {

  protected final RoutingContextInternal inner;
  private final String mountPoint;

  public RoutingContextWrapper(String mountPoint, Set<RouteImpl> iter, RoutingContextInternal inner, Router currentRouter) {
    super(mountPoint, iter, currentRouter);
    this.inner = inner;
    String parentMountPoint = inner.mountPoint();
    if (parentMountPoint == null) {
      // just use the override
      this.mountPoint = mountPoint;
    } else {
      // special cases:
      // * when a sub router is mounting on / basically it's telling that it wants to use the parent mount
      if ("/".equals(mountPoint)) {
        this.mountPoint = parentMountPoint;
      } else
        // * when the parent mount is / basically it's telling that it wants to use the sub router mount
        if ("/".equals(parentMountPoint)) {
          this.mountPoint = mountPoint;
        } else
        // * otherwise it's extending the parent path
        if (parentMountPoint.endsWith("/")) {
          this.mountPoint = parentMountPoint.substring(0, parentMountPoint.length() - 1) + mountPoint;
        } else {
          this.mountPoint = parentMountPoint + mountPoint;
        }
    }
  }

  @Override
  public synchronized RoutingContextInternal visitHandler(int id) {
    return inner.visitHandler(id);
  }

  @Override
  public boolean seenHandler(int id) {
    return inner.seenHandler(id);
  }

  @Override
  public HttpServerRequest request() {
    return inner.request();
  }

  @Override
  public HttpServerResponse response() {
    return inner.response();
  }

  @Override
  public void fail(int statusCode) {
    inner.fail(statusCode);
  }

  @Override
  public void fail(Throwable throwable) {
    inner.fail(throwable);
  }

  @Override
  public void fail(int statusCode, Throwable throwable) {
    inner.fail(statusCode, throwable);
  }

  @Override
  public RoutingContext put(String key, Object obj) {
    inner.put(key, obj);
    return this;
  }

  @Override
  public <T> T get(String key) {
    return inner.get(key);
  }

  @Override
  public <T> T get(String key, T defaultValue) {
    return inner.get(key, defaultValue);
  }

  @Override
  public <T> T remove(String key) {
    return inner.remove(key);
  }

  @Override
  public Map<String, Object> data() {
    return inner.data();
  }

  @Override
  public Vertx vertx() {
    return inner.vertx();
  }

  @Override
  public int addHeadersEndHandler(Handler<Void> handler) {
    return inner.addHeadersEndHandler(handler);
  }

  @Override
  public boolean removeHeadersEndHandler(int handlerID) {
    return inner.removeHeadersEndHandler(handlerID);
  }

  @Override
  public int addBodyEndHandler(Handler<Void> handler) {
    return inner.addBodyEndHandler(handler);
  }

  @Override
  public boolean removeBodyEndHandler(int handlerID) {
    return inner.removeBodyEndHandler(handlerID);
  }

  @Override
  public int addEndHandler(Handler<AsyncResult<Void>> handler) {
    return inner.addEndHandler(handler);
  }

  @Override
  public boolean removeEndHandler(int handlerID) {
    return inner.removeEndHandler(handlerID);
  }

  @Override
  public void setSession(Session session) {
    inner.setSession(session);
  }

  @Override
  public Session session() {
    return inner.session();
  }

  @Override
  public boolean isSessionAccessed() {
    return inner.isSessionAccessed();
  }

  @Override
  public UserContext user() {
    return inner.user();
  }

  @Override
  public void next() {
    if (!super.iterateNext()) {
      // We didn't route request to anything so go to parent,
      // but also propagate the current status
      inner.setMatchFailure(matchFailure);
      inner.next();
    }
  }

  @Override
  public void onContinue() {
    next();
  }

  @Override
  public boolean failed() {
    return inner.failed();
  }

  @Override
  public Throwable failure() {
    return inner.failure();
  }

  @Override
  public int statusCode() {
    return inner.statusCode();
  }

  @Override
  public String mountPoint() {
    return mountPoint;
  }

  @Override
  public RoutingContextInternal parent() {
    return inner;
  }

  @Override
  public String normalizedPath() {
    return inner.normalizedPath();
  }

  @Override
  public RequestBody body() {
    return inner.body();
  }

  @Override
  public void setBody(Buffer body) {
    inner.setBody(body);
  }

  @Override
  public List<FileUpload> fileUploads() {
    return inner.fileUploads();
  }

  @Override
  public void cancelAndCleanupFileUploads() {
    inner.cancelAndCleanupFileUploads();
  }

  @Override
  public String getAcceptableContentType() {
    return inner.getAcceptableContentType();
  }

  @Override
  public ParsedHeaderValues parsedHeaders() {
    return inner.parsedHeaders();
  }

  @Override
  public void setAcceptableContentType(String contentType) {
    inner.setAcceptableContentType(contentType);
  }

  @Override
  public void reroute(HttpMethod method, String path) {
    inner.reroute(method, path);
  }

  @Override
  public Map<String, String> pathParams() {
    return inner.pathParams();
  }

  @Override
  public @Nullable String pathParam(String name) {
    return inner.pathParam(name);
  }

  @Override
  public MultiMap queryParams() {
    return inner.queryParams();
  }

  @Override
  public MultiMap queryParams(Charset charset) {
    return inner.queryParams(charset);
  }

  @Override
  public @Nullable List<String> queryParam(String query) {
    return inner.queryParam(query);
  }

}
