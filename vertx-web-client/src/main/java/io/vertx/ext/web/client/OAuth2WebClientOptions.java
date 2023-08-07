/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.ext.web.client;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * @author Paulo Lopes
 */
@DataObject(generateConverter = true)
public class OAuth2WebClientOptions {

  /**
   * The default value of whether to perform a single token refresh if the response status code is 401 (Forbidden).
   */
  public static final boolean DEFAULT_RENEW_TOKEN_ON_FORBIDDEN = false;

  /**
   * The default leeway when validating token expiration times.
   */
  public static final int DEFAULT_LEEWAY = 0;

  /**
   * The default fail fast of requests they happen while refreshing/loading token.
   */
  public static final boolean DEFAULT_FAIL_FAST = true;

  private boolean renewTokenOnForbidden = DEFAULT_RENEW_TOKEN_ON_FORBIDDEN;
  private int leeway = DEFAULT_LEEWAY;
  private boolean failFast = DEFAULT_FAIL_FAST;

  public OAuth2WebClientOptions() {
  }

  /**
   * Copy constructor.
   *
   * @param other the options to copy
   */
  public OAuth2WebClientOptions(OAuth2WebClientOptions other) {
    this.renewTokenOnForbidden = other.renewTokenOnForbidden;
    this.leeway = other.leeway;
  }

  /**
   * Creates a new instance from JSON.
   *
   * @param json the JSON object
   */
  public OAuth2WebClientOptions(JsonObject json) {
    OAuth2WebClientOptionsConverter.fromJson(json, this);
  }

  /**
   * Convert to JSON
   *
   * @return the JSON
   */
  public JsonObject toJson() {
    final JsonObject json = new JsonObject();
    OAuth2WebClientOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * Weather to refresh or not the current user token if a forbidden http status response is received.
   *
   * @return default value is {@link #DEFAULT_RENEW_TOKEN_ON_FORBIDDEN}
   */
  public boolean isRenewTokenOnForbidden() {
    return renewTokenOnForbidden;
  }

  /**
   * Set a default behavior on how to handle the first forbidden response. {@code true} to attempt a token refresh and
   * replay the request. {@code false} to continue the request to the user handler/promise.
   *
   * @param renewTokenOnForbidden the desired intention.
   * @return fluent self
   */
  public OAuth2WebClientOptions setRenewTokenOnForbidden(boolean renewTokenOnForbidden) {
    this.renewTokenOnForbidden = renewTokenOnForbidden;
    return this;
  }

  /**
   * Weather to allow leeway while validating if a token is considered expired.
   *
   * @return default value is {@link #DEFAULT_LEEWAY}
   */
  public int getLeeway() {
    return leeway;
  }

  /**
   * Set a default leeway in seconds to be considered while validating tokens for expiration.
   *
   * @param leeway the desired leeway in seconds
   * @return fluent self
   */
  public OAuth2WebClientOptions setLeeway(int leeway) {
    this.leeway = leeway;
    return this;
  }

  /**
   * Should all requests that happen while this object is refreshing/loading token fail as soon as possible, or queue
   * the incoming requests until the in flight operation completes.
   *
   * @return default value is {@link #DEFAULT_FAIL_FAST}
   */
  public boolean getFailFast() {
    return failFast;
  }

  /**
   * Set if all requests that happen while this object is refreshing/loading token should fail as soon as possible.
   *
   * @param failFast requests while refreshing/loading token
   * @return fluent self
   */
  public OAuth2WebClientOptions setFailFast(boolean failFast) {
    this.failFast = failFast;
    return this;
  }
}
