/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.rxjava.ext.web;

import java.util.Map;
import rx.Observable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.Handler;

/**
 * A route is a holder for a set of criteria which determine whether an HTTP request or failure should be routed
 * to a handler.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.web.Route original} non RX-ified interface using Vert.x codegen.
 */

public class Route {

  final io.vertx.ext.web.Route delegate;

  public Route(io.vertx.ext.web.Route delegate) {
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  /**
   * Add an HTTP method for this route. By default a route will match all HTTP methods. If any are specified then the route
   * will only match any of the specified methods
   * @param method the HTTP method to add
   * @return a reference to this, so the API can be used fluently
   */
  public Route method(HttpMethod method) { 
    delegate.method(method);
    return this;
  }

  /**
   * Set the path prefix for this route. If set then this route will only match request URI paths which start with this
   * path prefix. Only a single path or path regex can be set for a route.
   * @param path the path prefix
   * @return a reference to this, so the API can be used fluently
   */
  public Route path(String path) { 
    delegate.path(path);
    return this;
  }

  /**
   * Set the path prefix as a regular expression. If set then this route will only match request URI paths, the beginning
   * of which match the regex. Only a single path or path regex can be set for a route.
   * @param path the path regex
   * @return a reference to this, so the API can be used fluently
   */
  public Route pathRegex(String path) { 
    delegate.pathRegex(path);
    return this;
  }

  /**
   * Add a content type produced by this route. Used for content based routing.
   * @param contentType the content type
   * @return a reference to this, so the API can be used fluently
   */
  public Route produces(String contentType) { 
    delegate.produces(contentType);
    return this;
  }

  /**
   * Add a content type consumed by this route. Used for content based routing.
   * @param contentType the content type
   * @return a reference to this, so the API can be used fluently
   */
  public Route consumes(String contentType) { 
    delegate.consumes(contentType);
    return this;
  }

  /**
   * Specify the order for this route. The router tests routes in that order.
   * @param order the order
   * @return a reference to this, so the API can be used fluently
   */
  public Route order(int order) { 
    delegate.order(order);
    return this;
  }

  /**
   * Specify this is the last route for the router.
   * @return a reference to this, so the API can be used fluently
   */
  public Route last() { 
    delegate.last();
    return this;
  }

  /**
   * Specify a request handler for the route. The router routes requests to handlers depending on whether the various
   * criteria such as method, path, etc match. There can be only one request handler for a route. If you set this more
   * than once it will overwrite the previous handler.
   * @param requestHandler the request handler
   * @return a reference to this, so the API can be used fluently
   */
  public Route handler(Handler<RoutingContext> requestHandler) { 
    delegate.handler(new Handler<io.vertx.ext.web.RoutingContext>() {
      public void handle(io.vertx.ext.web.RoutingContext event) {
        requestHandler.handle(RoutingContext.newInstance(event));
      }
    });
    return this;
  }

  /**
   * Like {@link io.vertx.rxjava.ext.web.Route#blockingHandler} called with ordered = true
   * @param requestHandler 
   * @return 
   */
  public Route blockingHandler(Handler<RoutingContext> requestHandler) { 
    delegate.blockingHandler(new Handler<io.vertx.ext.web.RoutingContext>() {
      public void handle(io.vertx.ext.web.RoutingContext event) {
        requestHandler.handle(RoutingContext.newInstance(event));
      }
    });
    return this;
  }

  /**
   * Specify a blocking request handler for the route.
   * This method works just like {@link io.vertx.rxjava.ext.web.Route#handler} excepted that it will run the blocking handler on a worker thread
   * so that it won't block the event loop. Note that it's safe to call context.next() from the
   * blocking handler as it will be executed on the event loop context (and not on the worker thread.
   *
   * If the blocking handler is ordered it means that any blocking handlers for the same context are never executed
   * concurrently but always in the order they were called. The default value of ordered is true. If you do not want this
   * behaviour and don't mind if your blocking handlers are executed in parallel you can set ordered to false.
   * @param requestHandler the blocking request handler
   * @param ordered if true handlers are executed in sequence, otherwise are run in parallel
   * @return a reference to this, so the API can be used fluently
   */
  public Route blockingHandler(Handler<RoutingContext> requestHandler, boolean ordered) { 
    delegate.blockingHandler(new Handler<io.vertx.ext.web.RoutingContext>() {
      public void handle(io.vertx.ext.web.RoutingContext event) {
        requestHandler.handle(RoutingContext.newInstance(event));
      }
    }, ordered);
    return this;
  }

  /**
   * Specify a failure handler for the route. The router routes failures to failurehandlers depending on whether the various
   * criteria such as method, path, etc match. There can be only one failure handler for a route. If you set this more
   * than once it will overwrite the previous handler.
   * @param failureHandler the request handler
   * @return a reference to this, so the API can be used fluently
   */
  public Route failureHandler(Handler<RoutingContext> failureHandler) { 
    delegate.failureHandler(new Handler<io.vertx.ext.web.RoutingContext>() {
      public void handle(io.vertx.ext.web.RoutingContext event) {
        failureHandler.handle(RoutingContext.newInstance(event));
      }
    });
    return this;
  }

  /**
   * Remove this route from the router
   * @return a reference to this, so the API can be used fluently
   */
  public Route remove() { 
    delegate.remove();
    return this;
  }

  /**
   * Disable this route. While disabled the router will not route any requests or failures to it.
   * @return a reference to this, so the API can be used fluently
   */
  public Route disable() { 
    delegate.disable();
    return this;
  }

  /**
   * Enable this route.
   * @return a reference to this, so the API can be used fluently
   */
  public Route enable() { 
    delegate.enable();
    return this;
  }

  public Route then(Handler<RoutingContext> handler) { 
    Route ret = Route.newInstance(delegate.then(new Handler<io.vertx.ext.web.RoutingContext>() {
      public void handle(io.vertx.ext.web.RoutingContext event) {
        handler.handle(RoutingContext.newInstance(event));
      }
    }));
    return ret;
  }

  /**
   * If true then the normalised request path will be used when routing (e.g. removing duplicate /)
   * Default is true
   * @param useNormalisedPath use normalised path for routing?
   * @return a reference to this, so the API can be used fluently
   */
  public Route useNormalisedPath(boolean useNormalisedPath) { 
    delegate.useNormalisedPath(useNormalisedPath);
    return this;
  }

  /**
   * @return the path prefix (if any) for this route
   */
  public String getPath() { 
    String ret = delegate.getPath();
    return ret;
  }


  public static Route newInstance(io.vertx.ext.web.Route arg) {
    return arg != null ? new Route(arg) : null;
  }
}
