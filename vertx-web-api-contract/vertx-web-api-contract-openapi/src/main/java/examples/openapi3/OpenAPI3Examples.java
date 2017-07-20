package examples.openapi3;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RequestParameter;
import io.vertx.ext.web.RequestParameters;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.designdriven.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.validation.ValidationException;

public class OpenAPI3Examples {

  public void constructRouterFactory(Vertx vertx) {
    OpenAPI3RouterFactory.createRouterFactoryFromFile(vertx, "src/main/resources/petstore.yaml", true, ar -> {
      if (ar.succeeded()) {
        // Spec loaded with success
        OpenAPI3RouterFactory routerFactory = ar.result();
      } else {
        // Something went wrong during router factory initialization
        Throwable exception = ar.cause();
      }
    });
  }

  public void constructRouterFactoryFromUrl(Vertx vertx) {
    OpenAPI3RouterFactory.createRouterFactoryFromURL(vertx, this.getClass().getResource("/petstore.yaml").toString(), true, ar -> {
      if (ar.succeeded()) {
        // Spec loaded with success
        OpenAPI3RouterFactory routerFactory = ar.result();
      } else {
        // Something went wrong during router factory initialization
        Throwable exception = ar.cause();
      }
    });
  }

  public void addRoute(Vertx vertx, OpenAPI3RouterFactory routerFactory) {
    routerFactory.addHandlerByOperationId("awesomeOperation", routingContext -> {
      RequestParameters params = routingContext.get("parsedParameters");
      RequestParameter body = params.body();
      JsonObject jsonBody = body.getJsonObject();
      // Do something with body
    });
    routerFactory.addFailureHandlerByOperationId("awesomeOperation", routingContext -> {
      // Handle failure
    });
  }

  public void addSecurityHandler(OpenAPI3RouterFactory routerFactory, Handler securityHandler) {
    routerFactory.addSecurityHandler("security_scheme_name", securityHandler);
  }

  public void addJWT(OpenAPI3RouterFactory routerFactory, JWTAuth jwtAuthProvider) {
    routerFactory.addSecurityHandler("jwt_auth", JWTAuthHandler.create(jwtAuthProvider));
  }

  public void generateRouter(Vertx vertx, OpenAPI3RouterFactory routerFactory) {
    Router router = routerFactory.getRouter();

    HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost"));
    server.requestHandler(router::accept).listen();
  }

  public void mainExample(Vertx vertx) {
    // Load the api spec. This operation is asynchronous
    OpenAPI3RouterFactory.createRouterFactoryFromFile(vertx, "src/main/resources/petstore.yaml", true,
      openAPI3RouterFactoryAsyncResult -> {
      if (openAPI3RouterFactoryAsyncResult.succeeded()) {
        // Spec loaded with success
        OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();
        // Add an handler with operationId
        routerFactory.addHandlerByOperationId("listPets", routingContext -> {
          // Handle listPets operation
          routingContext.response().setStatusMessage("Called listPets").end();
        });
        // Add a failure handler to the same operationId
        routerFactory.addFailureHandlerByOperationId("listPets", routingContext -> {
          // This is the failure handler
          Throwable failure = routingContext.failure();
          if (failure instanceof ValidationException)
            // Handle Validation Exception
            routingContext.response().setStatusCode(400).setStatusMessage("ValidationException thrown! " + (
              (ValidationException) failure).type().name()).end();
        });

        // Add an handler with a combination of HttpMethod and path
        routerFactory.addHandler(HttpMethod.POST, "/pets", routingContext -> {
          // Extract request body and use it
          RequestParameters params = routingContext.get("parsedParameters");
          JsonObject pet = params.body().getJsonObject();
          routingContext.response().putHeader("content-type", "application/json; charset=utf-8").end(pet
            .encodePrettily());
        });

        // Add a security handler
        routerFactory.addSecurityHandler("api_key", routingContext -> {
          // Handle security here
          routingContext.next();
        });

        // Before router creation you can enable or disable mounting of a default failure handler for
        // ValidationException
        routerFactory.enableValidationFailureHandler(false);

        // Now you have to generate the router
        Router router = routerFactory.getRouter();

        // Now you can use your Router instance
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost"));
        server.requestHandler(router::accept).listen();

      } else {
        // Something went wrong during router factory initialization
        Throwable exception = openAPI3RouterFactoryAsyncResult.cause();
      }
    });
  }
}
