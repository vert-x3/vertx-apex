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

package io.vertx.ext.web.common.template;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * A template template uses a specific template and the data in a routing context to render a resource into a buffer.
 * <p>
 * Concrete implementations exist for several well-known template engines.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface TemplateEngine {

  /**
   * Render the template. Template engines that support partials/fragments should extract the template base path from
   * the template filename up to the last file separator.
   *
   * Some engines support localization, for these engines, there is a predefined key "lang" to specify the language to
   * be used in the localization, the format should follow the standard locale formats e.g.: "en-gb", "pt-br", "en".
   *
   * @param context  the routing context
   * @param templateFileName  the template file name to use
   * @param handler  the handler that will be called with a result containing the buffer or a failure.
   */
  default void render(JsonObject context, String templateFileName, Handler<AsyncResult<Buffer>> handler) {
    render(context.getMap(), templateFileName, handler);
  }

  /**
   * @see TemplateEngine#render(JsonObject, String, Handler)
   * @param context  the routing context
   * @param templateFileName  the template file name to use
   */
  default Future<Buffer> render(JsonObject context, String templateFileName) {
    Promise<Buffer> promise = Promise.promise();
    render(context, templateFileName, promise);
    return promise.future();
  }

  /**
   * Render the template. Template engines that support partials/fragments should extract the template base path from
   * the template filename up to the last file separator.
   *
   * Some engines support localization, for these engines, there is a predefined key "lang" to specify the language to
   * be used in the localization, the format should follow the standard locale formats e.g.: "en-gb", "pt-br", "en".
   *
   * @param context  the routing context
   * @param templateFileName  the template file name to use
   * @param handler  the handler that will be called with a result containing the buffer or a failure.
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  void render(Map<String, Object> context, String templateFileName, Handler<AsyncResult<Buffer>> handler);

  /**
   * @see TemplateEngine#render(Map, String, Handler)
   * @param context  the routing context
   * @param templateFileName  the template file name to use
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  default Future<Buffer> render(Map<String, Object> context, String templateFileName) {
    Promise<Buffer> promise = Promise.promise();
    render(context, templateFileName, promise);
    return promise.future();
  }
}
