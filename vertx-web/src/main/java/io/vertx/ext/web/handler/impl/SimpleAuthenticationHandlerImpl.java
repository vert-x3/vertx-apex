package io.vertx.ext.web.handler.impl;

import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.audit.Marker;
import io.vertx.ext.auth.audit.SecurityAudit;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.SimpleAuthenticationHandler;
import io.vertx.ext.web.impl.RoutingContextInternal;

import java.util.function.Function;

public class SimpleAuthenticationHandlerImpl extends WebAuthenticationHandlerImpl<NOOPAuthenticationProvider> implements SimpleAuthenticationHandler {

  private Function<RoutingContext, Future<User>> authn;

  public SimpleAuthenticationHandlerImpl() {
    super(new NOOPAuthenticationProvider());
  }

  @Override
  public Future<User> authenticate(RoutingContext ctx) {
    if (authn != null) {
      final SecurityAudit audit = ((RoutingContextInternal) ctx).securityAudit();

      return authn.apply(ctx)
        .andThen(op -> audit.audit(Marker.AUTHENTICATION, op.succeeded()))
        .recover(err -> {
          if (err instanceof HttpException) {
            return Future.failedFuture(err);
          } else {
            return Future.failedFuture(new HttpException(401, err));
          }
        });
    } else {
      return Future.failedFuture("No Authenticate function");
    }
  }

  @Override
  public SimpleAuthenticationHandlerImpl authenticate(Function<RoutingContext, Future<User>> authnFunction) {
    this.authn = authnFunction;
    return this;
  }
}
