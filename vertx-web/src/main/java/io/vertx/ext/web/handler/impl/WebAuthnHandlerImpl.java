/*
 * Copyright 2014 Red Hat, Inc.
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
import io.vertx.ext.auth.webauthn.WebAuthn;
import io.vertx.ext.auth.webauthn.WebAuthnCredentials;
import io.vertx.ext.auth.webauthn.impl.attestation.AttestationException;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.WebAuthnHandler;
import io.vertx.ext.web.impl.OrderListener;
import io.vertx.ext.web.impl.Origin;
import io.vertx.ext.auth.common.UserContextInternal;
import io.vertx.ext.web.impl.RoutingContextInternal;

public class WebAuthnHandlerImpl extends WebAuthenticationHandlerImpl<WebAuthn> implements WebAuthnHandler, OrderListener {

  private static final boolean CONFORMANCE = Boolean.getBoolean("io.vertx.ext.web.fido2.conformance.tests");

  private int order = -1;
  // the extra routes
  private Route register = null;
  private Route login = null;
  private Route response = null;
  // optional config
  private String origin;
  private String domain;

  public WebAuthnHandlerImpl(WebAuthn webAuthN) {
    super(webAuthN);
  }

  private static boolean containsRequiredString(JsonObject json, String key) {
    try {
      if (json == null) {
        return false;
      }
      if (!json.containsKey(key)) {
        return false;
      }
      Object s = json.getValue(key);
      return (s instanceof String) && !"".equals(s);
    } catch (ClassCastException e) {
      return false;
    }
  }

  private static boolean containsOptionalString(JsonObject json, String key) {
    try {
      if (json == null) {
        return true;
      }
      if (!json.containsKey(key)) {
        return true;
      }
      Object s = json.getValue(key);
      return (s instanceof String);
    } catch (ClassCastException e) {
      return false;
    }
  }

  private static boolean containsRequiredObject(JsonObject json, String key) {
    try {
      if (json == null) {
        return false;
      }
      if (!json.containsKey(key)) {
        return false;
      }
      JsonObject s = json.getJsonObject(key);
      return s != null;
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public Future<User> authenticate(RoutingContext ctx) {
    if (response == null) {
      return Future.failedFuture(new HttpException(500, new IllegalStateException("No callback mounted!")));
    }

    final User user = ctx.user().get();

    if (user == null) {
      return Future.failedFuture(new HttpException(401));
    } else {
      return Future.succeededFuture(user);
    }
  }


  @Override
  public WebAuthnHandler setupCredentialsCreateCallback(Route route) {
    this.register = route;
    if (order != -1) {
      mountRegister();
    }
    return this;
  }

  @Override
  public WebAuthnHandler setupCredentialsGetCallback(Route route) {
    this.login = route;
    if (order != -1) {
      mountLogin();
    }
    return this;
  }

  @Override
  public WebAuthnHandler setupCallback(Route route) {
    this.response = route;
    if (order != -1) {
      mountResponse();
    }
    return this;
  }

  @Override
  public WebAuthnHandler setOrigin(String origin) {
    if (origin != null) {
      Origin o = Origin.parse(origin);
      this.origin = o.encode();
      domain = o.host();
    } else {
      this.origin = null;
      domain = null;
    }
    return this;
  }

  private static void ok(RoutingContext ctx) {
    if (CONFORMANCE) {
      ctx.json(new JsonObject().put("status", "ok").put("errorMessage", ""));
    } else {
      ctx.response()
        .setStatusCode(204)
        .end();
    }
  }

  private static void ok(RoutingContext ctx, JsonObject result) {
    if (CONFORMANCE) {
      ctx.json(result.put("status", "ok").put("errorMessage", ""));
    } else {
      ctx.json(result);
    }
  }

  @Override
  public void onOrder(int order) {
    this.order = order;
    if (register != null) {
      mountRegister();
    }
    if (login != null) {
      mountLogin();
    }
    if (response != null) {
      mountResponse();
    }
  }

  private void mountRegister() {
    register
      // force a post if otherwise
      .method(HttpMethod.POST)
      .order(order - 1)
      .handler(ctx -> {
        try {
          // might throw runtime exception if there's no json or is bad formed
          final JsonObject webauthnRegister = ctx.body().asJsonObject();
          final Session session = ctx.session();

          // the register object should match a Webauthn user.
          // A user has only a required field: name
          // And optional fields: displayName and icon
          if (webauthnRegister == null || !containsRequiredString(webauthnRegister, "name")) {
            ctx.fail(400, new IllegalArgumentException("missing 'name' field from request json"));
          } else {
            // input basic validation is OK

            if (session == null) {
              ctx.fail(500, new IllegalStateException("No session or session handler is missing."));
              return;
            }

            authProvider.createCredentialsOptions(webauthnRegister)
              .onFailure(ctx::fail)
              .onSuccess(credentialsOptions -> {
                // save challenge to the session
                session
                  .put("challenge", credentialsOptions.getString("challenge"))
                  .put("username", webauthnRegister.getString("name"));

                ok(ctx, credentialsOptions);
              });
          }
        } catch (IllegalArgumentException e) {
          ctx.fail(400, e);
        } catch (RuntimeException e) {
          ctx.fail(e);
        }
      });
  }

  private void mountLogin() {
    login
      // force a post if otherwise
      .method(HttpMethod.POST)
      .order(order - 1)
      .handler(ctx -> {
        try {
          // might throw runtime exception if there's no json or is bad formed
          final JsonObject webauthnLogin = ctx.body().asJsonObject();
          final Session session = ctx.session();

          final String username = webauthnLogin == null ? null : webauthnLogin.getString("name");

          // input basic validation is OK

          if (session == null) {
            ctx.fail(500, new IllegalStateException("No session or session handler is missing."));
            return;
          }

          // STEP 18 Generate assertion
          authProvider.getCredentialsOptions(username)
            .onFailure(ctx::fail)
            .onSuccess(getAssertion -> {
              session
                .put("challenge", getAssertion.getString("challenge"))
                .put("username", username);

              ok(ctx, getAssertion);
            });
        } catch (IllegalArgumentException e) {
          ctx.fail(400, e);
        } catch (RuntimeException e) {
          ctx.fail(e);
        }
      });
  }

  private void mountResponse() {
    response
      // force a post if otherwise
      .method(HttpMethod.POST)
      .order(order - 1)
      .handler(ctx -> {
        try {
          // might throw runtime exception if there's no json or is bad formed
          final JsonObject webauthnResp = ctx.body().asJsonObject();
          // input validation
          if (
            webauthnResp == null ||
              !containsRequiredString(webauthnResp, "id") ||
              !containsRequiredString(webauthnResp, "rawId") ||
              !containsRequiredObject(webauthnResp, "response") ||
              !containsOptionalString(webauthnResp.getJsonObject("response"), "userHandle") ||
              !containsRequiredString(webauthnResp, "type") ||
              !"public-key".equals(webauthnResp.getString("type"))) {

            ctx.fail(400, new IllegalArgumentException("Response missing one or more of id/rawId/response[.userHandle]/type fields, or type is not public-key"));
            return;
          }

          // input basic validation is OK

          final Session session = ctx.session();

          if (session == null) {
            ctx.fail(500, new IllegalStateException("No session or session handler is missing."));
            return;
          }

          final WebAuthnCredentials credentials = new WebAuthnCredentials()
            .setOrigin(origin)
            .setDomain(domain)
            .setChallenge(session.remove("challenge"))
            .setUsername(session.get("username"))
            .setWebauthn(webauthnResp);

          final SecurityAudit audit = ((RoutingContextInternal) ctx).securityAudit();
          audit.credentials(credentials);

          authProvider.authenticate(credentials)
            .onSuccess(user -> {
              audit.audit(Marker.AUTHENTICATION, true);
              // save the user into the context
              ((UserContextInternal) ctx.user())
                .setUser(user);
              // the user has upgraded from unauthenticated to authenticated
              // session should be upgraded as recommended by owasp
              session.regenerateId();
              ok(ctx);
            })
            .onFailure(cause -> {
              audit.audit(Marker.AUTHENTICATION, false);
              if (cause instanceof AttestationException) {
                ctx.fail(400, cause);
              } else {
                ctx.fail(cause);
              }
            });
        } catch (IllegalArgumentException e) {
          ctx.fail(400, e);
        } catch (RuntimeException e) {
          ctx.fail(e);
        }
      });
  }
}
