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

package io.vertx.ext.web.handler.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.audit.Marker;
import io.vertx.ext.auth.audit.SecurityAudit;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.common.handler.impl.HTTPAuthorizationHandler;
import io.vertx.ext.auth.jwt.AbstractJWTHandler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.impl.RoutingContextInternal;

/**
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
public class JWTAuthHandlerImpl extends AbstractJWTHandler<RoutingContext> implements JWTAuthHandler {

  private final List<String> scopes;
  private String delimiter;

  public JWTAuthHandlerImpl(JWTAuth authProvider, String realm) {
    super(authProvider, Type.BEARER, realm);
    scopes = Collections.emptyList();
    this.delimiter = " ";
  }

  private JWTAuthHandlerImpl(JWTAuthHandlerImpl base, List<String> scopes, String delimiter) {
    super(base.authProvider, Type.BEARER, base.realm);
    Objects.requireNonNull(scopes, "scopes cannot be null");
    this.scopes = scopes;
    Objects.requireNonNull(delimiter, "delimiter cannot be null");
    this.delimiter = delimiter;
  }

  @Override
  public Future<User> authenticate(RoutingContext context) {

    return parseAuthorization(context)
      .compose(token -> {
        int segments = 0;
        for (int i = 0; i < token.length(); i++) {
          char c = token.charAt(i);
          if (c == '.') {
            if (++segments == 3) {
              return Future.failedFuture(new HttpException(400, "Too many segments in token"));
            }
            continue;
          }
          if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
            continue;
          }
          // invalid character
          return Future.failedFuture(new HttpException(400, "Invalid character in token: " + (int) c));
        }

        final TokenCredentials credentials = new TokenCredentials(token);
        final SecurityAudit audit = ((RoutingContextInternal) context).securityAudit();
        audit.credentials(credentials);

        return
          authProvider
            .authenticate(new TokenCredentials(token))
            .andThen(op -> audit.audit(Marker.AUTHENTICATION, op.succeeded()))
            .recover(err -> Future.failedFuture(new HttpException(401, err)));
      });
  }

  @Override
  public JWTAuthHandler withScope(String scope) {
    Objects.requireNonNull(scope, "scope cannot be null");
    List<String> updatedScopes = new ArrayList<>(this.scopes);
    updatedScopes.add(scope);
    return new JWTAuthHandlerImpl(this, updatedScopes, delimiter);
  }

  @Override
  public JWTAuthHandler withScopes(List<String> scopes) {
    Objects.requireNonNull(scopes, "scopes cannot be null");
    return new JWTAuthHandlerImpl(this, scopes, delimiter);
  }

  @Override
  public JWTAuthHandler scopeDelimiter(String delimiter) {
    Objects.requireNonNull(delimiter, "delimiter cannot be null");
    this.delimiter = delimiter;
    return this;
  }

  /**
   * The default behavior for post-authentication
   */
  @Override
  public void postAuthentication(RoutingContext ctx, User authenticated) {
    final User user = ctx.user().get();
    if (user == null) {
      // bad state
      fail(ctx, 403, "no user in the context");
      return;
    }
    // the user is authenticated, however the user may not have all the required scopes
    final List<String> scopes = getScopesOrSearchMetadata(this.scopes, ctx);

    if (scopes.size() > 0) {
      final JsonObject jwt = user.get("accessToken");
      if (jwt == null) {
        fail(ctx, 403, "Invalid JWT: null");
        return;
      }

      if (jwt.getValue("scope") == null) {
        fail(ctx, 403, "Invalid JWT: scope claim is required");
        return;
      }

      List<?> target;
      if (jwt.getValue("scope") instanceof String) {
        target =
          Stream.of(jwt.getString("scope")
              .split(delimiter))
            .collect(Collectors.toList());
      } else {
        target = jwt.getJsonArray("scope").getList();
      }

      if (target != null) {
        for (String scope : scopes) {
          if (!target.contains(scope)) {
            fail(ctx, 403, "JWT scopes != handler scopes");
            return;
          }
        }
      }
    }
    ctx.next();
  }
  
  // TODO remove duplicated code from WebAuthenticationHandlerImpl
  /**
   * This method is protected so custom auth handlers can override the default error handling
   */
  protected void processException(RoutingContext ctx, Throwable exception) {
    if (exception != null) {
      if (exception instanceof HttpException) {
        final int statusCode = ((HttpException) exception).getStatusCode();
        final String payload = ((HttpException) exception).getPayload();

        switch (statusCode) {
        case 302:
          ctx.response()
            .putHeader(HttpHeaders.LOCATION, payload)
            .setStatusCode(302)
            .end("Redirecting to " + payload + ".");
          return;
        case 401:
          if (!"XMLHttpRequest".equals(ctx.request().getHeader("X-Requested-With"))) {
            setAuthenticateHeader(ctx);
          }
          ctx.fail(401, exception);
          return;
        default:
          ctx.fail(statusCode, exception);
          return;
        }
      }
    }

    // fallback 500
    ctx.fail(exception);
  }

  private void fail(RoutingContext ctx, int code, String msg) {
    ctx.fail(code, new IllegalStateException(msg));
  }
}
