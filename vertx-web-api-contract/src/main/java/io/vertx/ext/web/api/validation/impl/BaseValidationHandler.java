package io.vertx.ext.web.api.validation.impl;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.impl.RequestParameterImpl;
import io.vertx.ext.web.api.impl.RequestParametersImpl;
import io.vertx.ext.web.api.validation.*;
import io.vertx.ext.web.impl.Utils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
public abstract class BaseValidationHandler implements ValidationHandler {

  private Map<String, ParameterValidationRule> pathParamsRules;
  private Map<String, ParameterValidationRule> cookieParamsRules;
  private ParameterTypeValidator cookieAdditionalPropertiesValidator;
  private String cookieAdditionalPropertiesObjectPropertyName;
  private Map<String, ParameterValidationRule> queryParamsRules;
  private ParameterTypeValidator queryAdditionalPropertiesValidator;
  private String queryAdditionalPropertiesObjectPropertyName;
  private Map<String, ParameterValidationRule> formParamsRules;
  private Map<String, ParameterValidationRule> headerParamsRules;
  private ParameterTypeValidator entireBodyValidator;
  private Map<String, Pattern> multipartFileRules; // key is filename, value is content type
  private List<String> bodyFileRules; // list of content-types

  private List<CustomValidator> customValidators;

  protected boolean bodyRequired;

  protected BaseValidationHandler() {
    pathParamsRules = new HashMap<>();
    cookieParamsRules = new HashMap<>();
    formParamsRules = new HashMap<>();
    queryParamsRules = new HashMap<>();
    headerParamsRules = new HashMap<>();
    multipartFileRules = new HashMap<>();
    bodyFileRules = new ArrayList<>();
    customValidators = new ArrayList<>();

    bodyRequired = false;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    try {
      RequestParametersImpl parsedParameters = new RequestParametersImpl();

      parsedParameters.setPathParameters(validatePathParams(routingContext));
      parsedParameters.setQueryParameters(validateQueryParams(routingContext));
      parsedParameters.setHeaderParameters(validateHeaderParams(routingContext));
      parsedParameters.setCookieParameters(validateCookieParams(routingContext));

      //Run custom validators
      for (CustomValidator customValidator : customValidators) {
        customValidator.validate(routingContext);
      }

      String contentType = routingContext.request().getHeader(HttpHeaders.CONTENT_TYPE);
      if (contentType != null && contentType.length() != 0) {
        boolean isMultipart = contentType.contains("multipart/form-data");

        if (multipartFileRules.size() != 0 && !isMultipart) {
          throw ValidationException.ValidationExceptionFactory.generateWrongContentTypeExpected(contentType,
            "multipart/form-data");
        }
        if (contentType.contains("application/x-www-form-urlencoded")) {
          parsedParameters.setFormParameters(validateFormParams(routingContext));
        } else if (isMultipart) {
          parsedParameters.setFormParameters(validateFormParams(routingContext));
          validateFileUpload(routingContext);
        } else if (Utils.isJsonContentType(contentType) || Utils.isXMLContentType(contentType)) {
          parsedParameters.setBody(validateEntireBody(routingContext));
        } else if (bodyRequired && !checkContentType(contentType)) {
          throw ValidationException.ValidationExceptionFactory.generateWrongContentTypeExpected(contentType, null);
        } // If content type is valid or body is not required, do nothing!
      } else if (bodyRequired) {
        throw ValidationException.ValidationExceptionFactory.generateWrongContentTypeExpected(contentType, null);
      }

      if (routingContext.data().containsKey("parsedParameters")) {
        ((RequestParametersImpl)routingContext.get("parsedParameters")).merge(parsedParameters);
      } else {
        routingContext.put("parsedParameters", parsedParameters);
      }
      routingContext.next();

    } catch (ValidationException e) {
      routingContext.fail(400, e);
    }
  }

