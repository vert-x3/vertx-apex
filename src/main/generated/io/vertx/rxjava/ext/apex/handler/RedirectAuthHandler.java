/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.rxjava.ext.apex.handler;

import java.util.Map;
import io.vertx.lang.rxjava.InternalHelper;
import rx.Observable;
import io.vertx.rxjava.ext.apex.RoutingContext;
import java.util.Set;
import io.vertx.rxjava.ext.auth.AuthProvider;

/**
 * An auth handler that's used to handle auth by redirecting user to a custom login page.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.apex.handler.RedirectAuthHandler original} non RX-ified interface using Vert.x codegen.
 */

public class RedirectAuthHandler implements AuthHandler {

  final io.vertx.ext.apex.handler.RedirectAuthHandler delegate;

  public RedirectAuthHandler(io.vertx.ext.apex.handler.RedirectAuthHandler delegate) {
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  public void handle(RoutingContext arg0) { 
    this.delegate.handle((io.vertx.ext.apex.RoutingContext) arg0.getDelegate());
  }

  /**
   * Add a required role for this auth handler
   * @param role the role
   * @return a reference to this, so the API can be used fluently
   */
  public AuthHandler addRole(String role) { 
    this.delegate.addRole(role);
    return this;
  }

  /**
   * Add a required permission for this auth handler
   * @param permission the permission
   * @return a reference to this, so the API can be used fluently
   */
  public AuthHandler addPermission(String permission) { 
    this.delegate.addPermission(permission);
    return this;
  }

  /**
   * Add a set of required roles for this auth handler
   * @param roles the set of roles
   * @return a reference to this, so the API can be used fluently
   */
  public AuthHandler addRoles(Set<String> roles) { 
    this.delegate.addRoles(roles);
    return this;
  }

  /**
   * Add a set of required permissions for this auth handler
   * @param permissions the set of permissions
   * @return a reference to this, so the API can be used fluently
   */
  public AuthHandler addPermissions(Set<String> permissions) { 
    this.delegate.addPermissions(permissions);
    return this;
  }

  /**
   * Create a handler
   * @param authProvider the auth service to use
   * @return the handler
   */
  public static AuthHandler create(AuthProvider authProvider) { 
    AuthHandler ret= AuthHandler.newInstance(io.vertx.ext.apex.handler.RedirectAuthHandler.create((io.vertx.ext.auth.AuthProvider) authProvider.getDelegate()));
    return ret;
  }

  /**
   * Create a handler
   * @param authProvider the auth service to use
   * @param loginRedirectURL the url to redirect the user to
   * @return the handler
   */
  public static AuthHandler create(AuthProvider authProvider, String loginRedirectURL) { 
    AuthHandler ret= AuthHandler.newInstance(io.vertx.ext.apex.handler.RedirectAuthHandler.create((io.vertx.ext.auth.AuthProvider) authProvider.getDelegate(), loginRedirectURL));
    return ret;
  }

  /**
   * Create a handler
   * @param authProvider the auth service to use
   * @param loginRedirectURL the url to redirect the user to
   * @param returnURLParam the name of param used to store return url information in session
   * @return the handler
   */
  public static AuthHandler create(AuthProvider authProvider, String loginRedirectURL, String returnURLParam) { 
    AuthHandler ret= AuthHandler.newInstance(io.vertx.ext.apex.handler.RedirectAuthHandler.create((io.vertx.ext.auth.AuthProvider) authProvider.getDelegate(), loginRedirectURL, returnURLParam));
    return ret;
  }


  public static RedirectAuthHandler newInstance(io.vertx.ext.apex.handler.RedirectAuthHandler arg) {
    return new RedirectAuthHandler(arg);
  }
}
