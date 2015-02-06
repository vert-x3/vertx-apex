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

package io.vertx.ext.apex.handler;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.apex.handler.impl.RedirectAuthHandlerImpl;
import io.vertx.ext.auth.AuthService;

/**
 * An auth handler that's used to handle auth by redirecting user to a custom login page.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface RedirectAuthHandler extends AuthHandler {

  /**
   * Default path the user will be redirected to
   */
  static final String DEFAULT_LOGIN_REDIRECT_URL = "/loginpage";

  /**
   * Default name of param used to store return url information in session
   */
  static final String DEFAULT_RETURN_URL_PARAM = "return_url";

  /**
   * Create a handler
   *
   * @param authService  the auth service to use
   * @return the handler
   */
  static AuthHandler create(AuthService authService) {
    return new RedirectAuthHandlerImpl(authService, DEFAULT_LOGIN_REDIRECT_URL, DEFAULT_RETURN_URL_PARAM);
  }

  /**
   * Create a handler
   *
   * @param authService  the auth service to use
   * @param loginRedirectURL  the url to redirect the user to
   * @return the handler
   */
  static AuthHandler create(AuthService authService, String loginRedirectURL) {
    return new RedirectAuthHandlerImpl(authService, loginRedirectURL, DEFAULT_RETURN_URL_PARAM);
  }

  /**
   * Create a handler
   *
   * @param authService  the auth service to use
   * @param loginRedirectURL  the url to redirect the user to
   * @param returnURLParam  the name of param used to store return url information in session
   * @return the handler
   */
  static AuthHandler create(AuthService authService, String loginRedirectURL, String returnURLParam) {
    return new RedirectAuthHandlerImpl(authService, loginRedirectURL, returnURLParam);
  }
}
