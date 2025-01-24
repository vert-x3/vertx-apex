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

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.impl.RouterImpl;

import java.util.List;
import java.util.Map;

/**
 * A router receives request from an {@link io.vertx.core.http.HttpServer} and routes it to the first matching
 * {@link Route} that it contains. A router can contain many routes.
 * <p>
 * Routers are also used for routing failures.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface Router extends Handler<HttpServerRequest> {

  /**
   * Put metadata to this router. Used for saved extra data.
   * Remove the existing value if value is null.
   *
   * @param key the metadata of key
   * @param value the metadata of value
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Router putMetadata(String key, Object value);

  /**
   * @return the metadata of this router, never returns null.
   */
  Map<String, Object> metadata();

  /**
   * Get some data from metadata.
   *
   * @param key the key for the metadata
   * @param <T> the type of the data
   * @return  the data
   */
  @SuppressWarnings("unchecked")
  default <T> T getMetadata(String key) {
    return (T) metadata().get(key);
  }

  /**
   * Create a router
   *
   * @param vertx  the Vert.x instance
   * @return the router
   */
  static Router router(Vertx vertx) {
    return new RouterImpl(vertx);
  }

  /**
   * Add a route with no matching criteria, i.e. it matches all requests or failures.
   *
   * @return  the route
   */
  Route route();

  /**
   * Add a route that matches the specified HTTP method and path
   *
   * @param method  the HTTP method to match
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route route(HttpMethod method, String path);

  /**
   * Add a route that matches the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route route(String path);

  /**
   * Add a route that matches the specified HTTP method and path regex
   *
   * @param method  the HTTP method to match
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route routeWithRegex(HttpMethod method, String regex);

  /**
   * Add a route that matches the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route routeWithRegex(String regex);

  /**
   * Add a route that matches any HTTP GET request
   *
   * @return the route
   */
  Route get();

  /**
   * Add a route that matches a HTTP GET request and the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route get(String path);

  /**
   * Add a route that matches a HTTP GET request and the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route getWithRegex(String regex);

  /**
   * Add a route that matches any HTTP HEAD request
   *
   * @return the route
   */
  Route head();

  /**
   * Add a route that matches a HTTP HEAD request and the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route head(String path);

  /**
   * Add a route that matches a HTTP HEAD request and the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route headWithRegex(String regex);

  /**
   * Add a route that matches any HTTP OPTIONS request
   *
   * @return the route
   */
  Route options();

  /**
   * Add a route that matches a HTTP OPTIONS request and the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route options(String path);

  /**
   * Add a route that matches a HTTP OPTIONS request and the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route optionsWithRegex(String regex);

  /**
   * Add a route that matches any HTTP PUT request
   *
   * @return the route
   */
  Route put();

  /**
   * Add a route that matches a HTTP PUT request and the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route put(String path);

  /**
   * Add a route that matches a HTTP PUT request and the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route putWithRegex(String regex);

  /**
   * Add a route that matches any HTTP POST request
   *
   * @return the route
   */
  Route post();

  /**
   * Add a route that matches a HTTP POST request and the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route post(String path);

  /**
   * Add a route that matches a HTTP POST request and the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route postWithRegex(String regex);

  /**
   * Add a route that matches any HTTP DELETE request
   *
   * @return the route
   */
  Route delete();

  /**
   * Add a route that matches a HTTP DELETE request and the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route delete(String path);

  /**
   * Add a route that matches a HTTP DELETE request and the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route deleteWithRegex(String regex);

  /**
   * Add a route that matches any HTTP TRACE request
   *
   * @return the route
   */
  Route trace();

  /**
   * Add a route that matches a HTTP TRACE request and the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route trace(String path);

  /**
   * Add a route that matches a HTTP TRACE request and the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route traceWithRegex(String regex);

  /**
   * Add a route that matches any HTTP CONNECT request
   *
   * @return the route
   */
  Route connect();

  /**
   * Add a route that matches a HTTP CONNECT request and the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route connect(String path);

  /**
   * Add a route that matches a HTTP CONNECT request and the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route connectWithRegex(String regex);

  /**
   * Add a route that matches any HTTP PATCH request
   *
   * @return the route
   */
  Route patch();

  /**
   * Add a route that matches a HTTP PATCH request and the specified path
   *
   * @param path  URI paths that begin with this path will match
   *
   * @return the route
   */
  Route patch(String path);

  /**
   * Add a route that matches a HTTP PATCH request and the specified path regex
   *
   * @param regex  URI paths that begin with a match for this regex will match
   *
   * @return the route
   */
  Route patchWithRegex(String regex);

  /**
   * @return a list of all the routes on this router
   */
  List<Route> getRoutes();

  /**
   * Remove all the routes from this router
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Router clear();

  /**
   * Specify an handler to handle an error for a particular status code. You can use to manage general errors too using status code 500.
   * The handler will be called when the context fails and other failure handlers didn't write the reply or when an exception is thrown inside an handler.
   * You <b>must not</b> use {@link RoutingContext#next()} inside the error handler
   * This does not affect the normal failure routing logic.
   *
   * @param statusCode status code the errorHandler is capable of handle
   * @param errorHandler error handler. Note: You <b>must not</b> use {@link RoutingContext#next()} inside the provided handler
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Router errorHandler(int statusCode, Handler<RoutingContext> errorHandler);

  /**
   * Specify an handler to handle an error for any status code that doesn't have a specific handler assigned.
   * The handler will be called when the context fails and other failure handlers didn't write the reply or when an exception is thrown inside an handler.
   * You <b>must not</b> use {@link RoutingContext#next()} inside the error handler
   * This does not affect the normal failure routing logic.
   *
   * @param errorHandler error handler. Note: You <b>must not</b> use {@link RoutingContext#next()} inside the provided handler
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Router uncaughtErrorHandler(Handler<RoutingContext> errorHandler);

  /**
   * Used to route a context to the router. Used for sub-routers. You wouldn't normally call this method directly.
   *
   * @param context  the routing context
   */
  void handleContext(RoutingContext context);

  /**
   * Used to route a failure to the router. Used for sub-routers. You wouldn't normally call this method directly.
   *
   * @param context  the routing context
   */
  void handleFailure(RoutingContext context);

  /**
   * When a Router routes are changed this handler is notified.
   * This is useful for routes that depend on the state of the router.
   *
   * @param handler a notification handler that will receive this router as argument
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Router modifiedHandler(Handler<Router> handler);


  /**
   * Set whether the router should parse "forwarded"-type headers
   *
   * @param allowForwardHeaders to enable parsing of "forwarded"-type headers
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Router allowForward(AllowForwardHeaders allowForwardHeaders);
}
