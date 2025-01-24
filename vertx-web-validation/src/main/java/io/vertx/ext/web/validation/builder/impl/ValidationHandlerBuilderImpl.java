package io.vertx.ext.web.validation.builder.impl;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestPredicate;
import io.vertx.ext.web.validation.RequestPredicateResult;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.BodyProcessorFactory;
import io.vertx.ext.web.validation.builder.ParameterProcessorFactory;
import io.vertx.ext.web.validation.builder.StyledParameterProcessorFactory;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.ext.web.validation.impl.ParameterLocation;
import io.vertx.ext.web.validation.impl.ValidationHandlerImpl;
import io.vertx.ext.web.validation.impl.body.BodyProcessor;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessor;
import io.vertx.json.schema.SchemaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ValidationHandlerBuilderImpl implements ValidationHandlerBuilder {

  SchemaRepository schemaRepository;

  Map<ParameterLocation, List<ParameterProcessor>> parameterProcessors = new HashMap<>();
  List<BodyProcessor> bodyProcessors = new ArrayList<>();
  List<Function<RoutingContext, RequestPredicateResult>> predicates = new ArrayList<>();

  public ValidationHandlerBuilderImpl(SchemaRepository schemaRepository) {
    this.schemaRepository = schemaRepository;
  }

  @Override
  public ValidationHandlerBuilder parameter(ParameterLocation location, ParameterProcessor processor) {
    parameterProcessors.computeIfAbsent(location, k -> new ArrayList<>()).add(processor);
    return this;
  }

  @Override
  public ValidationHandlerBuilder queryParameter(StyledParameterProcessorFactory parameterProcessor) {
    return parameter(ParameterLocation.QUERY, parameterProcessor.create(ParameterLocation.QUERY, schemaRepository));
  }

  @Override
  public ValidationHandlerBuilder queryParameter(ParameterProcessorFactory parameterProcessor) {
    return parameter(ParameterLocation.QUERY, parameterProcessor.create(ParameterLocation.QUERY, schemaRepository));
  }

  @Override
  public ValidationHandlerBuilder pathParameter(ParameterProcessorFactory parameterProcessor) {
    return parameter(ParameterLocation.PATH, parameterProcessor.create(ParameterLocation.PATH, schemaRepository));
  }

  @Override
  public ValidationHandlerBuilder cookieParameter(StyledParameterProcessorFactory parameterProcessor) {
    return parameter(ParameterLocation.COOKIE, parameterProcessor.create(ParameterLocation.COOKIE, schemaRepository));
  }

  @Override
  public ValidationHandlerBuilder cookieParameter(ParameterProcessorFactory parameterProcessor) {
    return parameter(ParameterLocation.COOKIE, parameterProcessor.create(ParameterLocation.COOKIE, schemaRepository));
  }

  @Override
  public ValidationHandlerBuilder headerParameter(ParameterProcessorFactory parameterProcessor) {
    return parameter(ParameterLocation.HEADER, parameterProcessor.create(ParameterLocation.HEADER, schemaRepository));
  }

  @Override
  public ValidationHandlerBuilder body(BodyProcessorFactory bodyProcessor) {
    bodyProcessors.add(bodyProcessor.create(schemaRepository));
    return this;
  }

  @Override
  public ValidationHandlerBuilder body(BodyProcessor bodyProcessor) {
    bodyProcessors.add(bodyProcessor);
    return this;
  }

  @Override
  public ValidationHandlerBuilder predicate(RequestPredicate predicate) {
    predicates.add(predicate);
    return this;
  }

  @Override
  public ValidationHandler build() {
    return new ValidationHandlerImpl(
      parameterProcessors,
      bodyProcessors,
      predicates
    );
  }
}
