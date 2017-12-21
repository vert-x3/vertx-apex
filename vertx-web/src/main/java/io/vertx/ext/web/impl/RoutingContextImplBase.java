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
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class RoutingContextImplBase implements RoutingContext {

  static final Logger log = LoggerFactory.getLogger(RoutingContextImplBase.class);

  private final Set<RouteImpl> routes;

  protected final String mountPoint;
  protected final HttpServerRequest request;
  protected final MqttPublishMessage message;
  protected Iterator<RouteImpl> iter;
  protected RouteImpl currentRoute;

  protected RoutingContextImplBase(String mountPoint, HttpServerRequest request, Set<RouteImpl> routes) {
    this.mountPoint = mountPoint;
    this.request = new HttpServerRequestWrapper(request);
    this.routes = routes;
    this.iter = routes.iterator();
  }

  protected RoutingContextImplBase(String mountPoint, MqttPublishMessage message, Set<RouteImpl> routes) {
    this.mountPoint = mountPoint;
    this.message = message;
    this.routes = routes;
    this.iter = routes.iterator();
  }

  @Override
  public String mountPoint() {
    return mountPoint;
  }

  @Override
  public Route currentRoute() {
    return currentRoute;
  }

  protected void restart() {
    this.iter = routes.iterator();
    currentRoute = null;
    next();
  }

  protected boolean iterateNext() {
    boolean failed = failed();
    if (currentRoute != null) { // Handle multiple handlers inside route object
      try {
        if (!failed && currentRoute.hasNextContextHandler()) {
          currentRoute.handleContext(this);
          return true;
        } else if (failed && currentRoute.hasNextFailureHandler()) {
          currentRoute.handleFailure(this);
          return true;
        }
      } catch (Throwable t) {
        if (log.isTraceEnabled()) log.trace("Throwable thrown from handler", t);
        if (!failed) {
          if (log.isTraceEnabled()) log.trace("Failing the routing");
          fail(t);
        } else {
          // Failure in handling failure!
          if (log.isTraceEnabled()) log.trace("Failure in handling failure");
          unhandledFailure(-1, t, currentRoute.router());
        }
        return true;
      }
    }
    while (iter.hasNext()) { // Search for more handlers
      RouteImpl route = iter.next();
      route.resetIndexes();
      if (route.matches(this, mountPoint(), failed)) {
        if (log.isTraceEnabled()) log.trace("Route matches: " + route);
        try {
          currentRoute = route;
          if (log.isTraceEnabled()) log.trace("Calling the " + (failed ? "failure" : "") + " handler");
          if (failed) {
            route.handleFailure(this);
          } else {
            route.handleContext(this);
          }
        } catch (Throwable t) {
          if (log.isTraceEnabled()) log.trace("Throwable thrown from handler", t);
          if (!failed) {
            if (log.isTraceEnabled()) log.trace("Failing the routing");
            fail(t);
          } else {
            // Failure in handling failure!
            if (log.isTraceEnabled()) log.trace("Failure in handling failure");
            unhandledFailure(-1, t, route.router());
          }
        }
        return true;
      }
    }
    return false;
  }

  protected boolean iterateNextMqtt() {
    boolean failed = failed();
    if (currentRoute != null) { // Handle multiple handlers inside route object
      try {
        if (!failed && currentRoute.hasNextContextHandler()) {
          currentRoute.handleContext(this);
          return true;
        } else if (failed && currentRoute.hasNextFailureHandler()) {
          currentRoute.handleFailure(this);
          return true;
        }
      } catch (Throwable t) {
        if (log.isTraceEnabled()) log.trace("Throwable thrown from handler", t);
        if (!failed) {
          if (log.isTraceEnabled()) log.trace("Failing the routing");
          fail(t);
        } else {
          // Failure in handling failure!
          if (log.isTraceEnabled()) log.trace("Failure in handling failure");
          unhandledFailure(-1, t, currentRoute.router());
        }
        return true;
      }
    }
    while (iter.hasNext()) { // Search for more handlers
      RouteImpl route = iter.next();
      route.resetIndexes();
      if (route.matchesMqtt(this, mountPoint(), failed)) {
        if (log.isTraceEnabled()) log.trace("Route matches: " + route);
        try {
          currentRoute = route;
          if (log.isTraceEnabled()) log.trace("Calling the " + (failed ? "failure" : "") + " handler");
          if (failed) {
            route.handleFailure(this);
          } else {
            route.handleContext(this);
          }
        } catch (Throwable t) {
          if (log.isTraceEnabled()) log.trace("Throwable thrown from handler", t);
          if (!failed) {
            if (log.isTraceEnabled()) log.trace("Failing the routing");
            fail(t);
          } else {
            // Failure in handling failure!
            if (log.isTraceEnabled()) log.trace("Failure in handling failure");
            unhandledFailure(-1, t, route.router());
          }
        }
        return true;
      }
    }
    return false;
  }


  protected void unhandledFailure(int statusCode, Throwable failure, RouterImpl router) {
    int code = statusCode != -1 ? statusCode : 500;
    if (failure != null) {
      if (router.exceptionHandler() != null) {
        router.exceptionHandler().handle(failure);
      } else {
        log.error("Unexpected exception in route", failure);
      }
    }
    if (!response().ended()) {
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
}
