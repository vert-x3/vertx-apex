package io.vertx.ext.web.handler.impl;

import java.util.Collections;
import java.util.List;

import io.vertx.ext.auth.common.AuthenticationHandler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * @see io.vertx.ext.auth.common.ScopedAuthentication
 * @param <SELF>
 */
public interface ScopedAuthentication<SELF extends AuthenticationHandler<RoutingContext>>
  extends io.vertx.ext.auth.common.ScopedAuthentication<RoutingContext, SELF> {

  @Override
  default List<String> getScopesOrSearchMetadata(List<String> scopes, RoutingContext ctx) {
    if (!scopes.isEmpty()) {
      return scopes;
    }

    final Route currentRoute = ctx.currentRoute();

    if (currentRoute == null) {
      return Collections.emptyList();
    }

    final Object value = currentRoute
      .metadata()
      .get("scopes");

    if (value == null) {
      return Collections.emptyList();
    }

    if (value instanceof List) {
      return (List<String>) value;
    }

    if (value instanceof String) {
      return Collections.singletonList((String) value);
    }

    throw new IllegalStateException("Invalid type for scopes metadata: " + value.getClass().getName());
  }
}
