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

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class RoutingContextImplBase implements RoutingContextInternal {

  private static final AtomicIntegerFieldUpdater<RoutingContextImplBase> CURRENT_ROUTE_NEXT_HANDLER_INDEX =
    AtomicIntegerFieldUpdater.newUpdater(RoutingContextImplBase.class, "currentRouteNextHandlerIndex");
  private static final AtomicIntegerFieldUpdater<RoutingContextImplBase> CURRENT_ROUTE_NEXT_FAILURE_HANDLER_INDEX =
    AtomicIntegerFieldUpdater.newUpdater(RoutingContextImplBase.class, "currentRouteNextFailureHandlerIndex");

  protected static final Logger LOG = LoggerFactory.getLogger(RoutingContext.class);

  private final Set<RouteImpl> routes;

  protected final Router currentRouter;
  protected final String mountPoint;
  private volatile int currentRouteNextHandlerIndex;
  private volatile int currentRouteNextFailureHandlerIndex;
  protected Iterator<RouteImpl> iter;
  protected RouteState currentRoute;
  // When Route#matches executes, if it returns != 0 this flag is configured
  // to write the correct status code at the end of routing process
  int matchFailure;
  // the current path matched string
  int matchRest = -1;
  boolean normalizedMatch;
  // internal runtime state
  private volatile long seen;

  protected Set<HttpMethod> allowedMethods = new HashSet<>();

  RoutingContextImplBase(String mountPoint, Set<RouteImpl> routes, Router currentRouter) {
    this.mountPoint = mountPoint;
    this.routes = routes;
    this.iter = routes.iterator();

    this.currentRouter = currentRouter;
    resetMatchFailure();
  }

  @Override
  public synchronized RoutingContextInternal visitHandler(int id) {
    seen |= id;
    return this;
  }

  @Override
  public boolean seenHandler(int id) {
    return (seen & id) != 0;
  }

  @Override
  public int restIndex() {
    return matchRest;
  }

  @Override
  public boolean normalizedMatch() {
    return normalizedMatch;
  }

  @Override
  public synchronized RoutingContextInternal setMatchFailure(int matchFailure) {
    this.matchFailure = matchFailure;
    return this;
  }

  @Override
  public String mountPoint() {
    return mountPoint;
  }

  @Override
  public Route currentRoute() {
    if (currentRoute == null) {
      return null;
    }
    return currentRoute.getRoute();
  }

  @Override
  public Router currentRouter() {
    return currentRouter;
  }

  int currentRouteNextHandlerIndex() {
    return currentRouteNextHandlerIndex;
  }

  int currentRouteNextFailureHandlerIndex() {
    return currentRouteNextFailureHandlerIndex;
  }

  void restart() {
    this.iter = routes.iterator();
    currentRoute = null;
    next();
  }

  final int nextFailureHandler(int limit) {
    int index;
    do {
      index = currentRouteNextFailureHandlerIndex;
      if (index >= limit) {
        return -1;
      }
    } while (!CURRENT_ROUTE_NEXT_FAILURE_HANDLER_INDEX.compareAndSet(this, index, index + 1));
    return index;
  }

  final int nextContextHandler(int limit) {
    int index;
    do {
      index = currentRouteNextHandlerIndex;
      if (index >= limit) {
        return -1;
      }
    } while (!CURRENT_ROUTE_NEXT_HANDLER_INDEX.compareAndSet(this, index, index + 1));
    return index;
  }

  boolean iterateNext() {
    boolean failed = failed();
    if (currentRoute != null) { // Handle multiple handlers inside route object
      try {
        Handler<RoutingContext> handler;
        if (!failed && (handler = currentRoute.nextContextHandler(this)) != null) {
          resetMatchFailure();
          handler.handle(this);
          return true;
        } else if (failed && (handler = currentRoute.nextFailureHandler(this)) != null) {
          handler.handle(this);
          return true;
        }
      } catch (Throwable t) {
        handleInHandlerRuntimeFailure(currentRoute.getRouter(), failed, t);
        return true;
      }
    }
    // Search for more handlers
    while (iter.hasNext()) {
      // state is locked at this moment
      RouteState routeState = iter.next().state();

      CURRENT_ROUTE_NEXT_HANDLER_INDEX.set(this, 0);
      CURRENT_ROUTE_NEXT_FAILURE_HANDLER_INDEX.set(this, 0);
      try {
        int matchResult = routeState.matches(this, mountPoint(), failed);
        if (matchResult == 0) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("Route matches: " + routeState);
          }
          resetMatchFailure();
          try {
            currentRoute = routeState;
            request().routed(currentRoute.getName());
            if (LOG.isTraceEnabled()) {
              LOG.trace("Calling the " + (failed ? "failure" : "") + " handler");
            }
            Handler<RoutingContext> handler;
            if (failed && (handler = currentRoute.nextFailureHandler(this)) != null) {
              handler.handle(this);
            } else if ((handler = currentRoute.nextContextHandler(this)) != null) {
              handler.handle(this);
            } else {
              continue;
            }
          } catch (Throwable t) {
            handleInHandlerRuntimeFailure(routeState.getRouter(), failed, t);
          }
          return true;
        } else if (matchResult == 405) {
          //We need to add supported methods for route in case we need to send 405 at end
          allowedMethods.addAll(routeState.getMethods());
          // invalid method match, means that
          // we should "update" the failure if not found to be invalid method
          if (this.matchFailure == 404) {
            this.matchFailure = matchResult;
          }
        } else if (matchResult != 404) {
          this.matchFailure = matchResult;
        }
      } catch (Throwable e) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("IllegalArgumentException thrown during iteration", e);
        }
        // Failure in matches algorithm (If the exception is instanceof IllegalArgumentException probably is a QueryStringDecoder error!)
        if (!this.response().ended()) {
          unhandledFailure((e instanceof IllegalArgumentException) ? 400 : -1, e, routeState.getRouter());
        }
        return true;
      }
    }
    return false;
  }

  private void handleInHandlerRuntimeFailure(RouterImpl router, boolean failed, Throwable t) {
    if (!failed) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Failing the routing");
      }
      fail(t);
    } else {
      // Failure in handling failure!
      if (LOG.isTraceEnabled()) {
        LOG.trace("Failure in handling failure");
      }
      unhandledFailure(-1, t, router);
    }
  }

  protected void unhandledFailure(int statusCode, Throwable failure, RouterImpl router) {
    int code = statusCode != -1 ?
      statusCode :
      (failure instanceof HttpException) ?
        ((HttpException) failure).getStatusCode() :
        500;

    Handler<RoutingContext> errorHandler = router.getErrorHandlerByStatusCode(code);
    if (errorHandler != null) {
      try {
        errorHandler.handle(this);
      } catch (Throwable t) {
        LOG.error("Error in error handler", t);
      }
    } else {
      // if there are no user defined handlers, we will log the exception
      LOG.error("Unhandled exception in router", failure);
    }

    if (!response().ended() && !response().closed()) {
      try {
        response().setStatusCode(code);
      } catch (IllegalArgumentException e) {
        // means that there are invalid chars in the status message
        response()
            .setStatusMessage(HttpResponseStatus.valueOf(code).reasonPhrase())
            .setStatusCode(code);
      }
      response().end(response().getStatusMessage());
    }
  }

  private void resetMatchFailure() {
    this.matchFailure = 404;
  }
}
