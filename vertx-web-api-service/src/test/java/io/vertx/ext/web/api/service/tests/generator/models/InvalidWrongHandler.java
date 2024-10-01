package io.vertx.ext.web.api.service.tests.generator.models;

import io.vertx.core.Future;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.WebApiServiceGen;

/**
 * @author <a href="https://github.com/slinkydeveloper">Francesco Guardiani</a>
 */
@WebApiServiceGen
public interface InvalidWrongHandler {

  Future<Integer> someMethod(ServiceRequest context);
}
