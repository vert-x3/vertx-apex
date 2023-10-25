/*
 * Copyright 2019 Red Hat, Inc.
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

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.impl.URIDecoder;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.auth.common.AuthenticationHandler;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates the route state, all mutations are atomic and return a new state with the mutation.
 * <p>
 * This class is thread-safe
 *
 * @author <a href="http://pmlopes@gmail.com">Paulo Lopes</a>
 */
final class RouteState {

  private static final Logger LOG = LoggerFactory.getLogger(RouteState.class);


  enum Priority {
    PLATFORM,
    SECURITY_POLICY,
    PROTOCOL_UPGRADE,
    BODY,
    MULTI_TENANT,
    AUTHENTICATION,
    INPUT_TRUST,
    AUTHORIZATION,
    USER
  }

  private static Priority weight(Handler<RoutingContext> handler) {
    if (handler instanceof PlatformHandler) {
      return Priority.PLATFORM;
    }
    if (handler instanceof SecurityPolicyHandler) {
      return Priority.SECURITY_POLICY;
    }
    if (handler instanceof ProtocolUpgradeHandler) {
      return Priority.PROTOCOL_UPGRADE;
    }
    if (handler instanceof BodyHandler) {
      return Priority.BODY;
    }
    if (handler instanceof MultiTenantHandler) {
      return Priority.MULTI_TENANT;
    }
    if (handler instanceof AuthenticationHandler) {
      return Priority.AUTHENTICATION;
    }
    if (handler instanceof InputTrustHandler) {
      return Priority.INPUT_TRUST;
    }
    if (handler instanceof AuthorizationHandler) {
      return Priority.AUTHORIZATION;
    }

    return Priority.USER;
  }

  private final RouteImpl route;

  private final Map<String, Object> metadata;
  private final String path;
  private final String name;
  private final int order;
  private final boolean enabled;
  private final Set<HttpMethod> methods;
  private final Set<MIMEHeader> consumes;
  private final boolean emptyBodyPermittedWithConsumes;
  private final Set<MIMEHeader> produces;
  private final List<Handler<RoutingContext>> contextHandlers;
  private final List<Handler<RoutingContext>> failureHandlers;
  private final boolean added;
  private final Pattern pattern;
  private final List<String> groups;
  private final boolean useNormalizedPath;
  private final Set<String> namedGroupsInRegex;
  private final Pattern virtualHostPattern;
  private final boolean pathEndsWithSlash;
  private final boolean exclusive;
  private final boolean exactPath;

  private RouteState(RouteImpl route, Map<String, Object> metadata, String path, String name, int order, boolean enabled, Set<HttpMethod> methods, Set<MIMEHeader> consumes, boolean emptyBodyPermittedWithConsumes, Set<MIMEHeader> produces, List<Handler<RoutingContext>> contextHandlers, List<Handler<RoutingContext>> failureHandlers, boolean added, Pattern pattern, List<String> groups, boolean useNormalizedPath, Set<String> namedGroupsInRegex, Pattern virtualHostPattern, boolean pathEndsWithSlash, boolean exclusive, boolean exactPath) {
    this.route = route;
    this.metadata = metadata;
    this.path = path;
    this.name = name;
    this.order = order;
    this.enabled = enabled;
    this.methods = methods;
    this.consumes = consumes;
    this.emptyBodyPermittedWithConsumes = emptyBodyPermittedWithConsumes;
    this.produces = produces;
    this.contextHandlers = contextHandlers;
    this.failureHandlers = failureHandlers;
    this.added = added;
    this.pattern = pattern;
    this.groups = groups;
    this.useNormalizedPath = useNormalizedPath;
    this.namedGroupsInRegex = namedGroupsInRegex;
    this.virtualHostPattern = virtualHostPattern;
    this.pathEndsWithSlash = pathEndsWithSlash;
    this.exclusive = exclusive;
    this.exactPath = exactPath;
  }

  RouteState(RouteImpl route, int order) {
    this(
      route,
      null,
      null,
      null,
      order,
      true,
      null,
      null,
      false,
      null,
      null,
      null,
      false,
      null,
      null,
      true,
      null,
      null,
      false,
      false,
      true);
  }

  public RouteImpl getRoute() {
    return route;
  }

