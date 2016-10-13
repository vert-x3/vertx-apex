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
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;

/**
 * A route is a holder for a set of criteria which determine whether an HTTP request or failure should be routed
 * to a handler.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface Route {

  /**
   * Add an HTTP method for this route. By default a route will match all HTTP methods. If any are specified then the route
   * will only match any of the specified methods
   *
   * @param method  the HTTP method to add
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route method(HttpMethod method);

  /**
   * Set the path prefix for this route. If set then this route will only match request URI paths which start with this
   * path prefix. Only a single path or path regex can be set for a route.
   *
   * @param path  the path prefix
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route path(String path);

  /**
   * Set the path prefix as a regular expression. If set then this route will only match request URI paths, the beginning
   * of which match the regex. Only a single path or path regex can be set for a route.
   *
   * @param path  the path regex
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route pathRegex(String path);

  /**
   * Add a content type produced by this route. Used for content based routing.
   *
   * @param contentType  the content type
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route produces(String contentType);

  /**
   * Add a content type consumed by this route. Used for content based routing.
   *
   * @param contentType  the content type
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route consumes(String contentType);

  /**
   * Specify the order for this route. The router tests routes in that order.
   *
   * @param order  the order
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route order(int order);

  /**
   * Specify this is the last route for the router.
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route last();

  /**
   * Specify a request handler for the route. The router routes requests to handlers depending on whether the various
   * criteria such as method, path, etc match. There can be only one request handler for a route. If you set this more
   * than once it will overwrite the previous handler.
   *
   * @param requestHandler  the request handler
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route handler(Handler<RoutingContext> requestHandler);


  /**
   * Like {@link io.vertx.ext.web.Route#blockingHandler(Handler, boolean)} called with ordered = true
   */
  @Fluent
  Route blockingHandler(Handler<RoutingContext> requestHandler);

  /**
   * Specify a blocking request handler for the route.
   * This method works just like {@link #handler(Handler)} excepted that it will run the blocking handler on a worker thread
   * so that it won't block the event loop. Note that it's safe to call context.next() from the
   * blocking handler as it will be executed on the event loop context (and not on the worker thread.
   *
   * If the blocking handler is ordered it means that any blocking handlers for the same context are never executed
   * concurrently but always in the order they were called. The default value of ordered is true. If you do not want this
   * behaviour and don't mind if your blocking handlers are executed in parallel you can set ordered to false.
   *
   * @param requestHandler  the blocking request handler
   * @param ordered if true handlers are executed in sequence, otherwise are run in parallel
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route blockingHandler(Handler<RoutingContext> requestHandler, boolean ordered);

  /**
   * Specify a failure handler for the route. The router routes failures to failurehandlers depending on whether the various
   * criteria such as method, path, etc match. There can be only one failure handler for a route. If you set this more
   * than once it will overwrite the previous handler.
   *
   * @param failureHandler  the request handler
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route failureHandler(Handler<RoutingContext> failureHandler);

  /**
   * Remove this route from the router
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route remove();

  /**
   * Disable this route. While disabled the router will not route any requests or failures to it.
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route disable();

  /**
   * Enable this route.
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route enable();

  Route then(Handler<RoutingContext> handler);

  /**
   * If true then the normalised request path will be used when routing (e.g. removing duplicate /)
   * Default is true
   *
   * @param useNormalisedPath  use normalised path for routing?
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Route useNormalisedPath(boolean useNormalisedPath);

  /**
   * @return the path prefix (if any) for this route
   */
  @Nullable
  String getPath();

}


