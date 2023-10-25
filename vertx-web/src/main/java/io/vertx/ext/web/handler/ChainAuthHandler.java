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
import io.vertx.ext.web.handler.impl.ChainAuthHandlerImpl;

/**
 * An auth handler that chains to a sequence of handlers.
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
@VertxGen
public interface ChainAuthHandler extends WebAuthenticationHandler {

  /**
   * Create a chain authentication handler that will assert that all handlers pass the verification.
   * @return a new chain authentication handler
   */
  static ChainAuthHandler all() {
    return new ChainAuthHandlerImpl(true);
  }

  /**
   * Create a chain authentication handler that will assert that any handler passes the verification.
   * @return a new chain authentication handler
   */
  static ChainAuthHandler any() {
    return new ChainAuthHandlerImpl(false);
  }

  /**
   * Appends a auth provider to the chain.
   *
   * @param other auth handler
   * @return self
   *
   */
  @Fluent
  ChainAuthHandler add(WebAuthenticationHandler other);
}
