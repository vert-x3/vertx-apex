package io.vertx.ext.web.validation.impl;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.MalformedValueException;
import io.vertx.ext.web.validation.builder.Bodies;
import io.vertx.ext.web.validation.impl.body.BodyProcessor;
import io.vertx.ext.web.validation.testutils.TestSchemas;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.json.schema.ValidationException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.vertx.json.schema.draft7.dsl.Schemas.schema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class JsonBodyProcessorImplTest {
  private SchemaRepository repository;

  @Mock
  RoutingContext mockedContext;
  @Mock
  HttpServerRequest mockerServerRequest;
  @Mock
  RequestBody mockerRequestBody;

  @BeforeEach
  public void setUp(Vertx vertx) {
    repository = SchemaRepository.create(new JsonSchemaOptions().setDraft(Draft.DRAFT7).setBaseUri("app://"));
  }

  @Test
  public void testContentTypeCheck() {
    BodyProcessor processor = Bodies.json(TestSchemas.SAMPLE_OBJECT_SCHEMA_BUILDER).create(repository);
    assertThat(processor.canProcess("application/json")).isTrue();
    assertThat(processor.canProcess("application/json; charset=utf-8")).isTrue();
    assertThat(processor.canProcess("application/superapplication+json")).isTrue();
  }

  @Test
  public void testJsonObject(VertxTestContext testContext) {
    when(mockedContext.body()).thenReturn(mockerRequestBody);
    when(mockerRequestBody.buffer()).thenReturn(TestSchemas.VALID_OBJECT.toBuffer());

    BodyProcessor processor = Bodies.json(TestSchemas.SAMPLE_OBJECT_SCHEMA_BUILDER).create(repository);

    processor.process(mockedContext).onComplete(testContext.succeeding(rp -> {
      testContext.verify(() -> {
        assertThat(rp.isJsonObject()).isTrue();
        assertThat(rp.getJsonObject())
          .isEqualTo(
            TestSchemas.VALID_OBJECT
          );
      });
      testContext.completeNow();
    }));
  }

  @Test
  public void testInvalidJsonObject(VertxTestContext testContext) {
    when(mockerServerRequest.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn("application/json");
    when(mockedContext.request()).thenReturn(mockerServerRequest);
    when(mockedContext.body()).thenReturn(mockerRequestBody);
    when(mockerRequestBody.buffer()).thenReturn(TestSchemas.INVALID_OBJECT.toBuffer());

    BodyProcessor processor = Bodies.json(TestSchemas.SAMPLE_OBJECT_SCHEMA_BUILDER).create(repository);

    processor.process(mockedContext).onComplete(testContext.failing(err -> {
      testContext.verify(() -> {
        assertThat(err)
          .isInstanceOf(BodyProcessorException.class)
          .hasFieldOrPropertyWithValue("actualContentType", "application/json")
          .hasCauseInstanceOf(ValidationException.class);
      });
      testContext.completeNow();
    }));
  }

  @Test
  public void testJsonArray(VertxTestContext testContext) {
    when(mockedContext.body()).thenReturn(mockerRequestBody);
    when(mockerRequestBody.buffer()).thenReturn(TestSchemas.VALID_ARRAY.toBuffer());

    BodyProcessor processor = Bodies.json(TestSchemas.SAMPLE_ARRAY_SCHEMA_BUILDER).create(repository);

    processor.process(mockedContext).onComplete(testContext.succeeding(rp -> {
      testContext.verify(() -> {
        assertThat(rp.isJsonArray()).isTrue();
        assertThat(rp.getJsonArray())
          .isEqualTo(
            TestSchemas.VALID_ARRAY
          );
      });
      testContext.completeNow();
    }));
  }

  @Test
  public void testInvalidJsonArray(VertxTestContext testContext) {
    when(mockerServerRequest.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn("application/json");
    when(mockedContext.request()).thenReturn(mockerServerRequest);
    when(mockedContext.body()).thenReturn(mockerRequestBody);
    when(mockerRequestBody.buffer()).thenReturn(TestSchemas.INVALID_ARRAY.toBuffer());

    BodyProcessor processor = Bodies.json(TestSchemas.SAMPLE_ARRAY_SCHEMA_BUILDER).create(repository);

    processor.process(mockedContext).onComplete(testContext.failing(err -> {
      testContext.verify(() -> {
        assertThat(err)
          .isInstanceOf(BodyProcessorException.class)
          .hasFieldOrPropertyWithValue("actualContentType", "application/json")
          .hasCauseInstanceOf(ValidationException.class);
      });
      testContext.completeNow();
    }));
  }

  @Test
  public void testMalformedJson() {
    when(mockerServerRequest.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn("application/json");
    when(mockedContext.request()).thenReturn(mockerServerRequest);
    when(mockedContext.body()).thenReturn(mockerRequestBody);
    when(mockerRequestBody.buffer()).thenReturn(Buffer.buffer("{\"a"));

    BodyProcessor processor = Bodies.json(TestSchemas.SAMPLE_ARRAY_SCHEMA_BUILDER).create(repository);

    assertThatCode(() -> processor.process(mockedContext))
      .isInstanceOf(BodyProcessorException.class)
      .hasFieldOrPropertyWithValue("actualContentType", "application/json")
      .hasCauseInstanceOf(DecodeException.class);
  }

  @Test
  public void testNull(VertxTestContext testContext) {
    when(mockedContext.body()).thenReturn(mockerRequestBody);
    when(mockerRequestBody.buffer()).thenReturn(Buffer.buffer("null"));

    BodyProcessor processor = Bodies.json(schema().withKeyword("type", "null")).create(repository);

    processor.process(mockedContext).onComplete(testContext.succeeding(rp -> {
      testContext.verify(() -> {
        assertThat(rp.isNull()).isTrue();
      });
      testContext.completeNow();
    }));
  }

  @Test
  public void testNullBody() {
    when(mockerServerRequest.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn("application/json");
    when(mockedContext.request()).thenReturn(mockerServerRequest);
    when(mockedContext.body()).thenReturn(mockerRequestBody);
    when(mockerRequestBody.buffer()).thenReturn(null);

    BodyProcessor processor = Bodies.json(schema().withKeyword("type", "null")).create(repository);

    assertThatCode(() -> processor.process(mockedContext))
      .isInstanceOf(BodyProcessorException.class)
      .hasFieldOrPropertyWithValue("actualContentType", "application/json")
      .hasCauseInstanceOf(MalformedValueException.class);
  }
}
