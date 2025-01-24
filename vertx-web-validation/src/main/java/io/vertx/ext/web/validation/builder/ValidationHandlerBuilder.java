package io.vertx.ext.web.validation.builder;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.validation.RequestPredicate;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.impl.ValidationHandlerBuilderImpl;
import io.vertx.ext.web.validation.impl.ParameterLocation;
import io.vertx.ext.web.validation.impl.body.BodyProcessor;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessor;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.json.schema.draft201909.Draft201909SchemaParser;
import io.vertx.json.schema.draft7.Draft7SchemaParser;
import io.vertx.json.schema.openapi3.OpenAPI3SchemaParser;

import static io.vertx.json.schema.Draft.DRAFT201909;
import static io.vertx.json.schema.Draft.DRAFT7;

/**
 * Builder for a {@link ValidationHandler}. <br/>
 * <p>
 * For more info look the docs
 */
@VertxGen
public interface ValidationHandlerBuilder {

  /**
   * Add a parameter given the location and the processor
   *
   * @param location
   * @param processor
   * @return
   */
  @Fluent
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  ValidationHandlerBuilder parameter(ParameterLocation location, ParameterProcessor processor);

  @Fluent
  ValidationHandlerBuilder queryParameter(StyledParameterProcessorFactory parameterProcessor);

  @Fluent
  ValidationHandlerBuilder queryParameter(ParameterProcessorFactory parameterProcessor);

  @Fluent
  ValidationHandlerBuilder pathParameter(ParameterProcessorFactory parameterProcessor);

  @Fluent
  ValidationHandlerBuilder cookieParameter(StyledParameterProcessorFactory parameterProcessor);

  @Fluent
  ValidationHandlerBuilder cookieParameter(ParameterProcessorFactory parameterProcessor);

  @Fluent
  ValidationHandlerBuilder headerParameter(ParameterProcessorFactory parameterProcessor);

  @Fluent
  ValidationHandlerBuilder body(BodyProcessorFactory bodyProcessor);

  @Fluent
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  ValidationHandlerBuilder body(BodyProcessor bodyProcessor);

  @Fluent
  ValidationHandlerBuilder predicate(RequestPredicate predicate);

  /**
   * Build the {@link ValidationHandler} from this builder
   *
   * @return
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  ValidationHandler build();

  static ValidationHandlerBuilder create(SchemaRepository repo) {
    return new ValidationHandlerBuilderImpl(repo);
  }

  /**
   * @deprecated {@link SchemaParser} is deprecated. Please use
   * {@link ValidationHandlerBuilder#create(io.vertx.json.schema.SchemaRepository)}.
   */
  @Deprecated
  static ValidationHandlerBuilder create(SchemaParser parser) {
    FileSystem fs = Vertx.currentContext().owner().fileSystem();

    if (parser instanceof Draft7SchemaParser) {
      // Draft7SchemaParser was using Draft7
      return create(SchemaRepository.create(new JsonSchemaOptions().setDraft(DRAFT7)).preloadMetaSchema(fs));
    } else if (parser instanceof Draft201909SchemaParser) {
      // Draft201909SchemaParser was using draft201909
      return create(SchemaRepository.create(new JsonSchemaOptions().setDraft(DRAFT201909)).preloadMetaSchema(fs));
    } else if (parser instanceof OpenAPI3SchemaParser) {
      // OpenAPI3SchemaParser was using Draft7 And Supported only OpenAPI 3.0
      // The baseUri is irrelevant, as it was also not possible before to pass it
      SchemaRepository repo = SchemaRepository.create(new JsonSchemaOptions().setDraft(DRAFT7)).preloadMetaSchema(fs);
      // Load the OpenAPI Spec
      String ref = "https://spec.openapis.org/oas/3.0/schema/2021-09-28";
      JsonObject raw = new JsonObject(fs.readFileBlocking(ref.substring("https://".length())));
      repo.dereference(ref, JsonSchema.of(raw));
      return create(repo);
    } else {
      throw new IllegalArgumentException("Passed SchemaParser is not supported");
    }
  }
}
