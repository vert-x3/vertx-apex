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

package io.vertx.ext.web.templ.impl;

import io.vertx.ext.web.impl.ConcurrentLRUCache;
import io.vertx.ext.web.templ.TemplateEngine;

import java.util.Objects;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class CachingTemplateEngine<T> implements TemplateEngine {

  protected final ConcurrentLRUCache<String, T> cache;
  protected String extension;
  protected String path;

  protected CachingTemplateEngine(String path, String ext, int maxCacheSize) {
    Objects.requireNonNull(ext);
    if (maxCacheSize < 1) {
      throw new IllegalArgumentException("maxCacheSize must be >= 1");
    }
    doSetExtension(ext);
    doSetPath(path);
    this.cache = new ConcurrentLRUCache<>(maxCacheSize);
  }

  protected String adjustLocation(String location) {
    if (!location.endsWith(extension)) {
      location += extension;
    }
    return location;
  }

  protected void doSetExtension(String ext) {
    this.extension = ext.charAt(0) == '.' ? ext : "." + ext;
  }

  protected void doSetPath(String path) {
    this.path = path;
  }



}
