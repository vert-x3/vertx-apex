package io.vertx.ext.web.validation;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.ext.web.validation.builder.Bodies;
import io.vertx.ext.web.validation.builder.Parameters;
import io.vertx.ext.web.validation.builder.Parsers;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.ext.web.validation.impl.ParameterLocation;
import io.vertx.ext.web.validation.impl.parser.ValueParser;
import io.vertx.json.schema.common.dsl.GenericSchemaBuilder;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static io.vertx.ext.web.validation.builder.Parameters.explodedParam;
import static io.vertx.ext.web.validation.builder.Parameters.optionalExplodedParam;
import static io.vertx.ext.web.validation.builder.Parameters.optionalParam;
import static io.vertx.ext.web.validation.builder.Parameters.param;
import static io.vertx.ext.web.validation.builder.Parameters.serializedParam;
import static io.vertx.ext.web.validation.testutils.TestRequest.cookie;
import static io.vertx.ext.web.validation.testutils.TestRequest.jsonBodyResponse;
import static io.vertx.ext.web.validation.testutils.TestRequest.requestHeader;
import static io.vertx.ext.web.validation.testutils.TestRequest.statusCode;
import static io.vertx.ext.web.validation.testutils.TestRequest.statusMessage;
import static io.vertx.ext.web.validation.testutils.TestRequest.testRequest;
import static io.vertx.ext.web.validation.testutils.TestRequest.urlEncode;
import static io.vertx.ext.web.validation.testutils.ValidationTestUtils.badBodyResponse;
import static io.vertx.ext.web.validation.testutils.ValidationTestUtils.badParameterResponse;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.draft7.dsl.Keywords.multipleOf;
import static io.vertx.json.schema.draft7.dsl.Schemas.arraySchema;
import static io.vertx.json.schema.draft7.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.draft7.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.draft7.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.draft7.dsl.Schemas.ref;
import static io.vertx.json.schema.draft7.dsl.Schemas.stringSchema;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
@SuppressWarnings("unchecked")
@ExtendWith(VertxExtension.class)
public class ValidationHandlerProcessorsIntegrationTest extends BaseValidationHandlerTest {

