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

/**
 * @see io.vertx.ext.web.common.HttpException
 */
public final class HttpException extends io.vertx.ext.web.common.HttpException {

  public static final HttpException UNAUTHORIZED = new HttpException(401);
  public static final HttpException BAD_REQUEST = new HttpException(400);
  public static final HttpException BAD_METHOD = new HttpException(405);

  public HttpException() {
    super();
  }

  public HttpException(int statusCode) {
    super(statusCode);
  }

  public HttpException(int statusCode, Throwable cause) {
    super(statusCode, cause);
  }

  public HttpException(int statusCode, String payload) {
    super(statusCode, payload);
  }

  public HttpException(int statusCode, String payload, Throwable cause) {
    super(statusCode, payload, cause);
  }

}