  public RouterImpl getRouter() {
    return route.router();
  }

  public RouteState putMetadata(String key, Object value) {
    Map<String, Object> metadata = this.metadata == null ? new HashMap<>() : new HashMap<>(this.metadata);
    if (value == null) {
      metadata.remove(key);
    } else {
      metadata.put(key, value);
    }

    return new RouteState(
      this.route,
      Collections.unmodifiableMap(metadata),
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public String getPath() {
    return path;
  }

  RouteState setPath(String path) {
    return new RouteState(
      this.route,
      this.metadata,
      path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public int getOrder() {
    return order;
  }

  RouteState setOrder(int order) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public boolean isEnabled() {
    return enabled;
  }

  RouteState setEnabled(boolean enabled) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public Set<HttpMethod> getMethods() {
    return methods;
  }

  RouteState setMethods(Set<HttpMethod> methods) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public RouteState addMethod(HttpMethod method) {
    RouteState newState = new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods == null ? new HashSet<>() : new HashSet<>(this.methods),
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);

    newState.methods.add(method);
    return newState;
  }

  public Set<MIMEHeader> getConsumes() {
    return consumes;
  }

  RouteState setConsumes(Set<MIMEHeader> consumes) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  RouteState addConsume(MIMEHeader mime) {
    RouteState newState = new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(this.consumes),
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);

    newState.consumes.add(mime);
    return newState;
  }

  public boolean isEmptyBodyPermittedWithConsumes() {
    return emptyBodyPermittedWithConsumes;
  }

  RouteState setEmptyBodyPermittedWithConsumes(boolean emptyBodyPermittedWithConsumes) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public Set<MIMEHeader> getProduces() {
    return produces;
  }

  RouteState setProduces(Set<MIMEHeader> produces) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  RouteState addProduce(MIMEHeader mime) {
    RouteState newState = new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces == null ? new LinkedHashSet<>() : new LinkedHashSet<>(this.produces),
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);

    newState.produces.add(mime);
    return newState;
  }

  public List<Handler<RoutingContext>> getContextHandlers() {
    return contextHandlers;
  }

  public int getContextHandlersLength() {
    return contextHandlers == null ? 0 : contextHandlers.size();
  }

  RouteState setContextHandlers(List<Handler<RoutingContext>> contextHandlers) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  RouteState addContextHandler(Handler<RoutingContext> contextHandler) {
    RouteState newState = new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers == null ? new ArrayList<>() : new ArrayList<>(this.contextHandlers),
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);

    int len = newState.contextHandlers.size();
    final Priority weight = weight(contextHandler);
    final Priority lastWeight;
    if (len > 0) {
      lastWeight = weight(newState.contextHandlers.get(len - 1));
      if (lastWeight.ordinal() > weight.ordinal()) {
        String message = "Cannot add [" + weight.name() + "] handler to route with [" + lastWeight.name() + "] handler at index " + (len - 1);
        // when lenient mode is disabled, throw IllegalStateException to signal that the setup is incorrect
        if (!Boolean.getBoolean("io.vertx.web.router.setup.lenient")) {
          throw new IllegalStateException(message);
        }
        LOG.warn(message);
      }
    } else {
      lastWeight = null;
    }

    if (lastWeight == Priority.PROTOCOL_UPGRADE) {
      // when lenient mode is disabled, don't log to signal that the setup might be incorrect
      if (!Boolean.getBoolean("io.vertx.web.router.setup.lenient")) {
        LOG.warn("Adding an handler after PROTOCOL_UPGRADE handler may not be reachable");
      }
    }

    newState.contextHandlers.add(contextHandler);
    return newState;
  }

  public List<Handler<RoutingContext>> getFailureHandlers() {
    return failureHandlers;
  }

  public int getFailureHandlersLength() {
    return failureHandlers == null ? 0 : failureHandlers.size();
  }

  RouteState setFailureHandlers(List<Handler<RoutingContext>> failureHandlers) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  RouteState addFailureHandler(Handler<RoutingContext> failureHandler) {
    RouteState newState = new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers == null ? new ArrayList<>() : new ArrayList<>(this.failureHandlers),
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);

