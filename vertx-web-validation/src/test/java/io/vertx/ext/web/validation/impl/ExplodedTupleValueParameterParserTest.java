package io.vertx.ext.web.validation.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.validation.MalformedValueException;
import io.vertx.ext.web.validation.impl.parameter.ExplodedArrayValueParameterParser;
import io.vertx.ext.web.validation.impl.parameter.ExplodedTupleValueParameterParser;
import io.vertx.ext.web.validation.impl.parser.ValueParser;
import io.vertx.ext.web.validation.testutils.TestParsers;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.draft7.Draft7SchemaParser;
import io.vertx.junit5.VertxExtension;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(VertxExtension.class)
public class ExplodedTupleValueParameterParserTest {

  SchemaRouter router;
  SchemaParser parser;

  @BeforeEach
  public void setUp(Vertx vertx) {
    router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    parser = Draft7SchemaParser.create(router);
  }

  @Test
  public void testValid() {
    ExplodedTupleValueParameterParser parser = new ExplodedTupleValueParameterParser(
      "bla", TestParsers.SAMPLE_TUPLE_ITEMS_PARSERS, ValueParser.BOOLEAN_PARSER
    );

    Map<String, List<String>> map = new HashMap<>();
    map.put("bla", Lists.newArrayList("1", "hello", "2", "true"));
    map.put("other", Collections.singletonList("aaa"));

    Object result = parser.parseParameter(map);

    assertThat(result)
      .isInstanceOfSatisfying(JsonArray.class, ja ->
        assertThat(ja)
          .isEqualTo(TestParsers.SAMPLE_TUPLE.copy().add(true))
      );

    assertThat(map)
      .containsKey("other")
      .doesNotContainKey("bla");
  }

  @Test
  public void testNoAdditionalItems() {
    ExplodedTupleValueParameterParser parser = new ExplodedTupleValueParameterParser(
      "bla", TestParsers.SAMPLE_TUPLE_ITEMS_PARSERS, null
    );

    Map<String, List<String>> map = new HashMap<>();
    map.put("bla", Lists.newArrayList("1", "hello", "2", "true"));
    map.put("other", Collections.singletonList("aaa"));

    Object result = parser.parseParameter(map);

    assertThat(result)
      .isInstanceOfSatisfying(JsonArray.class, ja ->
        assertThat(ja)
          .isEqualTo(TestParsers.SAMPLE_TUPLE.copy().add("true"))
      );

    assertThat(map)
      .containsKey("other")
      .doesNotContainKey("bla");
  }

  @Test
  public void testNullAndEmptyString() {
    ExplodedTupleValueParameterParser parser = new ExplodedTupleValueParameterParser(
      "bla", TestParsers.SAMPLE_TUPLE_ITEMS_PARSERS, ValueParser.BOOLEAN_PARSER
    );

    Map<String, List<String>> map = new HashMap<>();
    map.put("bla", Lists.newArrayList("", "", "2", "true"));
    map.put("other", Collections.singletonList("aaa"));

    Object result = parser.parseParameter(map);

    assertThat(result)
      .isInstanceOfSatisfying(JsonArray.class, ja ->
        assertThat(ja)
          .isEqualTo(new JsonArray().addNull().add("").add(2d).add(true))
      );

    assertThat(map)
      .containsKey("other")
      .doesNotContainKey("bla");
  }

  @Test
  public void testInvalid() {
    ExplodedArrayValueParameterParser parser = new ExplodedArrayValueParameterParser(
      "bla", ValueParser.BOOLEAN_PARSER
    );

    Map<String, List<String>> map = new HashMap<>();
    map.put("bla", Lists.newArrayList("hello", "1", "hello"));
    map.put("other", Collections.singletonList("aaa"));

    assertThatExceptionOfType(MalformedValueException.class)
      .isThrownBy(() -> parser.parseParameter(map));

    assertThat(map)
      .containsKey("other")
      .doesNotContainKey("bla");
  }

}
