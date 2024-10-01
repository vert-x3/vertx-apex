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
package io.vertx.ext.web.tests.handler.sockjs;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.WebSocketBase;
import io.vertx.test.core.TestUtils;
import org.junit.Test;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SockJSWriteTest extends SockJSTestBase {

  @Test
  public void testRaw() throws Exception {
    waitFor(2);
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.write(Buffer.buffer(expected)).onComplete(onSuccess(v -> {
        complete();
      }));
    };
    startServers();
    vertx.runOnContext(v -> {
      wsClient.connect("/test/websocket").onComplete(onSuccess(ws -> {
        ws.handler(buffer -> {
          if (buffer.toString().equals(expected)) {
            complete();
          }
        });
      }));
    });
    await();
  }

  @Test
  public void testRawFailure() throws Exception {
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.endHandler(v -> {
        socket.write(Buffer.buffer(expected)).onComplete(onFailure(err -> {
          testComplete();
        }));
      });
    };
    startServers();
    vertx.runOnContext(v -> {
      wsClient.connect("/test/websocket").onComplete(onSuccess(WebSocketBase::close));
    });
    await();
  }

  @Test
  public void testWebSocket() throws Exception {
    waitFor(2);
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.write(Buffer.buffer(expected)).onComplete(onSuccess(v -> {
        complete();
      }));
    };
    startServers();
    vertx.runOnContext(v -> {
      wsClient.connect("/test/400/8ne8e94a/websocket").onComplete(onSuccess(ws -> {
        ws.handler(buffer -> {
          if (buffer.toString().equals("a[\"" + expected + "\"]")) {
            complete();
          }
        });
      }));
    });
    await();
  }

  @Test
  public void testWebSocketFailure() throws Exception {
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.endHandler(v -> {
        socket.write(Buffer.buffer(expected)).onComplete(onFailure(err -> {
          testComplete();
        }));
      });
    };
    startServers();
    vertx.runOnContext(v -> {
      wsClient.connect("/test/400/8ne8e94a/websocket").onComplete(onSuccess(WebSocketBase::close));
    });
    await();
  }

  @Test
  public void testEventSource() throws Exception {
    waitFor(2);
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.write(Buffer.buffer(expected)).onComplete(onSuccess(v -> {
        complete();
      }));
    };
    startServers();
    client.request(HttpMethod.GET, "/test/400/8ne8e94a/eventsource")
      .onComplete(onSuccess(req -> req.send().onComplete(onSuccess(resp -> {
        resp.handler(buffer -> {
          if (buffer.toString().equals("data: a[\"" + expected + "\"]\r\n\r\n")) {
            complete();
          }
        });
      }))));
    await();
  }

  @Test
  public void testEventSourceFailure() throws Exception {
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.endHandler(v -> {
        socket.write(Buffer.buffer(expected)).onComplete(onFailure(err -> {
          testComplete();
        }));
      });
    };
    startServers();
    client.request(HttpMethod.GET, "/test/400/8ne8e94a/eventsource")
      .onComplete(onSuccess(req -> req.send().onComplete(onSuccess(resp -> {
        req.connection().close();
      }))));
    await();
  }

  @Test
  public void testXHRStreaming() throws Exception {
    waitFor(2);
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.write(Buffer.buffer(expected)).onComplete(onSuccess(v -> {
        complete();
      }));
    };
    startServers();
    client.request(HttpMethod.POST, "/test/400/8ne8e94a/xhr_streaming")
      .onComplete(onSuccess(req -> req.send(Buffer.buffer()).onComplete(onSuccess(resp -> {
        assertEquals(200, resp.statusCode());
        resp.handler(buffer -> {
          if (buffer.toString().equals("a[\"" + expected + "\"]\n")) {
            complete();
          }
        });
      }))));
    await();
  }

  @Test
  public void testXHRStreamingFailure() throws Exception {
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.endHandler(v -> {
        socket.write(Buffer.buffer(expected)).onComplete(onFailure(err -> {
          testComplete();
        }));
      });
    };
    startServers();
    client.request(HttpMethod.POST, "/test/400/8ne8e94a/xhr_streaming")
      .onComplete(onSuccess(req -> req.send().onComplete(onSuccess(resp -> {
        req.connection().close();
      }))));
    await();
  }

  @Test
  public void testXHRPolling() throws Exception {
    waitFor(2);
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.write(Buffer.buffer(expected)).onComplete(onSuccess(v -> {
        complete();
      }));
    };
    startServers();
    Runnable[] task = new Runnable[1];
    task[0] = () ->
      client.request(HttpMethod.POST, "/test/400/8ne8e94a/xhr")
        .onComplete(onSuccess(req -> req.send(Buffer.buffer()).onComplete(onSuccess(resp -> {
          assertEquals(200, resp.statusCode());
          resp.handler(buffer -> {
            if (buffer.toString().equals("a[\"" + expected + "\"]\n")) {
              complete();
            } else {
              task[0].run();
            }
          });
        }))));
    task[0].run();
    await();
  }

  @Test
  public void testXHRPollingClose() throws Exception {
    // Take 5 seconds which is the hearbeat timeout
    waitFor(3);
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.write(Buffer.buffer(expected)).onComplete(onFailure(err -> {
        complete();
      }));
      socket.endHandler(v -> {
        socket.write(Buffer.buffer(expected)).onComplete(onFailure(err -> {
          complete();
        }));
      });
      socket.close();
    };
    startServers();
    client.request(HttpMethod.POST, "/test/400/8ne8e94a/xhr")
      .onComplete(onSuccess(req -> req.send().onComplete(onSuccess(resp -> {
        assertEquals(200, resp.statusCode());
        complete();
      }))));
    await();
  }

  @Test
  public void testXHRPollingShutdown() throws Exception {
    // Take 5 seconds which is the hearbeat timeout
    waitFor(3);
    String expected = TestUtils.randomAlphaString(64);
    socketHandler = () -> socket -> {
      socket.write(Buffer.buffer(expected)).onComplete(onFailure(err -> {
        complete();
      }));
      socket.endHandler(v -> {
        socket.write(Buffer.buffer(expected)).onComplete(onFailure(err -> {
          complete();
        }));
      });
    };
    startServers();
    client.request(HttpMethod.POST, "/test/400/8ne8e94a/xhr")
      .onComplete(onSuccess(req -> req.send().onComplete(onSuccess(resp -> {
        assertEquals(200, resp.statusCode());
        complete();
      }))));
    await();
  }
}
