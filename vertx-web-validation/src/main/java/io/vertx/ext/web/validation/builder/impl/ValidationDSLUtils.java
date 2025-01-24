package io.vertx.ext.web.validation.builder.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.validation.builder.ArrayParserFactory;
import io.vertx.ext.web.validation.builder.ObjectParserFactory;
import io.vertx.ext.web.validation.builder.StyledParameterProcessorFactory;
import io.vertx.ext.web.validation.builder.TupleParserFactory;
import io.vertx.ext.web.validation.impl.ParameterLocation;
import io.vertx.ext.web.validation.impl.ValueParserInferenceUtils;
import io.vertx.ext.web.validation.impl.parameter.DeepObjectValueParameterParser;
import io.vertx.ext.web.validation.impl.parameter.ExplodedArrayValueParameterParser;
import io.vertx.ext.web.validation.impl.parameter.ExplodedObjectValueParameterParser;
import io.vertx.ext.web.validation.impl.parameter.ExplodedTupleValueParameterParser;
import io.vertx.ext.web.validation.impl.parameter.ParameterParser;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessor;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessorImpl;
import io.vertx.ext.web.validation.impl.parameter.SingleValueParameterParser;
import io.vertx.ext.web.validation.impl.parser.ValueParser;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.TupleSchemaBuilder;

import java.util.function.BiFunction;

public class ValidationDSLUtils {

  public static BiFunction<ParameterLocation, SchemaRepository, ParameterProcessor> createArrayParamFactory(String parameterName, ArrayParserFactory arrayParserFactory, ArraySchemaBuilder schemaBuilder, boolean isOptional) {
    JsonObject schemaJson = schemaBuilder.toJson();
    ValueParser<String> parser = arrayParserFactory.newArrayParser(
      ValueParserInferenceUtils.infeerItemsParserForArraySchema(schemaJson)
    );
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      isOptional,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), parser),
      schemaRepository,
      schemaJson
    );
  }

  public static BiFunction<ParameterLocation, SchemaRepository, ParameterProcessor> createTupleParamFactory(String parameterName, TupleParserFactory tupleParserFactory, TupleSchemaBuilder schemaBuilder, boolean isOptional) {
    JsonObject schemaJson = schemaBuilder.toJson();
    ValueParser<String> parser = tupleParserFactory.newTupleParser(
      ValueParserInferenceUtils.infeerTupleParsersForArraySchema(schemaJson),
      ValueParserInferenceUtils.infeerAdditionalItemsParserForArraySchema(schemaJson)
    );
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      isOptional,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), parser),
      schemaRepository,
      schemaJson
    );
  }

  public static BiFunction<ParameterLocation, SchemaRepository, ParameterProcessor> createObjectParamFactory(String parameterName, ObjectParserFactory objectParserFactory, ObjectSchemaBuilder schemaBuilder, boolean isOptional) {
    JsonObject schemaJson = schemaBuilder.toJson();
    ValueParser<String> parser =
      objectParserFactory.newObjectParser(
        ValueParserInferenceUtils.infeerPropertiesParsersForObjectSchema(schemaJson),
        ValueParserInferenceUtils.infeerPatternPropertiesParsersForObjectSchema(schemaJson),
        ValueParserInferenceUtils.infeerAdditionalPropertiesParserForObjectSchema(schemaJson)
      );
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      isOptional,
      new SingleValueParameterParser(location.lowerCaseIfNeeded(parameterName), parser),
      schemaRepository,
      schemaJson
    );
  }

  public static StyledParameterProcessorFactory createExplodedArrayParamFactory(String parameterName,
                                                                                ArraySchemaBuilder schemaBuilder,
                                                                                boolean isOptional) {
    JsonObject schemaJson = schemaBuilder.toJson();
    return (location, schemaRepository) -> {
      ParameterParser parser = new ExplodedArrayValueParameterParser(
        location.lowerCaseIfNeeded(parameterName),
        ValueParserInferenceUtils.infeerItemsParserForArraySchema(schemaJson)
      );
      return new ParameterProcessorImpl(
        parameterName,
        location,
        isOptional,
        parser,
        schemaRepository,
        schemaJson
      );
    };
  }

  public static StyledParameterProcessorFactory createExplodedTupleParamFactory(String parameterName,
                                                                                TupleSchemaBuilder schemaBuilder,
                                                                                boolean isOptional) {
    JsonObject schemaJson = schemaBuilder.toJson();
    return (location, schemaRepository) -> {
      ParameterParser parser = new ExplodedTupleValueParameterParser(
        location.lowerCaseIfNeeded(parameterName),
        ValueParserInferenceUtils.infeerTupleParsersForArraySchema(schemaJson),
        ValueParserInferenceUtils.infeerAdditionalItemsParserForArraySchema(schemaJson)
      );
      return new ParameterProcessorImpl(
        parameterName,
        location,
        isOptional,
        parser,
        schemaRepository,
        schemaJson
      );
    };
  }

  public static StyledParameterProcessorFactory createExplodedObjectParamFactory(String parameterName,
                                                                                 ObjectSchemaBuilder schemaBuilder,
                                                                                 boolean isOptional) {
    JsonObject schemaJson = schemaBuilder.toJson();
    return (location, schemaRepository) -> {
      ParameterParser parser = new ExplodedObjectValueParameterParser(
        parameterName,
        ValueParserInferenceUtils.infeerPropertiesParsersForObjectSchema(schemaJson, location::lowerCaseIfNeeded),
        ValueParserInferenceUtils.infeerPatternPropertiesParsersForObjectSchema(schemaJson),
        ValueParserInferenceUtils.infeerAdditionalPropertiesParserForObjectSchema(schemaJson)
      );
      return new ParameterProcessorImpl(
        parameterName,
        location,
        isOptional,
        parser,
        schemaRepository,
        schemaJson
      );
    };
  }

  public static StyledParameterProcessorFactory createDeepObjectParamFactory(String parameterName,
                                                                             ObjectSchemaBuilder schemaBuilder,
                                                                             boolean isOptional) {
    JsonObject schemaJson = schemaBuilder.toJson();
    ParameterParser parser = new DeepObjectValueParameterParser(
      parameterName, ValueParserInferenceUtils.infeerPropertiesParsersForObjectSchema(schemaJson),
      ValueParserInferenceUtils.infeerPatternPropertiesParsersForObjectSchema(schemaJson),
      ValueParserInferenceUtils.infeerAdditionalPropertiesParserForObjectSchema(schemaJson)
    );
    return (location, schemaRepository) -> new ParameterProcessorImpl(
      parameterName,
      location,
      isOptional,
      parser,
      schemaRepository,
      schemaJson
    );
  }
}
