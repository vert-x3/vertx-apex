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
package io.vertx.ext.web.common;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * An utility exception class to signal HTTP failures.
 *
 * The class with convey an http status code, by default is is {@code 500}. The exception may contain a cause throwable
 * and for special cases a simple payload string may be added for context. The payload can be used for example perform
 * a redirect.
 *
 * The message for the exception is inferred from the standard http error code using {@link HttpResponseStatus}.
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public abstract class HttpException extends RuntimeException {

  private final int statusCode;
  private final String payload;

  public HttpException() {
    this(500, null, null);
  }

  public HttpException(int statusCode) {
    this(statusCode, null, null);
  }

  public HttpException(int statusCode, Throwable cause) {
      this(statusCode, null, cause);
    }

  public HttpException(int statusCode, String payload) {
    this(statusCode, payload, null);
  }

  public HttpException(int statusCode, String payload, Throwable cause) {
    super(HttpResponseStatus.valueOf(statusCode).reasonPhrase(), cause, false, false);
    this.statusCode = statusCode;
    this.payload = payload;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getPayload() {
    return payload;
  }
}
