/*
 * Copyright 2021 Red Hat, Inc.
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
package io.vertx.ext.web.handler.crud;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * Represents a user defined create object function. Given the {@code newObject} request body, the function returns a
 * future result with the newly created {@code unique identifier} for the inserted object.
 *
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
@VertxGen
@FunctionalInterface
public interface CreateHandler<T> {

  /**
   * Create/Insert new object function. The function must return the newly created unique identifier for the object.
   *
   * @param newObject the object to store.
   * @return Future result with the newly created unique identifier.
   */
  Future<String> handle(T newObject);
}
