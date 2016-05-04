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

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.WebTestBase;
import io.vertx.ext.web.handler.TemplateHandler;
import org.junit.Test;

/**
 * @author Dan Kristensen
 */
public class PebbleTemplateTest extends WebTestBase {

	@Test
	public void testTemplateHandlerOnClasspath() throws Exception {
		final TemplateEngine engine = PebbleTemplateEngine.create();
		testTemplateHandler(engine, "somedir", "test-pebble-template2.peb",
				"Hello badger and foxRequest path is /test-pebble-template2.peb");
	}

	@Test
	public void testTemplateHandlerOnFileSystem() throws Exception {
		final TemplateEngine engine = PebbleTemplateEngine.create();
		testTemplateHandler(engine, "src/test/filesystemtemplates", "test-pebble-template3.peb",
				"Hello badger and foxRequest path is /test-pebble-template3.peb");
	}


	@Test
	public void testTemplateContextVariables() throws Exception {
		final TemplateEngine engine = PebbleTemplateEngine.create();
		testTemplateHandler(engine, "src/test/filesystemtemplates", "test-pebble-context-variables.peb",
				"Hello badger and foxRequest path is /test-pebble-context-variables.peb");
	}

	@Test
	public void testTemplateContextVariables_contextOverride() throws Exception {
		final TemplateEngine engine = PebbleTemplateEngine.create();

		router.route().handler(context -> {
			context.put("foo", "badger");
			context.put("bar", "fox");
			context.put("context", "NOT GONNA BE USED!");

			context.next();
		});
		router.route().handler(TemplateHandler.create(engine, "src/test/filesystemtemplates", "text/plain"));

		String expected = "Hello badger and foxRequest path is /test-pebble-context-variables.peb";

		testRequest(HttpMethod.GET, "/test-pebble-context-variables.peb", 200, "OK", expected);
	}

	@Test
	public void testTemplateHandlerNoExtension() throws Exception {
		final TemplateEngine engine = PebbleTemplateEngine.create();
		testTemplateHandler(engine, "somedir", "test-pebble-template2", "Hello badger and foxRequest path is /test-pebble-template2");
	}

	@Test
	public void testTemplateHandlerChangeExtension() throws Exception {
		final TemplateEngine engine = PebbleTemplateEngine.create().setExtension("beb");
		testTemplateHandler(engine, "somedir", "test-pebble-template2", "Cheerio badger and foxRequest path is /test-pebble-template2");
	}

	private void testTemplateHandler(TemplateEngine engine, String directoryName, String templateName, String expected) throws Exception {
		router.route().handler(context -> {
			context.put("foo", "badger");
			context.put("bar", "fox");
			context.next();
		});
		router.route().handler(TemplateHandler.create(engine, directoryName, "text/plain"));
		testRequest(HttpMethod.GET, "/" + templateName, 200, "OK", expected);
	}

	@Test
	public void testNoSuchTemplate() throws Exception {
		final TemplateEngine engine = PebbleTemplateEngine.create();
		router.route().handler(TemplateHandler.create(engine, "nosuchtemplate.peb", "text/plain"));
		testRequest(HttpMethod.GET, "/foo.peb", 500, "Internal Server Error");
	}

}