package io.vertx.ext.web.validation.impl.body;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.MalformedValueException;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.impl.parser.ObjectParser;
import io.vertx.ext.web.validation.impl.parser.ValueParser;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FormBodyProcessorImpl extends ObjectParser<List<String>> implements BodyProcessor {

  private final String contentType;
  private final SchemaRepository repo;
  private final JsonObject schema;

  public FormBodyProcessorImpl(Map<String, ValueParser<List<String>>> propertiesParsers, Map<Pattern,
    ValueParser<List<String>>> patternPropertiesParsers, ValueParser<List<String>> additionalPropertiesParsers,
                               String contentType, SchemaRepository repo, JsonObject schema) {
    super(propertiesParsers, patternPropertiesParsers, additionalPropertiesParsers);
    this.contentType = contentType;
    this.repo = repo;
    this.schema = schema;
  }

  @Override
  public boolean canProcess(String contentType) {
    return contentType.contains(this.contentType);
  }

  @Override
  public Future<RequestParameter> process(RoutingContext requestContext) {
    try {
      MultiMap multiMap = requestContext.request().formAttributes();
      JsonObject object = new JsonObject();
      for (String key : multiMap.names()) {
        List<String> serialized = multiMap.getAll(key);
        Map.Entry<String, Object> parsed = parseField(key, serialized);
        if (parsed != null) object.put(parsed.getKey(), parsed.getValue());
      }
      return Future.<RequestParameter>future(p -> {
        OutputUnit result = repo.validator(JsonSchema.of(schema)).validate(object);
        if (result.getValid()) {
          p.complete(RequestParameter.create(object));
        } else {
          p.fail(result.toException(""));
        }
      }).recover(err -> Future.failedFuture(
        BodyProcessorException.createValidationError(requestContext.parsedHeaders().contentType().value(), err)
      ));
    } catch (MalformedValueException e) {
      return Future.failedFuture(BodyProcessorException.createParsingError(requestContext.request().getHeader(HttpHeaders.CONTENT_TYPE), e));
    }
  }

  @Override
  protected ValueParser<List<String>> getAdditionalPropertiesParserIfRequired() {
    return (this.additionalPropertiesParser != null) ? this.additionalPropertiesParser : JsonArray::new;
  }

  @Override
  protected boolean mustNullateValue(List<String> serialized, ValueParser<List<String>> parser) {
    return serialized == null || serialized.isEmpty();
  }

}
