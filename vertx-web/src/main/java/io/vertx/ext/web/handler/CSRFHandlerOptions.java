/*
 * Copyright 2024 Red Hat, Inc.
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

package io.vertx.ext.web.handler;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import static io.vertx.ext.web.handler.SessionHandlerOptions.DEFAULT_SESSION_TIMEOUT;

/**
 * {@link CSRFHandler} options.
 */
@DataObject
@JsonGen(publicConverter = false)
public class CSRFHandlerOptions {

  public static final String DEFAULT_COOKIE_NAME = "XSRF-TOKEN";

  public static final String DEFAULT_COOKIE_PATH = "/";

  public static final String DEFAULT_HEADER_NAME = "X-XSRF-TOKEN";

  private String origin;
  private boolean nagHttps;
  private String cookieName;
  private String cookiePath;
  private String headerName;
  private long timeout;
  private boolean cookieHttpOnly;
  private boolean cookieSecure;

  /**
   * Default constructor.
   */
  public CSRFHandlerOptions() {
    cookieName = DEFAULT_COOKIE_NAME;
    cookiePath = DEFAULT_COOKIE_PATH;
    headerName = DEFAULT_HEADER_NAME;
    timeout = DEFAULT_SESSION_TIMEOUT;
  }

  /**
   * Copy constructor.
   *
   * @param other the options to copy
   */
  public CSRFHandlerOptions(CSRFHandlerOptions other) {
    this();
    this.origin = other.origin;
    this.nagHttps = other.nagHttps;
    this.cookieName = other.cookieName;
    this.cookiePath = other.cookiePath;
    this.headerName = other.headerName;
    this.timeout = other.timeout;
    this.cookieHttpOnly = other.cookieHttpOnly;
    this.cookieSecure = other.cookieSecure;
  }

  /**
   * Constructor to create options from JSON.
   *
   * @param json the JSON
   */
  public CSRFHandlerOptions(JsonObject json) {
    this();
    CSRFHandlerOptionsConverter.fromJson(json, this);
  }

  public String getOrigin() {
    return origin;
  }

  /**
   * Set the origin for this server. When this value is set, extra validation will occur. The request
   * must match the origin server, port and protocol.
   *
   * @param origin the origin for this server e.g.: {@code https://www.foo.com}.
   * @return a reference to this, so the API can be used fluently
   */
  public CSRFHandlerOptions setOrigin(String origin) {
    this.origin = origin;
    return this;
  }

  public boolean isNagHttps() {
    return nagHttps;
  }

  /**
   * Whether the handler should give warning messages if it is used in other than https protocols.
   *
   * @param nagHttps {@code true} to nag
   * @return a reference to this, so the API can be used fluently
   */
  public CSRFHandlerOptions setNagHttps(boolean nagHttps) {
    this.nagHttps = nagHttps;
    return this;
  }

  public String getCookieName() {
    return cookieName;
  }

  /**
   * Set the cookie name. By default, {@code XSRF-TOKEN} is used as it is the expected name by AngularJS however other frameworks might use other names.
   *
   * @param cookieName a new name for the cookie.
   * @return a reference to this, so the API can be used fluently
   */
  public CSRFHandlerOptions setCookieName(String cookieName) {
    this.cookieName = cookieName;
    return this;
  }

  public String getCookiePath() {
    return cookiePath;
  }

  /**
   * Set the cookie path. By default, {@code /} is used.
   *
   * @param cookiePath a new path for the cookie.
   * @return a reference to this, so the API can be used fluently
   */
  public CSRFHandlerOptions setCookiePath(String cookiePath) {
    this.cookiePath = cookiePath;
    return this;
  }

  public String getHeaderName() {
    return headerName;
  }

  /**
   * Set the header name.
   * By default, {@code X-XSRF-TOKEN} is used as it is the expected name by AngularJS however other frameworks might use other names.
   *
   * @param headerName a new name for the header.
   * @return a reference to this, so the API can be used fluently
   */
  public CSRFHandlerOptions setHeaderName(String headerName) {
    this.headerName = headerName;
    return this;
  }

  public long getTimeout() {
    return timeout;
  }

  /**
   * Set the timeout for tokens generated by the handler, by default it uses the default from the session handler.
   *
   * @param timeout token timeout
   * @return a reference to this, so the API can be used fluently
   */
  public CSRFHandlerOptions setTimeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  public boolean isCookieHttpOnly() {
    return cookieHttpOnly;
  }

  /**
   * Set the cookie {@code httpOnly} attribute. When setting to {@code false} the CSRF handler will behave in Double Submit Cookie mode.
   * When set to {@code true} then it will operate in Cookie-to-header mode.
   * <p>
   * For more information <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#double-submit-cookie">https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#double-submit-cookie</a>
   *
   * @param cookieHttpOnly a new name for the header.
   * @return a reference to this, so the API can be used fluently
   */
  public CSRFHandlerOptions setCookieHttpOnly(boolean cookieHttpOnly) {
    this.cookieHttpOnly = cookieHttpOnly;
    return this;
  }

  public boolean isCookieSecure() {
    return cookieSecure;
  }

  /**
   * Sets the cookie {@code secure} flag.
   * When set this flag instructs browsers to only send the cookie over HTTPS.
   *
   * @param cookieSecure {@code true} to set the secure flag on the cookie
   * @return a reference to this, so the API can be used fluently
   */
  public CSRFHandlerOptions setCookieSecure(boolean cookieSecure) {
    this.cookieSecure = cookieSecure;
    return this;
  }
}
