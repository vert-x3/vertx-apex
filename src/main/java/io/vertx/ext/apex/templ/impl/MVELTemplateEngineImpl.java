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

package io.vertx.ext.apex.templ.impl;

import de.neuland.jade4j.JadeConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.apex.templ.MVELTemplateEngine;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.impl.Utils;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class MVELTemplateEngineImpl extends CachingTemplateEngine<CompiledTemplate> implements MVELTemplateEngine {

  private final JadeConfiguration config = new JadeConfiguration();

  public MVELTemplateEngineImpl(String resourcePrefix, String ext, int maxCacheSize) {
    super(resourcePrefix, ext, maxCacheSize);
  }

  @Override
  public void render(RoutingContext context, String templateFileName, Handler<AsyncResult<Buffer>> handler) {
    try {
      CompiledTemplate template = cache.get(templateFileName);
      if (template == null) {
        // real compile
        String loc = adjustLocation(templateFileName);
        String templateText = Utils.readResourceToString(loc);
        if (templateText == null) {
          throw new IllegalArgumentException("Cannot find template " + loc);
        }
        template = TemplateCompiler.compileTemplate(templateText);
        cache.put(templateFileName, template);
      }
      Map<String, RoutingContext> variables = new HashMap<>(1);
      variables.put("context", context);
      handler.handle(Future.succeededFuture(Buffer.buffer((String)TemplateRuntime.execute(template, variables))));
    } catch (Exception ex) {
      handler.handle(Future.failedFuture(ex));
    }
  }


}
