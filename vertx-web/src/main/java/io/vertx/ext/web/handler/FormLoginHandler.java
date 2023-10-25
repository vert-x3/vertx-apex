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
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.handler.impl.FormLoginHandlerImpl;

/**
 * Handler that handles login from a form on a custom login page.
 * <p>
 * Used in conjunction with the {@link RedirectAuthHandler}.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface FormLoginHandler extends WebAuthenticationHandler {

  /**
   * The default value of the form attribute which will contain the username
   */
  String DEFAULT_USERNAME_PARAM = "username";

  /**
   * The default value of the form attribute which will contain the password
   */
  String DEFAULT_PASSWORD_PARAM = "password";

  /**
   * The default value of the session attribute which will contain the return url
   */
  String DEFAULT_RETURN_URL_PARAM = "return_url";

  /**
   * Create a handler
   *
   * @param authProvider  the auth service to use
   * @return the handler
   */
  static FormLoginHandler create(AuthenticationProvider authProvider) {
    return new FormLoginHandlerImpl(authProvider, DEFAULT_USERNAME_PARAM, DEFAULT_PASSWORD_PARAM,
      DEFAULT_RETURN_URL_PARAM, null);
  }

  /**
   * Create a handler
   *
   * @param authProvider  the auth service to use
   * @param usernameParam   the value of the form attribute which will contain the username
   * @param passwordParam   the value of the form attribute which will contain the password
   * @param returnURLParam   the value of the session attribute which will contain the return url
   * @param directLoggedInOKURL a url to redirect to if the user logs in directly at the url of the form login handler
   *                            without being redirected here first
   *
   * @return the handler
   */
  static FormLoginHandler create(AuthenticationProvider authProvider, String usernameParam, String passwordParam,
                                 String returnURLParam, String directLoggedInOKURL) {
    return new FormLoginHandlerImpl(authProvider, usernameParam, passwordParam, returnURLParam, directLoggedInOKURL);
  }

  /**
   * Set the name of the form param used to submit the username
   * @param usernameParam  the name of the param
   * @return a reference to this for a fluent API
   */
  @Fluent
  FormLoginHandler setUsernameParam(String usernameParam);

  /**
   * Set the name of the form param used to submit the password
   * @param passwordParam  the name of the param
   * @return a reference to this for a fluent API
   */
  @Fluent
  FormLoginHandler setPasswordParam(String passwordParam);

  /**
   * Set the name of the session attrioute used to specify the return url
   * @param returnURLParam  the name of the param
   * @return a reference to this for a fluent API
   */
  @Fluent
  FormLoginHandler setReturnURLParam(String returnURLParam);

  /**
   * Set the url to redirect to if the user logs in directly at the url of the form login handler
   * without being redirected here first
   * @param directLoggedInOKURL  the URL to redirect to
   * @return a reference to this for a fluent API
   */
  @Fluent
  FormLoginHandler setDirectLoggedInOKURL(String directLoggedInOKURL);


}


