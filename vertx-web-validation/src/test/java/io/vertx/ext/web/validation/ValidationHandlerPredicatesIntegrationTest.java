package io.vertx.ext.web.validation;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.regex.Pattern;

import static io.vertx.ext.web.validation.testutils.TestRequest.statusCode;
import static io.vertx.ext.web.validation.testutils.TestRequest.testRequest;
import static io.vertx.ext.web.validation.testutils.ValidationTestUtils.failurePredicateResponse;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
@SuppressWarnings("unchecked")
@ExtendWith(VertxExtension.class)
public class ValidationHandlerPredicatesIntegrationTest extends BaseValidationHandlerTest {

  @Test
  public void testRequiredBodyPredicate(VertxTestContext testContext, @TempDir Path tempDir) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    ValidationHandler validationHandler = ValidationHandlerBuilder.create(schemaRepo)
      .predicate(RequestPredicate.BODY_REQUIRED)
      .build();

    router.route("/testRequiredBody")
      .handler(BodyHandler.create(tempDir.toAbsolutePath().toString()))
      .handler(validationHandler)
      .handler(routingContext ->
        routingContext
          .response()
          .setStatusCode(200)
          .end()
      );

    testRequest(client, HttpMethod.POST, "/testRequiredBody")
      .expect(statusCode(200))
      .sendJson(new JsonObject(), testContext, checkpoint);

    testRequest(client, HttpMethod.GET, "/testRequiredBody")
      .expect(statusCode(400), failurePredicateResponse())
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testRequiredBody")
      .expect(statusCode(400), failurePredicateResponse())
      .send(testContext, checkpoint);
  }

  @Test
  public void testFileUploadExists(VertxTestContext testContext, @TempDir Path tempDir) {
    Checkpoint checkpoint = testContext.checkpoint(4);

    ValidationHandler validationHandler = ValidationHandlerBuilder.create(schemaRepo)
      .predicate(RequestPredicate.multipartFileUploadExists(
        "myfile",
        Pattern.quote("text/plain")
      ))
      .build();

    router.post("/testFileUpload")
      .handler(BodyHandler.create(tempDir.toAbsolutePath().toString()))
      .handler(validationHandler)
      .handler(routingContext ->
        routingContext
          .response()
          .setStatusCode(200)
          .end()
      );

    testRequest(client, HttpMethod.POST, "/testFileUpload")
      .expect(statusCode(200))
      .send(testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFileUpload")
      .expect(statusCode(400))
      .sendMultipartForm(MultipartForm.create(), testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFileUpload")
      .expect(statusCode(400))
      .sendMultipartForm(MultipartForm.create().attribute("myfile", "bla"), testContext, checkpoint);

    testRequest(client, HttpMethod.POST, "/testFileUpload")
      .expect(statusCode(200))
      .sendMultipartForm(MultipartForm.create().textFileUpload("myfile", "myfile.txt", "src/test/resources/myfile" +
        ".txt", "text/plain"), testContext, checkpoint);
  }

}
