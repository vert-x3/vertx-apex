package io.vertx.ext.web.api.service.futures;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.RouteToEBServiceHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.validation.BaseValidationHandlerTest;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.vertx.ext.web.validation.builder.Parameters.param;
import static io.vertx.ext.web.validation.testutils.TestRequest.jsonBodyResponse;
import static io.vertx.ext.web.validation.testutils.TestRequest.statusCode;
import static io.vertx.ext.web.validation.testutils.TestRequest.statusMessage;
import static io.vertx.ext.web.validation.testutils.TestRequest.testRequest;
import static io.vertx.json.schema.draft7.dsl.Schemas.intSchema;

@SuppressWarnings("unchecked")
@ExtendWith(VertxExtension.class)
public class RouteToEBServiceFuturesHandlerTest extends BaseValidationHandlerTest {

  MessageConsumer<JsonObject> consumer;

  @AfterEach
  public void tearDown() {
    if (consumer != null) consumer.unregister();
  }

  @Test
  public void serviceProxyTypedTestWithRequestParameter(final Vertx vertx, final VertxTestContext testContext) {
    final Checkpoint checkpoint = testContext.checkpoint();

    final FuturesService service = new FuturesServiceImpl();
    final ServiceBinder serviceBinder = new ServiceBinder(vertx).setAddress("someAddress");
    consumer = serviceBinder.register(FuturesService.class, service);

    router
      .post("/testFutureWithRequestParameter/:param")
      .handler(BodyHandler.create())
      .handler(ValidationHandler.builder(schemaRepo).pathParameter(param("param", intSchema())).build())
      .handler(
        RouteToEBServiceHandler.build(vertx.eventBus(), "someAddress", "testFutureWithRequestParameter"));

    testRequest(client, HttpMethod.POST, "/testFutureWithRequestParameter/123")
      .expect(statusCode(200), statusMessage("OK"))
      .expect(jsonBodyResponse(new JsonObject().put("param", 123)))
      .send(testContext, checkpoint);
  }

  @Test
  public void serviceProxyTypedTestWithIntParameter(final Vertx vertx, final VertxTestContext testContext) {
    final Checkpoint checkpoint = testContext.checkpoint();

    final FuturesService service = new FuturesServiceImpl();
    final ServiceBinder serviceBinder = new ServiceBinder(vertx).setAddress("someAddress");
    consumer = serviceBinder.register(FuturesService.class, service);

    router
      .post("/testFutureWithIntParameter/:param")
      .handler(BodyHandler.create())
      .handler(ValidationHandler.builder(schemaRepo).pathParameter(param("param", intSchema())).build())
      .handler(
        RouteToEBServiceHandler.build(vertx.eventBus(), "someAddress", "testFutureWithIntParameter"));

    testRequest(client, HttpMethod.POST, "/testFutureWithIntParameter/123")
      .expect(statusCode(200), statusMessage("OK"))
      .expect(jsonBodyResponse(new JsonObject().put("param", 123)))
      .send(testContext, checkpoint);
  }

  @Test
  public void serviceProxyTypedTest(final Vertx vertx, final VertxTestContext testContext) {
    final Checkpoint checkpoint = testContext.checkpoint();

    final FuturesService service = new FuturesServiceImpl();
    final ServiceBinder serviceBinder = new ServiceBinder(vertx).setAddress("someAddress");
    consumer = serviceBinder.register(FuturesService.class, service);

    router
      .post("/testFuture")
      .handler(BodyHandler.create())
      .handler(ValidationHandler.builder(schemaRepo).build())
      .handler(
        RouteToEBServiceHandler.build(vertx.eventBus(), "someAddress", "testFuture"));

    testRequest(client, HttpMethod.POST, "/testFuture")
      .expect(statusCode(200), statusMessage("OK"))
      .expect(jsonBodyResponse(new JsonObject().put("foo", "bar")))
      .send(testContext, checkpoint);
  }

}
