package io.vertx.ext.web.validation.impl.parameter;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.impl.ParameterLocation;
import io.vertx.json.schema.SchemaRepository;

import java.util.List;
import java.util.Map;

/**
 * Entry point for managing request parameters
 */
public interface ParameterProcessor {

  Future<RequestParameter> process(Map<String, List<String>> params);

  String getName();

  ParameterLocation getLocation();

  /**
   * Create a new request parameter processor
   *
   * @param parameterName Name of the parameter
   * @param location      Location of the parameter
   * @param isOptional    true if is optional
   * @param parser        parser for the parameter
   * @param repo          schema repository for the parameter
   * @param schema        schema for the parameter
   * @return
   */
  static ParameterProcessor create(String parameterName, ParameterLocation location, boolean isOptional,
                                   ParameterParser parser, SchemaRepository repo, JsonObject schema) {
    return new ParameterProcessorImpl(parameterName, location, isOptional, parser, repo, schema);
  }
}