    newState.failureHandlers.add(failureHandler);
    return newState;
  }

  public boolean isAdded() {
    return added;
  }

  RouteState setAdded(boolean added) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public Pattern getPattern() {
    return pattern;
  }

  RouteState setPattern(Pattern pattern) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public List<String> getGroups() {
    return groups;
  }

  RouteState setGroups(List<String> groups) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  RouteState addGroup(String group) {
    RouteState newState = new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups == null ? new ArrayList<>() : new ArrayList<>(groups),
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);

    newState.groups.add(group);
    return newState;
  }

  public boolean isUseNormalizedPath() {
    return useNormalizedPath;
  }

  RouteState setUseNormalizedPath(boolean useNormalizedPath) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public Set<String> getNamedGroupsInRegex() {
    return namedGroupsInRegex;
  }

  RouteState setNamedGroupsInRegex(Set<String> namedGroupsInRegex) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  RouteState addNamedGroupInRegex(String namedGroupInRegex) {
    RouteState newState = new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex == null ? new HashSet<>() : new HashSet<>(this.namedGroupsInRegex),
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);

    newState.namedGroupsInRegex.add(namedGroupInRegex);
    return newState;
  }

  public Pattern getVirtualHostPattern() {
    return virtualHostPattern;
  }

  RouteState setVirtualHostPattern(Pattern virtualHostPattern) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public boolean isPathEndsWithSlash() {
    return pathEndsWithSlash;
  }

  RouteState setPathEndsWithSlash(boolean pathEndsWithSlash) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  public boolean isExclusive() {
    return exclusive;
  }

  RouteState setExclusive(boolean exclusive) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      exclusive,
      this.exactPath);
  }

  public boolean isExactPath() {
    return exactPath;
  }

  RouteState setExactPath(boolean exactPath) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      this.name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      exactPath);
  }
  RouteState setName(String name) {
    return new RouteState(
      this.route,
      this.metadata,
      this.path,
      name,
      this.order,
      this.enabled,
      this.methods,
      this.consumes,
      this.emptyBodyPermittedWithConsumes,
      this.produces,
      this.contextHandlers,
      this.failureHandlers,
      this.added,
      this.pattern,
      this.groups,
      this.useNormalizedPath,
      this.namedGroupsInRegex,
      this.virtualHostPattern,
      this.pathEndsWithSlash,
      this.exclusive,
      this.exactPath);
  }

  private boolean containsMethod(HttpServerRequest request) {
    if (!isEmpty(methods)) {
      return methods.contains(request.method());
    }
    return false;
  }

  private static <T> boolean isEmpty(Collection<T> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * @return 0 if route matches, otherwise it return the status code
   */
  public int matches(RoutingContextImplBase context, String mountPoint, boolean failure) {

    if (failure && !hasNextFailureHandler(context) || !failure && !hasNextContextHandler(context)) {
      return 404;
    }
    if (!enabled) {
      return 404;
    }
    HttpServerRequest request = context.request();
    if (path != null && pattern == null && !pathMatches(mountPoint, context)) {
      return 404;
    }
    if (pattern != null) {
      // need to reset "rest"
      context.pathParams()
        .remove("*");

      String path = useNormalizedPath ? context.normalizedPath() : context.request().path();

      if (mountPoint != null) {
        int strip = mountPoint.length();
        // mount point can have significant slash
        if (mountPoint.charAt(strip - 1)== '/') {
          strip--;
        }
        if (path != null) {
          path = path.substring(strip);
        }
      }

      Matcher m;
      if (path != null && (m = pattern.matcher(path)).matches()) {
        if (!isEmpty(methods) && !containsMethod(request)) {
          // If I'm here path or path pattern matches, but the method is wrong
          return 405;
        }

        context.matchRest = -1;
        context.normalizedMatch = useNormalizedPath;

        if (m.groupCount() > 0) {
          if (!exactPath) {
            context.matchRest = m.start("rest");
            // always replace
            context.pathParams()
              .put("*", path.substring(context.matchRest));
          }

          if (!isEmpty(groups)) {
            // Pattern - named params
            // decode the path as it could contain escaped chars.
            final int len = Math.min(groups.size(), m.groupCount());
            for (int i = 0; i < len; i++) {
              final String k = groups.get(i);
              String undecodedValue;
              // We try to take value in three ways:
              // 1. group name of type p0, p1, pN (most frequent and used by vertx params)
              // 2. group name inside the regex
              // 3. No group name
              try {
                undecodedValue = m.group("p" + i);
              } catch (IllegalArgumentException e) {
                try {
                  undecodedValue = m.group(k);
                } catch (IllegalArgumentException e1) {
                  // Groups starts from 1 (0 group is total match)
                  undecodedValue = m.group(i + 1);
                }
              }
              if (undecodedValue != null) {
                addPathParam(context, k, undecodedValue);
              }
            }
          } else {
            // Straight regex - un-named params
            // decode the path as it could contain escaped chars.
            if (!isEmpty(namedGroupsInRegex)) {
              for (String namedGroup : namedGroupsInRegex) {
                String namedGroupValue = m.group(namedGroup);
                if (namedGroupValue != null) {
                  addPathParam(context, namedGroup, namedGroupValue);
                }
              }
            }
            for (int i = 0; i < m.groupCount(); i++) {
              String group = m.group(i + 1);
              if (group != null) {
                final String k = "param" + i;
                addPathParam(context, k, group);
              }
            }
          }
        }
      } else {
        return 404;
      }
    } else {
      // no pattern check for wrong method
      if (!isEmpty(methods) && !containsMethod(request)) {
        // If I'm here path or path pattern matches, but the method is wrong
        return 405;
      }
    }

    if (!isEmpty(consumes)) {
      // Can this route consume the specified content type
      MIMEHeader contentType = context.parsedHeaders().contentType();
      MIMEHeader consumal = contentType.findMatchedBy(consumes);
      if (consumal == null && !(contentType.rawValue().isEmpty() && emptyBodyPermittedWithConsumes)) {
        if (contentType.rawValue().isEmpty()) {
          return 400;
        } else {
          return 415;
        }
      }
    }
    if (!isEmpty(produces)) {
      List<MIMEHeader> acceptableTypes = context.parsedHeaders().accept();
      if(!acceptableTypes.isEmpty()) {
        MIMEHeader selectedAccept = context.parsedHeaders().findBestUserAcceptedIn(acceptableTypes, produces);
        if (selectedAccept != null) {
          context.setAcceptableContentType(selectedAccept.rawValue());
        } else {
          return 406;
        }
      }
    }
    if (!virtualHostMatches(context.request())) {
      return 404;
    }
    return 0;
  }

  private boolean pathMatches(String mountPoint, RoutingContext ctx) {
    final boolean rootRouter = mountPoint == null;
    final boolean pathEndsWithSlash;
    final String thePath;

    if (rootRouter) {
      thePath = path;
      pathEndsWithSlash = this.pathEndsWithSlash;
    } else {
      boolean mountPointEndsWithSlash = mountPoint.charAt(mountPoint.length() - 1) == '/';
      // path is "/"
      if (path.length() == 1) {
        // mount point is always assumed to be a directory so
        // we must ignore the final slash
        thePath = mountPoint;
        // so this is a special case we can't consider the configured route but the mount point itself
        pathEndsWithSlash = mountPointEndsWithSlash;
      } else {
        // solve the double slash when mount point ends with slash
        if (mountPointEndsWithSlash) {
          thePath = mountPoint + path.substring(1);
        } else {
          thePath = mountPoint + path;
        }
        pathEndsWithSlash = this.pathEndsWithSlash;
      }
    }

    String requestPath;

    if (useNormalizedPath) {
      // never null
      requestPath = ctx.normalizedPath();
    } else {
      requestPath = ctx.request().path();
      // can be null
      if (requestPath == null) {
        requestPath = "/";
      }
    }

    if (exactPath) {
      // exact path has no "rest"
      ctx.pathParams()
        .remove("*");

      return pathMatchesExact(thePath, requestPath, pathEndsWithSlash);
    } else {
      if (pathEndsWithSlash) {
        // the route expects a path that ends in "/*". This is a special case
        // we need to optionally allow any request just like if it was a "*" but
        // treat the slash
        final int pathLen = thePath.length();
        final int reqLen = requestPath.length();

        if (reqLen < pathLen - 2) {
          // we miss at least 2 characters
          return false;
        }

        if (reqLen == pathLen - 1) {
          // request misses 1 character, there is the chance that this request doesn't include the final slash
          // because the mount path ended with a wildcard we are relaxed in the check
          if (thePath.regionMatches(0, requestPath, 0, pathLen - 1)) {
            // handle the "rest" as path param *, always known to be empty
            ctx.pathParams()
              .put("*", "/");
            return true;
          }
        }
      }

      if (requestPath.startsWith(thePath)) {
        // handle the "rest" as path param *
        ctx.pathParams()
          .put("*", URIDecoder.decodeURIComponent(requestPath.substring(thePath.length()), false));
        return true;
      }
      return false;
    }
  }

  private boolean virtualHostMatches(HttpServerRequest request) {
    if (virtualHostPattern == null) {
      return true;
    }

    HostAndPort authority = request.authority();
    if (authority == null) {
      return false;
    }

    String host = authority.host();
    int len = host.length();

    // knowing that the shortest IPv6 is [::]
    if (len > 3 && host.charAt(0) == '[') {
      // attempt to parse IPv6
      int delim = host.indexOf(']');
      if (delim != -1) {
        // the delim must be the terminal character OR right before a ':'
        if (delim == len - 1 || host.charAt(delim + 1) == ':') {
          // OK
          host = host.substring(1, delim);
          return virtualHostPattern.matcher(host).matches();
        }
      }
    }

    // assume IPv4 or name
    int portSeparatorIdx = host.lastIndexOf(':');
    if (portSeparatorIdx != -1) {
      host = host.substring(0, portSeparatorIdx);
    }

    return virtualHostPattern.matcher(host).matches();
  }

  private static boolean pathMatchesExact(String base, String other, boolean significantSlash) {
    // Ignore trailing slash when matching paths
    int len = other.length();

    if (significantSlash) {
      if (other.charAt(len -1) != '/') {
        // final slash is significant but missing
        return false;
      }
    } else {
      if (other.charAt(len -1) == '/') {
        // final slash is not significant, ignore it
        len--;
      }
    }

    // lengths are not the same (fail)
    if (base.length() != len) {
      return false;
    }

    // content must match
    return other.regionMatches(0, base, 0, len);
  }

  private void addPathParam(RoutingContext context, String name, String value) {
    HttpServerRequest request = context.request();
    final String decodedValue = URIDecoder.decodeURIComponent(value, false);
    if (!request.params().contains(name)) {
      request.params().add(name, decodedValue);
    }
    context.pathParams().put(name, decodedValue);
  }

  boolean hasNextContextHandler(RoutingContextImplBase context) {
    return context.currentRouteNextHandlerIndex() < getContextHandlersLength();
  }

  boolean hasNextFailureHandler(RoutingContextImplBase context) {
    return context.currentRouteNextFailureHandlerIndex() < getFailureHandlersLength();
  }

  void handleContext(RoutingContextImplBase context) {
    contextHandlers
      .get(context.currentRouteNextHandlerIndex() - 1)
      .handle(context);
  }

  void handleFailure(RoutingContextImplBase context) {
    failureHandlers
      .get(context.currentRouteNextFailureHandlerIndex() - 1)
      .handle(context);
  }

  public String getName() {
    if (name != null) {
      return name;
    }
    if (path != null) {
      return path;
    }
    if (pattern != null) {
      return pattern.pattern();
    }
    return null;
  }

  @Override
  public String toString() {
    return "RouteState{" +
      "metadata=" + metadata +
      ", path='" + path + '\'' +
      ", name=" + name +
      ", order=" + order +
      ", enabled=" + enabled +
      ", methods=" + methods +
      ", consumes=" + consumes +
      ", emptyBodyPermittedWithConsumes=" + emptyBodyPermittedWithConsumes +
      ", produces=" + produces +
      ", contextHandlers=" + contextHandlers +
      ", failureHandlers=" + failureHandlers +
      ", added=" + added +
      ", pattern=" + pattern +
      ", groups=" + groups +
      ", useNormalizedPath=" + useNormalizedPath +
      ", namedGroupsInRegex=" + namedGroupsInRegex +
      ", virtualHostPattern=" + virtualHostPattern +
      ", pathEndsWithSlash=" + pathEndsWithSlash +
      ", exclusive=" + exclusive +
      ", exactPath=" + exactPath +
      '}';
  }
}
