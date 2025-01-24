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
package io.vertx.ext.web.client;

import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.impl.MultipartFormUpload;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.test.core.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class MultipartFormUploadTest {

  @ClassRule
  public static TemporaryFolder testFolder = new TemporaryFolder();

  private Vertx vertx;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext ctx) {
    vertx.close(ctx.asyncAssertSuccess());
  }

  @Test
  public void testSimpleAttribute(TestContext ctx) throws Exception {
    Async async = ctx.async();
    Buffer result = Buffer.buffer();
    Context context = vertx.getOrCreateContext();
    MultipartFormUpload upload = new MultipartFormUpload(context, MultipartForm.create().attribute("foo", "bar"), false, HttpPostRequestEncoder.EncoderMode.RFC1738);
    upload.endHandler(v -> {
      assertEquals("foo=bar", result.toString());
      async.complete();
    });
    upload.handler(result::appendBuffer);
    upload.resume();
    context.runOnContext(v -> upload.run());
  }

  @Test
  public void testFileUpload(TestContext ctx) throws Exception {
    testFileUpload(ctx, false);
  }

  @Test
  public void testFileUploadPaused(TestContext ctx) throws Exception {
    testFileUpload(ctx, true);
  }

  private void testFileUpload(TestContext ctx, boolean paused) throws Exception {
    File file = testFolder.newFile();
    Files.write(file.toPath(), TestUtils.randomByteArray(32 * 1024));

    String filename = file.getName();
    String pathname = file.getAbsolutePath();

    Async async = ctx.async();
    Context context = vertx.getOrCreateContext();
    context.runOnContext(v1 -> {
      try {
        MultipartFormUpload upload = new MultipartFormUpload(context, MultipartForm.create().textFileUpload(
          "the-file",
          filename,
          pathname,
          "text/plain"), true, HttpPostRequestEncoder.EncoderMode.RFC1738);
        List<Buffer> buffers = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger end = new AtomicInteger();
        upload.endHandler(v2 -> {
          assertEquals(0, end.getAndIncrement());
          ctx.assertTrue(buffers.size() > 0);
          async.complete();
        });
        upload.handler(buffer -> {
          assertEquals(0, end.get());
          buffers.add(buffer);
        });
        if (!paused) {
          upload.resume();
        }
        upload.run();
        if (paused) {
          context.runOnContext(v3 -> upload.resume());
        }
      } catch (Exception e) {
        ctx.fail(e);
        throw new AssertionError(e);
      }
    });
  }

  @Test
  public void testCorrectHeadersForH2(TestContext ctx) throws Exception {
    Async async = ctx.async();
    Buffer result = Buffer.buffer();
    Context context = vertx.getOrCreateContext();
    String longString = TestUtils.randomAlphaString(10_000);
    MultipartForm form = MultipartForm.create().attribute("foo", longString);
    MultipartFormUpload upload = new MultipartFormUpload(context, form, false, HttpVersion.HTTP_2, HttpPostRequestEncoder.EncoderMode.RFC1738);
    try {
      upload.endHandler(v -> {
        assertEquals("foo=" + longString, result.toString());
        async.complete();
      });
      upload.handler(result::appendBuffer);
      assertFalse(upload.headers().contains(HttpHeaders.TRANSFER_ENCODING));
      assertTrue(upload.headers().contains(HttpHeaders.CONTENT_LENGTH));
      upload.resume();
      context.runOnContext(v -> upload.run());
    } catch (Exception e) {
      ctx.fail(e);
    }
  }
}