  private Map<String, RequestParameter> validatePathParams(RoutingContext routingContext) throws ValidationException {
    // Validation process validate only params that are registered in the validation -> extra params are allowed
    Map<String, RequestParameter> parsedParams = new HashMap<>();

    for (ParameterValidationRule rule : pathParamsRules.values()) {
      String name = rule.getName();
      if (name != null) {
        String pathParam = routingContext.pathParam(name);
        if (pathParam != null || !rule.isOptional() ) {
            RequestParameter parsedParam = rule.validateSingleParam(pathParam);
            if (parsedParams.containsKey(parsedParam.getName()))
              parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
            parsedParams.put(parsedParam.getName(), parsedParam);
        }
      } else // Path params are required!
        throw ValidationException.ValidationExceptionFactory.generateNotFoundValidationException(name,
          ParameterLocation.PATH);
    }
    return parsedParams;
  }

  private Map<String, RequestParameter> validateCookieParams(RoutingContext routingContext) throws ValidationException {
    // Validation process validate only params that are registered in the validation -> extra params are allowed
    if (!routingContext.request().headers().contains("Cookie"))
      return new HashMap<>();
    QueryStringDecoder decoder = new QueryStringDecoder("/?" + routingContext.request().getHeader("Cookie")); // Some hack to reuse this object
    Map<String, List<String>> cookies = new HashMap<>();
    for (Map.Entry<String, List<String>> e : decoder.parameters().entrySet()) {
      String key = e.getKey().trim();
      if (cookies.containsKey(key))
        cookies.get(key).addAll(e.getValue());
      else
        cookies.put(key, e.getValue());
    }
    Map<String, RequestParameter> parsedParams = new HashMap<>();
    for (ParameterValidationRule rule : cookieParamsRules.values()) {
      String name = rule.getName().trim();
      if (cookies.containsKey(name)) {
        List<String> p = cookies.remove(name);
        if (p.size() != 0) {
          RequestParameter parsedParam = rule.validateArrayParam(p);
          if (parsedParams.containsKey(parsedParam.getName()))
            parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
          parsedParams.put(parsedParam.getName(), parsedParam);
        } else {
          throw ValidationException.ValidationExceptionFactory.generateNotMatchValidationException(name + " can't be empty");
        }
      } else {
        if (rule.parameterTypeValidator().getDefault() != null) {
          RequestParameter parsedParam = new RequestParameterImpl(name, rule.parameterTypeValidator().getDefault());
          if (parsedParams.containsKey(parsedParam.getName()))
            parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
          parsedParams.put(parsedParam.getName(), parsedParam);
        } else if (!rule.isOptional())
          throw ValidationException.ValidationExceptionFactory.generateNotFoundValidationException(name,
            ParameterLocation.COOKIE);
      }
    }
    if (cookieAdditionalPropertiesValidator != null) {
      for (Map.Entry<String, List<String>> e : cookies.entrySet()) {
        try {
          Map<String, RequestParameter> r = new HashMap<>();
          r.put(e.getKey(), cookieAdditionalPropertiesValidator.isValidCollection(e.getValue()));
          RequestParameter parsedParam = new RequestParameterImpl(cookieAdditionalPropertiesObjectPropertyName, r);
          if (parsedParams.containsKey(cookieAdditionalPropertiesObjectPropertyName))
            parsedParam = parsedParam.merge(parsedParams.get(cookieAdditionalPropertiesObjectPropertyName));
          parsedParams.put(parsedParam.getName(), parsedParam);
        } catch (ValidationException ex) {
          ex.setParameterName(cookieAdditionalPropertiesObjectPropertyName);
          e.setValue(e.getValue());
          throw ex;
        }
      }
    }
    return parsedParams;
  }

