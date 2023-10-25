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

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.audit.Marker;
import io.vertx.ext.auth.audit.SecurityAudit;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.impl.RoutingContextInternal;

import java.util.function.Function;

import static io.vertx.ext.web.handler.HttpException.UNAUTHORIZED;

/**
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
public class APIKeyHandlerImpl extends WebAuthenticationHandlerImpl<AuthenticationProvider> implements APIKeyHandler {

  enum Type {
    HEADER,
    PARAMETER,
    COOKIE
  }

  private Type source = Type.HEADER;
  private String value = "X-API-KEY";
  private Function<String, Future<String>> tokenExtractor = null;

  public APIKeyHandlerImpl(AuthenticationProvider authProvider) {
    super(authProvider);
  }


  @Override
  public APIKeyHandler header(String headerName) {
    if (headerName == null) {
      throw new IllegalArgumentException("'headerName' cannot be null");
    }
    source = Type.HEADER;
    value = headerName;
    return this;
  }

  @Override
  public APIKeyHandler parameter(String paramName) {
    if (paramName == null) {
      throw new IllegalArgumentException("'paramName' cannot be null");
    }
    source = Type.PARAMETER;
    value = paramName;
    return this;
  }

  @Override
  public APIKeyHandler cookie(String cookieName) {
    if (cookieName == null) {
      throw new IllegalArgumentException("'cookieName' cannot be null");
    }
    source = Type.COOKIE;
    value = cookieName;
    return this;
  }

  @Override
  public Future<User> authenticate(RoutingContext context) {
    final SecurityAudit audit = ((RoutingContextInternal) context).securityAudit();
    final String token;

    switch (source) {
      case HEADER:
        MultiMap headers = context.request().headers();
        if (headers != null && headers.contains(value)) {
          token = headers.get(value);
        } else {
          return Future.failedFuture(UNAUTHORIZED);
        }
        break;
      case PARAMETER:
        MultiMap params = context.request().params();
        if (params != null && params.contains(value)) {
          token = params.get(value);
        } else {
          return Future.failedFuture(UNAUTHORIZED);
        }
        break;
      case COOKIE:
        Cookie cookie = context.request().getCookie(value);
        if (cookie != null) {
          token = cookie.getValue();
        } else {
          return Future.failedFuture(UNAUTHORIZED);
        }
        break;
      default:
        // fallback if no api key was found
        return Future.failedFuture(UNAUTHORIZED);
    }

    if (tokenExtractor != null) {
      return tokenExtractor
        .apply(token)
        .compose(processedToken -> authenticate(audit, processedToken));
    } else {
      return authenticate(audit, token);
    }
  }

  private Future<User> authenticate(SecurityAudit audit, String token) {
    final TokenCredentials credentials = new TokenCredentials(token);
    audit.credentials(credentials);

    return authProvider
      .authenticate(credentials)
      .andThen(op -> audit.audit(Marker.AUTHENTICATION, op.succeeded()))
      .recover(err -> Future.failedFuture(new HttpException(401, err)));
  }

  @Override
  public APIKeyHandler tokenExtractor(Function<String, Future<String>> tokenExtractor) {
    this.tokenExtractor = tokenExtractor;
    return this;
  }
}
