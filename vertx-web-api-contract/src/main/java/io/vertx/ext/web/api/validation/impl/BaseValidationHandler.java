package io.vertx.ext.web.api.validation.impl;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.impl.RequestParameterImpl;
import io.vertx.ext.web.api.impl.RequestParametersImpl;
import io.vertx.ext.web.api.validation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
public abstract class BaseValidationHandler implements ValidationHandler {

  private Map<String, ParameterValidationRule> pathParamsRules;
  private Map<String, ParameterValidationRule> cookieParamsRules;
  private Map<String, ParameterValidationRule> queryParamsRules;
  private Map<String, ParameterValidationRule> formParamsRules;
  private Map<String, ParameterValidationRule> headerParamsRules;
  private ParameterTypeValidator entireBodyValidator;
  private Map<String, Pattern> multipartFileRules; // key is filename, value is content type
  private List<String> bodyFileRules; // list of content-types

  private List<CustomValidator> customValidators;

  protected boolean expectedBodyNotEmpty;

  protected BaseValidationHandler() {
    pathParamsRules = new HashMap<>();
    cookieParamsRules = new HashMap<>();
    formParamsRules = new HashMap<>();
    queryParamsRules = new HashMap<>();
    headerParamsRules = new HashMap<>();
    multipartFileRules = new HashMap<>();
    bodyFileRules = new ArrayList<>();
    customValidators = new ArrayList<>();

    expectedBodyNotEmpty = false;
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

      String contentType = routingContext.request().getHeader("Content-Type");
      if (contentType != null && contentType.length() != 0) {
        if (multipartFileRules.size() != 0 && !contentType.contains("multipart/form-data"))
          throw ValidationException.ValidationExceptionFactory.generateWrongContentTypeExpected(contentType,
            "multipart/form-data");
        if (contentType.contains("application/x-www-form-urlencoded") || contentType.contains("multipart/form-data")) {
          parsedParameters.setFormParameters(validateFormParams(routingContext));
          if (contentType.contains("multipart/form-data")) validateFileUpload(routingContext);
        } else if (contentType.contains("application/json") || contentType.contains("application/xml"))
          parsedParameters.setBody(validateEntireBody(routingContext));
        else {
          routingContext.fail(400);
          return;
        }
      } else {
        if (expectedBodyNotEmpty && !checkContentType(contentType))
          throw ValidationException.ValidationExceptionFactory.generateWrongContentTypeExpected(contentType, null);
      }

      routingContext.put("parsedParameters", parsedParameters);

      routingContext.next();
    } catch (ValidationException e) {
      routingContext.fail(e);
    }
  }

  private Map<String, RequestParameter> validatePathParams(RoutingContext routingContext) throws ValidationException {
    // Validation process validate only params that are registered in the validation -> extra params are allowed
    Map<String, RequestParameter> parsedParams = new HashMap<>();
    Map<String, String> pathParams = routingContext.pathParams();
    for (ParameterValidationRule rule : pathParamsRules.values()) {
      String name = rule.getName();
      if (pathParams.containsKey(name)) {
        RequestParameter parsedParam = rule.validateSingleParam(pathParams.get(name));
        if (parsedParams.containsKey(parsedParam.getName()))
          parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
        parsedParams.put(parsedParam.getName(), parsedParam);
      } else // Path params are required!
        throw ValidationException.ValidationExceptionFactory.generateNotFoundValidationException(name,
          ParameterLocation.PATH);
    }
    return parsedParams;
  }

  private Map<String, RequestParameter> validateCookieParams(RoutingContext routingContext) throws ValidationException {
    // Validation process validate only params that are registered in the validation -> extra params are allowed
    if (!routingContext.request().headers().contains("Cookie"))
      return null;
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
        RequestParameter parsedParam = rule.validateArrayParam(cookies.get(name));
        if (parsedParams.containsKey(parsedParam.getName()))
          parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
        parsedParams.put(parsedParam.getName(), parsedParam);
      } else {
        if (rule.allowEmptyValue() && rule.parameterTypeValidator().getDefault() != null) {
          RequestParameter parsedParam = new RequestParameterImpl(name, rule.parameterTypeValidator().getDefault());
          if (parsedParams.containsKey(parsedParam.getName()))
            parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
          parsedParams.put(parsedParam.getName(), parsedParam);
        } else if (!rule.isOptional())
          throw ValidationException.ValidationExceptionFactory.generateNotFoundValidationException(name,
            ParameterLocation.COOKIE);
      }
    }
    return parsedParams;
  }

  private Map<String, RequestParameter> validateQueryParams(RoutingContext routingContext) throws ValidationException {
    // Validation process validate only params that are registered in the validation -> extra params are allowed
    Map<String, RequestParameter> parsedParams = new HashMap<>();
    MultiMap queryParams = routingContext.queryParams();
    for (ParameterValidationRule rule : queryParamsRules.values()) {
      String name = rule.getName();
      if (queryParams.contains(name)) {
        RequestParameter parsedParam = rule.validateArrayParam(queryParams.getAll(name));
        if (parsedParams.containsKey(parsedParam.getName()))
          parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
        parsedParams.put(parsedParam.getName(), parsedParam);
      } else if (rule.allowEmptyValue() && rule.parameterTypeValidator().getDefault() != null) {
        RequestParameter parsedParam = new RequestParameterImpl(name, rule.parameterTypeValidator().getDefault());
        if (parsedParams.containsKey(parsedParam.getName()))
          parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
        parsedParams.put(parsedParam.getName(), parsedParam);
      } else if (!rule.isOptional())
        throw ValidationException.ValidationExceptionFactory.generateNotFoundValidationException(name,
          ParameterLocation.QUERY);
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
        RequestParameter parsedParam = rule.validateArrayParam(headersParams.getAll(name));
        if (parsedParams.containsKey(parsedParam.getName()))
          parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
        parsedParams.put(parsedParam.getName(), parsedParam);
      } else if (rule.allowEmptyValue() && rule.parameterTypeValidator().getDefault() != null) {
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
        // Decode values because I assume they are text/plain in this phase
        List<String> values = new ArrayList<>();
        for (String s : formParams.getAll(name)) {
          try {
            values.add(URLDecoder.decode(s, "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
          RequestParameter parsedParam = rule.validateArrayParam(values);
          if (parsedParams.containsKey(parsedParam.getName()))
            parsedParam = parsedParam.merge(parsedParams.get(parsedParam.getName()));
          parsedParams.put(parsedParam.getName(), parsedParam);
        }
      } else if (rule.allowEmptyValue() && rule.parameterTypeValidator().getDefault() != null) {
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

  private boolean existFileUpload(Set<FileUpload> files, String name, Pattern contentType) {
    for (FileUpload f : files) {
      if (f.name().equals(name) && contentType.matcher(f.contentType()).matches()) return true;
    }
    return false;
  }

  private void validateFileUpload(RoutingContext routingContext) throws ValidationException {
    Set<FileUpload> fileUploads = routingContext.fileUploads();
    for (Map.Entry<String, Pattern> expectedFile : multipartFileRules.entrySet()) {
      if (!existFileUpload(fileUploads, expectedFile.getKey(), expectedFile.getValue()))
        throw ValidationException.ValidationExceptionFactory.generateFileNotFoundValidationException(expectedFile
          .getKey(), expectedFile.getValue().toString());
    }
  }

  private RequestParameter validateEntireBody(RoutingContext routingContext) throws ValidationException {
    if (entireBodyValidator != null) return entireBodyValidator.isValid(routingContext.getBodyAsString());
    else return RequestParameter.create(null);
  }

  private boolean checkContentType(String contentType) {
    for (String ct : bodyFileRules) {
      if (ct.equals(contentType)) return true;
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
    if (!formParamsRules.containsKey(rule.getName())) {
      formParamsRules.put(rule.getName(), rule);
      expectedBodyNotEmpty = true;
    }
  }

  protected void addHeaderParamRule(ParameterValidationRule rule) {
    if (!headerParamsRules.containsKey(rule.getName())) headerParamsRules.put(rule.getName(), rule);
  }

  protected void addCustomValidator(CustomValidator customValidator) {
    customValidators.add(customValidator);
  }

  protected void addMultipartFileRule(String formName, String contentType) {
    if (!multipartFileRules.containsKey(formName)) multipartFileRules.put(formName, Pattern.compile(contentType));
    expectedBodyNotEmpty = true;
  }

  protected void addBodyFileRule(String contentType) {
    bodyFileRules.add(contentType);
    expectedBodyNotEmpty = true;
  }

  protected void setEntireBodyValidator(ParameterTypeValidator entireBodyValidator) {
    this.entireBodyValidator = entireBodyValidator;
    expectedBodyNotEmpty = true;
  }
}