  private Map<String, RequestParameter> validateQueryParams(RoutingContext routingContext) throws ValidationException {
    // Validation process validate only params that are registered in the validation -> extra params are allowed
    Map<String, RequestParameter> parsedParams = new HashMap<>();
    MultiMap queryParams = MultiMap.caseInsensitiveMultiMap().addAll(routingContext.queryParams());
    for (ParameterValidationRule rule : queryParamsRules.values()) {
      String name = rule.getName();
      if (queryParams.contains(name)) {
        List<String> p = queryParams.getAll(name);
        queryParams.remove(name);
        if (p.size() != 0) {
          RequestParameter parsedParam = rule.validateArrayParam(p);
          if (parsedParams.containsKey(parsedParam.getName()))
            parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
          parsedParams.put(parsedParam.getName(), parsedParam);
        } else {
          throw ValidationException.ValidationExceptionFactory.generateNotMatchValidationException(name + " can't be empty");
        }
      } else if (rule.parameterTypeValidator().getDefault() != null) {
        RequestParameter parsedParam = new RequestParameterImpl(name, rule.parameterTypeValidator().getDefault());
        if (parsedParams.containsKey(parsedParam.getName()))
          parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
        parsedParams.put(parsedParam.getName(), parsedParam);
      } else if (!rule.isOptional())
        throw ValidationException.ValidationExceptionFactory.generateNotFoundValidationException(name,
          ParameterLocation.QUERY);
    }
    if (queryAdditionalPropertiesValidator != null) {
      for (Map.Entry<String, String> e : queryParams.entries()) {
        try {
          Map<String, RequestParameter> r = new HashMap<>();
          r.put(e.getKey(), queryAdditionalPropertiesValidator.isValid(e.getValue()));
          RequestParameter parsedParam = new RequestParameterImpl(queryAdditionalPropertiesObjectPropertyName, r);
          if (parsedParams.containsKey(queryAdditionalPropertiesObjectPropertyName))
            parsedParam = parsedParam.merge(parsedParams.get(queryAdditionalPropertiesObjectPropertyName));
          parsedParams.put(parsedParam.getName(), parsedParam);
        } catch (ValidationException ex) {
          ex.setParameterName(queryAdditionalPropertiesObjectPropertyName);
          e.setValue(e.getValue());
          throw ex;
        }
      }
    }
    return parsedParams;
  }

  private Map<String, RequestParameter> validateHeaderParams(RoutingContext routingContext) throws ValidationException {
    // Validation process validate only params that are registered in the validation -> extra params are allowed
    Map<String, RequestParameter> parsedParams = new HashMap<>();
    MultiMap headersParams = routingContext.request().headers();
    for (ParameterValidationRule rule : headerParamsRules.values()) {
      String name = rule.getName();
      if (headersParams.contains(name)) {
        List<String> p = headersParams.getAll(name);
        if (p.size() != 0) {
          RequestParameter parsedParam = rule.validateArrayParam(p);
          if (parsedParams.containsKey(parsedParam.getName()))
            parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
          parsedParams.put(parsedParam.getName(), parsedParam);
        } else {
          throw ValidationException.ValidationExceptionFactory.generateNotMatchValidationException(name + " can't be empty");
        }
      } else if (rule.parameterTypeValidator().getDefault() != null) {
        RequestParameter parsedParam = new RequestParameterImpl(name, rule.parameterTypeValidator().getDefault());
        if (parsedParams.containsKey(parsedParam.getName()))
          parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
        parsedParams.put(parsedParam.getName(), parsedParam);
      } else if (!rule.isOptional())
        throw ValidationException.ValidationExceptionFactory.generateNotFoundValidationException(name,
          ParameterLocation.HEADER);
    }
    return parsedParams;
  }

  private Map<String, RequestParameter> validateFormParams(RoutingContext routingContext) throws ValidationException {
    // Validation process validate only params that are registered in the validation -> extra params are allowed
    Map<String, RequestParameter> parsedParams = new HashMap<>();
    MultiMap formParams = routingContext.request().formAttributes();
    for (ParameterValidationRule rule : formParamsRules.values()) {
      String name = rule.getName();
      if (formParams.contains(name)) {
        List<String> p = formParams.getAll(name);
        if (p.size() != 0) {
          RequestParameter parsedParam = rule.validateArrayParam(p);
          if (parsedParams.containsKey(parsedParam.getName()))
            parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
          parsedParams.put(parsedParam.getName(), parsedParam);
        } else {
          throw ValidationException.ValidationExceptionFactory.generateNotMatchValidationException(name + " can't be empty");
        }
      } else if (rule.parameterTypeValidator().getDefault() != null) {
        RequestParameter parsedParam = new RequestParameterImpl(name, rule.parameterTypeValidator().getDefault());
        if (parsedParams.containsKey(parsedParam.getName()))
          parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
        parsedParams.put(parsedParam.getName(), parsedParam);
      } else if (!rule.isOptional())
        throw ValidationException.ValidationExceptionFactory.generateNotFoundValidationException(name,
          ParameterLocation.BODY_FORM);
    }
    return parsedParams;
  }

