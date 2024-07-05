/*
 * Copyright (c) 2011-2021 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.ext.web.client;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.client.impl.Oauth2WebClientAware;

/**
 * An asynchronous OAuth2/OIDC aware HTTP / HTTP/2 client called {@code WebClientOAuth2}.
 * <p>
 * This client wraps a {@link WebClient} and makes it session aware adding features to it:
 * <ul>
 *   <li>Request an {@code access_token} if no user is created</li>
 *   <li>Refresh {@code access_token} if current user is expired</li>
 * </ul>
 * <p>
 */
@VertxGen
public interface OAuth2WebClient extends WebClient {

  /**
   * Create a session aware web client using the provided {@code webClient} instance.
   *
   * @param webClient the web client instance
   * @param oAuth2Auth Configured oAuth2Auth provider to be used when {@link #withCredentials(Credentials)} used
   * @return the created client
   */
  static OAuth2WebClient create(WebClient webClient, OAuth2Auth oAuth2Auth) {
    return create(webClient, oAuth2Auth, new OAuth2WebClientOptions());
  }

  /**
   * Create a session aware web client using the provided {@code webClient} instance.
   *
   * @param webClient the web client instance
   * @param oAuth2Auth Configured oAuth2Auth provider to be used when {@link #withCredentials(Credentials)} used
   * @param options extra configuration for this object
   * @return the created client
   */
  static OAuth2WebClient create(WebClient webClient, OAuth2Auth oAuth2Auth, OAuth2WebClientOptions options) {
    return new Oauth2WebClientAware(webClient, oAuth2Auth, options);
  }

  /**
   * Mark that request should be dispatched with authentication obtained from passed {@code OAuth2Auth} provider
   *
   * @return a reference to this, so the API can be used fluently
   */

  @Fluent
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  OAuth2WebClient withCredentials(Credentials credentials);

  /**
   * Get the authenticated user (if any) that is associated with this client.
   * @return the current user associated with this client or null if no user is associated
   */
  User getUser();
}
