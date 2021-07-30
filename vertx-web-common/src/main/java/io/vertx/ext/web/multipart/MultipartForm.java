/*
 * Copyright (c) 2011-2018 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.web.multipart;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.multipart.impl.MultipartFormImpl;

import java.nio.charset.Charset;

/**
 * A multipart form.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface MultipartForm extends Iterable<FormDataPart> {

  /**
   * @return a multipart form instance
   */
  static MultipartForm create() {
    return new MultipartFormImpl();
  }

  /**
   * Set the {@code charset} to use when encoding the form. The default charset is {@link java.nio.charset.StandardCharsets#UTF_8}.
   *
   * @param charset the charset to use
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MultipartForm setCharset(String charset);

  /**
   * Set the {@code charset} to use when encoding the form. The default charset is {@link java.nio.charset.StandardCharsets#UTF_8}.
   *
   * @param charset the charset to use
   * @return a reference to this, so the API can be used fluently
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  @Fluent
  MultipartForm setCharset(Charset charset);

  /**
   * @return the charset to use when encoding the form
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  Charset getCharset();

  /**
   * Add an attribute form data part.
   *
   * @param name  the name of the attribute
   * @param value the value of the attribute
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MultipartForm attribute(String name, String value);

  /**
   * Add a text file upload form data part.
   *
   * @param name      name of the parameter
   * @param filename  filename of the file
   * @param pathname  the pathname of the file
   * @param mediaType the MIME type of the file
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MultipartForm textFileUpload(String name, String filename, String pathname, String mediaType);

  /**
   * Add a text file upload form data part.
   *
   * @param name      name of the parameter
   * @param filename  filename of the file
   * @param content  the content of the file
   * @param mediaType the MIME type of the file
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MultipartForm textFileUpload(String name, String filename, Buffer content, String mediaType);

  /**
   * Add a binary file upload form data part.
   *
   * @param name      name of the parameter
   * @param filename  filename of the file
   * @param pathname  the pathname of the file
   * @param mediaType the MIME type of the file
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MultipartForm binaryFileUpload(String name, String filename, String pathname, String mediaType);

  /**
   * Add a binary file upload form data part.
   *
   * @param name      name of the parameter
   * @param filename  filename of the file
   * @param content  the content of the file
   * @param mediaType the MIME type of the file
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MultipartForm binaryFileUpload(String name, String filename, Buffer content, String mediaType);

}
