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
import io.vertx.ext.web.handler.impl.BasicAuthHandlerImpl;

/**
 * An auth handler that provides HTTP Basic Authentication support.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface BasicAuthHandler extends WebAuthenticationHandler {

  /**
   * The default realm to use
   */
  String DEFAULT_REALM = "vertx-web";

  /**
   * Create a basic auth handler
   *
   * @param authProvider  the auth provider to use
   * @return the auth handler
   */
  static BasicAuthHandler create(AuthenticationProvider authProvider) {
    return new BasicAuthHandlerImpl(authProvider, DEFAULT_REALM);
  }

  /**
   * Create a basic auth handler, specifying realm
   *
   * @param authProvider  the auth service to use
   * @param realm  the realm to use
   * @return the auth handler
   */
  static BasicAuthHandler create(AuthenticationProvider authProvider, String realm) {
    return new BasicAuthHandlerImpl(authProvider, realm);
  }
}
