/* ******************************************************************************
 * Copyright (c) 2019 Stephane Bastian
 *
 * This program and the accompanying materials are made available under the 2
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 3
 *
 * Contributors: 1
 *   Stephane Bastian - initial API and implementation
 * ******************************************************************************/
package io.vertx.ext.web.handler.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.AuthorizationContext;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthorizationHandler;
import io.vertx.ext.web.handler.HttpException;

/**
 * Implementation of the {@link io.vertx.ext.web.handler.AuthorizationHandler}
 *
 * @author <a href="mail://stephane.bastian.dev@gmail.com">Stephane Bastian</a>
 */
public class AuthorizationHandlerImpl implements AuthorizationHandler {

  private final static Logger LOG = LoggerFactory.getLogger(AuthorizationHandler.class);

  private final static int FORBIDDEN_CODE = 403;
  private final static HttpException FORBIDDEN_EXCEPTION = new HttpException(FORBIDDEN_CODE);

  private final Authorization authorization;
  private final Collection<AuthorizationProvider> authorizationProviders;
  private BiConsumer<RoutingContext, AuthorizationContext> variableHandler;

  public AuthorizationHandlerImpl(Authorization authorization) {
    this.authorization = Objects.requireNonNull(authorization);
    this.authorizationProviders = new ArrayList<>();
  }

  @Override
  public void handle(RoutingContext routingContext) {
    if (routingContext.user() == null) {
      routingContext.fail(FORBIDDEN_CODE, FORBIDDEN_EXCEPTION);
    } else {
      // before starting any potential async operation here
      // pause parsing the request body. The reason is that
      // we don't want to loose the body or protocol upgrades
      // for async operations
      final boolean parseEnded = routingContext.request().isEnded();
      if (!parseEnded) {
        routingContext.request().pause();
      }
      try {
        // create the authorization context
        AuthorizationContext authorizationContext = getAuthorizationContext(routingContext);
        // check or fetch authorizations
        checkOrFetchAuthorizations(routingContext, parseEnded, authorizationContext, authorizationProviders.iterator());
      } catch (RuntimeException e) {
        // resume as the error handler may allow this request to become valid again
        resume(routingContext.request(), parseEnded);
        routingContext.fail(e);
      }
    }
  }

  @Override
  public AuthorizationHandler variableConsumer(BiConsumer<RoutingContext, AuthorizationContext> handler) {
    this.variableHandler = handler;
    return this;
  }

  /**
   * this method checks that the specified authorization match the current content.
   * It doesn't fetch all providers at once in order to do early-out, but rather tries to be smart and fetch authorizations one provider at a time
   *
   * @param routingContext the current routing context
   * @param authorizationContext the current authorization context
   * @param providers the providers iterator
   */
  private void checkOrFetchAuthorizations(RoutingContext routingContext, boolean parseEnded, AuthorizationContext authorizationContext, Iterator<AuthorizationProvider> providers) {
    if (authorization.match(authorizationContext)) {
      // resume the processing of the request
      resume(routingContext.request(), parseEnded);
      routingContext.next();
      return;
    }
    if (!providers.hasNext()) {
      // resume as the error handler may allow this request to become valid again
      resume(routingContext.request(), parseEnded);
      routingContext.fail(FORBIDDEN_CODE, FORBIDDEN_EXCEPTION);
      return;
    }

    // there was no match, in this case we do the following:
    // 1) contact the next provider we haven't contacted yet
    // 2) if there is a match, get out right away otherwise repeat 1)
    while (providers.hasNext()) {
      AuthorizationProvider provider = providers.next();
      // we haven't fetch authorization from this provider yet
      if (! routingContext.user().authorizations().getProviderIds().contains(provider.getId())) {
        provider.getAuthorizations(routingContext.user(), authorizationResult -> {
          if (authorizationResult.failed()) {
            LOG.warn("An error occured getting authorization - providerId: " + provider.getId(), authorizationResult.cause());
            // note that we don't 'record' the fact that we tried to fetch the authorization provider. therefore it will be re-fetched later-on
          }
          checkOrFetchAuthorizations(routingContext, parseEnded, authorizationContext, providers);
        });
        // get out right now as the callback will decide what to do next
        return;
      }
    }
  }

  private AuthorizationContext getAuthorizationContext(RoutingContext event) {
    final AuthorizationContext result = AuthorizationContext.create(event.user());
    if (variableHandler != null) {
      variableHandler.accept(event, result);
    }
    return result;
  }

  @Override
  public AuthorizationHandler addAuthorizationProvider(AuthorizationProvider authorizationProvider) {
    Objects.requireNonNull(authorizationProvider);

    this.authorizationProviders.add(authorizationProvider);
    return this;
  }

  private void resume(HttpServerRequest request, final boolean parseEnded) {
    if (!parseEnded && !request.headers().contains(HttpHeaders.UPGRADE, HttpHeaders.WEBSOCKET, true)) {
      request.resume();
    }
  }
}
