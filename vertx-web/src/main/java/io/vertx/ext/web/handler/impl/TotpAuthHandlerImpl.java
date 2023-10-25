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
package io.vertx.ext.web.handler.impl;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.audit.Marker;
import io.vertx.ext.auth.audit.SecurityAudit;
import io.vertx.ext.auth.otp.OtpCredentials;
import io.vertx.ext.auth.otp.OtpKey;
import io.vertx.ext.auth.otp.OtpKeyGenerator;
import io.vertx.ext.auth.otp.totp.TotpAuth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.OtpAuthHandler;
import io.vertx.ext.web.impl.OrderListener;
import io.vertx.ext.web.impl.RoutingContextInternal;

import static io.vertx.ext.web.handler.HttpException.UNAUTHORIZED;

/**
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
public class TotpAuthHandlerImpl extends WebAuthenticationHandlerImpl<TotpAuth> implements OtpAuthHandler, OrderListener {

  private final OtpKeyGenerator otpKeyGen;

  private String verifyUrl;
  private String issuer;
  private String label;

  private int order = -1;
  // the extra routes
  private Route register = null;
  private Route verify = null;

  public TotpAuthHandlerImpl(TotpAuth totp, OtpKeyGenerator otpKeyGen) {
    super(totp, "totp");
    this.otpKeyGen = otpKeyGen;
  }

  @Override
  public Future<User> authenticate(RoutingContext ctx) {
    if (verify == null) {
      return Future.failedFuture(new HttpException(500, new IllegalStateException("No callback mounted!")));
    }

    final User user = ctx.user().get();

    if (user == null) {
      return Future.failedFuture(UNAUTHORIZED);
    } else {
      Boolean userOtp = user.get("mfa");
      // user hasn't 2fa yet?
      if (userOtp == null || !userOtp) {
        if (verifyUrl == null) {
          return Future.failedFuture(new HttpException(401, "User TOTP verification missing"));
        } else {
          final Session session = ctx.session();
          if (session != null) {
            String uri = ctx.request().uri();
            session
              .put("redirect_uri", uri);
          }

          return Future.failedFuture(new HttpException(302, verifyUrl));
        }
      } else {
        return Future.succeededFuture(user);
      }
    }
  }

  @Override
  public OtpAuthHandler verifyUrl(String verifyUrl) {
    this.verifyUrl = verifyUrl;
    return this;
  }

  @Override
  public OtpAuthHandler issuer(String issuer) {
    this.issuer = issuer;
    return this;
  }

  @Override
  public OtpAuthHandler label(String label) {
    this.label = label;
    return this;
  }

  @Override
  public OtpAuthHandler setupRegisterCallback(Route route) {
    this.register = route;
    if (order != -1) {
      mountRegister();
    }

    return this;
  }

  @Override
  public OtpAuthHandler setupCallback(Route route) {
    this.verify = route;
    if (order != -1) {
      mountVerify();
    }

    return this;
  }

  @Override
  public void onOrder(int order) {
    this.order = order;

    if (register != null) {
      mountRegister();
    }
    if (verify != null) {
      mountVerify();
    }
  }

  private void mountRegister() {
    register
    // force a post if otherwise
      .method(HttpMethod.POST)
      .order(order - 1)
      .handler(ctx -> {
        final User user = ctx.user().get();
        if (user == null || user.get("username") == null) {
          ctx.fail(new IllegalStateException("User object misses 'username' attribute"));
          return;
        }
        final OtpKey key = otpKeyGen.generate();
        authProvider.createAuthenticator(user.get("username"), key)
          .onFailure(ctx::fail)
          .onSuccess(authenticator ->
            ctx.json(
              new JsonObject()
                .put("issuer", issuer)
                .put("label", label)
                .put("url", authProvider.generateUri(key, issuer, user.get("username"), label))));
      });

  }

  private void mountVerify() {
    verify
    // force a post if otherwise
      .method(HttpMethod.POST)
      .order(order - 1)
      .handler(ctx -> {
        final User user = ctx.user().get();

        if (user == null || user.get("username") == null) {
          ctx.fail(new IllegalStateException("User object misses 'username' attribute"));
          return;
        }

        if ( ctx.request().getParam("code") == null) {
          ctx.fail(new HttpException(400, "Missing 'code' form attribute"));
          return;
        }

        final OtpCredentials credentials = new OtpCredentials(user.get("username"), ctx.request().getParam("code"));
        final SecurityAudit audit = ((RoutingContextInternal) ctx).securityAudit();
        audit.credentials(credentials);

        authProvider.authenticate(credentials)
          .onSuccess(newUser -> {
            audit.audit(Marker.AUTHENTICATION, true);
            user.principal().mergeIn(user.principal());
            user.attributes().mergeIn(user.attributes());
            // marker
            user.attributes().put("mfa", "totp");
            String redirect = "/";
            final Session session = ctx.session();
            if (session != null) {
              // the user has upgraded from unauthenticated to authenticated
              // session should be upgraded as recommended by owasp
              session.regenerateId();

              String back = session.get("redirect_uri");
              if (back != null) {
                redirect = back;
              }
            }
            ctx.redirect(redirect);
          })
          .onFailure(err -> {
            audit.audit(Marker.AUTHENTICATION, true);
            ctx.fail(401, err);
          });
      });
  }
}
