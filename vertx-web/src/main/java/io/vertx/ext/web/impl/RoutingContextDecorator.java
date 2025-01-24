package io.vertx.ext.web.impl;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.*;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Decorate a {@link RoutingContext} and simply delegate all method calls to the decorated handler
 *
 * @author <a href="mailto:stephane.bastian.dev@gmail.com>Stéphane Bastian</a>
 *
 */
public class RoutingContextDecorator implements RoutingContextInternal {

  private final Route currentRoute;
  private final RoutingContextInternal decoratedContext;

  public RoutingContextDecorator(Route currentRoute, RoutingContextInternal decoratedContext) {
    Objects.requireNonNull(currentRoute);
    Objects.requireNonNull(decoratedContext);
    this.currentRoute = currentRoute;
    this.decoratedContext = decoratedContext;
  }

  @Override
  public RoutingContextInternal visitHandler(int id) {
    return decoratedContext.visitHandler(id);
  }

  @Override
  public boolean seenHandler(int id) {
    return decoratedContext.seenHandler(id);
  }

  @Override
  public RoutingContextInternal setMatchFailure(int matchFailure) {
    return decoratedContext.setMatchFailure(matchFailure);
  }

  @Override
  public int addBodyEndHandler(Handler<Void> handler) {
    return decoratedContext.addBodyEndHandler(handler);
  }

  @Override
  @Deprecated
  public RoutingContext addCookie(io.vertx.core.http.Cookie cookie) {
    return decoratedContext.addCookie(cookie);
  }

  @Override
  public int addEndHandler(Handler<AsyncResult<Void>> handler) {
    return decoratedContext.addEndHandler(handler);
  }

  @Override
  public int addHeadersEndHandler(Handler<Void> handler) {
    return decoratedContext.addHeadersEndHandler(handler);
  }

  @Override
  @Deprecated
  public int cookieCount() {
    return decoratedContext.cookieCount();
  }

  @Override
  @Deprecated
  public Map<String, io.vertx.core.http.Cookie> cookieMap() {
    return decoratedContext.cookieMap();
  }

  @Override
  public Route currentRoute() {
    return currentRoute;
  }

  @Override
  public Router currentRouter() {
    return decoratedContext.currentRouter();
  }

  @Override
  public @Nullable RoutingContextInternal parent() {
    return decoratedContext.parent();
  }

  @Override
  public Map<String, Object> data() {
    return decoratedContext.data();
  }

  @Override
  public void fail(int statusCode) {
    // make sure the fail handler run on the correct context
    vertx().runOnContext(future -> decoratedContext.fail(statusCode));
  }

  @Override
  public void fail(Throwable throwable) {
    // make sure the fail handler run on the correct context
    vertx().runOnContext(future -> decoratedContext.fail(throwable));
  }

  @Override
  public void fail(int statusCode, Throwable throwable) {
    vertx().runOnContext(future -> decoratedContext.fail(statusCode, throwable));
  }

  @Override
  public boolean failed() {
    return decoratedContext.failed();
  }

  @Override
  public Throwable failure() {
    return decoratedContext.failure();
  }

  @Override
  public List<FileUpload> fileUploads() {
    return decoratedContext.fileUploads();
  }

  @Override
  public void cancelAndCleanupFileUploads() {
    decoratedContext.cancelAndCleanupFileUploads();
  }

  @Override
  public <T> T get(String key) {
    return decoratedContext.get(key);
  }

  @Override
  public <T> T get(String key, T defaultValue) {
    return decoratedContext.get(key, defaultValue);
  }

  @Override
  public <T> T remove(String key) {
    return decoratedContext.remove(key);
  }

  @Override
  public String getAcceptableContentType() {
    return decoratedContext.getAcceptableContentType();
  }

  @Override
  public RequestBody body() {
    return decoratedContext.body();
  }

  @Override
  @Deprecated
  public Cookie getCookie(String name) {
    return decoratedContext.getCookie(name);
  }

  @Override
  public String mountPoint() {
    return decoratedContext.mountPoint();
  }

  @Override
  public void next() {
    // make sure the next handler run on the correct context
    vertx().runOnContext(future -> decoratedContext.next());
  }

  @Override
  public String normalizedPath() {
    return decoratedContext.normalizedPath();
  }

  @Override
  public RoutingContext put(String key, Object obj) {
    return decoratedContext.put(key, obj);
  }

  @Override
  public boolean removeBodyEndHandler(int handlerID) {
    return decoratedContext.removeBodyEndHandler(handlerID);
  }

  @Override
  @Deprecated
  public Cookie removeCookie(String name, boolean invalidate) {
    return decoratedContext.removeCookie(name, invalidate);
  }

  @Override
  public boolean removeEndHandler(int handlerID) {
    return decoratedContext.removeEndHandler(handlerID);
  }

  @Override
  public boolean removeHeadersEndHandler(int handlerID) {
    return decoratedContext.removeHeadersEndHandler(handlerID);
  }

  @Override
  public HttpServerRequest request() {
    return decoratedContext.request();
  }

  @Override
  public HttpServerResponse response() {
    return decoratedContext.response();
  }

  @Override
  public User user() {
    return decoratedContext.user();
  }

  @Override
  public UserContext userContext() {
    return decoratedContext.userContext();
  }

  @Override
  public Session session() {
    return decoratedContext.session();
  }

  @Override
  public boolean isSessionAccessed() {
    return decoratedContext.isSessionAccessed();
  }

  @Override
  public ParsedHeaderValues parsedHeaders() {
    return decoratedContext.parsedHeaders();
  }

  @Override
  public void setAcceptableContentType(String contentType) {
    decoratedContext.setAcceptableContentType(contentType);
  }

  @Override
  public void reroute(HttpMethod method, String path) {
    decoratedContext.reroute(method, path);
  }

  @Override
  public void addOrReplacePathParam(String name, String value) {
    decoratedContext.addOrReplacePathParam(name, value);
  }

  @Override
  public boolean removePathParam(String s) {
    return decoratedContext.removePathParam(s);
  }

  @Override
  public Map<String, String> pathParams() {
    return decoratedContext.pathParams();
  }

  @Override
  public @Nullable String pathParam(String name) {
    return decoratedContext.pathParam(name);
  }

  @Override
  public MultiMap queryParams() {
    return decoratedContext.queryParams();
  }

  @Override
  public MultiMap queryParams(Charset charset) {
    return decoratedContext.queryParams(charset);
  }

  @Override
  public @Nullable List<String> queryParam(String query) {
    return decoratedContext.queryParam(query);
  }

  @Override
  @Deprecated
  public void setBody(Buffer body) {
    decoratedContext.setBody(body);
  }

  @Override
  @Deprecated
  public void setSession(Session session) {
    decoratedContext.setSession(session);
  }

  @Override
  public int restIndex() {
    return decoratedContext.restIndex();
  }

  @Override
  public boolean normalizedMatch() {
    return decoratedContext.normalizedMatch();
  }

  @Override
  public void setUser(User user) {
    decoratedContext.setUser(user);
  }

  @Override
  public void clearUser() {
    decoratedContext.clearUser();
  }

  @Override
  public int statusCode() {
    return decoratedContext.statusCode();
  }

  @Override
  public Vertx vertx() {
    return decoratedContext.vertx();
  }

}
