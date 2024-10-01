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

package io.vertx.ext.web.tests.handler;

import io.vertx.core.http.*;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.tests.WebTestBase;
import org.junit.Test;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class StaticHandler2Test extends WebTestBase {

  protected StaticHandler stat;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    stat = StaticHandler.create();
    router.route("/static/*").handler(stat);
  }

  @Test
  public void testGetDefaultIndex() throws Exception {
    // without slash... forwards to slash
    testRequest(HttpMethod.GET, "/static/swaggerui", null, res-> {
      assertEquals("/static/swaggerui/", res.getHeader("Location"));
    }, 301, "Moved Permanently", null);

    testRequest(HttpMethod.GET, "/static/swaggerui/", 200, "OK", "<html><body>Fake swagger UI</body></html>\n");

    // also index.html retreives the final file
    testRequest(HttpMethod.GET, "/static/swaggerui/index.html", 200, "OK", "<html><body>Fake swagger UI</body></html>\n");
  }
}
