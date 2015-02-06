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

package io.vertx.ext.apex.templ;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.apex.handler.TemplateHandler;
import io.vertx.ext.apex.ApexTestBase;
import org.junit.Test;

/**
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ThymeleafTemplateTest extends ApexTestBase {

  @Test
  public void testTemplateHandler() throws Exception {
    TemplateEngine engine = ThymeleafTemplateEngine.create();
    testTemplateHandler(engine, "somedir", "test-thymeleaf-template2.html");
  }

  private void testTemplateHandler(TemplateEngine engine, String directoryName, String templateName) throws Exception {
    router.route().handler(context -> {
      context.put("foo", "badger");
      context.put("bar", "fox");
      context.next();
    });
    router.route().handler(TemplateHandler.create(engine, directoryName, "text/html"));
    String expected =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
        "\n" +
        "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
        "<head>\n" +
        "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
        "</head>\n" +
        "<body>\n" +
        "<p>badger</p>\n" +
        "<p>fox</p>\n" +
        "<p>/test-thymeleaf-template2.html</p>\n" +
        "<p>blah</p>\n" +
        "<p>wibble</p>\n" +
        "</body>\n" +
        "</html>";

    testRequest(HttpMethod.GET, "/" + templateName + "?param1=blah&param2=wibble", 200, "OK", expected);
  }

  @Test
  public void testNoSuchTemplate() throws Exception {
    TemplateEngine engine = ThymeleafTemplateEngine.create();
    router.route().handler(TemplateHandler.create(engine, "nosuchtemplate.html", "text/html"));
    testRequest(HttpMethod.GET, "/foo.html", 500, "Internal Server Error");
  }

}