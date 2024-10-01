package io.vertx.ext.web.tests.handler.sockjs;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.tests.WebTestBase;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.junit.Test;

public class SockJSCORSTest extends WebTestBase {

  @Test
  public void testSockJSInternalCORSHandling() {
    router
      .route()
      .handler(BodyHandler.create());
    SockJSProtocolTest.installTestApplications(router, vertx);
    client.request(HttpMethod.GET, "/echo/info?t=21321")
      .compose(req -> req
        .putHeader(HttpHeaders.ORIGIN, "http://example.com")
        .send())
      .onComplete(onSuccess(resp -> {
        assertEquals(200, resp.statusCode());
        assertEquals("http://example.com", resp.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("true", resp.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        complete();
    }));
    await();
  }

  @Test
  public void testNoConflictsSockJSAndCORSHandler() {
    router
      .route()
      .handler(CorsHandler.create().addRelativeOrigin(".*").allowCredentials(false))
      .handler(BodyHandler.create());
    SockJSProtocolTest.installTestApplications(router, vertx);
    client.request(HttpMethod.GET, "/echo/info?t=21321")
      .compose(req -> req
        .putHeader(HttpHeaders.ORIGIN, "http://example.com")
        .send())
      .onComplete(onSuccess(resp -> {
        assertEquals(200, resp.statusCode());
        assertEquals("*", resp.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertFalse(resp.headers().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        //If the SockJS handles the CORS stuff, it would reply with allow credentials true and allow origin example.com
        complete();
      }));
    await();
  }

  @Test(expected = IllegalStateException.class)
  public void testNoConflictsSockJSAndCORSHandlerBadSetup() {
    router
      .route()
      .handler(BodyHandler.create())
      .handler(CorsHandler.create().addRelativeOrigin(".*").allowCredentials(false));
    SockJSProtocolTest.installTestApplications(router, vertx);
  }

  @Test
  public void testNoConflictsSockJSAndCORSHandlerBadSetupLenient() {
    try {
      System.setProperty("io.vertx.web.router.setup.lenient", "true");
      router
        .route()
        .handler(BodyHandler.create())
        .handler(CorsHandler.create().addRelativeOrigin(".*").allowCredentials(false));
      SockJSProtocolTest.installTestApplications(router, vertx);
    } finally {
      System.clearProperty("io.vertx.web.router.setup.lenient");
    }
  }
}
