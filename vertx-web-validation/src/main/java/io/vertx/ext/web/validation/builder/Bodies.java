package io.vertx.ext.web.validation.builder;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.validation.impl.ValueParserInferenceUtils;
import io.vertx.ext.web.validation.impl.body.FormBodyProcessorImpl;
import io.vertx.ext.web.validation.impl.body.JsonBodyProcessorImpl;
import io.vertx.ext.web.validation.impl.body.TextPlainBodyProcessorImpl;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.StringSchemaBuilder;

/**
 * In this interface you can find all available {@link BodyProcessorFactory} to use in
 * {@link ValidationHandlerBuilder}. <br/>
 * <p>
 * To create new schemas using {@link SchemaBuilder}, look at the
 * <a href="https://vertx.io/docs/vertx-json-schema/java/">docs of vertx-json-schema</a>
 */
@VertxGen
public interface Bodies {

  /**
   * Create a json body processor
   *
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static BodyProcessorFactory json(SchemaBuilder schemaBuilder) {
    return schemaRepository -> new JsonBodyProcessorImpl(schemaRepository, schemaBuilder.toJson());
  }

  /**
   * Create a {@code text/plain} body processor
   *
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static BodyProcessorFactory textPlain(StringSchemaBuilder schemaBuilder) {
    return schemaRepository -> new TextPlainBodyProcessorImpl(schemaRepository, schemaBuilder.toJson());
  }

  /**
   * Create a form {@code application/x-www-form-urlencoded} processor
   *
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static BodyProcessorFactory formUrlEncoded(ObjectSchemaBuilder schemaBuilder) {
    return schemaRepository -> {
      JsonObject jsonSchema = schemaBuilder.toJson();
      return new FormBodyProcessorImpl(
        ValueParserInferenceUtils.infeerPropertiesFormValueParserForObjectSchema(jsonSchema),
        ValueParserInferenceUtils.infeerPatternPropertiesFormValueParserForObjectSchema(jsonSchema),
        ValueParserInferenceUtils.infeerAdditionalPropertiesFormValueParserForObjectSchema(jsonSchema),
        "application/x-www-form-urlencoded",
        schemaRepository,
        jsonSchema
      );
    };
  }

  /**
   * Create a form {@code multipart/form-data} processor
   *
   * @param schemaBuilder
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static BodyProcessorFactory multipartFormData(ObjectSchemaBuilder schemaBuilder) {
    return schemaRepository -> {
      JsonObject jsonSchema = schemaBuilder.toJson();
      return new FormBodyProcessorImpl(
        ValueParserInferenceUtils.infeerPropertiesFormValueParserForObjectSchema(jsonSchema),
        ValueParserInferenceUtils.infeerPatternPropertiesFormValueParserForObjectSchema(jsonSchema),
        ValueParserInferenceUtils.infeerAdditionalPropertiesFormValueParserForObjectSchema(jsonSchema),
        "multipart/form-data",
        schemaRepository,
        jsonSchema
      );
    };
  }
}
