/*
 * Copyright 2023 Red Hat, Inc.
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
package io.vertx.ext.auth.common;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.ext.auth.User;

/**
 * A web user is extended user coupled to the context and is used to perform verifications
 * and actions on behalf of the user. Actions can be:
 *
 * <ul>
 *   <li>{@link  #refresh()} - Require a re-authentication to confirm the user is present</li>
 *   <li>{@link  #impersonate()} - Require a re-authentication to switch user identities</li>
 *   <li>{@link  #restore()} - De-escalate a previous impersonate call</li>
 *   <li>{@link  #logout()} - Logout the user from this application and redirect to a uri</li>
 *   <li>{@link  #clear()} - Same as logout, without requirind a redirect</li>
 * </ul>
 */
@VertxGen
public interface UserContext {

  /**
   * Get the authenticated user (if any). This will usually be injected by an auth handler if authentication if successful.
   * @return  the user, or null if the current user is not authenticated.
   */
  @Nullable User get();

  default boolean authenticated() {
    return get() != null;
  }

  /**
   * When performing a web identity operation, hint if possible to the identity provider to use the given login.
   * @param loginHint the desired login name, for example: {@code admin}.
   * @return fluent self
   */
  @Fluent
  UserContext loginHint(String loginHint);

  /**
   * Forces the current user to re-authenticate. The user will be redirected to the same origin where this call was
   * made. It is important to notice that the redirect will only allow sources originating from a HTTP GET request.
   *
   * @return future result of the operation.
   */
  Future<Void> refresh();

  /**
   * Forces the current user to re-authenticate. The user will be redirected to the given uri. It is important to
   * notice that the redirect will only allow targets using an HTTP GET request.
   *
   * @param redirectUri the uri to redirect the user to after the re-authentication.
   *
   * @return future result of the operation.
   */
  Future<Void> refresh(String redirectUri);

  /**
   * Impersonates a second identity. The user will be redirected to the same origin where this call was
   * made. It is important to notice that the redirect will only allow sources originating from a HTTP GET request.
   *
   * @return future result of the operation.
   */
  Future<Void> impersonate();

  /**
   * Impersonates a second identity. The user will be redirected to the given uri. It is important to
   * notice that the redirect will only allow targets using an HTTP GET request.
   *
   * @param redirectUri the uri to redirect the user to after the authentication.
   *
   * @return future result of the operation.
   */
  Future<Void> impersonate(String redirectUri);

  /**
   *  Undo a previous call to a impersonation. The user will be redirected to the same origin where this call was
   * made. It is important to notice that the redirect will only allow sources originating from a HTTP GET request.
   *
   * @return future result of the operation.
   */
  Future<Void> restore();

  /**
   * Undo a previous call to an impersonation. The user will be redirected to the given uri. It is important to
   * notice that the redirect will only allow targets using an HTTP GET request.
   *
   * @param redirectUri the uri to redirect the user to after the re-authentication.
   *
   * @return future result of the operation.
   */
  Future<Void> restore(String redirectUri);


  /**
   * Logout can be called from any route handler which needs to terminate a login session. Invoking logout will remove
   * the {@link io.vertx.ext.auth.User} and clear the {@link Session} (if any) in the current context. Followed by a
   * redirect to the given uri.
   *
   * @param redirectUri the uri to redirect the user to after the logout.
   *
   * @return future result of the operation.
   */
  Future<Void> logout(String redirectUri);

  /**
   * Logout can be called from any route handler which needs to terminate a login session. Invoking logout will remove
   * the {@link io.vertx.ext.auth.User} and clear the {@link Session} (if any) in the current context. Followed by a
   * redirect to {@code /}.
   *
   * @return future result of the operation.
   */
  Future<Void> logout();

  /**
   * Clear can be called from any route handler which needs to terminate a login session. Invoking logout will remove
   * the {@link io.vertx.ext.auth.User} and clear the {@link Session} (if any) in the current context. Unlike
   * {@link #logout()} no redirect will be performed.
   */
  void clear();
}
