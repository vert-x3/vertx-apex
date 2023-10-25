/*
 * Copyright 2021 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.web.handler;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.auth.otp.OtpKeyGenerator;
import io.vertx.ext.auth.otp.hotp.HotpAuth;
import io.vertx.ext.auth.otp.totp.TotpAuth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.HotpAuthHandlerImpl;
import io.vertx.ext.web.handler.impl.TotpAuthHandlerImpl;

/**
 * An auth handler that provides One Time Password (Multi-Factor) Authentication support.
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
@VertxGen
public interface OtpAuthHandler extends WebAuthenticationHandler {

  /**
   * Create a new instance of this handler using a time based one time password authentication provider.
   * @param totpAuth the time based OTP provider.
   * @return new instance of the handler.
   */
  static OtpAuthHandler create(TotpAuth totpAuth) {
    return new TotpAuthHandlerImpl(totpAuth, OtpKeyGenerator.create());
  }

  /**
   * Create a new instance of this handler using a hash based one time password authentication provider.
   * @param hotpAuth the hash based OTP provider.
   * @return new instance of the handler.
   */
  static OtpAuthHandler create(HotpAuth hotpAuth) {
    return new HotpAuthHandlerImpl(hotpAuth, OtpKeyGenerator.create());
  }

  /**
   * Specify the URL where requests are to be redirected when a user is already known in the request.
   *
   * A user is already known when the {@link UserContext#get()} is not {@code null}.
   *
   * If no redirect is provided, requests are terminated immediately with status code {@code 401}.
   *
   * @param url the location where users are to be asked for the OTP code.
   * @return fluent self.
   */
  @Fluent
  OtpAuthHandler verifyUrl(String url);

  /**
   * Setup the optional route where authenticators are allowed to register. Registration is only allowed on requests with
   * a valid user.
   *
   * A user is valid when the {@link UserContext#get()} is not {@code null}.
   *
   * @param route the location where users are to register new authenticator devices/apps.
   * @return fluent self.
   */
  @Fluent
  OtpAuthHandler setupRegisterCallback(Route route);

  /**
   * Setup the required route where authenticators to submit the challenge response. Challenges
   * are only allowed on requests with a valid user.
   *
   * A user is valid when the {@link UserContext#get()} is not {@code null}.
   *
   * @param route the location where users are to submit challenge responses from authenticator devices/apps.
   * @return fluent self.
   */
  @Fluent
  OtpAuthHandler setupCallback(Route route);

  /**
   * Configure the {@code issuer} value to be shown in the authenticator URL.
   *
   * @param issuer a {@code String} for example {@code Vert.x OTP}
   * @return fluent self.
   */
  @Fluent
  OtpAuthHandler issuer(String issuer);

  /**
   * Configure the {@code label} value to be shown in the authenticator URL. When this value is provided it will
   * overwrite the default label which is composed of the {@code issuer} and the current {@code user} id.
   *
   * @param label a {@code String} for example {@code Vert.x OTP}
   * @return fluent self.
   */
  @Fluent
  OtpAuthHandler label(String label);
}
