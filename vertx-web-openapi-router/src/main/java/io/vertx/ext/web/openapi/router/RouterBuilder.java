/*
 * Copyright (c) 2023, SAP SE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.ext.web.openapi.router;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.ext.web.openapi.router.impl.RouterBuilderImpl;
import io.vertx.openapi.validation.RequestUtils;

import java.util.List;

/**
 * Interface to build a Vert.x Web {@link Router} from an OpenAPI 3 contract.
 * <p></p>
 * The router is mounting its handlers in the following order:
 * <ol>
 *   <li>RootHandler</li> in the order the root handlers were added to the RouterBuilder.
 *   <li>ValidationHandler</li> This handler is implementing the marker interface
 *   {@link io.vertx.ext.web.handler.InputTrustHandler}. Because of this, all handlers of type <i>PLATFORM</i>,
 *   <i>SECURITY_POLICY</i>, <i>BODY</i> and <i>AUTHENTICATION</i> must be mounted as root handlers if required.
 *   <li>UserHandler</li> The custom user handlers defined in the {@link OpenAPIRoute} in the same order as they are
 *   added to the route.
 *   <li>FailureHandler</li> The failure handlers defined in the {@link OpenAPIRoute} in the same order as they are
 *   added to the route.
 * </ol>
 */
@VertxGen
public interface RouterBuilder {
  String KEY_META_DATA_OPERATION = "openApiOperationId";

  String KEY_META_DATA_VALIDATED_REQUEST = "openApiValidatedRequest";

  /**
   * Create a new {@link RouterBuilder}. Like {@link #create(Vertx, OpenAPIContract, RequestExtractor)} but uses a default
   * implementation for the <i>extractor</i>.
   *
   * @param vertx    the related Vert.x instance
   * @param contract the contract that describes the endpoint
   * @return an instance of {@link RouterBuilder}
   */
  static RouterBuilder create(Vertx vertx, OpenAPIContract contract) {
    return new RouterBuilderImpl(vertx, contract,
      (routingContext, operation) -> RequestUtils.extract(routingContext.request(), operation));
  }

  /**
   * Create a new {@link RouterBuilder}.
   *
   * @param vertx     the related Vert.x instance
   * @param contract  the contract that describes the endpoint
   * @param extractor the extractor is used to extract and transform the parameters and body of the related request in
   *                  a format that can be validated by the {@link io.vertx.openapi.validation.RequestValidator}.
   * @return an instance of {@link RouterBuilder}
   */
  static RouterBuilder create(Vertx vertx, OpenAPIContract contract, RequestExtractor extractor) {
    return new RouterBuilderImpl(vertx, contract, extractor);
  }

  /**
   * Access to a route defined in the contract with {@code operationId}
   *
   * @param operationId the id of the operation
   * @return the requested route, or null if the passed operationId doesn't exist.
   */
  @Nullable
  OpenAPIRoute getRoute(String operationId);

  /**
   * @return all routes defined in the contract
   */
  List<OpenAPIRoute> getRoutes();

  /**
   * Add global handler to be applied prior to {@link Router} being generated. <br/>
   *
   * @param rootHandler the root handler to add
   * @return self
   */
  @Fluent
  RouterBuilder rootHandler(Handler<RoutingContext> rootHandler);

  /**
   * Creates a new security scheme for the required {@link AuthenticationHandler}.
   * @return a security scheme.
   */
  Security security(String securitySchemeName);

  /**
   * Construct a new router based on the related OpenAPI contract.
   *
   * @return a Router based on the related OpenAPI contract.
   */
  Router createRouter();
}
