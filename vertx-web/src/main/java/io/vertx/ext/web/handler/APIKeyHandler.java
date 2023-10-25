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
import io.vertx.core.Future;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.handler.impl.APIKeyHandlerImpl;

import java.util.function.Function;

/**
 * An authentication handler that provides API Key support.
 *
 * API keys can be extracted from {@code HTTP headers/query parameters/cookies}.
 *
 * By default this handler will extract the API key from an HTTP header named {@code X-API-KEY}.
 *
 * @author Paulo Lopes
 */
@VertxGen
public interface APIKeyHandler extends WebAuthenticationHandler {

  /**
   * Create an API Key authentication handler
   *
   * @param authProvider the auth provider to use
   * @return the auth handler
   */
  static APIKeyHandler create(AuthenticationProvider authProvider) {
    return new APIKeyHandlerImpl(authProvider);
  }

  /**
   * Specify the source for the api key extraction as an HTTP header with the given name.
   *
   * @param headerName the header name containing the API key
   * @return fluent self
   */
  @Fluent
  APIKeyHandler header(String headerName);

  /**
   * Specify the source for the api key extraction as an HTTP query parameter with the given name.
   *
   * @param paramName the parameter name containing the API key
   * @return fluent self
   */
  @Fluent
  APIKeyHandler parameter(String paramName);

  /**
   * Specify the source for the api key extraction as an HTTP cookie with the given name.
   *
   * @param cookieName the cookie name containing the API key
   * @return fluent self
   */
  @Fluent
  APIKeyHandler cookie(String cookieName);

  /**
   * Transform from user's token format to the AuthenticationHandler's format.
   *
   * @param tokenExtractor extract the token from the origin payload
   * @return fluent self
   */
  @Fluent
  APIKeyHandler tokenExtractor(Function<String, Future<String>> tokenExtractor);
}
