/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.ext.web.handler.sse;

import io.vertx.core.VertxException;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class SSETestEstablishConnection extends SSETestBase {

  @Test
  public void noToken() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    eventSource().connect("/sse", handler -> {
      assertFalse(handler.succeeded());
      assertTrue(handler.failed());
      assertNotNull(handler.cause());
      assertTrue(handler.cause() instanceof VertxException);
      final VertxException ve = (VertxException) handler.cause();
      assertTrue(ve.getMessage().endsWith("401"));
      latch.countDown();
    });
    awaitLatch(latch);
  }

  @Test
  public void invalidToken() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    eventSource().connect("/sse?token=somethingdefinitelynotvalid", handler -> {
      assertFalse(handler.succeeded());
      assertTrue(handler.failed());
      assertTrue(handler.cause() instanceof VertxException);
      final VertxException cre = (VertxException) handler.cause();
      assertTrue(cre.getMessage().endsWith("403"));
      latch.countDown();
    });
    awaitLatch(latch);
  }

  @Test
  public void validConnection() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    eventSource().connect("/sse?token=" + TOKEN, handler -> {
      assertTrue(handler.succeeded());
      assertFalse(handler.failed());
      assertNull(handler.cause());
      latch.countDown();
    });
    awaitLatch(latch);
  }

}
