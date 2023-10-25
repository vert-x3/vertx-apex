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

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.handler.impl.RedirectAuthHandlerImpl;

/**
 * An auth handler that's used to handle auth by redirecting user to a custom login page.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface RedirectAuthHandler extends WebAuthenticationHandler {

  /**
   * Default path the user will be redirected to
   */
  String DEFAULT_LOGIN_REDIRECT_URL = "/loginpage";

  /**
   * Default name of param used to store return url information in session
   */
  String DEFAULT_RETURN_URL_PARAM = "return_url";

  /**
   * Create a handler
   *
   * @param authProvider  the auth service to use
   * @return the handler
   */
  static RedirectAuthHandler create(AuthenticationProvider authProvider) {
    return new RedirectAuthHandlerImpl(authProvider, DEFAULT_LOGIN_REDIRECT_URL, DEFAULT_RETURN_URL_PARAM);
  }

  /**
   * Create a handler
   *
   * @param authProvider  the auth service to use
   * @param loginRedirectURL  the url to redirect the user to
   * @return the handler
   */
  static RedirectAuthHandler create(AuthenticationProvider authProvider, String loginRedirectURL) {
    return new RedirectAuthHandlerImpl(authProvider, loginRedirectURL, DEFAULT_RETURN_URL_PARAM);
  }

  /**
   * Create a handler
   *
   * @param authProvider  the auth service to use
   * @param loginRedirectURL  the url to redirect the user to
   * @param returnURLParam  the name of param used to store return url information in session
   * @return the handler
   */
  static RedirectAuthHandler create(AuthenticationProvider authProvider, String loginRedirectURL, String returnURLParam) {
    return new RedirectAuthHandlerImpl(authProvider, loginRedirectURL, returnURLParam);
  }
}
