package io.vertx.ext.web.validation.impl;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
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
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class TextPlainBodyProcessorTest {

  private SchemaRepository repository;

  @Mock
  RoutingContext mockedContext;
  @Mock
  HttpServerRequest mockerServerRequest;
  @Mock
  RequestBody mockedRequestBody;

  @BeforeEach
  public void setUp(Vertx vertx) {
    repository = SchemaRepository.create(new JsonSchemaOptions().setDraft(Draft.DRAFT7).setBaseUri("app://"));
  }

  @Test
  public void testString(VertxTestContext testContext) {
    when(mockedContext.body()).thenReturn(mockedRequestBody);
    when(mockedRequestBody.asString()).thenReturn(TestSchemas.VALID_STRING);

    BodyProcessor processor = Bodies.textPlain(TestSchemas.SAMPLE_STRING_SCHEMA_BUILDER).create(repository);

    processor.process(mockedContext).onComplete(testContext.succeeding(rp -> {
      testContext.verify(() -> {
        assertThat(rp.isString()).isTrue();
        assertThat(rp.getString())
          .isEqualTo(
            TestSchemas.VALID_STRING
          );
      });
      testContext.completeNow();
    }));
  }

  @Test
  public void testNullBody() {
    when(mockerServerRequest.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn("text/plain");
    when(mockedContext.request()).thenReturn(mockerServerRequest);
    when(mockedContext.body()).thenReturn(mockedRequestBody);

    BodyProcessor processor = Bodies.textPlain(TestSchemas.SAMPLE_STRING_SCHEMA_BUILDER).create(repository);

    assertThatCode(() -> processor.process(mockedContext))
      .isInstanceOf(BodyProcessorException.class)
      .hasFieldOrPropertyWithValue("actualContentType", "text/plain")
      .hasCauseInstanceOf(MalformedValueException.class);
  }

}
