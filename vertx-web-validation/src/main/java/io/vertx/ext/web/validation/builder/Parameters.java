package io.vertx.ext.web.validation.builder;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.web.validation.builder.impl.ValidationDSLUtils;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessorImpl;
import io.vertx.ext.web.validation.impl.parameter.SingleValueParameterParser;
import io.vertx.ext.web.validation.impl.parser.ValueParser;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.json.schema.common.dsl.BooleanSchemaBuilder;
import io.vertx.json.schema.common.dsl.NumberSchemaBuilder;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.StringSchemaBuilder;
import io.vertx.json.schema.common.dsl.TupleSchemaBuilder;

/**
 * In this interface you can find all available {@link ParameterProcessorFactory} to use in
 * {@link ValidationHandlerBuilder}. <br/>
 * <p>
 * To create new schemas using {@link SchemaBuilder}, look at the
 * <a href="https://vertx.io/docs/vertx-json-schema/java/">docs of vertx-json-schema</a>
 */
@VertxGen
public interface Parameters {

  /**
   * Creates a new required number parameter. Depending on the type provided in {@code schemaBuilder}, the parser
   * will parse the number as {@link Long} or {@link Double}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory param(String parameterName, NumberSchemaBuilder schemaBuilder) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      false,
      new SingleValueParameterParser(
        location.lowerCaseIfNeeded(parameterName),
        schemaBuilder.isIntegerSchema() ? ValueParser.LONG_PARSER : ValueParser.DOUBLE_PARSER
      ),
      schemaRepository,
      schemaBuilder.toJson()
    );
  }

  /**
   * Creates a new optional number parameter. Depending on the type provided in {@code schemaBuilder}, the parser
   * will parse the number as {@link Long} or {@link Double}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory optionalParam(String parameterName, NumberSchemaBuilder schemaBuilder) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      true,
      new SingleValueParameterParser(
        location.lowerCaseIfNeeded(parameterName),
        schemaBuilder.isIntegerSchema() ? ValueParser.LONG_PARSER : ValueParser.DOUBLE_PARSER
      ),
      schemaRepository,
      schemaBuilder.toJson()
    );
  }

  /**
   * Creates a new required string parameter
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory param(String parameterName, StringSchemaBuilder schemaBuilder) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      false,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), ValueParser.NOOP_PARSER),
      schemaRepository,
      schemaBuilder.toJson()
    );
  }

  /**
   * Creates a new optional string parameter
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory optionalParam(String parameterName, StringSchemaBuilder schemaBuilder) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      true,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), ValueParser.NOOP_PARSER),
      schemaRepository,
      schemaBuilder.toJson()
    );
  }

  /**
   * Creates a new required boolean parameter
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory param(String parameterName, BooleanSchemaBuilder schemaBuilder) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      false,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), ValueParser.BOOLEAN_PARSER),
      schemaRepository,
      schemaBuilder.toJson()
    );
  }

  /**
   * Creates a new optional boolean parameter
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory optionalParam(String parameterName, BooleanSchemaBuilder schemaBuilder) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      true,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), ValueParser.BOOLEAN_PARSER),
      schemaRepository,
      schemaBuilder.toJson()
    );
  }

  /**
   * Creates a new required array parameter serialized as comma separated
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory param(String parameterName, ArraySchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createArrayParamFactory(
      parameterName,
      Parsers.commaSeparatedArrayParser(),
      schemaBuilder,
      false
    )::apply;
  }

  /**
   * Creates a new optional array parameter serialized as comma separated
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory optionalParam(String parameterName, ArraySchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createArrayParamFactory(
      parameterName,
      Parsers.commaSeparatedArrayParser(),
      schemaBuilder,
      true
    )::apply;
  }

  /**
   * Creates a new required tuple parameter serialized as comma separated
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory param(String parameterName, TupleSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createTupleParamFactory(
      parameterName,
      Parsers.commaSeparatedTupleParser(),
      schemaBuilder,
      false
    )::apply;
  }

  /**
   * Creates a new optional tuple parameter serialized as comma separated
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory optionalParam(String parameterName, TupleSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createTupleParamFactory(
      parameterName,
      Parsers.commaSeparatedTupleParser(),
      schemaBuilder,
      true
    )::apply;
  }

  /**
   * Creates a required object parameter serialized as comma separated, like {@code key1,value1,key2,value2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory param(String parameterName, ObjectSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createObjectParamFactory(
      parameterName,
      Parsers.commaSeparatedObjectParser(),
      schemaBuilder,
      false
    )::apply;
  }

  /**
   * Creates an optional object parameter serialized as comma separated, like {@code key1,value1,key2,value2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory optionalParam(String parameterName, ObjectSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createObjectParamFactory(
      parameterName,
      Parsers.commaSeparatedObjectParser(),
      schemaBuilder,
      true
    )::apply;
  }

  /**
   * Creates a required parameter providing a {@link ValueParser}
   *
   * @param parameterName
   * @param schemaBuilder
   * @param valueParser
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory param(String parameterName, SchemaBuilder schemaBuilder,
                                         ValueParser<String> valueParser) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      false,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), valueParser),
      schemaRepository,
      schemaBuilder.toJson()
    );
  }

  /**
   * Creates an optional parameter providing a {@link ValueParser}
   *
   * @param parameterName
   * @param schemaBuilder
   * @param valueParser
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ParameterProcessorFactory optionalParam(String parameterName, SchemaBuilder schemaBuilder,
                                                 ValueParser<String> valueParser) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      true,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), valueParser),
      schemaRepository,
      schemaBuilder.toJson()
    );
  }

  /**
   * Creates a required parameter serialized as valid json
   *
   * @param parameterName
   * @param builder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory jsonParam(String parameterName, SchemaBuilder builder) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      false,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), ValueParser.JSON_PARSER),
      schemaRepository,
      builder.toJson()
    );
  }

  /**
   * Creates an optional parameter serialized as valid json
   *
   * @param parameterName
   * @param builder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory optionalJsonParam(String parameterName, SchemaBuilder builder) {
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      true,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), ValueParser.JSON_PARSER),
      schemaRepository,
      builder.toJson()
    );
  }

  /**
   * Creates a required array parameter deserializable using the provided parser factory. Look at {@link Parsers} for
   * available parser factories
   *
   * @param parameterName
   * @param arrayParserFactory
   * @param schemaBuilder
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory serializedParam(String parameterName, ArrayParserFactory arrayParserFactory,
                                                         ArraySchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createArrayParamFactory(parameterName, arrayParserFactory, schemaBuilder, false)::apply;
  }

  /**
   * Creates an optional array parameter deserializable using the provided parser factory. Look at {@link Parsers}
   * for available parser factories
   *
   * @param parameterName
   * @param arrayParserFactory
   * @param schemaBuilder
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory optionalSerializedParam(String parameterName,
                                                                 ArrayParserFactory arrayParserFactory,
                                                                 ArraySchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createArrayParamFactory(parameterName, arrayParserFactory, schemaBuilder, true)::apply;
  }

  /**
   * Creates a required tuple parameter deserializable using the provided parser factory. Look at {@link Parsers} for
   * available parser factories
   *
   * @param parameterName
   * @param tupleParserFactory
   * @param schemaBuilder
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory serializedParam(String parameterName, TupleParserFactory tupleParserFactory,
                                                         TupleSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createTupleParamFactory(parameterName, tupleParserFactory, schemaBuilder, false)::apply;
  }

  /**
   * Creates an optional tuple parameter deserializable using the provided parser factory. Look at {@link Parsers}
   * for available parser factories
   *
   * @param parameterName
   * @param tupleParserFactory
   * @param schemaBuilder
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory optionalSerializedParam(String parameterName,
                                                                 TupleParserFactory tupleParserFactory,
                                                                 TupleSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createTupleParamFactory(parameterName, tupleParserFactory, schemaBuilder, true)::apply;
  }

  /**
   * Creates a required object parameter deserializable using the provided parser factory. Look at {@link Parsers}
   * for available parser factories
   *
   * @param parameterName
   * @param objectParserFactory
   * @param schemaBuilder
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory serializedParam(String parameterName,
                                                         ObjectParserFactory objectParserFactory,
                                                         ObjectSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createObjectParamFactory(parameterName, objectParserFactory, schemaBuilder, false)::apply;
  }

  /**
   * Creates an optional object parameter deserializable using the provided parser factory. Look at {@link Parsers}
   * for available parser factories
   *
   * @param parameterName
   * @param objectParserFactory
   * @param schemaBuilder
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory optionalSerializedParam(String parameterName,
                                                                 ObjectParserFactory objectParserFactory,
                                                                 ObjectSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createObjectParamFactory(parameterName, objectParserFactory, schemaBuilder, true)::apply;
  }

  /**
   * Creates a required exploded array parameter. Exploded parameters looks like {@code parameterName=item1
   * &parameterName=item2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory explodedParam(String parameterName, ArraySchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createExplodedArrayParamFactory(parameterName, schemaBuilder, false);
  }

  /**
   * Creates an optional exploded array parameter. Exploded parameters looks like {@code parameterName=item1
   * &parameterName=item2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory optionalExplodedParam(String parameterName, ArraySchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createExplodedArrayParamFactory(parameterName, schemaBuilder, true);
  }

  /**
   * Creates a required exploded tuple parameter. Exploded parameters looks like {@code parameterName=item1
   * &parameterName=item2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory explodedParam(String parameterName, TupleSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createExplodedTupleParamFactory(parameterName, schemaBuilder, false);
  }

  /**
   * Creates an optional exploded tuple parameter. Exploded parameters looks like {@code parameterName=item1
   * &parameterName=item2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory optionalExplodedParam(String parameterName, TupleSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createExplodedTupleParamFactory(parameterName, schemaBuilder, true);
  }

  /**
   * Creates a required exploded object parameter. Exploded parameters looks like {@code key1=value1&key2=value2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory explodedParam(String parameterName, ObjectSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createExplodedObjectParamFactory(parameterName, schemaBuilder, false);
  }

  /**
   * Creates an optional exploded object parameter. Exploded parameters looks like {@code key1=value1&key2=value2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory optionalExplodedParam(String parameterName,
                                                               ObjectSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createExplodedObjectParamFactory(parameterName, schemaBuilder, true);
  }

  /**
   * Creates a required deep object parameter. Deep object parameters looks like {@code parameterName[key1]=value1
   * &parameterName[key2]=value2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory deepObjectParam(String parameterName, ObjectSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createDeepObjectParamFactory(parameterName, schemaBuilder, false);
  }

  /**
   * Creates an optional deep object parameter. Deep object parameters looks like {@code parameterName[key1]=value1
   * &parameterName[key2]=value2}
   *
   * @param parameterName
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StyledParameterProcessorFactory optionalDeepObjectParam(String parameterName,
                                                                 ObjectSchemaBuilder schemaBuilder) {
    return ValidationDSLUtils.createDeepObjectParamFactory(parameterName, schemaBuilder, true);
  }
}
