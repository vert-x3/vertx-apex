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

package io.vertx.ext.apex.handler;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.ext.apex.handler.impl.TemplateHandlerImpl;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.templ.TemplateEngine;

/**
 *
 * A handler which renders responses using a template engine and where the template name is selected from the URI
 * path.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface TemplateHandler extends Handler<RoutingContext> {

  /**
   * The default directory where templates will be looked for
   */
  static final String DEFAULT_TEMPLATE_DIRECTORY = "templates";

  /**
   * The default content type header to be used in the response
   */
  static final String DEFAULT_CONTENT_TYPE = "text/html";

  /**
   * Create a handler
   *
   * @param engine  the template engine
   * @return the handler
   */
  static TemplateHandler create(TemplateEngine engine) {
    return new TemplateHandlerImpl(engine, DEFAULT_TEMPLATE_DIRECTORY, DEFAULT_CONTENT_TYPE);
  }

  /**
   * Create a handler
   *
   * @param engine  the template engine
   * @param templateDirectory  the template directory where templates will be looked for
   * @param contentType  the content type header to be used in the response
   * @return the handler
   */
  static TemplateHandler create(TemplateEngine engine, String templateDirectory, String contentType) {
    return new TemplateHandlerImpl(engine, templateDirectory, contentType);
  }

  @Override
  void handle(RoutingContext context);

}
