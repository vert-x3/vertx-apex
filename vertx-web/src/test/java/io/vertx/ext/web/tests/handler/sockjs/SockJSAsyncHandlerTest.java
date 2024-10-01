/*
 * Copyright 2019 Red Hat, Inc.
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

package io.vertx.ext.web.tests.handler.sockjs;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.handler.BodyHandler;
import org.junit.Test;

/**
 * @author Ben Ripkens
 */
public class SockJSAsyncHandlerTest extends SockJSTestBase {

  @Override
  public void setUp() throws Exception {
    // Use two servers so we test with HTTP request/response with load balanced SockJSSession access
    numServers = 2;
    preSockJSHandlerSetup = router -> {
      router.route().handler(BodyHandler.create());
      // simulate an async handler
      router.route().handler(rtx -> rtx.vertx().executeBlocking(() -> true).onComplete(r -> rtx.next()));
    };
    super.setUp();
  }

  @Test
  public void testHandleMessageFromXhrTransportWithAsyncHandler() throws Exception {
    waitFor(2);
    socketHandler = () -> socket -> {
      socket.handler(buf -> {
        assertEquals("Hello World", buf.toString());
        complete();
      });
    };

    startServers();

    client
      .request(HttpMethod.POST, "/test/400/8ne8e94a/xhr")
      .compose(req1 -> req1.send(Buffer.buffer()).onComplete(onSuccess(resp1 -> {
      assertEquals(200, resp1.statusCode());
        client
          .request(HttpMethod.POST, "/test/400/8ne8e94a/xhr_send")
          .compose(req2 -> req2.send(Buffer.buffer("\"Hello World\""))).onComplete(onSuccess(resp2 -> {
          assertEquals(204, resp2.statusCode());
          complete();
        }));
    })));

    await();
  }
}
