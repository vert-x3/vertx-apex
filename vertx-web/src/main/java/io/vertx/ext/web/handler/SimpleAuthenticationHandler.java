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
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.SimpleAuthenticationHandlerImpl;

import java.util.function.Function;

/**
 * A user customizable authentication handler.
 * <p>
 * An auth handler allows your application to provide authentication support. The handler is not fully functional
 * without a authentication function. This function takes the {@link RoutingContext} as input and returns a {@link Future}.
 *
 * The future should return a non {@code null} user object. In the {@link #authenticate(Function)} you have full control
 * on the request, so all operations like redirect, next, fail are allowed. There are some rules that need to be followed
 * in order to allow this handler to properly interop with {@link ChainAuthHandler}.
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
@VertxGen
public interface SimpleAuthenticationHandler extends WebAuthenticationHandler {

  /**
   * Creates a new instance of the simple authentication handler.
   * @return a new instance.
   */
  static SimpleAuthenticationHandler create() {
    return new SimpleAuthenticationHandlerImpl();
  }

  /**
   * This function will allow you to perform authentication the way you intended to. The process should be self
   * explanatory, for example if the request contains a given property completing the function with a non {@code null}
   * {@link User} object is enough to allow the handler to continue.
   *
   * In order to signal errors, you should not call {@link RoutingContext#fail(int)} or {@link RoutingContext#fail(Throwable)}.
   *
   * Errors should be signaled using the {@link HttpException} type. Any other kind of errors will be wrapped as a 401
   * error. For example forbidden access should be signaled with: {@code new HttpException(403)}. This is required when
   * working with {@link ChainAuthHandler}. By using exceptions to signal failures instead of immediately terminating
   * the request, it allows the chain to proceed to the next handler if needed.
   *
   * @param authenticationFunction the authentication function.
   * @return self
   */
  @Fluent
  SimpleAuthenticationHandler authenticate(Function<RoutingContext, Future<User>> authenticationFunction);
}