  private boolean existFileUpload(List<FileUpload> files, String name, Pattern contentType) {
    for (FileUpload f : files) {
      if (f.name().equals(name) && contentType.matcher(f.contentType()).matches()) return true;
    }
    return false;
  }

  private void validateFileUpload(RoutingContext routingContext) throws ValidationException {
    List<FileUpload> fileUploads = routingContext.fileUploads();
    for (Map.Entry<String, Pattern> expectedFile : multipartFileRules.entrySet()) {
      if (!existFileUpload(fileUploads, expectedFile.getKey(), expectedFile.getValue()))
        throw ValidationException.ValidationExceptionFactory.generateFileNotFoundValidationException(expectedFile
          .getKey(), expectedFile.getValue().toString());
    }
  }

  private RequestParameter validateEntireBody(RoutingContext routingContext) throws ValidationException {
    if (entireBodyValidator != null) return entireBodyValidator.isValid(routingContext.body().asString());
    else return RequestParameter.create(null);
  }

  private boolean checkContentType(String contentType) {
    for (String ct : bodyFileRules) {
      if (contentType.contains(ct)) return true;
    }
    return false;
  }

  protected void addRule(ParameterValidationRule rule, ParameterLocation location) {
    switch (location) {
      case PATH:
        addPathParamRule(rule);
        break;
      case HEADER:
        addHeaderParamRule(rule);
        break;
      case COOKIE:
        addCookieParamRule(rule);
        break;
      case QUERY:
        addQueryParamRule(rule);
        break;
      case BODY_FORM:
        addFormParamRule(rule);
        break;
    }
  }

  protected void addPathParamRule(ParameterValidationRule rule) {
    if (!pathParamsRules.containsKey(rule.getName())) pathParamsRules.put(rule.getName(), rule);
  }

  protected void addCookieParamRule(ParameterValidationRule rule) {
    if (!cookieParamsRules.containsKey(rule.getName())) cookieParamsRules.put(rule.getName(), rule);
  }

  protected void addQueryParamRule(ParameterValidationRule rule) {
    if (!queryParamsRules.containsKey(rule.getName())) queryParamsRules.put(rule.getName(), rule);
  }

  protected void addFormParamRule(ParameterValidationRule rule) {
    if (!formParamsRules.containsKey(rule.getName()))
      formParamsRules.put(rule.getName(), rule);
    bodyRequired = true;
  }

  protected void addHeaderParamRule(ParameterValidationRule rule) {
    if (!headerParamsRules.containsKey(rule.getName())) headerParamsRules.put(rule.getName(), rule);
  }

  protected void addCustomValidator(CustomValidator customValidator) {
    customValidators.add(customValidator);
  }

  protected void addMultipartFileRule(String formName, String contentType) {
    if (!multipartFileRules.containsKey(formName)) multipartFileRules.put(formName, Pattern.compile(contentType));
    bodyRequired = true;
  }

  protected void addBodyFileRule(String contentType) {
    bodyFileRules.add(contentType);
    bodyRequired = true;
  }

  protected void setEntireBodyValidator(ParameterTypeValidator entireBodyValidator) {
    this.entireBodyValidator = entireBodyValidator;
    bodyRequired = true;
  }

  protected void setCookieAdditionalPropertyHandler(ParameterTypeValidator validator, String objectParameterName) {
    this.cookieAdditionalPropertiesValidator = validator;
    this.cookieAdditionalPropertiesObjectPropertyName = objectParameterName;
  }
  protected void setQueryAdditionalPropertyHandler(ParameterTypeValidator validator, String objectParameterName) {
    this.queryAdditionalPropertiesValidator = validator;
    this.queryAdditionalPropertiesObjectPropertyName = objectParameterName;
  }

  public boolean isBodyRequired() {
    return bodyRequired;
  }

}
