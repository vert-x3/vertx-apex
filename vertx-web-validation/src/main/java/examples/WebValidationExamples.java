package examples;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.docgen.Source;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.RequestPredicate;
import io.vertx.ext.web.validation.RequestPredicateException;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.Bodies;
import io.vertx.ext.web.validation.builder.Parameters;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;

import static io.vertx.ext.web.validation.builder.Parameters.param;
import static io.vertx.json.schema.common.dsl.Schemas.arraySchema;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.json.schema.draft7.dsl.Keywords.maximum;

@Source
public class WebValidationExamples {

  public void withoutWebValidation(Router router) {
    router
      .get("/user")
      .handler(routingContext -> {
        // Retrieve aParam
        String aParamUnparsed = routingContext.queryParam("aParam").get(0);
        if (aParamUnparsed == null) {
          routingContext.fail(400);
          return;
        }
        // Parse aParam
        int aParam;
        try {
          aParam = Integer.parseInt(aParamUnparsed);
        } catch (NumberFormatException e) {
          routingContext.fail(400, e);
          return;
        }
        // Check if aParam is maximum 100
        if (aParam > 100) {
          routingContext.fail(400);
          return;
        }

        // aParam is ready, now we can focus on
        // Business logic to process the request
      });
  }

  public void withWebValidation(Router router, SchemaRepository schemaRepository) {
    router
      .get("/user")
      .handler(
        ValidationHandlerBuilder
          .create(schemaRepository)
          .queryParameter(param(
            "aParam",
            intSchema().with(maximum(100))
          ))
          .build()
      )
      .handler(routingContext -> {
        RequestParameters parameters =
          routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
        int aParam = parameters.queryParameter("aParam").getInteger();
        // Business logic to process the request
      });

  }

  public void parameters(SchemaRepository schemaRepository) {
    ValidationHandlerBuilder
      .create(schemaRepository)
      .pathParameter(Parameters.param("myPathParam", stringSchema()))
      .queryParameter(Parameters.optionalParam("myQueryParam", intSchema()));
  }

  public void bodies(SchemaRepository schemaRepository) {
    ObjectSchemaBuilder bodySchemaBuilder = objectSchema()
      .property("username", stringSchema())
      .property("password", stringSchema());
    ValidationHandlerBuilder
      .create(schemaRepository)
      .body(Bodies.json(bodySchemaBuilder))
      .body(Bodies.formUrlEncoded(bodySchemaBuilder));
  }

  public void parametersComplex(SchemaRepository schemaRepository) {
    ValidationHandlerBuilder
      .create(schemaRepository)
      .queryParameter(Parameters.explodedParam(
        "myArray",
        arraySchema().items(stringSchema())
      ))  // Accepts myArray=item1&myArray=item2
      .queryParameter(Parameters.deepObjectParam(
        "myDeepObject",
        objectSchema()
          .property("name", stringSchema())
      )); // Accepts myDeepObject[name]=francesco
  }

  public void requestBodyRequired(SchemaRepository schemaRepository) {
    ValidationHandlerBuilder
      .create(schemaRepository)
      .predicate(RequestPredicate.BODY_REQUIRED);
  }


  public void buildAndMount(Router router, SchemaRepository schemaRepository) {
    router
      .get("/user")
      .handler(
        ValidationHandlerBuilder
          .create(schemaRepository)
          .build()
      );
  }

  public void useParameters(Router router, SchemaRepository schemaRepository,
                            ObjectSchemaBuilder objectBodySchemaBuilder) {
    router
      .get("/user")
      .handler(
        ValidationHandlerBuilder
          .create(schemaRepository)
          .queryParameter(Parameters.explodedParam(
            "myArray",
            arraySchema().items(stringSchema())
          ))
          .body(Bodies.json(objectBodySchemaBuilder))
          .body(Bodies.formUrlEncoded(objectBodySchemaBuilder))
          .build()
      ).handler(routingContext -> {
        RequestParameters parameters =
          routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
        JsonArray myArray = parameters.queryParameter("myArray").getJsonArray();
        JsonObject body = parameters.body().getJsonObject();
      });
  }

  public void manageFailure(Router router) {
    router.errorHandler(400, routingContext -> {
      if (routingContext.failure() instanceof BadRequestException) {
        if (routingContext.failure() instanceof ParameterProcessorException) {
          // Something went wrong while parsing/validating a parameter
        } else if (routingContext.failure() instanceof BodyProcessorException) {
          // Something went wrong while parsing/validating the body
        } else if (routingContext.failure() instanceof RequestPredicateException) {
          // A request predicate is unsatisfied
        }
      }
    });
  }

}
