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
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.audit.Marker;
import io.vertx.ext.auth.audit.SecurityAudit;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.impl.RoutingContextInternal;

import java.nio.charset.StandardCharsets;

import static io.vertx.ext.auth.impl.Codec.base64Decode;

/**
 * @author <a href="http://pmlopes@gmail.com">Paulo Lopes</a>
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class BasicAuthHandlerImpl extends WebHTTPAuthorizationHandler<AuthenticationProvider> implements BasicAuthHandler {

  public BasicAuthHandlerImpl(AuthenticationProvider authProvider, String realm) {
    super(authProvider, Type.BASIC, realm);
  }

  @Override
  public Future<User> authenticate(RoutingContext context) {

    return parseAuthorization(context)
      .compose(header -> {

        final String suser;
        final String spass;

        try {
          // decode the payload
          String decoded = new String(base64Decode(header), StandardCharsets.UTF_8);

          int colonIdx = decoded.indexOf(":");
          if (colonIdx != -1) {
            suser = decoded.substring(0, colonIdx);
            spass = decoded.substring(colonIdx + 1);
          } else {
            suser = decoded;
            spass = null;
          }
        } catch (RuntimeException e) {
          return Future.failedFuture(new HttpException(400, e));
        }

        final SecurityAudit audit = ((RoutingContextInternal) context).securityAudit();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(suser, spass);
        audit.credentials(credentials);

        return authProvider.authenticate(new UsernamePasswordCredentials(suser, spass))
          .andThen(result -> audit.audit(Marker.AUTHENTICATION, result.succeeded()))
          .recover(err -> Future.failedFuture(new HttpException(401, err)));
      });
  }
}
