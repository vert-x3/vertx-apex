package io.vertx.ext.web.tests;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;

@RunWith(VertxUnitRunner.class)
public class RoutingContextNullCurrentRouteTest {

    static final int PORT = 9091;
    private Vertx vertx;

    @Before
    public void before(TestContext context) {
        vertx = Vertx.vertx();
        Async async = context.async();
        vertx.deployVerticle(TestVerticle.class.getName()).onComplete(context.asyncAssertSuccess(event -> async.complete()));
    }

    @Test
    public void test(TestContext testContext) {
        HttpClient client =
                vertx.createHttpClient(new HttpClientOptions()
                        .setConnectTimeout(10000));
        client.request(HttpMethod.GET, PORT, "127.0.0.1", "/test")
          .compose(HttpClientRequest::send).onComplete(testContext.asyncAssertSuccess(resp -> {
          testContext.assertEquals(HttpURLConnection.HTTP_NO_CONTENT, resp.statusCode());
        }));
    }

    @After
    public void after(TestContext context) {
        vertx.close().onComplete(context.asyncAssertSuccess());
    }

    public static class TestVerticle extends AbstractVerticle {

        @Override
        public void start(Promise<Void> startFuture) throws Exception {

            Router router = Router.router(vertx);
            router.get("/test").handler(routingCount ->
                    vertx.setTimer(5000, timerId -> {
                        HttpServerResponse response = routingCount.response();
                        if (routingCount.currentRoute() == null) {
                            response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                                    .end();
                        } else {
                            response.setStatusCode(HttpURLConnection.HTTP_NO_CONTENT)
                                    .end();
                        }
                    }));

            vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(PORT).onComplete(asyncResult -> {
                        if (asyncResult.succeeded()) {
                            startFuture.complete();
                        } else {
                            startFuture.fail(asyncResult.cause());
                        }
                    });
        }
    }
}
