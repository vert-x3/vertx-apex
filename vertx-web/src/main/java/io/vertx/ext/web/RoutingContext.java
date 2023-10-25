/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.web;

import io.vertx.codegen.annotations.*;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.auth.common.AuthenticationContext;
import io.vertx.ext.auth.common.UserContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;
import io.vertx.ext.web.impl.Utils;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.vertx.codegen.annotations.GenIgnore.PERMITTED_TYPE;

/**
 * Represents the context for the handling of a request in Vert.x-Web.
 * <p>
 * A new instance is created for each HTTP request that is received in the
 * {@link Router#handle(Object)} of the router.
 * <p>
 * The same instance is passed to any matching request or failure handlers during the routing of the request or
 * failure.
 * <p>
 * The context provides access to the {@link HttpServerRequest} and {@link HttpServerResponse}
 * and allows you to maintain arbitrary data that lives for the lifetime of the context. Contexts are discarded once they
 * have been routed to the handler for the request.
 * <p>
 * The context also provides access to the {@link Session}, cookies and body for the request, given the correct handlers
 * in the application.
 * <p>
 * If you use the internal error handler
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface RoutingContext extends AuthenticationContext {

  /**
   * @return the HTTP request object
   */
  @CacheReturn
  HttpServerRequest request();

  /**
   * @return the HTTP response object
   */
  @CacheReturn
  HttpServerResponse response();

  /**
   * Tell the router to route this context to the next matching route (if any).
   * This method, if called, does not need to be called during the execution of the handler, it can be called
   * some arbitrary time later, if required.
   * <p>
   * If next is not called for a handler then the handler should make sure it ends the response or no response
   * will be sent.
   */
  void next();

  /**
   * Fail the context with the specified status code.
   * <p>
   * This will cause the router to route the context to any matching failure handlers for the request. If no failure handlers
   * match It will trigger the error handler matching the status code. You can define such error handler with
   * {@link Router#errorHandler(int, Handler)}. If no error handler is not defined, It will send a default failure response with provided status code.
   *
   * @param statusCode  the HTTP status code
   */
  void fail(int statusCode);

  /**
   * Fail the context with the specified throwable and 500 status code.
   * <p>
   * This will cause the router to route the context to any matching failure handlers for the request. If no failure handlers
   * match It will trigger the error handler matching the status code. You can define such error handler with
   * {@link Router#errorHandler(int, Handler)}. If no error handler is not defined, It will send a default failure response with 500 status code.
   *
   * @param throwable  a throwable representing the failure
   */
  void fail(Throwable throwable);

  /**
   * Fail the context with the specified throwable and the specified the status code.
   * <p>
   * This will cause the router to route the context to any matching failure handlers for the request. If no failure handlers
   * match It will trigger the error handler matching the status code. You can define such error handler with
   * {@link Router#errorHandler(int, Handler)}. If no error handler is not defined, It will send a default failure response with provided status code.
   *
   * @param statusCode the HTTP status code
   * @param throwable a throwable representing the failure
   */
  void fail(int statusCode, Throwable throwable);

  /**
   * Put some arbitrary data in the context. This will be available in any handlers that receive the context.
   *
   * @param key  the key for the data
   * @param obj  the data
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  RoutingContext put(String key, Object obj);

  /**
   * Get some data from the context. The data is available in any handlers that receive the context.
   *
   * @param key  the key for the data
   * @param <T>  the type of the data
   * @return  the data
   * @throws ClassCastException if the data is not of the expected type
   */
  <T> @Nullable T get(String key);

  /**
   * Get some data from the context. The data is available in any handlers that receive the context.
   *
   * @param key  the key for the data
   * @param <T>  the type of the data
   * @param defaultValue when the underlying data doesn't contain the key this will be the return value.
   * @return  the data
   * @throws ClassCastException if the data is not of the expected type
   */
  <T> T get(String key, T defaultValue);

  /**
   * Remove some data from the context. The data is available in any handlers that receive the context.
   *
   * @param key  the key for the data
   * @param <T>  the type of the data
   * @return  the previous data associated with the key
   * @throws ClassCastException if the data is not of the expected type
   */
  <T> @Nullable T remove(String key);

  /**
   * @return all the context data as a map
   */
  @GenIgnore(PERMITTED_TYPE)
  <T> Map<String, T> data();

  /**
   * @return the Vert.x instance associated to the initiating {@link Router} for this context
   */
  @CacheReturn
  Vertx vertx();

  /**
   * @return the mount point for this router. It will be null for a top level router. For a sub-router it will be the path
   * at which the subrouter was mounted.
   */
  @Nullable String mountPoint();

  /**
   * @return the current route this context is being routed through.
   */
  @Nullable Route currentRoute();

  /**
   * Normalizes a path as per <a href="http://tools.ietf.org/html/rfc3986#section-5.2.4>rfc3986</a>.
   *
   * There are 2 extra transformations that are not part of the spec but kept for backwards compatibility:
   *
   * double slash // will be converted to single slash and the path will always start with slash.
   *
   * Null paths are normalized to {@code /}.
   *
   * @return normalized path
   */
  String normalizedPath();

  RequestBody body();

  /**
   * @return a list of {@link FileUpload} (if any) for the request. The context must have first been routed to a
   * {@link io.vertx.ext.web.handler.BodyHandler} for this to work.
   */
  List<FileUpload> fileUploads();

  /**
   * Cancel all unfinished file upload in progress and delete all uploaded files.
   */
  void cancelAndCleanupFileUploads();

  /**
   * Get the session. The context must have first been routed to a {@link io.vertx.ext.web.handler.SessionHandler}
   * for this to be populated.
   * Sessions live for a browser session, and are maintained by session cookies.
   * @return  the session.
   */
  @Nullable Session session();

  /**
   * Whether the {@link RoutingContext#session()} has been already called or not. This is usually used by the
   * {@link io.vertx.ext.web.handler.SessionHandler}.
   *
   * @return true if the session has been accessed.
   */
  boolean isSessionAccessed();

  /**
   * Control the user associated with this request. The user context allows accessing the security user object as well
   * as perform authentication refreshes, logout and other operations.
   * @return the user context
   */
  UserContext user();

  /**
   * If the context is being routed to failure handlers after a failure has been triggered by calling
   * {@link #fail(Throwable)} then this will return that throwable. It can be used by failure handlers to render a response,
   * e.g. create a failure response page.
   *
   * @return  the throwable used when signalling failure
   */
  @CacheReturn
  @Nullable
  Throwable failure();

  /**
   * If the context is being routed to failure handlers after a failure has been triggered by calling
   * {@link #fail(int)}  then this will return that status code.  It can be used by failure handlers to render a response,
   * e.g. create a failure response page.
   *
   * When the status code has not been set yet (it is undefined) its value will be -1.
   *
   * @return  the status code used when signalling failure
   */
  @CacheReturn
  int statusCode();

  /**
   * If the route specifies produces matches, e.g. produces `text/html` and `text/plain`, and the `accept` header
   * matches one or more of these then this returns the most acceptable match.
   *
   * @return  the most acceptable content type.
   */
  @Nullable String getAcceptableContentType();

  /**
   * The headers:
   * <ol>
   * <li>Accept</li>
   * <li>Accept-Charset</li>
   * <li>Accept-Encoding</li>
   * <li>Accept-Language</li>
   * <li>Content-Type</li>
   * </ol>
   * Parsed into {@link ParsedHeaderValue}
   * @return A container with the parsed headers.
   */
  @CacheReturn
  ParsedHeaderValues parsedHeaders();

  /**
   * Add a handler that will be called just before headers are written to the response. This gives you a hook where
   * you can write any extra headers before the response has been written when it will be too late.
   *
   * @param handler  the handler
   * @return  the id of the handler. This can be used if you later want to remove the handler.
   */
  int addHeadersEndHandler(Handler<Void> handler);

  /**
   * Remove a headers end handler
   *
   * @param handlerID  the id as returned from {@link io.vertx.ext.web.RoutingContext#addHeadersEndHandler(Handler)}.
   * @return true if the handler existed and was removed, false otherwise
   */
  boolean removeHeadersEndHandler(int handlerID);

  /**
   * Provides a handler that will be called after the last part of the body is written to the wire.
   * The handler is called asynchronously of when the response has been received by the client.
   * This provides a hook allowing you to do more operations once the request has been sent over the wire.
   * Do not use this for resource cleanup as this handler might never get called (e.g. if the connection is reset).
   *
   * @param handler  the handler
   * @return  the id of the handler. This can be used if you later want to remove the handler.
   */
  int addBodyEndHandler(Handler<Void> handler);

  /**
   * Remove a body end handler
   *
   * @param handlerID  the id as returned from {@link io.vertx.ext.web.RoutingContext#addBodyEndHandler(Handler)}.
   * @return true if the handler existed and was removed, false otherwise
   */
  boolean removeBodyEndHandler(int handlerID);

  /**
   * Add an end handler for the request/response context. This will be called when the response is disposed or an
   * exception has been encountered to allow consistent cleanup. The handler is called asynchronously of when the
   * response has been received by the client.
   *
   * @param handler the handler that will be called with either a success or failure result.
   * @return  the id of the handler. This can be used if you later want to remove the handler.
   */
  int addEndHandler(Handler<AsyncResult<Void>> handler);

  /**
   * Remove an end handler
   *
   * @param handlerID  the id as returned from {@link io.vertx.ext.web.RoutingContext#addEndHandler(Handler)}.
   * @return true if the handler existed and was removed, false otherwise
   */
  boolean removeEndHandler(int handlerID);

  /**
   * @return true if the context is being routed to failure handlers.
   */
  boolean failed();

  /**
   * Set the acceptable content type. Used by
   * @param contentType  the content type
   */
  void setAcceptableContentType(@Nullable String contentType);

  /**
   * Restarts the current router with a new path and reusing the original method. All path parameters are then parsed
   * and available on the params list. Query params will also be allowed and available.
   *
   * @param path the new http path.
   */
  default void reroute(String path) {
    reroute(request().method(), path);
  }

  /**
   * Restarts the current router with a new method and path. All path parameters are then parsed and available on the
   * params list. Query params will also be allowed and available.
   *
   * @param method the new http request
   * @param path the new http path.
   */
  void reroute(HttpMethod method, String path);

  /**
   * Returns the languages for the current request. The languages are determined from the <code>Accept-Language</code>
   * header and sorted on quality.
   *
   * When 2 or more entries have the same quality then the order used to return the best match is based on the lowest
   * index on the original list. For example if a user has en-US and en-GB with same quality and this order the best
   * match will be en-US because it was declared as first entry by the client.
   *
   * @return The best matched language for the request
   */
  @CacheReturn
  default List<LanguageHeader> acceptableLanguages(){
    return parsedHeaders().acceptLanguage();
  }

  /**
   * Helper to return the user preferred language.
   * It is the same action as returning the first element of the acceptable languages.
   *
   * @return the users preferred locale.
   */
  @CacheReturn
  default LanguageHeader preferredLanguage() {
    List<? extends LanguageHeader> acceptableLanguages = acceptableLanguages();
    return acceptableLanguages.size() > 0 ? acceptableLanguages.get(0) : null;
  }

  /**
   * Returns a map of named parameters as defined in path declaration with their actual values
   *
   * @return the map of named parameters
   */
  Map<String, String> pathParams();

  /**
   * Gets the value of a single path parameter
   *
   * @param name the name of parameter as defined in path declaration
   * @return the actual value of the parameter or null if it doesn't exist
   */
  @Nullable
  String pathParam(String name);

  /**
   * Returns a map of all query parameters inside the <a href="https://en.wikipedia.org/wiki/Query_string">query string</a><br/>
   * The query parameters are lazily decoded: the decoding happens on the first time this method is called. If the query string is invalid
   * it fails the context
   *
   * @return the multimap of query parameters
   */
  MultiMap queryParams();

  /**
   * Always decode the current query string with the given {@code encoding}. The decode result is never cached. Callers
   * to this method are expected to cache the result if needed. Usually users should use {@link #queryParams()}.
   *
   * This method is only useful when the requests without content type ({@code GET} requests as an example) expect that
   * query params are in the ASCII format {@code ISO-5559-1}.
   *
   * @param encoding a non null character set.
   * @return the multimap of query parameters
   */
  @GenIgnore(PERMITTED_TYPE)
  MultiMap queryParams(Charset encoding);

  /**
   * Gets the value of a single query parameter. For more info {@link RoutingContext#queryParams()}
   *
   * @param name The name of query parameter
   * @return The list of all parameters matching the parameter name. It returns an empty list if no query parameter with {@code name} was found
   */
  List<String> queryParam(String name);

  /**
   * Set Content-Disposition get to "attachment" with optional {@code filename} mime type.
   *
   * @param filename the filename for the attachment
   */
  @Fluent
  default RoutingContext attachment(String filename) {
    if (filename != null) {
      String contentType = MimeMapping.getMimeTypeForFilename(filename);

      if (contentType != null) {
        response()
          .putHeader(HttpHeaders.CONTENT_TYPE, contentType);
      }

    }

    response()
      .putHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);

    return this;
  }

  /**
   * Perform a 302 redirect to {@code url}. If a custom 3xx code is already defined, then that
   * one will be preferred.
   * <p/>
   * The string "back" is special-cased
   * to provide Referrer support, when Referrer
   * is not present "/" is used.
   * <p/>
   * Examples:
   * <p/>
   * redirect('back');
   * redirect('/login');
   * redirect('http://google.com');
   *
   * @param url the target url
   */
  default Future<Void> redirect(String url) {
    // location
    if ("back".equals(url)) {
      url = request().getHeader(HttpHeaders.REFERER);
      if (url == null) {
        url = "/";
      }
    }

    response()
      .putHeader(HttpHeaders.LOCATION, url);

    // status
    int status = response().getStatusCode();

    if (status < 300 || status >= 400) {
      // if a custom code is in use that will be
      // respected
      response().setStatusCode(302);
    }

    return response()
      .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
      .end("Redirecting to " + url + ".");
  }

  /**
   * Encode an Object to JSON and end the request.
   * When {@code Content-Type} is not set then correct {@code Content-Type} will be applied to the response
   * @param json the json
   * @return a future to handle the end of the request
   */
  default Future<Void> json(Object json) {
    final HttpServerResponse res = response();
    final boolean hasContentType = res.headers().contains(HttpHeaders.CONTENT_TYPE);

    if (json == null) {
      // http://www.iana.org/assignments/media-types/application/json
      // No "charset" parameter is defined for this registration.
      // Adding one really has no effect on compliant recipients.

      // apply the content type header only if content type header is not set
      if(!hasContentType) {
        res.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      }
      return res.end("null");
    } else {
      try {
        Buffer buffer = Json.encodeToBuffer(json);
        // http://www.iana.org/assignments/media-types/application/json
        // No "charset" parameter is defined for this registration.
        // Adding one really has no effect on compliant recipients.

        // apply the content type header only if the encoding succeeds and content type header is not set
        if(!hasContentType) {
          res.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        }
        return res.end(buffer);
      } catch (EncodeException | UnsupportedOperationException e) {
        // handle the failure
        fail(e);
        // as the operation failed return a failed future
        // this is purely a notification
        return ((ContextInternal) vertx().getOrCreateContext()).failedFuture(e);
      }
    }
  }

  /**
   * Check if the incoming request contains the "Content-Type"
   * get field, and it contains the give mime `type`.
   * If there is no request body, `false` is returned.
   * If there is no content type, `false` is returned.
   * Otherwise, it returns true if the `type` that matches.
   * <p/>
   * Examples:
   * <p/>
   * // With Content-Type: text/html; charset=utf-8
   * is("html"); // => true
   * is("text/html"); // => true
   * <p/>
   * // When Content-Type is application/json
   * is("application/json"); // => true
   * is("html"); // => false
   *
   * @param type content type
   * @return The most close value
   */
  @CacheReturn
  default boolean is(String type) {
    MIMEHeader contentType = parsedHeaders().contentType();

    if (contentType == null) {
      return false;
    }

    ParsedHeaderValue value;


    // if we received an incomplete CT
    if (type.indexOf('/') == -1) {
      // when the content is incomplete we assume */type, e.g.:
      // json -> */json
      value = new ParsableMIMEValue("*/" + type).forceParse();
    } else {
      value = new ParsableMIMEValue(type).forceParse();
    }

    return contentType.isMatchedBy(value);
  }

  /**
   * Check if the request is fresh, aka
   * Last-Modified and/or the ETag
   * still match.
   *
   * @return true if content is fresh according to the cache.
   */
  default boolean isFresh() {
    final HttpMethod method = request().method();

    // GET or HEAD for weak freshness validation only
    if (method != HttpMethod.GET && method != HttpMethod.HEAD) {
      return false;
    }

    final int s = response().getStatusCode();
    // 2xx or 304 as per rfc2616 14.26
    if ((s >= 200 && s < 300) || 304 == s) {
      return Utils.fresh(this);
    }

    return false;
  }

  /**
   * Set the ETag of a response.
   * This will normalize the quotes if necessary.
   * <p/>
   * etag('md5hashsum');
   * etag('"md5hashsum"');
   * ('W/"123456789"');
   *
   * @param etag the etag value
   */
  @Fluent
  default RoutingContext etag(String etag) {
    boolean quoted =
      // at least 2 characters
      etag.length() > 2 &&
        // either starts with " or W/"
        (etag.charAt(0) == '\"' || etag.startsWith("W/\"")) &&
        // ends with "
        etag.charAt(etag.length() -1) == '\"';

    if (!quoted) {
      response().putHeader(HttpHeaders.ETAG, "\"" + etag + "\"");
    } else {
      response().putHeader(HttpHeaders.ETAG, etag);
    }

    return this;
  }

  /**
   * Set the Last-Modified date using a Instant.
   *
   * @param instant the last modified instant
   */
  @Fluent
  @GenIgnore(PERMITTED_TYPE)
  default RoutingContext lastModified(Instant instant) {
    response().putHeader(HttpHeaders.LAST_MODIFIED, Utils.formatRFC1123DateTime(instant.toEpochMilli()));
    return this;
  }

  /**
   * Set the Last-Modified date using a String.
   *
   * @param instant the last modified instant
   */
  @Fluent
  default RoutingContext lastModified(String instant) {
    response().putHeader(HttpHeaders.LAST_MODIFIED, instant);
    return this;
  }

  /**
   * Shortcut to the response end.
   * @param chunk a chunk
   * @return future
   */
  default Future<Void> end(String chunk) {
    return response().end(chunk);
  }

  /**
   * Shortcut to the response end.
   * @param buffer a chunk
   * @return future
   */
  default Future<Void> end(Buffer buffer) {
    return response().end(buffer);
  }

  /**
   * Shortcut to the response end.
   * @return future
   */
  default Future<Void> end() {
    return response().end();
  }

}
