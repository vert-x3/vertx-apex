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
import io.vertx.core.Vertx;
import io.vertx.ext.auth.htdigest.HtdigestAuth;
import io.vertx.ext.web.handler.impl.DigestAuthHandlerImpl;

/**
 * An auth handler that provides HTTP Basic Authentication support.
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
@VertxGen
public interface DigestAuthHandler extends WebAuthenticationHandler {

  /**
   * The default nonce expire timeout to use in milliseconds.
   */
  long DEFAULT_NONCE_EXPIRE_TIMEOUT = 3600000;

  /**
   * Create a digest auth handler
   *
   * @param vertx        the vertx instance
   * @param authProvider the auth provider to use
   * @return the auth handler
   */
  static DigestAuthHandler create(Vertx vertx, HtdigestAuth authProvider) {
    return new DigestAuthHandlerImpl(vertx, authProvider, DEFAULT_NONCE_EXPIRE_TIMEOUT);
  }

  /**
   * Create a digest auth handler, specifying the expire timeout for nonces.
   *
   * @param vertx              the vertx instance
   * @param authProvider       the auth service to use
   * @param nonceExpireTimeout the nonce expire timeout in milliseconds.
   * @return the auth handler
   */
  static DigestAuthHandler create(Vertx vertx, HtdigestAuth authProvider, long nonceExpireTimeout) {
    return new DigestAuthHandlerImpl(vertx, authProvider, nonceExpireTimeout);
  }
}
