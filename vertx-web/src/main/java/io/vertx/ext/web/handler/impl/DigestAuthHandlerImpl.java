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
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Shareable;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.auth.audit.Marker;
import io.vertx.ext.auth.audit.SecurityAudit;
import io.vertx.ext.auth.common.handler.impl.HTTPAuthorizationHandler;
import io.vertx.ext.auth.htdigest.HtdigestAuth;
import io.vertx.ext.auth.htdigest.HtdigestCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.DigestAuthHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.impl.RoutingContextInternal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vertx.ext.auth.impl.Codec.base16Encode;
import static io.vertx.ext.web.handler.HttpException.UNAUTHORIZED;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class DigestAuthHandlerImpl extends WebHTTPAuthorizationHandler<HtdigestAuth> implements DigestAuthHandler {

  private final static Logger LOG = LoggerFactory.getLogger(HTTPAuthorizationHandler.class);

  /**
   * Default name for map used to store nonces
   */
  private static final String DEFAULT_NONCE_MAP_NAME = "htdigest.nonces";

  /**
   * Shareable objects should be immutable.
   */
  private static class Nonce implements Shareable {
    private final long createdAt;
    private final int count;

    Nonce(int count) {
      this(System.currentTimeMillis(), count);
    }

    Nonce(long createdAt, int count) {
      this.createdAt = createdAt;
      this.count = count;
    }
  }

  private static final Pattern PARSER = Pattern.compile("(\\w+)=[\"]?([^\"]*)[\"]?$");
  private static final Pattern SPLITTER = Pattern.compile(",(?=(?:[^\"]|\"[^\"]*\")*$)");

  private static final MessageDigest MD5;

  static {
    try {
      MD5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private final VertxContextPRNG random;
  private final LocalMap<String, Nonce> nonces;

  private final long nonceExpireTimeout;

  private long lastExpireRun;

  public DigestAuthHandlerImpl(Vertx vertx, HtdigestAuth authProvider, long nonceExpireTimeout) {
    super(authProvider, Type.DIGEST, authProvider.realm());
    random = VertxContextPRNG.current(vertx);
    nonces = vertx.sharedData().getLocalMap(DEFAULT_NONCE_MAP_NAME);
    this.nonceExpireTimeout = nonceExpireTimeout;
  }

  @Override
  public Future<User> authenticate(RoutingContext context) {
    // clean up nonce
    long now = System.currentTimeMillis();
    if (now - lastExpireRun > nonceExpireTimeout / 2) {
      Set<String> toRemove = new HashSet<>();
      nonces.forEach((String key, Nonce n) -> {
        if (n != null && n.createdAt + nonceExpireTimeout < now) {
          toRemove.add(key);
        }
      });

      for (String n : toRemove) {
        nonces.remove(n);
      }
      lastExpireRun = now;
    }

    return parseAuthorization(context)
      .compose(header -> {
        final HtdigestCredentials credentials = new HtdigestCredentials();

        try {
          // Split the parameters by comma.
          String[] tokens = SPLITTER.split(header);
          // Parse parameters.
          int i = 0;
          int len = tokens.length;

          while (i < len) {
            // Strip quotes and whitespace.
            Matcher m = PARSER.matcher(tokens[i]);
            if (m.find()) {
              switch (m.group(1)) {
                case "algorithm":
                  credentials.setAlgorithm(m.group(2));
                  break;
                case "cnonce":
                  credentials.setCnonce(m.group(2));
                  break;
                case "method":
                  credentials.setMethod(m.group(2));
                  break;
                case "nc":
                  credentials.setNc(m.group(2));
                  break;
                case "nonce":
                  credentials.setNonce(m.group(2));
                  break;
                case "opaque":
                  credentials.setOpaque(m.group(2));
                  break;
                case "qop":
                  credentials.setQop(m.group(2));
                  break;
                case "realm":
                  credentials.setRealm(m.group(2));
                  break;
                case "response":
                  credentials.setResponse(m.group(2));
                  break;
                case "uri":
                  credentials.setUri(m.group(2));
                  break;
                case "username":
                  credentials.setUsername(m.group(2));
                  break;
                default:
                  LOG.info("Uknown parameter: " + m.group(1));
              }
            }

            ++i;
          }

          final String nonce = credentials.getNonce();

          // check for expiration
          if (!nonces.containsKey(nonce)) {
            return Future.failedFuture(UNAUTHORIZED);
          }

          // check for nonce counter (prevent replay attack)
          if (credentials.getQop() != null) {
            int nc = Integer.parseInt(credentials.getNc(), 16);
            final Nonce n = nonces.get(nonce);
            if (nc <= n.count) {
              return Future.failedFuture(UNAUTHORIZED);
            }
            // update the nounce count
            nonces.put(nonce, new Nonce(n.createdAt, nc));
          }

        } catch (RuntimeException e) {
          return Future.failedFuture(e);
        }

        // validate the opaque value
        final Session session = context.session();
        if (session != null) {
          String opaque = (String) session.data().get("opaque");
          if (opaque != null && !opaque.equals(credentials.getOpaque())) {
            return Future.failedFuture(UNAUTHORIZED);
          }
        }

        // we now need to pass some extra info
        credentials.setMethod(context.request().method().name());

        final SecurityAudit audit = ((RoutingContextInternal) context).securityAudit();
        audit.credentials(credentials);

        return authProvider
          .authenticate(credentials)
          .andThen(op -> audit.audit(Marker.AUTHENTICATION, op.succeeded()))
          .recover(err -> Future.failedFuture(new HttpException(401, err)));
      });
  }

  @Override
  public boolean setAuthenticateHeader(RoutingContext context) {
    final byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    // generate nonce
    String nonce = md5(bytes);
    // save it
    nonces.put(nonce, new Nonce(0));

    // generate opaque
    String opaque = null;
    final Session session = context.session();
    if (session != null) {
      opaque = (String) session.data().get("opaque");
    }

    if (opaque == null) {
      random.nextBytes(bytes);
      // generate random opaque
      opaque = md5(bytes);
    }

    context.response()
      .headers()
      .add("WWW-Authenticate", "Digest realm=\"" + realm + "\", qop=\"auth\", nonce=\"" + nonce + "\", opaque=\"" + opaque + "\"");

    return true;
  }

  private static synchronized String md5(byte[] payload) {
    MD5.reset();
    return base16Encode(MD5.digest(payload));
  }
}
