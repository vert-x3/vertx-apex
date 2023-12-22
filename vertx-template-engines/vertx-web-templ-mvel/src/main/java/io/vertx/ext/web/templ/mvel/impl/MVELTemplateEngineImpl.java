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

package io.vertx.ext.web.templ.mvel.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.common.template.CachingTemplateEngine;
import io.vertx.ext.web.common.template.impl.TemplateHolder;
import io.vertx.ext.web.templ.mvel.MVELTemplateEngine;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.util.StringAppender;

import java.nio.charset.Charset;
import java.util.Map;

import static io.vertx.core.impl.Utils.isWindows;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class MVELTemplateEngineImpl extends CachingTemplateEngine<CompiledTemplate> implements MVELTemplateEngine {

  private final Vertx vertx;

  public MVELTemplateEngineImpl(Vertx vertx, String extension) {
    super(vertx, extension);
    this.vertx = vertx;
  }

  @Override
  public Future<Buffer> render(Map<String, Object> context, String templateFile) {
    try {
      String src = adjustLocation(templateFile);
      TemplateHolder<CompiledTemplate> template = getTemplate(src);

      if (template == null) {
        int idx = findLastFileSeparator(src);
        String baseDir = "";
        if (idx != -1) {
          baseDir = src.substring(0, idx);
        }

        if (!vertx.fileSystem().existsBlocking(src)) {
          return Future.failedFuture("Cannot find template " + src);
        }

        template = new TemplateHolder<>(
          TemplateCompiler
          .compileTemplate(
            vertx.fileSystem()
              .readFileBlocking(src)
              .toString(Charset.defaultCharset())),
          baseDir);

        putTemplate(src, template);
      }

      final CompiledTemplate mvel = template.template();
      final String baseDir = template.baseDir();

      return Future.succeededFuture(
        Buffer.buffer(
          (String) new TemplateRuntime(mvel.getTemplate(), null, mvel.getRoot(), baseDir)
            .execute(new StringAppender(), context, new MapVariableResolverFactory())
        ));
    } catch (Exception ex) {
      return Future.failedFuture(ex);
    }
  }

  private static int findLastFileSeparator(String src) {
    if (isWindows()) {
      return Math.max(src.lastIndexOf('/'), src.lastIndexOf('\\'));
    }
    return src.lastIndexOf('/');
  }
}
