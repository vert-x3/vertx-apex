package io.vertx.ext.web.validation.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.validation.MalformedValueException;
import io.vertx.ext.web.validation.impl.parser.SplitterCharTupleParser;
import io.vertx.ext.web.validation.impl.parser.ValueParser;
import io.vertx.ext.web.validation.testutils.TestParsers;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.draft7.Draft7SchemaParser;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(VertxExtension.class)
public class SplitterCharTupleValueParserTest {

  SchemaRouter router;
  SchemaParser parser;

  @BeforeEach
  public void setUp(Vertx vertx) {
    router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    parser = Draft7SchemaParser.create(router);
  }

  @Test
  public void testValid() {
    SplitterCharTupleParser parser = new SplitterCharTupleParser(
      TestParsers.SAMPLE_TUPLE_ITEMS_PARSERS, ValueParser.BOOLEAN_PARSER,","
    );

    Object result = parser.parse("1,hello,2,true");

    assertThat(result)
      .isInstanceOfSatisfying(JsonArray.class, ja ->
        assertThat(ja)
          .isEqualTo(TestParsers.SAMPLE_TUPLE.copy().add(true))
      );
  }

  @Test
  public void testNoAdditionalProperties() {
    SplitterCharTupleParser parser = new SplitterCharTupleParser(
      TestParsers.SAMPLE_TUPLE_ITEMS_PARSERS, null,","
    );

    Object result = parser.parse("1,hello,2,true");

    assertThat(result)
      .isInstanceOfSatisfying(JsonArray.class, ja ->
        assertThat(ja)
          .isEqualTo(TestParsers.SAMPLE_TUPLE.copy().add("true"))
      );
  }

  @Test
  public void testNull() {
    SplitterCharTupleParser parser = new SplitterCharTupleParser(
      TestParsers.SAMPLE_TUPLE_ITEMS_PARSERS, null,","
    );

    Object result = parser.parse(",hello,");

    assertThat(result)
      .isInstanceOfSatisfying(JsonArray.class, ja ->
        assertThat(ja)
          .isEqualTo(new JsonArray().addNull().add("hello").addNull())
      );
  }

  @Test
  public void testEmptyString() {
    SplitterCharTupleParser parser = new SplitterCharTupleParser(
      TestParsers.SAMPLE_TUPLE_ITEMS_PARSERS, null,","
    );

    Object result = parser.parse("1,,2");

    assertThat(result)
      .isInstanceOfSatisfying(JsonArray.class, ja ->
        assertThat(ja)
          .isEqualTo(new JsonArray().add(1d).add("").add(2d))
      );
  }

  @Test
  public void testInvalid() {
    SplitterCharTupleParser parser = new SplitterCharTupleParser(
      TestParsers.SAMPLE_TUPLE_ITEMS_PARSERS, ValueParser.BOOLEAN_PARSER,","
    );

    assertThatExceptionOfType(MalformedValueException.class)
      .isThrownBy(() -> parser.parse("true,hello"));
  }
}
