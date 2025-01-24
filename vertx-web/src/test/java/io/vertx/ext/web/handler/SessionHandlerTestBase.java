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

package io.vertx.ext.web.handler;

import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.WebTestBase;
import io.vertx.ext.web.sstore.AbstractSession;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.*;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class SessionHandlerTestBase extends WebTestBase {

	protected SessionStore store;

	@Test
	public void testSessionCookieName() throws Exception {
		String sessionCookieName = "acme.sillycookie";
		router.route().handler(SessionHandler.create(store).setSessionCookieName(sessionCookieName));
		router.route().handler(rc -> rc.response().end());
		testRequest(HttpMethod.GET, "/", null, resp -> {
			String setCookie = resp.headers().get("set-cookie");
			assertTrue(setCookie.startsWith(sessionCookieName + "="));
		}, 200, "OK", null);
	}

  @Test
  public void testSessionCookieDomain() throws Exception {
    router.route().handler(SessionHandler.create(store).setSessionCookieDomain("example.com"));
    router.route().handler(rc -> rc.response().end());
    testRequest(HttpMethod.GET, "/", null, resp -> {
      String setCookie = resp.headers().get("set-cookie");
      assertTrue(setCookie.contains("Domain=example.com"));
    }, 200, "OK", null);
  }

  @Test
  public void testSessionCookieDefaultDomain() throws Exception {
    router.route().handler(SessionHandler.create(store));
    router.route().handler(rc -> rc.response().end());
    testRequest(HttpMethod.GET, "/", null, resp -> {
      String setCookie = resp.headers().get("set-cookie");
      assertFalse(setCookie.contains("Domain"));
    }, 200, "OK", null);
  }

	@Test
	public void testSessionCookiePath() throws Exception {
		router.route().handler(SessionHandler.create(store).setSessionCookiePath("/path"));
		router.route().handler(rc -> rc.response().end());
		testRequest(HttpMethod.GET, "/", null, resp -> {
			String setCookie = resp.headers().get("set-cookie");
			assertTrue(setCookie.contains("Path=/path"));
		}, 200, "OK", null);
	}

	@Test
	public void testSessionCookieHttpOnlyFlag() throws Exception {
		router.route().handler(SessionHandler.create(store).setCookieHttpOnlyFlag(true));
		router.route().handler(rc -> rc.response().end());

		testRequest(HttpMethod.GET, "/", null, resp -> {
			String setCookie = resp.headers().get("set-cookie");

			assertTrue(setCookie.contains("; HTTPOnly"));
		}, 200, "OK", null);
	}

	@Test
	public void testSessionCookieSecureFlag() throws Exception {
		router.route().handler(SessionHandler.create(store).setCookieSecureFlag(true));
		router.route().handler(rc -> rc.response().end());

		testRequest(HttpMethod.GET, "/", null, resp -> {
			String setCookie = resp.headers().get("set-cookie");

			assertTrue(setCookie.contains("; Secure"));
		}, 200, "OK", null);
	}

	@Test
	public void testSessionCookieSecureFlagAndHttpOnlyFlags() throws Exception {
		router.route().handler(SessionHandler.create(store).setCookieSecureFlag(true).setCookieHttpOnlyFlag(true));
		router.route().handler(rc -> rc.response().end());

		testRequest(HttpMethod.GET, "/", null, resp -> {
			String setCookie = resp.headers().get("set-cookie");

			assertTrue(setCookie.contains("; Secure"));
			assertTrue(setCookie.contains("; HTTPOnly"));
		}, 200, "OK", null);
	}

	@Test
	public void testSessionFields() throws Exception {
		router.route().handler(SessionHandler.create(store));
		AtomicReference<String> rid = new AtomicReference<>();
		router.route().handler(rc -> {
			Session sess = rc.session();
			assertNotNull(sess);
			assertTrue(System.currentTimeMillis() - sess.lastAccessed() < 500);
			assertNotNull(sess.id());
			rid.set(sess.value());
			assertFalse(sess.isDestroyed());
			assertEquals(SessionHandler.DEFAULT_SESSION_TIMEOUT, sess.timeout());
			rc.response().end();
		});
		testRequest(HttpMethod.GET, "/", null, resp -> {
			String setCookie = resp.headers().get("set-cookie");
			assertTrue(setCookie.startsWith(SessionHandler.DEFAULT_SESSION_COOKIE_NAME + "="));
			int pos = setCookie.indexOf("; Path=" + SessionHandler.DEFAULT_SESSION_COOKIE_PATH);
			String sessID = setCookie.substring(18, pos);
			assertEquals(rid.get(), sessID);
		}, 200, "OK", null);
	}

	@Test
	public void testSession() throws Exception {
    SessionHandler sessionHandler = SessionHandler.create(store);
    router.route().handler(sessionHandler);
    AtomicReference<String> rid = new AtomicReference<>();
    AtomicInteger requestCount = new AtomicInteger();
    router.route().handler(rc -> {
      Session sess = rc.session();
      assertNotNull(sess);
      assertNotNull(sess.id());
      switch (requestCount.get()) {
        case 0:
          rid.set(sess.id());
          sess.put("foo", "bar");
          break;
        case 1:
          assertEquals(rid.get(), sess.id());
          assertEquals("bar", sess.get("foo"));
          sess.put("eek", "wibble");
          break;
        case 2:
          assertEquals(rid.get(), sess.id());
          assertEquals("bar", sess.get("foo"));
          assertEquals("wibble", sess.get("eek"));
      }
      requestCount.incrementAndGet();
      sessionHandler.flush(rc).onFailure(rc::fail).onSuccess(v -> rc.response().end());
    });
    AtomicReference<String> rSetCookie = new AtomicReference<>();
    testRequest(HttpMethod.GET, "/", null, resp -> {
      String setCookie = resp.headers().get("set-cookie");
      rSetCookie.set(setCookie);
    }, 200, "OK", null);
    testRequest(HttpMethod.GET, "/", req -> req.putHeader("cookie", rSetCookie.get()), resp -> {
      String setCookie = resp.headers().get("set-cookie");
      // if the cookie was regenerated
      if (setCookie != null) {
        rSetCookie.set(setCookie);
      }
    }, 200, "OK", null);
    testRequest(HttpMethod.GET, "/", req -> req.putHeader("cookie", rSetCookie.get()), null, 200, "OK", null);
  }

	@Test
	public void testSessionExpires() throws Exception {
		long timeout = 1000;
		router.route().handler(SessionHandler.create(store).setSessionTimeout(timeout));
		AtomicReference<String> rid = new AtomicReference<>();
		AtomicInteger requestCount = new AtomicInteger();
		router.route().handler(rc -> {
			Session sess = rc.session();
			assertNotNull(sess);
			assertTrue(System.currentTimeMillis() - sess.lastAccessed() < 500);
			assertNotNull(sess.id());
			switch (requestCount.get()) {
			case 0:
				sess.put("foo", "bar");
				break;
			case 1:
				assertFalse(rid.get().equals(sess.id())); // New session
				assertNull(sess.get("foo"));
				break;
			}
			rid.set(sess.id());
			requestCount.incrementAndGet();
			rc.response().end();
		});
		AtomicReference<String> rSetCookie = new AtomicReference<>();
		testRequest(HttpMethod.GET, "/", null, resp -> {
			String setCookie = resp.headers().get("set-cookie");
			rSetCookie.set(setCookie);
		}, 200, "OK", null);
		Thread.sleep(2 * (LocalSessionStore.DEFAULT_REAPER_INTERVAL + timeout));
    testRequest(HttpMethod.GET, "/", req -> req.putHeader("cookie", rSetCookie.get()), null, 200, "OK", null);
    waitUntil(() -> testSessionBlocking(rid.get(), Objects::nonNull));
		Thread.sleep(2 * (LocalSessionStore.DEFAULT_REAPER_INTERVAL + timeout));
    waitUntil(() -> testSessionBlocking(rid.get(), Objects::isNull));
  }

  private boolean testSessionBlocking(String sessionId, Function<Session, Boolean> test) {
    CompletableFuture<Boolean> cf = new CompletableFuture<>();
    store.get(sessionId, ar -> {
      if (ar.succeeded()) {
        cf.complete(test.apply(ar.result()));
      } else {
        cf.completeExceptionally(ar.cause());
      }
    });
    try {
      return cf.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    } catch (ExecutionException e) {
      throw new AssertionError(e);
    }
  }

	@Test
	public void testDestroySession() throws Exception {
		router.route().handler(SessionHandler.create(store));
		AtomicReference<String> rid = new AtomicReference<>();
		AtomicInteger requestCount = new AtomicInteger();
		router.route().handler(rc -> {
			Session sess = rc.session();
			assertNotNull(sess);
			assertTrue(System.currentTimeMillis() - sess.lastAccessed() < 500);
			assertNotNull(sess.id());
			switch (requestCount.get()) {
			case 0:
				rid.set(sess.id());
				sess.put("foo", "bar");
				sess.destroy();
				break;
			case 1:
				assertFalse(rid.get().equals(sess.id())); // New session
				assertNull(sess.get("foo"));
				rid.set(sess.id());
				sess.destroy();
				break;
			}
			requestCount.incrementAndGet();
			rc.response().end();
		});
		testRequest(HttpMethod.GET, "/", null, resp -> {
			String setCookie = resp.headers().get("set-cookie");
			assertNull(setCookie);
			// the cookie got destroyed even before the end of the request, so no side
			// effects are expected
		}, 200, "OK", null);
		testRequest(HttpMethod.GET, "/", null, null, 200, "OK", null);
		Thread.sleep(500); // Needed because session.destroy is async
		CountDownLatch latch1 = new CountDownLatch(1);
		store.get(rid.get(), onSuccess(res -> {
			assertNull(res);
			latch1.countDown();
		}));
		awaitLatch(latch1);
	}

	@Test
	public void testLastAccessed1() throws Exception {
    router.route().handler(SessionHandler.create(store));
    AtomicReference<Session> rid = new AtomicReference<>();
    router.route().handler(rc -> {
      rid.set(rc.session());
      rc.response().end();
    });
    testRequest(HttpMethod.GET, "/", 200, "OK");
    long start = rid.get().lastAccessed();
    int millis = 250;
    Thread.sleep(millis);
    testRequest(HttpMethod.GET, "/", 200, "OK");
    assertTrue(rid.get().lastAccessed() - start >= millis);
  }

	@Test
	public void testLastAccessed2() throws Exception {
		router.route().handler(SessionHandler.create(store));
		AtomicReference<Session> rid = new AtomicReference<>();
		router.route().handler(rc -> {
			rid.set(rc.session());
			rc.session().put("foo", "bar");
			vertx.setTimer(1000, tid -> rc.response().end());
		});
		testRequest(HttpMethod.GET, "/", 200, "OK");
		// accessed() is called after request too
		assertTrue(rid.get().lastAccessed() - System.currentTimeMillis() < 500);
	}

	@Test
	public void testIssue172_setnull() throws Exception {
		router.route().handler(SessionHandler.create(store));
		AtomicReference<Session> rid = new AtomicReference<>();

		router.route().handler(rc -> {
			rid.set(rc.session());
			rc.session().put("foo", null);
			vertx.setTimer(1000, tid -> rc.response().end());
		});
		testRequest(HttpMethod.GET, "/", 200, "OK");
	}

	@Test
	public void testSessionCookieAttack() throws Exception {
		router.route().handler(SessionHandler.create(store));
		// faking that there was some auth error
		router.route().handler(rc -> rc.fail(401));

		testRequest(HttpMethod.GET, "/", null, resp -> assertNull(resp.headers().get("set-cookie")), 401,
				"Unauthorized", null);
	}

	@Test
	public void testSessionCookieInvalidatedOnError() throws Exception {
		final AtomicInteger counter = new AtomicInteger(0);
		final AtomicReference<String> id = new AtomicReference<>();

		router.route().handler(SessionHandler.create(store));
		// faking some error
		router.route().handler(rc -> {
			switch (counter.getAndIncrement()) {
			case 0:
				// store the reference
				id.set(rc.session().id());
				rc.response().end();
				break;
			case 1:
				assertEquals(id.get(), rc.session().id());
				rc.fail(500);
				break;
			case 2:
				assertEquals(id.get(), rc.session().id());
				rc.response().end();
				break;
			}
		});

		AtomicReference<String> sessionID = new AtomicReference<>();
		// first call will get a session cookie
		testRequest(HttpMethod.GET, "/", null, resp -> {
			assertNotNull(resp.headers().get("set-cookie"));
			String setCookie = resp.headers().get("set-cookie");
			sessionID.set(setCookie);
		}, 200, "OK", null);
		// ensure that on the second call, in case of error, the cookie is not present
		testRequest(HttpMethod.GET, "/", req -> req.putHeader("cookie", sessionID.get()),
				resp -> assertNull(resp.headers().get("set-cookie")), 500, "Internal Server Error", null);
		// ensure that on the third call, the session is still valid
		testRequest(HttpMethod.GET, "/", req -> req.putHeader("cookie", sessionID.get()),
				resp -> assertNull(resp.headers().get("set-cookie")), 200, "OK", null);
	}

	protected long doTestSessionRetryTimeout() throws Exception {
		router.route().handler(SessionHandler.create(store));
		AtomicReference<Session> rid = new AtomicReference<>();

		router.get("/0").handler(rc -> {
			rid.set(rc.session());
			rc.session().put("foo", "foo_value");
			rc.response().end();
		});
		router.get("/1").handler(rc -> {
			rid.set(rc.session());
			assertEquals("foo_value", rc.session().get("foo"));
			rc.session().destroy();
			rc.response().end();
		});
		router.get("/2").handler(rc -> {
			rid.set(rc.session());
			assertEquals(null, rc.session().<String>get("foo"));
			rc.response().end();
		});

		AtomicReference<String> sessionID = new AtomicReference<>();
		testRequest(HttpMethod.GET, "/0", req -> {
		}, resp -> {
			String setCookie = resp.headers().get("set-cookie");
			System.out.println(setCookie);
			sessionID.set(setCookie);
		}, 200, "OK", null);
		CountDownLatch responseReceived = new CountDownLatch(1);
		testRequest(HttpMethod.GET, "/1", req -> req.putHeader("cookie", sessionID.get()), resp -> {
			responseReceived.countDown();
		}, 200, "OK", null);
		awaitLatch(responseReceived);
		long now = System.nanoTime();
		testRequest(HttpMethod.GET, "/2", req -> req.putHeader("cookie", sessionID.get()), 200, "OK", null);
		return MILLISECONDS.convert(System.nanoTime() - now, NANOSECONDS);
	}

  @Test
  public void testInvalidation() throws Exception {
    router.route().handler(SessionHandler.create(store).setCookieSameSite(CookieSameSite.STRICT));
    AtomicReference<Session> rid = new AtomicReference<>();

    router.get("/0").handler(rc -> {
      rid.set(rc.session());
      rc.session().put("foo", "foo_value");
      rc.response().end();
    });
    router.get("/1").handler(rc -> {
      rid.set(rc.session());
      assertEquals("foo_value", rc.session().get("foo"));
      rc.session().destroy();
      rc.response().end();
    });

    AtomicReference<String> sessionID = new AtomicReference<>();
    testRequest(HttpMethod.GET, "/0", req -> {
    }, resp -> {
      String setCookie = resp.headers().get("set-cookie");
      sessionID.set(setCookie);
    }, 200, "OK", null);
    CountDownLatch responseReceived = new CountDownLatch(1);
    testRequest(HttpMethod.GET, "/1", req -> req.putHeader("cookie", sessionID.get()), resp -> {
      // ensure that expired cookies still contain the the configured properties
      assertTrue(resp.headers().get("set-cookie").contains("SameSite=Strict"));
      responseReceived.countDown();
    }, 200, "OK", null);
    awaitLatch(responseReceived);
  }

	@Test
	public void testSessionFixation() throws Exception {

		final AtomicReference<String> sessionId = new AtomicReference<>();

		router.route().handler(SessionHandler.create(store));
		// call #0 anonymous a random id should be returned
		router.route("/0").handler(rc -> {
			sessionId.set(rc.session().value());
			rc.response().end();
		});
		// call #1 fake auth security upgrade is done so session id must change
		router.route("/1").handler(rc -> {
			// previous id must match
			assertEquals(sessionId.get(), rc.session().value());
			rc.session().regenerateId();
			rc.response().end();
		});

		testRequest(HttpMethod.GET, "/0", null, resp -> {
			String setCookie = resp.headers().get("set-cookie");
			assertNotNull(setCookie);
		}, 200, "OK", null);

		CountDownLatch responseReceived = new CountDownLatch(1);
		testRequest(HttpMethod.GET, "/1",
				req -> req.putHeader("cookie", "vertx-web.session=" + sessionId.get() + "; Path=/"), resp -> {
					String setCookie = resp.headers().get("set-cookie");
					assertNotNull(setCookie);
					assertFalse(("vertx-web.session=" + sessionId.get() + "; Path=/").equals(setCookie));
					responseReceived.countDown();
				}, 200, "OK", null);
		awaitLatch(responseReceived);

    assertWaitUntil(() -> {
      CompletableFuture<Session> cf = new CompletableFuture<>();
      store.get(sessionId.get(), get -> {
        if (get.succeeded()) {
          cf.complete(get.result());
        } else {
          cf.completeExceptionally(get.cause());
        }
      });
      try {
        return cf.get() == null;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        throw new RuntimeException(e.getCause());
      }
    }, TimeUnit.MILLISECONDS.convert(1, MINUTES), "old id must not be valid anymore");
	}

	@Test
	public void testSessionIdLength() throws Exception {

		router.route().handler(SessionHandler.create(store));

		router.route("/1").handler(rc -> {
			// previous id must match
			assertFalse("abc".equals(rc.session().id()));
			rc.response().end();
		});

		testRequest(HttpMethod.GET, "/1", req -> req.putHeader("cookie", "vertx-web.session=abc; Path=/"), resp -> {
			String setCookie = resp.headers().get("set-cookie");
			assertNotNull(setCookie);
		}, 200, "OK", null);
	}

	@Test
	public void testVersion() throws Exception {
		AbstractSession session = (AbstractSession) store.createSession(10000);

		assertEquals(0, session.version());
		session.put("k", "v");

		store.put(session, res -> {
			if (res.failed()) {
				fail("failed to store");
			}
			store.get(session.value(), res1 -> {
				if (res1.failed()) {
					fail("failed to store");
				}

				AbstractSession session1 = (AbstractSession) res1.result();
				// session was stored for the first time so it must have version 1
				assertEquals(1, session1.version());
				// confirm that the content is present
				assertEquals("v", session1.get("k"));

				store.put(session1, res2 -> {
					if (res2.failed()) {
						fail("failed to store");
					}
					store.get(session1.value(), res3 -> {
						if (res3.failed()) {
							fail("failed to store");
						}

						AbstractSession session2 = (AbstractSession) res3.result();
						// session was stored again but no changes were applied to the content
						// therefore version should remain the same
						assertEquals(1, session2.version());
						// confirm the content is present
						assertEquals("v", session2.get("k"));

						// update content
						session2.put("k", "w");

						store.put(session2, res4 -> {
							if (res4.failed()) {
								fail("failed to store");
							}
							store.get(session2.value(), res5 -> {
								if (res5.failed()) {
									fail("failed to store");
								}

								AbstractSession session3 = (AbstractSession) res5.result();
								// session was stored again but changes were applied to the content
								// therefore version should must increment
								assertEquals(2, session3.version());
								// confirm the content is present
								assertEquals("w", session3.get("k"));
								testComplete();
							});
						});
					});
				});
			});
		});

		await();
	}

  @Test
  public void testLazySessionNotAccessed() throws Exception {
    String sessionCookieName = "acme.sillycookie";
    router.route().handler(SessionHandler.create(store).setSessionCookieName(sessionCookieName).setLazySession(true));
    router.route().handler(rc -> rc.response().end());
    testRequest(HttpMethod.GET, "/", null, resp -> {
      String setCookie = resp.headers().get("set-cookie");
      assertTrue(setCookie == null);
    }, 200, "OK", null);
  }

  @Test
  public void testLazySessionAccessed() throws Exception {
    String sessionCookieName = "acme.sillycookie";
    router.route().handler(SessionHandler.create(store).setSessionCookieName(sessionCookieName).setLazySession(true));
    router.route().handler(rc -> {
      rc.session();
      rc.response().end();
    });
    testRequest(HttpMethod.GET, "/", null, resp -> {
      String setCookie = resp.headers().get("set-cookie");
      assertTrue(setCookie.startsWith(sessionCookieName + "="));
    }, 200, "OK", null);
  }

  @Test
  public void testRegenerateAndCookies() throws Exception {

    final SessionHandler sessionHandler =
      SessionHandler.create(LocalSessionStore.create(vertx))
        .setLazySession(true)
        .setCookieHttpOnlyFlag(true)
        .setSessionCookieName("vid");

    router.route("/login")
      .handler(sessionHandler)
      .handler(ctx -> {
        ctx.session().regenerateId();
        ctx.end(ctx.request().path());
      });

    router.route("/user")
      .handler(sessionHandler)
      .handler(ctx -> ctx.end(ctx.request().path()));

    final AtomicReference<String> sessionID = new AtomicReference<>();

    testRequest(HttpMethod.GET, "/login", null, resp -> {
      String setCookie = resp.headers().get("set-cookie");
      assertNotNull(setCookie);
      sessionID.set(setCookie);
    }, 200, "OK", null);

    // second call
    testRequest(HttpMethod.GET, "/user", req -> {
      req.putHeader("Cookie", sessionID.get());
    }, resp -> {
      String setCookie = resp.headers().get("set-cookie");
      // cookie doesn't change, no need to re-issue it again
      assertNull(setCookie);
    }, 200, "OK", null);
  }
}
