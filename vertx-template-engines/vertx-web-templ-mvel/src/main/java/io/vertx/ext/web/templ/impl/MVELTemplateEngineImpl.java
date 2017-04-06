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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.VertxInternal;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Utils;
import io.vertx.ext.web.templ.MVELTemplateEngine;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.util.StringAppender;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class MVELTemplateEngineImpl extends CachingTemplateEngine<CompiledTemplate> implements MVELTemplateEngine {

  public MVELTemplateEngineImpl() {
    super(DEFAULT_TEMPLATE_EXTENSION, DEFAULT_MAX_CACHE_SIZE);
  }

  @Override
  public MVELTemplateEngine setExtension(String extension) {
    doSetExtension(extension);
    return this;
  }

  @Override
  public MVELTemplateEngine setMaxCacheSize(int maxCacheSize) {
    this.cache.setMaxSize(maxCacheSize);
    return this;
  }

  @Override
  public void render(RoutingContext context, String templateFileName, Handler<AsyncResult<Buffer>> handler) {
    render(context.vertx(), context, templateFileName, handler);
  }

  @Override
  public <T> void render(Vertx vertx, T context, String templateFileName, Handler<AsyncResult<Buffer>> handler) {
    try {
      CompiledTemplate template = cache.get(templateFileName);
      if (template == null) {
        // real compile
        String loc = adjustLocation(templateFileName);
        String templateText = Utils.readFileToString(vertx, loc);
        if (templateText == null) {
          throw new IllegalArgumentException("Cannot find template " + loc);
        }
        template = TemplateCompiler.compileTemplate(templateText);
        cache.put(templateFileName, template);
      }
      Map<String, T> variables = new HashMap<>(1);
      variables.put("context", context);
      final VertxInternal vertxInternal = (VertxInternal) vertx;
      String directoryName = vertxInternal.resolveFile(templateFileName).getParent();
      handler.handle(Future.succeededFuture(
        Buffer.buffer(
          (String) new TemplateRuntime(template.getTemplate(), null, template.getRoot(), directoryName)
            .execute(new StringAppender(), variables, new ImmutableDefaultFactory())
        )
      ));
    } catch (Exception ex) {
      handler.handle(Future.failedFuture(ex));
    }
  }

}
