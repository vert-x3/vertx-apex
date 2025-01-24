package io.vertx.ext.web.validation.builder;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.web.validation.impl.ParameterLocation;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessor;
import io.vertx.json.schema.SchemaRepository;

/**
 * This interface is used to build parameter processors supported on every {@link ParameterLocation}.
 * You can use in query and cookie more complex parameters with {@link StyledParameterProcessorFactory}. <br/>
 * <p>
 * Look at {@link Parameters} for all available factories
 */
@VertxGen
@FunctionalInterface
public interface ParameterProcessorFactory {

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  ParameterProcessor create(ParameterLocation location, SchemaRepository schemaRepo);

}
