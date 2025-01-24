package io.vertx.ext.web.validation.impl.body;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.MalformedValueException;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;

public class TextPlainBodyProcessorImpl implements BodyProcessor {

  private final SchemaRepository repo;
  private final JsonObject schema;

  public TextPlainBodyProcessorImpl(SchemaRepository repo, JsonObject schema) {
    this.repo = repo;
    this.schema = schema;
  }

  @Override
  public boolean canProcess(String contentType) {
    return contentType.contains("text/plain");
  }

  @Override
  public Future<RequestParameter> process(RoutingContext requestContext) {
    String body = requestContext.body().asString();
    if (body == null) {
      throw BodyProcessorException.createParsingError(
        requestContext.request().getHeader(HttpHeaders.CONTENT_TYPE),
        new MalformedValueException("Null body")
      );
    }
    return Future.<RequestParameter>future(p -> {
      OutputUnit result = repo.validator(JsonSchema.of(schema)).validate(body);
      if (result.getValid()) {
        p.complete(RequestParameter.create(body));
      } else {
        p.fail(result.toException(""));
      }
    });
  }
}
