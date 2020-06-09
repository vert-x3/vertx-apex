package io.vertx.ext.web.api.service;

import io.vertx.core.json.JsonObject;

/**
 * Converter and mapper for {@link io.vertx.ext.web.api.service.ServiceResponse}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.web.api.service.ServiceResponse} original class using Vert.x codegen.
 */
public class ServiceResponseConverter {


   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ServiceResponse obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "payload":
          if (member.getValue() instanceof String) {
            obj.setPayload(io.vertx.core.buffer.Buffer.buffer(java.util.Base64.getDecoder().decode((String)member.getValue())));
          }
          break;
        case "statusCode":
          if (member.getValue() instanceof Number) {
            obj.setStatusCode(((Number)member.getValue()).intValue());
          }
          break;
        case "statusMessage":
          if (member.getValue() instanceof String) {
            obj.setStatusMessage((String)member.getValue());
          }
          break;
      }
    }
  }

   static void toJson(ServiceResponse obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(ServiceResponse obj, java.util.Map<String, Object> json) {
    if (obj.getPayload() != null) {
      json.put("payload", java.util.Base64.getEncoder().encodeToString(obj.getPayload().getBytes()));
    }
    if (obj.getStatusCode() != null) {
      json.put("statusCode", obj.getStatusCode());
    }
    if (obj.getStatusMessage() != null) {
      json.put("statusMessage", obj.getStatusMessage());
    }
  }
}
