package io.vertx.ext.web.validation;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRepository;

/**
 * This is the entry point of this module. Provides the parsing, validation and puts the parsed objects into
 * {@link RoutingContext}. <br/>
 * <p>
 * You can easily build a new validation handler using a {@link ValidationHandlerBuilder}, that you can create with
 * {@link ValidationHandlerBuilder#create(SchemaRepository)}. <br/>
 * <p>
 * For more info read the doc. <br/>
 *
 * @author Francesco Guardiani @slinkydeveloper
 */
@VertxGen
public interface ValidationHandler extends Handler<RoutingContext> {

  String REQUEST_CONTEXT_KEY = "requestParameters";

  /**
   * @param schemaParser a SchemaParser
   * @return an instance of {@link ValidationHandlerBuilder}.
   * @deprecated {@link SchemaParser} is deprecated. Please use
   * {@link ValidationHandlerBuilder#create(io.vertx.json.schema.SchemaRepository)}.
   */
  @Deprecated
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ValidationHandlerBuilder builder(SchemaParser schemaParser) {
    return ValidationHandlerBuilder.create(schemaParser);
  }
}
