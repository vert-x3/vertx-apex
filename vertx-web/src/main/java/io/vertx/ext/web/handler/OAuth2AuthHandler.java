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

package io.vertx.ext.web.handler;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl;

/**
 * An auth handler that provides OAuth2 Authentication support. This handler is suitable for AuthCode flows.
 *
 * @author Paulo Lopes
 */
@VertxGen
public interface OAuth2AuthHandler extends AuthenticationHandler {

  /**
   * Create a OAuth2 auth handler with host pinning
   *
   * @param vertx  the vertx instance
   * @param authProvider  the auth provider to use
   * @param callbackURL the callback URL you entered in your provider admin console, usually it should be something like: `https://myserver:8888/callback`
   * @return the auth handler
   */
  static OAuth2AuthHandler create(Vertx vertx, OAuth2Auth authProvider, String callbackURL) {
    if (callbackURL == null) {
      throw new IllegalArgumentException("callbackURL cannot be null");
    }
    return new OAuth2AuthHandlerImpl(vertx, authProvider, callbackURL);
  }

  /**
   * Create a OAuth2 auth handler without host pinning.
   * Most providers will not look to the redirect url but always redirect to
   * the preconfigured callback. So this factory does not provide a callback url.
   *
   * @param vertx  the vertx instance
   * @param authProvider  the auth provider to use
   * @return the auth handler
   */
  static OAuth2AuthHandler create(Vertx vertx, OAuth2Auth authProvider) {
    return new OAuth2AuthHandlerImpl(vertx, authProvider, null);
  }

  /**
   * Extra parameters needed to be passed while requesting a token.
   *
   * @param extraParams extra optional parameters.
   * @return self
   */
  @Fluent
  OAuth2AuthHandler extraParams(JsonObject extraParams);

  /**
   * scopes to be requested while requesting a token.
   *
   * @param scope scope.
   * @return self
   */
  @Fluent
  OAuth2AuthHandler withScope(String scope);

  /**
   * Indicates the type of user interaction that is required. Not all providers support this or the full list.
   *
   * Well known values are:
   *
   * <ul>
   *   <li><b>login</b> will force the user to enter their credentials on that request, negating single-sign on.</li>
   *   <li><b>none</b> is the opposite - it will ensure that the user isn't presented with any interactive prompt whatsoever. If the request can't be completed silently via single-sign on, the Microsoft identity platform endpoint will return an interaction_required error.</li>
   *   <li><b>consent</b> will trigger the OAuth consent dialog after the user signs in, asking the user to grant permissions to the app.</li>
   *   <li><b>select_account</b> will interrupt single sign-on providing account selection experience listing all the accounts either in session or any remembered account or an option to choose to use a different account altogether.</li>
   *   <li><b></b></li>
   * </ul>
   *
   * @param prompt the prompt choice.
   * @return self
   */
  @Fluent
  OAuth2AuthHandler prompt(String prompt);

  /**
   * add the callback handler to a given route.
   * @param route a given route e.g.: `/callback`
   * @return self
   */
  @Fluent
  OAuth2AuthHandler setupCallback(Route route);
}
