package io.vertx.ext.web.handler.graphql;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Converter and Codec for {@link io.vertx.ext.web.handler.graphql.GraphiQLOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.web.handler.graphql.GraphiQLOptions} original class using Vert.x codegen.
 */
public class GraphiQLOptionsConverter implements JsonCodec<GraphiQLOptions, JsonObject> {

  public static final GraphiQLOptionsConverter INSTANCE = new GraphiQLOptionsConverter();

  @Override public JsonObject encode(GraphiQLOptions value) { return (value != null) ? value.toJson() : null; }

  @Override public GraphiQLOptions decode(JsonObject value) { return (value != null) ? new GraphiQLOptions(value) : null; }

  @Override public Class<GraphiQLOptions> getTargetClass() { return GraphiQLOptions.class; }

  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, GraphiQLOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "enabled":
          if (member.getValue() instanceof Boolean) {
            obj.setEnabled((Boolean)member.getValue());
          }
          break;
        case "graphQLUri":
          if (member.getValue() instanceof String) {
            obj.setGraphQLUri((String)member.getValue());
          }
          break;
        case "headers":
          if (member.getValue() instanceof JsonObject) {
            java.util.Map<String, java.lang.String> map = new java.util.LinkedHashMap<>();
            ((Iterable<java.util.Map.Entry<String, Object>>)member.getValue()).forEach(entry -> {
              if (entry.getValue() instanceof String)
                map.put(entry.getKey(), (String)entry.getValue());
            });
            obj.setHeaders(map);
          }
          break;
        case "query":
          if (member.getValue() instanceof String) {
            obj.setQuery((String)member.getValue());
          }
          break;
        case "variables":
          if (member.getValue() instanceof JsonObject) {
            obj.setVariables(((JsonObject)member.getValue()).copy());
          }
          break;
      }
    }
  }

  public static void toJson(GraphiQLOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(GraphiQLOptions obj, java.util.Map<String, Object> json) {
    json.put("enabled", obj.isEnabled());
    if (obj.getGraphQLUri() != null) {
      json.put("graphQLUri", obj.getGraphQLUri());
    }
    if (obj.getHeaders() != null) {
      JsonObject map = new JsonObject();
      obj.getHeaders().forEach((key, value) -> map.put(key, value));
      json.put("headers", map);
    }
    if (obj.getQuery() != null) {
      json.put("query", obj.getQuery());
    }
    if (obj.getVariables() != null) {
      json.put("variables", obj.getVariables());
    }
  }
}
