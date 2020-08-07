package io.vertx.ext.web.openapi;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.*;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.json.schema.SchemaParser;
import io.vertx.ext.json.schema.SchemaRouter;
import io.vertx.ext.json.schema.ValidationException;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.openapi.impl.OpenAPI3RouterFactoryImpl;
import io.vertx.ext.web.openapi.impl.OpenAPIHolderImpl;

import java.util.List;
import java.util.function.Function;

/**
 * Interface for OpenAPI3RouterFactory. <br/>
 * To add an handler, use {@link RouterFactory#operation(String)} (String, Handler)}<br/>
 * Usage example:
 * <pre>
 * {@code
 * RouterFactory.create(vertx, "src/resources/spec.yaml", asyncResult -> {
 *  if (!asyncResult.succeeded()) {
 *     // IO failure or spec invalid
 *  } else {
 *     OpenAPI3RouterFactory routerFactory = asyncResult.result();
 *     routerFactory.operation("operation_id").handler(routingContext -> {
 *        // Do something
 *     }, routingContext -> {
 *        // Do something with failure handler
 *     });
 *     Router router = routerFactory.createRouter();
 *  }
 * });
 * }
 * </pre>
 * <br/>
 * Handlers are loaded in this order:<br/>
 *  <ol>
 *   <li>Body handler (Customizable with {@link this#bodyHandler(BodyHandler)}</li>
 *   <li>Custom global handlers configurable with {@link this#rootHandler(Handler)}</li>
 *   <li>Global security handlers defined in upper spec level</li>
 *   <li>Operation specific security handlers</li>
 *   <li>Generated validation handler</li>
 *   <li>User handlers or "Not implemented" handler</li>
 * </ol>
 *
 * @author Francesco Guardiani @slinkydeveloper
 */
@VertxGen
public interface RouterFactory {

  /**
   * Access to an operation defined in the contract with {@code operationId}
   *
   * @param operationId
   * @return
   */
  @Nullable Operation operation(String operationId);

  /**
   * Access to all operations defined in the contract
   *
   * @return
   */
  List<Operation> operations();

  /**
   * Supply your own BodyHandler if you would like to control body limit, uploads directory and deletion of uploaded files
   * @param bodyHandler
   * @return self
   */
  @Fluent
  RouterFactory bodyHandler(BodyHandler bodyHandler);

  /**
   * Add global handler to be applied prior to {@link Router} being generated. <br/>
   * Please note that you should not add a body handler inside that list. If you want to modify the body handler, please use {@link RouterFactory#bodyHandler(BodyHandler)}
   *
   * @param rootHandler
   * @return this factory
   */
  @Fluent
  RouterFactory rootHandler(Handler<RoutingContext> rootHandler);

  /**
   * Mount to paths that have to follow a security schema a security handler
   *
   * @param securitySchemaName
   * @param handler
   * @return this factory
   */
  @Fluent
  RouterFactory securityHandler(String securitySchemaName, AuthenticationHandler handler);

  /**
   * Introspect the OpenAPI spec to mount handlers for all operations that specifies a x-vertx-event-bus annotation. Please give a look at <a href="https://vertx.io/docs/vertx-web-api-service/java/">vertx-web-api-service documentation</a> for more informations
   *
   * @return this factory
   */
  @Fluent
  RouterFactory mountServicesFromExtensions();

  /**
   * Introspect the Web Api Service interface to route to service all matching method names with operation ids. Please give a look at <a href="https://vertx.io/docs/vertx-web-api-service/java/">vertx-web-api-service documentation</a> for more informations
   *
   * @return this factory
   */
  @Fluent
  @GenIgnore
  RouterFactory mountServiceInterface(Class interfaceClass, String address);

  /**
   * Set options of router factory. For more info {@link RouterFactoryOptions}
   *
   * @param options
   * @return this factory
   */
  @Fluent
  RouterFactory setOptions(RouterFactoryOptions options);

  /**
   * @return options of router factory. For more info {@link RouterFactoryOptions}
   */
  RouterFactoryOptions getOptions();

  /**
   * @return holder used by this factory to process the OpenAPI. You can use it to resolve {@code $ref}s
   */
  OpenAPIHolder getOpenAPI();

  /**
   * @return schema router used by this factory to internally manage all {@link io.vertx.ext.json.schema.Schema} instances
   */
  SchemaRouter getSchemaRouter();

  /**
   * @return schema parser used by this factory to parse all {@link io.vertx.ext.json.schema.Schema}
   */
  SchemaParser getSchemaParser();

  /**
   * When set, this function is called while creating the payload of {@link io.vertx.ext.web.api.service.ServiceRequest}
   * @param serviceExtraPayloadMapper
   * @return this factory
   */
  @Fluent
  RouterFactory serviceExtraPayloadMapper(Function<RoutingContext, JsonObject> serviceExtraPayloadMapper);

  /**
   * Construct a new router based on spec. It will fail if you are trying to mount a spec with security schemes
   * without assigned handlers<br/>
   *
   * <b>Note:</b> Router is built when this function is called and the path definition ordering in contract is respected.
   *
   * @return
   */
  Router createRouter();


  /**
   * Create a new {@link RouterFactory}
   *
   * @param vertx
   * @param url location of your spec. It can be an absolute path, a local path or remote url (with HTTP/HTTPS protocol)
   * @return Future completed with success when specification is loaded and valid
   */
  static Future<RouterFactory> create(Vertx vertx, String url) {
    return create(vertx, url, new OpenAPILoaderOptions());
  }

  /**
   * Like {@link this#create(Vertx, String)}
   */
  static void create(Vertx vertx, String url, Handler<AsyncResult<RouterFactory>> handler) {
    RouterFactory.create(vertx, url).onComplete(handler);
  }

  /**
   * Create a new {@link RouterFactory}
   *
   * @param vertx
   * @param url location of your spec. It can be an absolute path, a local path or remote url (with HTTP/HTTPS protocol)
   * @param options options for specification loading
   * @return Future completed with success when specification is loaded and valid
   */
  static Future<RouterFactory> create(Vertx vertx,
                     String url,
                     OpenAPILoaderOptions options) {
    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    Promise<RouterFactory> promise = ctx.promise();

    OpenAPIHolderImpl loader = new OpenAPIHolderImpl(vertx.createHttpClient(), vertx.fileSystem(), options);
    loader.loadOpenAPI(url).onComplete(ar -> {
      if (ar.failed()) {
        if (ar.cause() instanceof ValidationException) {
          promise.fail(RouterFactoryException.createInvalidSpecException(ar.cause()));
        } else {
          promise.fail(RouterFactoryException.createInvalidFileSpec(url, ar.cause()));
        }
      } else {
        RouterFactory factory;
        try {
          factory = new OpenAPI3RouterFactoryImpl(vertx, loader, options);
        } catch (Exception e) {
          promise.fail(RouterFactoryException.createRouterFactoryInstantiationError(e, url));
          return;
        }
        promise.complete(factory);
      }
    });

    return promise.future();
  }

  /**
   * Like {@link this#create(Vertx, String, OpenAPILoaderOptions)}
   */
  static void create(Vertx vertx,
                     String url,
                     OpenAPILoaderOptions options,
                     Handler<AsyncResult<RouterFactory>> handler) {
    RouterFactory.create(vertx, url, options).onComplete(handler);
  }

}