  @Test
  public void testPathParamsSimpleTypes(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .pathParameter(param("a", stringSchema()))
      .pathParameter(param("b", booleanSchema()))
      .pathParameter(param("c", intSchema()))
      .build();
    router.get("/testPathParams/:a/:b/:c")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .setStatusMessage(
            params.pathParameter("a").getString() + params.pathParameter("b").getBoolean() + params.pathParameter("c").getInteger()
          ).end();
      });
    String a = "hello";
    String b = "true";
    String c = "10";

    testRequest(client, HttpMethod.GET, String.format("/testPathParams/%s/%s/%s", a, b, c))
      .expect(statusCode(200), statusMessage(a + b + c))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/testPathParams/hello/bla/10")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "b",
        ParameterLocation.PATH
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testQueryParamsSimpleTypes(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .queryParameter(param("param1", booleanSchema()))
      .queryParameter(param("param2", intSchema()))
      .build();
    router
      .get("/testQueryParams")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext.response().setStatusMessage(
          params.queryParameter("param1").getBoolean().toString() + params.queryParameter("param2").getInteger().toString()
        ).end();
      });
    testRequest(client, HttpMethod.GET, "/testQueryParams?param1=true&param2=10")
      .expect(statusCode(200), statusMessage("true10"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/testQueryParams?param1=true&param2=bla")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "param2",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testQueryJsonObjectAsyncParam(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .queryParameter(Parameters.jsonParam("myTree", ref(JsonPointer.fromURI(URI.create("tree_schema.json")))))
      .build();
    router
      .get("/test")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .putHeader("content-type", "application/json")
          .end(params.queryParameter("myTree").getJsonObject().toBuffer());
      });

    JsonObject testSuccessObj = new JsonObject()
      .put("value", "aaa")
      .put("childs", new JsonArray().add(
        new JsonObject().put("value", "bbb")
      ));

    testRequest(client, HttpMethod.GET, "/test?myTree=" + urlEncode(testSuccessObj.encode()))
      .expect(statusCode(200), jsonBodyResponse(testSuccessObj))
      .send(testContext, checkpoint);

    JsonObject testFailureObj = testSuccessObj.copy();
    testFailureObj.remove("value");

    testRequest(client, HttpMethod.GET, "/test?myTree=" + urlEncode(testFailureObj.encode()))
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.VALIDATION_ERROR,
        "myTree",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testQueryParamsAsyncValidation(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(4);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .queryParameter(param("param1", booleanSchema()))
      .queryParameter(param("param2", intSchema().withKeyword("maximum", 10).withKeyword("minimum", 0),
        ValueParser.LONG_PARSER))
      .build();
    router
      .get("/test")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext.response().setStatusMessage(
          params.queryParameter("param1").getBoolean().toString() + params.queryParameter("param2").getInteger().toString()
        ).end();
      });
    testRequest(client, HttpMethod.GET, "/test?param1=true&param2=5")
      .expect(statusCode(200), statusMessage("true5"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?param1=bla&param2=5")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "param1",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?param1=true&param2=bla")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "param2",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?param1=true&param2=15")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.VALIDATION_ERROR,
        "param2",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testQueryParamOptional(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .queryParameter(param("param1", booleanSchema()))
      .queryParameter(optionalParam("param2", intSchema()))
      .build();
    router
      .get("/testQueryParams")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext.response().setStatusMessage(
          "" + params.queryParameter("param1") + params.queryParameter("param2")
        ).end();
      });

    testRequest(client, HttpMethod.GET, "/testQueryParams?param1=true&param2=10")
      .expect(statusCode(200), statusMessage("true10"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/testQueryParams?param1=true")
      .expect(statusCode(200), statusMessage("truenull"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/testQueryParams?param1=true&param2=bla")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "param2",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testQueryParamArrayExploded(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .queryParameter(explodedParam("parameter",
        arraySchema().items(intSchema().with(multipleOf(2)))
      ))
      .build();
    router
      .get("/test")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext.response().setStatusMessage(
          params.queryParameter("parameter").getJsonArray().stream().map(Object::toString).collect(Collectors.joining(","))
        ).end();
      });

    testRequest(client, HttpMethod.GET, "/test?parameter=2&parameter=4&parameter=6")
      .expect(statusCode(200), statusMessage("2,4,6"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?parameter=2&parameter=2&parameter=false")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "parameter",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?parameter=2&parameter=2&parameter=1")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.VALIDATION_ERROR,
        "parameter",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testQueryParamArrayCommaSeparated(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .queryParameter(serializedParam(
        "parameter",
        Parsers.commaSeparatedArrayParser(),
        arraySchema().items(intSchema().with(multipleOf(2)))
      ))
      .build();
    router
      .get("/test")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext.response().setStatusMessage(
          params.queryParameter("parameter").getJsonArray().stream().map(Object::toString).collect(Collectors.joining(","))
        ).end();
      });

    testRequest(client, HttpMethod.GET, "/test?parameter=" + urlEncode("2,4,6"))
      .expect(statusCode(200), statusMessage("2,4,6"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?parameter=" + urlEncode("1,false,3"))
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "parameter",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?parameter=" + urlEncode("6,2,1"))
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.VALIDATION_ERROR,
        "parameter",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);
  }


  @Test
  public void testQueryParamDefault(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .queryParameter(optionalParam("param1", intSchema().defaultValue(10)))
      .queryParameter(param("param2", intSchema()))
      .build();
    router
      .get("/test")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext.response().setStatusMessage(
          "" + params.queryParameter("param1") + params.queryParameter("param2")
        ).end();
      });

    testRequest(client, HttpMethod.GET, "/test?param1=5&param2=10")
      .expect(statusCode(200), statusMessage("510"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?param2=10")
      .expect(statusCode(200), statusMessage("1010"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?param1=5")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.MISSING_PARAMETER_WHEN_REQUIRED_ERROR,
        "param2",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);
  }


  @Test
  public void testQueryArrayParamsArrayAndPathParam(VertxTestContext testContext) throws Exception {
    Checkpoint checkpoint = testContext.checkpoint(2);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .pathParameter(param("pathParam", booleanSchema()))
      .queryParameter(explodedParam("awesomeArray", arraySchema().items(intSchema())))
      .queryParameter(param("anotherParam", numberSchema()))
      .build();
    router
      .get("/testQueryParams/:pathParam")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext.response().setStatusMessage(
          params.pathParameter("pathParam").toString() +
            params.queryParameter("awesomeArray").toString() +
            params.queryParameter("anotherParam").toString()
        ).end();
      });

    testRequest(client, HttpMethod.GET, "/testQueryParams/true?awesomeArray=1&awesomeArray=2&awesomeArray=3" +
      "&anotherParam=5.2")
      .expect(statusCode(200), statusMessage("true" + new JsonArray().add(1).add(2).add(3).toString() + "5.2"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/testQueryParams/true?awesomeArray=1&awesomeArray=bla&awesomeArray=3" +
      "&anotherParam=5.2")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "awesomeArray",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testHeaderParamsSimpleTypes(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .headerParameter(param("x-a", stringSchema()))
      .headerParameter(param("x-b", booleanSchema()))
      .headerParameter(param("x-c", intSchema()))
      .build();
    router.get("/testHeaderParams")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .setStatusMessage(String
            .format("%s%s%s", params.headerParameter("x-a"), params.headerParameter("x-b"), params.headerParameter("x" +
              "-c"))
          ).end();
      });
    String a = "hello";
    String b = "false";
    String c = "10";

    testRequest(client, HttpMethod.GET, "/testHeaderParams")
      .with(requestHeader("x-a", a), requestHeader("x-b", b), requestHeader("x-c", c))
      .expect(statusCode(200), statusMessage(a + b + c))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/testHeaderParams")
      .with(requestHeader("x-a", a), requestHeader("x-b", "bla"), requestHeader("x-c", c))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "x-b",
        ParameterLocation.HEADER
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testHeaderParamsAsync(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(4);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .headerParameter(param("x-a", stringSchema()))
      .headerParameter(param("x-b", booleanSchema()))
      .headerParameter(param("x-c", intSchema().withKeyword("maximum", 10).withKeyword("minimum", 0),
        ValueParser.LONG_PARSER))
      .build();
    router.get("/test")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .setStatusMessage(String
            .format("%s%s%s", params.headerParameter("x-a"), params.headerParameter("x-b"), params.headerParameter("x" +
              "-c"))
          ).end();
      });
    String a = "hello";
    String b = "false";
    String c = "10";

    testRequest(client, HttpMethod.GET, "/test")
      .with(requestHeader("x-a", a), requestHeader("x-b", b), requestHeader("x-c", c))
      .expect(statusCode(200), statusMessage(a + b + c))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test")
      .with(requestHeader("x-a", a), requestHeader("x-b", "bla"), requestHeader("x-c", c))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "x-b",
        ParameterLocation.HEADER
      ))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test")
      .with(requestHeader("x-a", a), requestHeader("x-b", b), requestHeader("x-c", "bla"))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "x-c",
        ParameterLocation.HEADER
      ))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test")
      .with(requestHeader("x-a", a), requestHeader("x-b", b), requestHeader("x-c", "15"))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.VALIDATION_ERROR,
        "x-c",
        ParameterLocation.HEADER
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testCookieParamsSimpleTypes(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .cookieParameter(param("param1", booleanSchema()))
      .cookieParameter(param("param2", intSchema()))
      .build();
    router
      .get("/testCookieParams")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .setStatusMessage(
            params.cookieParameter("param1").toString() + params.cookieParameter("param2").toString()
          )
          .end();
      });

    QueryStringEncoder successParams = new QueryStringEncoder("/");
    successParams.addParam("param1", "true");
    successParams.addParam("param2", "10");

    testRequest(client, HttpMethod.GET, "/testCookieParams")
      .with(cookie(successParams))
      .expect(statusCode(200), statusMessage("true10"))
      .send(testContext, checkpoint);

    QueryStringEncoder failureParams = new QueryStringEncoder("/");
    failureParams.addParam("param1", "true");
    failureParams.addParam("param2", "bla");

    testRequest(client, HttpMethod.GET, "/testCookieParams")
      .with(cookie(failureParams))
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "param2",
        ParameterLocation.COOKIE
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testCookieParamsAsync(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    ValidationHandler validationHandler = ValidationHandlerBuilder
      .create(schemaRepo)
      .cookieParameter(param("param1", booleanSchema()))
      .cookieParameter(param("param2", intSchema().withKeyword("maximum", 10).withKeyword("minimum", 0),
        ValueParser.LONG_PARSER))
      .build();
    router
      .get("/test")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .setStatusMessage(
            params.cookieParameter("param1").toString() + params.cookieParameter("param2").toString()
          )
          .end();
      });

    QueryStringEncoder successParams = new QueryStringEncoder("/");
    successParams.addParam("param1", "true");
    successParams.addParam("param2", "10");
    testRequest(client, HttpMethod.GET, "/test")
      .with(cookie(successParams))
      .expect(statusCode(200), statusMessage("true10"))
      .send(testContext, checkpoint);

    QueryStringEncoder failureParams1 = new QueryStringEncoder("/");
    failureParams1.addParam("param1", "true");
    failureParams1.addParam("param2", "bla");
    testRequest(client, HttpMethod.GET, "/test")
      .with(cookie(failureParams1))
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "param2",
        ParameterLocation.COOKIE
      ))
      .send(testContext, checkpoint);

    QueryStringEncoder failureParams2 = new QueryStringEncoder("/");
    failureParams2.addParam("param1", "true");
    failureParams2.addParam("param2", "15");
    testRequest(client, HttpMethod.GET, "/test")
      .with(cookie(failureParams2))
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.VALIDATION_ERROR,
        "param2",
        ParameterLocation.COOKIE
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testFormURLEncoded(VertxTestContext testContext, @TempDir Path tempDir) throws Exception {
    Checkpoint checkpoint = testContext.checkpoint(2);

    ValidationHandler validationHandler = ValidationHandlerBuilder.create(schemaRepo)
      .body(Bodies.formUrlEncoded(objectSchema().requiredProperty("parameter", intSchema())))
      .build();

    router.route().handler(BodyHandler.create(tempDir.toAbsolutePath().toString()));
    router
      .post("/testFormParam")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .setStatusMessage(params.body().getJsonObject().getInteger("parameter").toString())
          .end();
      });

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(200), statusMessage("5"))
      .sendURLEncodedForm(MultiMap.caseInsensitiveMultiMap().add("parameter", "5"), testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(400))
      .expect(
        badBodyResponse(BodyProcessorException.BodyProcessorErrorType.PARSING_ERROR)
      )
      .sendURLEncodedForm(MultiMap.caseInsensitiveMultiMap().add("parameter", "bla"), testContext, checkpoint);
  }

  @Test
  public void testMultipartForm(VertxTestContext testContext, @TempDir Path tempDir) throws Exception {
    Checkpoint checkpoint = testContext.checkpoint(2);

    ValidationHandler validationHandler = ValidationHandlerBuilder.create(schemaRepo)
      .body(Bodies.multipartFormData(objectSchema().requiredProperty("parameter", intSchema())))
      .build();

    router.route().handler(BodyHandler.create(tempDir.toAbsolutePath().toString()));
    router
      .post("/testFormParam")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .setStatusMessage(params.body().getJsonObject().getInteger("parameter").toString())
          .end();
      });

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(200), statusMessage("5"))
      .sendMultipartForm(MultipartForm.create().attribute("parameter", "5"), testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(400))
      .expect(
        badBodyResponse(BodyProcessorException.BodyProcessorErrorType.PARSING_ERROR)
      )
      .sendMultipartForm(MultipartForm.create().attribute("parameter", "bla"), testContext, checkpoint);
  }

  @Test
  public void testBothFormTypes(VertxTestContext testContext, @TempDir Path tempDir) throws Exception {
    Checkpoint checkpoint = testContext.checkpoint(6);

    ObjectSchemaBuilder bodySchema = objectSchema().requiredProperty("parameter", intSchema());

    ValidationHandler validationHandler = ValidationHandlerBuilder.create(schemaRepo)
      .body(Bodies.multipartFormData(bodySchema))
      .body(Bodies.formUrlEncoded(bodySchema))
      .build();

    router.route().handler(BodyHandler.create(tempDir.toAbsolutePath().toString()));
    router
      .post("/testFormParam")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        if (params.body() != null) {
          routingContext
            .response()
            .setStatusMessage(params.body().getJsonObject().getInteger("parameter").toString())
            .end();
        } else {
          routingContext
            .response()
            .setStatusMessage("No body")
            .end();
        }
      });

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(200), statusMessage("5"))
      .sendURLEncodedForm(MultiMap.caseInsensitiveMultiMap().add("parameter", "5"), testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(400))
      .expect(
        badBodyResponse(BodyProcessorException.BodyProcessorErrorType.PARSING_ERROR)
      )
      .sendURLEncodedForm(MultiMap.caseInsensitiveMultiMap().add("parameter", "bla"), testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(200), statusMessage("5"))
      .sendMultipartForm(MultipartForm.create().attribute("parameter", "5"), testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(400))
      .expect(
        badBodyResponse(BodyProcessorException.BodyProcessorErrorType.PARSING_ERROR)
      )
      .sendMultipartForm(MultipartForm.create().attribute("parameter", "bla"), testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(200), statusMessage("No body"))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(400))
      .expect(
        badBodyResponse(BodyProcessorException.BodyProcessorErrorType.MISSING_MATCHING_BODY_PROCESSOR)
      )
      .sendJson(new JsonObject(), testContext, checkpoint);
  }

  @Test
  public void testSameResultWithDifferentBodyTypes(VertxTestContext testContext, @TempDir Path tempDir) throws Exception {
    Checkpoint checkpoint = testContext.checkpoint(3);

    JsonObject expectedResult = new JsonObject()
      .put("int", 10)
      .put("string", "hello")
      .put("array", new JsonArray().add(1).add(1.1));

    ObjectSchemaBuilder bodySchema = objectSchema()
      .requiredProperty("int", intSchema())
      .requiredProperty("string", stringSchema())
      .property("array", arraySchema().items(numberSchema()));

    ValidationHandler validationHandler = ValidationHandlerBuilder.create(schemaRepo)
      .body(Bodies.json(bodySchema))
      .body(Bodies.multipartFormData(bodySchema))
      .body(Bodies.formUrlEncoded(bodySchema))
      .build();

    router.route().handler(BodyHandler.create(tempDir.toAbsolutePath().toString()));
    router
      .post("/testFormParam")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        if (params.body().getJsonObject().equals(expectedResult)) {
          routingContext
            .response()
            .setStatusCode(200)
            .end();
        } else {
          routingContext
            .response()
            .setStatusCode(500)
            .end();
        }
      });

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(200))
      .sendURLEncodedForm(
        MultiMap
          .caseInsensitiveMultiMap()
          .add("int", "10")
          .add("string", "hello")
          .add("array", "1")
          .add("array", "1.1"),
        testContext, checkpoint
      );

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(200))
      .sendMultipartForm(
        MultipartForm.create()
          .attribute("int", "10")
          .attribute("string", "hello")
          .attribute("array", "1")
          .attribute("array", "1.1"),
        testContext, checkpoint
      );

    testRequest(client, HttpMethod.POST, "/testFormParam")
      .expect(statusCode(200))
      .sendJson(expectedResult, testContext, checkpoint);
  }

  @Test
  public void testValidationHandlerChaining(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(1);

    ValidationHandler validationHandler1 = ValidationHandlerBuilder.create(schemaRepo)
      .queryParameter(param("param1", intSchema()))
      .build();

    ValidationHandler validationHandler2 = ValidationHandlerBuilder.create(schemaRepo)
      .queryParameter(param("param2", booleanSchema()))
      .build();

    router.get("/testHandlersChaining")
      .handler(validationHandler1)
      .handler(validationHandler2)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .setStatusMessage(
            params.queryParameter("param1").toString() +
              params.queryParameter("param2").toString()
          )
          .end();
      });

    testRequest(client, HttpMethod.GET, "/testHandlersChaining?param1=10&param2=true")
      .expect(statusCode(200), statusMessage("10true"))
      .send(testContext, checkpoint);
  }

  @Test
  public void testJsonBody(VertxTestContext testContext, @TempDir Path tempDir) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    ValidationHandler validationHandler = ValidationHandlerBuilder.create(schemaRepo)
      .body(Bodies.json(objectSchema()))
      .build();

    router.post("/test")
      .handler(BodyHandler.create(tempDir.toAbsolutePath().toString()))
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .setStatusMessage(
            params.body().getJsonObject().toString()
          )
          .end();
      });

    testRequest(client, HttpMethod.POST, "/test")
      .expect(statusCode(200), statusMessage("{}"))
      .sendJson(new JsonObject(), testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/test")
      .expect(statusCode(400))
      .expect(badBodyResponse(BodyProcessorException.BodyProcessorErrorType.VALIDATION_ERROR))
      .sendJson("aaa", testContext, checkpoint);
  }

  @Test
  public void testJsonBodyAsyncCircular(VertxTestContext testContext, @TempDir Path tempDir) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    SchemaBuilder childs = arraySchema().items(new GenericSchemaBuilder().withKeyword("$ref", "#"));
    SchemaBuilder treeSchema = objectSchema().requiredProperty("value", stringSchema()).property("childs", childs);

    ValidationHandler validationHandler1 = ValidationHandlerBuilder.create(schemaRepo)
      .body(Bodies.json(treeSchema))
      .build();

    JsonObject testObj = new JsonObject()
      .put("value", "aaa")
      .put("childs", new JsonArray().add(
        new JsonObject().put("value", "bbb")
      ));

    router.post("/test")
      .handler(BodyHandler.create(tempDir.toAbsolutePath().toString()))
      .handler(validationHandler1)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .putHeader("content-type", "application/json")
          .end(params.body().getJsonObject().toBuffer());
      });

    testRequest(client, HttpMethod.POST, "/test")
      .expect(statusCode(200), jsonBodyResponse(testObj))
      .sendJson(testObj, testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/test")
      .expect(statusCode(400))
      .expect(badBodyResponse(BodyProcessorException.BodyProcessorErrorType.VALIDATION_ERROR))
      .sendJson("aaa", testContext, checkpoint);
  }

  @Test
  public void testQueryExpandedObjectAdditionalPropertiesAndDefault(VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(4);

    ValidationHandler validationHandler = ValidationHandlerBuilder.create(schemaRepo)
      .queryParameter(optionalExplodedParam("explodedObject",
        objectSchema()
          .property("wellKnownProperty", intSchema())
          .additionalProperties(booleanSchema())
          .defaultValue(new JsonObject())
      ))
      .build();

    router.get("/test")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .putHeader("content-type", "application/json")
          .end(params.queryParameter("explodedObject").getJsonObject().toBuffer());
      });

    testRequest(client, HttpMethod.GET, "/test")
      .expect(statusCode(200), jsonBodyResponse(new JsonObject()))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?wellKnownProperty=10")
      .expect(statusCode(200), jsonBodyResponse(new JsonObject().put("wellKnownProperty", 10)))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?wellKnownProperty=10&myFlag=false")
      .expect(statusCode(200), jsonBodyResponse(new JsonObject().put("wellKnownProperty", 10).put("myFlag", false)))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/test?wellKnownProperty=10&myFlag=bla")
      .expect(statusCode(400))
      .expect(badParameterResponse(
        ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR,
        "explodedObject",
        ParameterLocation.QUERY
      ))
      .send(testContext, checkpoint);
  }

  @Test
  public void testSimpleHeaderCaseInsensitivity(VertxTestContext testContext) {
    ValidationHandler validationHandler = ValidationHandlerBuilder.create(schemaRepo)
      .headerParameter(param("AnHeader", intSchema()))
      .build();

    router.get("/test")
      .handler(validationHandler)
      .handler(routingContext -> {
        RequestParameters params = routingContext.get("parsedParameters");
        routingContext
          .response()
          .putHeader("content-type", "application/json")
          .end(params.headerParameter("anHeader").getInteger().toString());
      });

    testRequest(client, HttpMethod.GET, "/test")
      .with(requestHeader("anheader", "10"))
      .expect(statusCode(200), jsonBodyResponse(10))
      .send(testContext);
  }

}
