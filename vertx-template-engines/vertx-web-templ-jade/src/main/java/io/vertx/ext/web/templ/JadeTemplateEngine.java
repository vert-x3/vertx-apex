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

package io.vertx.ext.web.templ;

import de.neuland.jade4j.JadeConfiguration;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.web.templ.impl.JadeTemplateEngineImpl;

/**
 * A template engine that uses Jade.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface JadeTemplateEngine extends TemplateEngine {

  /**
   * Default max number of templates to cache
   */
  int DEFAULT_MAX_CACHE_SIZE = 10000;

  /**
   * Default template extension
   */
  String DEFAULT_TEMPLATE_EXTENSION = "jade";

  /**
   * Create a template engine using defaults
   *
   * @return  the engine
   */
  static JadeTemplateEngine create() {
    return new JadeTemplateEngineImpl();
  }

  /**
   * Set the extension for the engine
   *
   * @param extension  the extension
   * @return a reference to this for fluency
   */
  JadeTemplateEngine setExtension(String extension);

  /**
   * Set the max cache size for the engine
   *
   * @param maxCacheSize  the maxCacheSize
   * @return a reference to this for fluency
   */
  JadeTemplateEngine setMaxCacheSize(int maxCacheSize);

  /**
   * Set caching option for templates
   *
   * @param flag
   * @return
   */
  JadeTemplateEngine setCaching(boolean flag);

  /**
   * Get a reference to the internal JadeConfiguration object so it
   * can be configured.
   *
   * @return a reference to the internal JadeConfiguration instance.
   */
  @GenIgnore
  JadeConfiguration getJadeConfiguration();

}
