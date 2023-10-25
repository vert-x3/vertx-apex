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
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.audit.Marker;
import io.vertx.ext.auth.audit.SecurityAudit;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.FormLoginHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.impl.RoutingContextInternal;

import static io.vertx.ext.web.handler.HttpException.BAD_METHOD;
import static io.vertx.ext.web.handler.HttpException.BAD_REQUEST;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class FormLoginHandlerImpl extends WebAuthenticationHandlerImpl<AuthenticationProvider> implements FormLoginHandler {

  private String usernameParam;
  private String passwordParam;
  private String returnURLParam;
  private String directLoggedInOKURL;

  @Override
  public FormLoginHandler setUsernameParam(String usernameParam) {
    this.usernameParam = usernameParam;
    return this;
  }

  @Override
  public FormLoginHandler setPasswordParam(String passwordParam) {
    this.passwordParam = passwordParam;
    return this;
  }

  @Override
  public FormLoginHandler setReturnURLParam(String returnURLParam) {
    this.returnURLParam = returnURLParam;
    return this;
  }

  @Override
  public FormLoginHandler setDirectLoggedInOKURL(String directLoggedInOKURL) {
    this.directLoggedInOKURL = directLoggedInOKURL;
    return this;
  }

  public FormLoginHandlerImpl(AuthenticationProvider authProvider, String usernameParam, String passwordParam,
                              String returnURLParam, String directLoggedInOKURL) {
    super(authProvider);
    this.usernameParam = usernameParam;
    this.passwordParam = passwordParam;
    this.returnURLParam = returnURLParam;
    this.directLoggedInOKURL = directLoggedInOKURL;
  }

  @Override
  public Future<User> authenticate(RoutingContext context) {
    HttpServerRequest req = context.request();
    if (req.method() != HttpMethod.POST) {
      return Future.failedFuture(BAD_METHOD); // Must be a POST
    } else {
      if (!context.body().available()) {
        return Future.failedFuture("BodyHandler is required to process POST requests");
      } else {
        MultiMap params = req.formAttributes();
        String username = params.get(usernameParam);
        String password = params.get(passwordParam);
        if (username == null || password == null) {
          return Future.failedFuture(BAD_REQUEST);
        } else {
          final SecurityAudit audit = ((RoutingContextInternal) context).securityAudit();
          final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
          audit.credentials(credentials);

          return authProvider
            .authenticate(new UsernamePasswordCredentials(username, password))
            .andThen(op -> audit.audit(Marker.AUTHENTICATION, op.succeeded()))
            .recover(err -> Future.failedFuture(new HttpException(401, err)));
        }
      }
    }
  }

  @Override
  public void postAuthentication(RoutingContext ctx, User user) {
    HttpServerRequest req = ctx.request();
    Session session = ctx.session();
    if (session != null) {
      String returnURL = session.remove(returnURLParam);
      if (returnURL != null) {
        // Now redirect back to the original url
        ctx.redirect(returnURL);
        return;
      }
    }
    // Either no session or no return url
    if (directLoggedInOKURL != null) {
      // Redirect to the default logged in OK page - this would occur
      // if the user logged in directly at this URL without being redirected here first from another
      // url
      ctx.redirect(directLoggedInOKURL);
    } else {
      // Just show a basic page
      req.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
        .end(DEFAULT_DIRECT_LOGGED_IN_OK_PAGE);
    }
  }

  private static final String DEFAULT_DIRECT_LOGGED_IN_OK_PAGE = "<html><body><h1>Login successful</h1></body></html>";
}
