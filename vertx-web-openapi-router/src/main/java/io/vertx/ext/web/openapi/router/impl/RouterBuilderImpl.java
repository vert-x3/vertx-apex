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

package io.vertx.ext.web.openapi.router.impl;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.WebAuthenticationHandler;
import io.vertx.ext.web.handler.InputTrustHandler;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RequestExtractor;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.ext.web.openapi.router.Security;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Path;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.impl.RequestValidatorImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RouterBuilderImpl implements RouterBuilderInternal {

  private final static Logger LOG = LoggerFactory.getLogger(RouterBuilderImpl.class);

  private static final String PATH_PARAM_PLACEHOLDER_REGEX = "\\{(.*?)}";

  // VisibleForTesting
  final List<Handler<RoutingContext>> rootHandlers = new ArrayList<>();
  final AuthenticationHandlers securityHandlers = new AuthenticationHandlers();
  private final Vertx vertx;
  private final OpenAPIContract contract;

  private final Map<String, OpenAPIRoute> openAPIRoutes;

  private final RequestExtractor extractor;

  public RouterBuilderImpl(Vertx vertx, OpenAPIContract contract, RequestExtractor extractor) {
    this.vertx = vertx;
    this.contract = contract;
    this.extractor = extractor;
    this.openAPIRoutes =
      contract.operations().stream().collect(Collectors.toMap(Operation::getOperationId, OpenAPIRouteImpl::new));
  }

  /**
   * @param openAPIPath the path with placeholders in OpenAPI format
   * @return the path with placeholders in vertx-web format
   */
  // VisibleForTesting
  static String toVertxWebPath(String openAPIPath) {
    return openAPIPath.replaceAll(PATH_PARAM_PLACEHOLDER_REGEX, ":$1");
  }

  @Override
  public @Nullable OpenAPIRoute getRoute(String operationId) {
    return openAPIRoutes.get(operationId);
  }

  @Override
  public List<OpenAPIRoute> getRoutes() {
    return new ArrayList<>(openAPIRoutes.values());
  }

  @Override
  public RouterBuilder rootHandler(Handler<RoutingContext> rootHandler) {
    rootHandlers.add(rootHandler);
    return this;
  }

  @Override
  public RouterBuilder security(String securitySchemeName, WebAuthenticationHandler authenticationHandler,
                                String callback) {
    securityHandlers.addRequirement(securitySchemeName, authenticationHandler, callback);
    return this;
  }

  @Override
  public Security security(String securitySchemeName) {
    return new SecurityImpl(this, contract.securityScheme(securitySchemeName), securitySchemeName);
  }

  @Override
  public Router createRouter() {
    Router router = Router.router(vertx);
    RequestValidator validator = new RequestValidatorImpl(vertx, contract);

    Route globalRoute = router.route();
    rootHandlers.forEach(globalRoute::handler);

    // add the callback handler
    securityHandlers.applyCallbackHandlers(router);

    for (Path path : contract.getPaths()) {
      for (Operation operation : path.getOperations()) {
        Route route = router.route(operation.getHttpMethod(), toVertxWebPath(path.getName()));
        route.putMetadata(KEY_META_DATA_OPERATION, operation.getOperationId());

        OpenAPIRoute openAPIRoute = getRoute(operation.getOperationId());
        Objects.requireNonNull(openAPIRoute, "No route found for operation " + operation.getOperationId());

        if (openAPIRoute.getHandlers().size() > 0 || openAPIRoute.getFailureHandlers().size() > 0) {
          securityHandlers.solve(operation, route, openAPIRoute.doSecurity());

          if (openAPIRoute.doValidation()) {
            InputTrustHandler validationHandler = rc -> extractor.extractValidatableRequest(rc, operation)
              .compose(validatableRequest -> validator.validate(validatableRequest, operation.getOperationId()))
              .onSuccess(rp -> {
                rc.put(KEY_META_DATA_VALIDATED_REQUEST, rp);
                rc.next();
              }).onFailure(rc::fail);
            route.handler(validationHandler);
          }

          openAPIRoute.getHandlers().forEach(route::handler);
          openAPIRoute.getFailureHandlers().forEach(route::failureHandler);
        } else {
          LOG.warn("No handlers found for operation " + operation.getOperationId() + " - skipping route creation");
          // terminate the request with 503 (Not Implemented)
          route
            .handler(ctx -> ctx.response().setStatusCode(503).end());
        }
      }
    }
    return router;
  }
}
