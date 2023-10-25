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

package io.vertx.ext.auth.common.handler.impl;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.common.AuthenticationContext;
import io.vertx.ext.auth.common.Session;
import io.vertx.ext.auth.common.UserContextInternal;
import io.vertx.ext.auth.common.handler.AuthenticationHandlerInternal;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class AuthenticationHandlerImpl<C extends AuthenticationContext, T extends AuthenticationProvider> implements AuthenticationHandlerInternal<C> {

  protected final T authProvider;
  // signal the kind of Multi-Factor Authentication used by the handler
  protected final String mfa;

  public AuthenticationHandlerImpl(T authProvider) {
    this(authProvider, null);
  }

  public AuthenticationHandlerImpl(T authProvider, String mfa) {
    this.authProvider = authProvider;
    this.mfa = mfa;
  }

  public void handle(C ctx) {

    if (handlePreflight(ctx)) {
      return;
    }

    // pause the request
    if (!ctx.request().isEnded()) {
      ctx.request().pause();
    }

    final User user = ctx.user().get();

    if (user != null) {
      if (mfa != null) {
        // if we're dealing with MFA, the user principal must include a matching mfa
        if (mfa.equals(user.get("mfa"))) {
          // proceed with the router
          if (!ctx.request().isEnded()) {
            ctx.request().resume();
          }
          postAuthentication(ctx, user);
          return;
        }
      } else {
        // proceed with the router
        if (!ctx.request().isEnded()) {
          ctx.request().resume();
        }
        postAuthentication(ctx, user);
        return;
      }
    }
    // perform the authentication
    authenticate(ctx)
      .onSuccess(authenticated -> {
        ((UserContextInternal) ctx.user())
          .setUser(authenticated);
        Session session = ctx.session();
        if (session != null) {
          // the user has upgraded from unauthenticated to authenticated
          // session should be upgraded as recommended by owasp
          session.regenerateId();
        }
        // proceed with the router
        if (!ctx.request().isEnded()) {
          ctx.request().resume();
        }
        postAuthentication(ctx, authenticated);
      })
      .onFailure(cause -> {
        // to allow further processing if needed
        if (!ctx.request().isEnded()) {
          ctx.request().resume();
        }
        processException(ctx, cause);
    });
  }

  protected abstract void processException(C ctx, Throwable cause);

  private boolean handlePreflight(C ctx) {
    final HttpServerRequest request = ctx.request();
    // See: https://www.w3.org/TR/cors/#cross-origin-request-with-preflight-0
    // Preflight requests should not be subject to security due to the reason UAs will remove the Authorization header
    if (request.method() == HttpMethod.OPTIONS) {
      // check if there is a access control request header
      final String accessControlRequestHeader = ctx.request().getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
      if (accessControlRequestHeader != null) {
        // lookup for the Authorization header
        for (String ctrlReq : accessControlRequestHeader.split(",")) {
          if (ctrlReq.equalsIgnoreCase("Authorization")) {
            // this request has auth in access control, so we can allow preflighs without authentication
            ctx.onContinue();
            return true;
          }
        }
      }
    }

    return false;
  }
}
