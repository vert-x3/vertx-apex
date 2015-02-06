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
import io.vertx.ext.apex.handler.impl.BasicAuthHandlerImpl;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.auth.AuthService;

/**
 * An auth handler that provides HTTP Basic Authentication support.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface BasicAuthHandler extends AuthHandler {

  /**
   * The default realm to use
   */
  static final String DEFAULT_REALM = "apex";

  /**
   * Create a basic auth handler
   *
   * @param authService  the auth service to use
   * @return the auth handler
   */
  static AuthHandler create(AuthService authService) {
    return new BasicAuthHandlerImpl(authService, DEFAULT_REALM);
  }

  /**
   * Create a basic auth handler, specifying realm
   *
   * @param authService  the auth service to use
   * @param realm  the realm to use
   * @return the auth handler
   */
  static AuthHandler create(AuthService authService, String realm) {
    return new BasicAuthHandlerImpl(authService, realm);
  }

  @Override
  void handle(RoutingContext context);
}
