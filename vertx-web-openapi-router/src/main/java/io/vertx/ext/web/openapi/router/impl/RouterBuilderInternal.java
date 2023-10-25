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
package io.vertx.ext.web.openapi.router.impl;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.ext.web.handler.WebAuthenticationHandler;
import io.vertx.ext.web.openapi.router.RouterBuilder;

interface RouterBuilderInternal extends RouterBuilder {

  /**
   * Mount to paths that have to follow a security schema a security handler. This method will not perform any
   * validation weather or not the given {@code securitySchemeName} is present in the OpenAPI document.
   *
   * For most use cases the method {@link #security(String)} should be used.
   *
   * @param securitySchemeName the components security scheme id
   * @param handler the authentication handler
   * @param callback the callback path to be used for the authentication handler
   * @return self
   */
  @Fluent
  RouterBuilder security(String securitySchemeName, WebAuthenticationHandler handler, String callback);
}
