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

import io.vertx.core.*;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.impl.RoutingContextInternal;
import io.vertx.ext.web.impl.Signature;
import io.vertx.ext.web.impl.UserContextInternal;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.impl.SessionInternal;

import java.util.Objects;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class SessionHandlerImpl implements SessionHandler {

  public static final String SESSION_USER_HOLDER_KEY = "__vertx.userHolder";
  public static final String SESSION_FLUSHED_KEY = "__vertx.session-flushed";
  public static final String SESSION_STOREUSER_KEY = "__vertx.session-storeuser";

  private static final Logger LOG = LoggerFactory.getLogger(SessionHandlerImpl.class);

  private final SessionStore sessionStore;

  private String sessionCookieName = DEFAULT_SESSION_COOKIE_NAME;
  private String sessionCookieDomain = DEFAULT_SESSION_COOKIE_DOMAIN;
  private String sessionCookiePath = DEFAULT_SESSION_COOKIE_PATH;
  private long sessionTimeout = DEFAULT_SESSION_TIMEOUT;
  private boolean nagHttps = DEFAULT_NAG_HTTPS;
  private boolean sessionCookieSecure = DEFAULT_COOKIE_SECURE_FLAG;
  private boolean sessionCookieHttpOnly = DEFAULT_COOKIE_HTTP_ONLY_FLAG;
  private int minLength = DEFAULT_SESSIONID_MIN_LENGTH;
  private boolean lazySession = DEFAULT_LAZY_SESSION;
  private long cookieMaxAge = -1;

  private boolean cookieless;
  private CookieSameSite cookieSameSite;
  private Signature signature;

  public SessionHandlerImpl(SessionStore sessionStore) {
    this.sessionStore = sessionStore;
  }

  @Override
  public SessionHandler setSessionTimeout(long timeout) {
    this.sessionTimeout = timeout;
    return this;
  }

  @Override
  public SessionHandler setNagHttps(boolean nag) {
    this.nagHttps = nag;
    return this;
  }

  @Override
  public SessionHandler setCookieSecureFlag(boolean secure) {
    this.sessionCookieSecure = secure;
    return this;
  }

  @Override
  public SessionHandler setCookieHttpOnlyFlag(boolean httpOnly) {
    this.sessionCookieHttpOnly = httpOnly;
    return this;
  }

  @Override
  public SessionHandler setSessionCookieDomain(String sessionCookieDomain) {
    this.sessionCookieDomain = sessionCookieDomain;
    return this;
  }

  @Override
  public SessionHandler setSessionCookieName(String sessionCookieName) {
    this.sessionCookieName = sessionCookieName;
    return this;
  }

  @Override
  public SessionHandler setSessionCookiePath(String sessionCookiePath) {
    this.sessionCookiePath = sessionCookiePath;
    return this;
  }

  @Override
  public SessionHandler setMinLength(int minLength) {
    this.minLength = minLength;
    return this;
  }

  @Override
  public SessionHandler setCookieSameSite(CookieSameSite policy) {
    this.cookieSameSite = policy;
    return this;
  }

  @Override
  public SessionHandler setLazySession(boolean lazySession) {
    this.lazySession = lazySession;
    return this;
  }

  @Override
  public SessionHandler setCookieMaxAge(long cookieMaxAge) {
    this.cookieMaxAge = cookieMaxAge;
    return this;
  }

  @Override
  public SessionHandler setCookieless(boolean cookieless) {
    this.cookieless = cookieless;
    return this;
  }

  @Override
  public SessionHandler setSigningSecret(String secret) {
    this.signature = new Signature(secret);
    return this;
  }

  @Override
  public Future<Void> flush(RoutingContext context, boolean ignoreStatus) {
    return flush(context, false, ignoreStatus);
  }

  /**
   * Ensure that the cookie properties are always set the same way
   * on generation and on update.
   *
   * @param cookie the cookie to set
   */
  private void setCookieProperties(Cookie cookie, boolean expired) {
    if (sessionCookieDomain != null) {
      cookie.setDomain(sessionCookieDomain);
    }
    cookie.setPath(sessionCookiePath);
    cookie.setSecure(sessionCookieSecure);
    cookie.setHttpOnly(sessionCookieHttpOnly);
    cookie.setSameSite(cookieSameSite);
    if (!expired) {
      // set max age if user requested it - else it's a session cookie
      if (cookieMaxAge >= 0) {
        cookie.setMaxAge(cookieMaxAge);
      }
    }
  }

  private Future<Void> flush(RoutingContext context, boolean skipCrc, boolean ignoreStatus) {
    final boolean sessionUsed = context.isSessionAccessed();
    final Session session = context.session();
    final ContextInternal ctx = (ContextInternal) context.vertx()
      .getOrCreateContext();

    if (session == null) {
      // No session in context
      return ctx.succeededFuture();
    }

    if (!session.isDestroyed()) {
      final int currentStatusCode = context.response().getStatusCode();
      // Store the session (only and only if there was no error)
      if (ignoreStatus || (currentStatusCode >= 200 && currentStatusCode < 400)) {
        // store the current user into the session
        Boolean storeUser = context.get(SESSION_STOREUSER_KEY);
        if (storeUser != null && storeUser) {
          // during the request the user might have been removed
          if (context.user() != null) {
            session.put(SESSION_USER_HOLDER_KEY, new UserHolder(context));
          }
        }

        if (session.isRegenerated()) {
          // this means that a session id has been changed, usually it means a session
          // upgrade
          // (e.g.: anonymous to authenticated) or that the security requirements have
          // changed
          // see:
          // https://www.owasp.org/index.php/Session_Management_Cheat_Sheet#Session_ID_Life_Cycle

          if (cookieless) {
            // restore defaults
            session.setAccessed();
          } else {
            // the session cookie needs to be updated to the new id
            final Cookie cookie = sessionCookie(context, session);
            // restore defaults
            session.setAccessed();
            String cookieValue = session.value();
            if (Objects.nonNull(signature)) {
              cookieValue = signature.sign(cookieValue);
            }
            cookie.setValue(cookieValue);
            setCookieProperties(cookie, false);
          }

          // we must invalidate the old id
          return sessionStore.delete(session.oldId())
            .compose(delete -> {
              // we must wait for the result of the previous call in order to save the new one
              return sessionStore.put(session)
                .onSuccess(put -> {
                  context.put(SESSION_FLUSHED_KEY, true);
                  if (session instanceof SessionInternal) {
                    ((SessionInternal) session).flushed(skipCrc);
                  }
                });
            });
        } else if (!lazySession || sessionUsed) {
          if (!cookieless) {
            // if lazy mode activated, no need to store the session nor to create the session cookie if not used.
            sessionCookie(context, session);
          }
          session.setAccessed();
          return sessionStore.put(session)
            .onSuccess(put -> {
              context.put(SESSION_FLUSHED_KEY, true);
              if (session instanceof SessionInternal) {
                ((SessionInternal) session).flushed(skipCrc);
              }
            });
        } else {
          // No-Op, just accept that the store skipped
          context.put(SESSION_FLUSHED_KEY, true);
          return ctx.succeededFuture();
        }
      } else {
        // No-Op, just accept that the store skipped
        context.put(SESSION_FLUSHED_KEY, true);
        return ctx.succeededFuture();
      }
    } else {
      if (!cookieless) {
        // invalidate the cookie as the session has been destroyed
        final Cookie expiredCookie = context.response().removeCookie(sessionCookieName);
        if (expiredCookie != null) {
          setCookieProperties(expiredCookie, true);
        }
      }
      // if the session was regenerated in the request
      // the old id must also be removed
      if (session.isRegenerated()) {
        return sessionStore.delete(session.oldId())
          .compose(delete -> {
            // delete from the storage
            return sessionStore.delete(session.id())
              .onSuccess(delete2 -> {
                context.put(SESSION_FLUSHED_KEY, true);
              });
          });
      } else {
        // delete from the storage
        return sessionStore.delete(session.id())
          .onSuccess(delete -> {
            context.put(SESSION_FLUSHED_KEY, true);
          });
      }
    }
  }

  @Override
  public void handle(RoutingContext context) {

    // we need to keep state since we can be called again on reroute
    if (!((RoutingContextInternal) context).seenHandler(RoutingContextInternal.SESSION_HANDLER)) {
      ((RoutingContextInternal) context).visitHandler(RoutingContextInternal.SESSION_HANDLER);
    } else {
      context.next();
      return;
    }

    HttpServerRequest request = context.request();
    if (nagHttps && LOG.isDebugEnabled()) {
      String uri = request.absoluteURI();
      if (!uri.startsWith("https:")) {
        LOG.debug(
          "Using session cookies without https could make you susceptible to session hijacking: " + uri);
      }
    }

    // Look for existing session id
    String sessionID = getSessionId(context);
    if (sessionID != null && sessionID.length() > minLength) {
      // this handler is asynchronous, we need to pause the request
      // if we want to be able to process it later, during a body handler or protocol upgrade
      if (!context.request().isEnded()) {
        context.request().pause();
      }

      final ContextInternal ctx = (ContextInternal) context.vertx().getOrCreateContext();

      // we passed the OWASP min length requirements
      getSession(ctx, sessionID)
        .onFailure(err -> {
          if (!context.request().isEnded()) {
            context.request().resume();
          }
          context.fail(err);
        })
        .onSuccess(session -> {
          if (session != null) {
            ((RoutingContextInternal) context).setSession(session);
            // attempt to load the user from the session
            UserHolder holder = session.get(SESSION_USER_HOLDER_KEY);
            if (holder != null) {
              holder.refresh(context);
            } else {
              // signal we must store the user to link it to the
              // session as it wasn't found
              context.put(SESSION_STOREUSER_KEY, true);
            }
            addStoreSessionHandler(context);
          } else {
            // Cannot find session - either it timed out, or was explicitly destroyed at the
            // server side on a
            // previous request.

            // OWASP clearly states that we shouldn't recreate the session as it allows
            // session fixation.
            // create a new anonymous session.
            createNewSession(context);
          }
          if (!context.request().isEnded()) {
            context.request().resume();
          }
          context.next();
        });
    } else {
      // requirements were not met, so a anonymous session is created.
      createNewSession(context);
      context.next();
    }
  }

  public Session newSession(RoutingContext context) {
    Session session = sessionStore.createSession(sessionTimeout, minLength);
    ((RoutingContextInternal) context).setSession(session);
    if (!cookieless) {
      context.response().removeCookie(sessionCookieName, false);
    }
    // it's a new session we must store the user too otherwise it won't be linked
    context.put(SESSION_STOREUSER_KEY, true);

    flush(context, true, true)
      .onFailure(err -> LOG.warn("Failed to flush the session to the underlying store", err));

    return session;
  }

  @Override
  public Future<Void> setUser(RoutingContext context, User user) {
    if (!cookieless) {
      context.response().removeCookie(sessionCookieName, false);
    }
    ((UserContextInternal) context.userContext())
      .setUser(user);
    // signal we must store the user to link it to the session
    context.put(SESSION_STOREUSER_KEY, true);
    return flush(context, true, true);
  }

  private String getSessionId(RoutingContext context) {
    if (cookieless) {
      // cookieless sessions store the session on the path or the request
      // a session is identified by a sequence of characters between braces
      String path = context.normalizedPath();
      int s = -1;
      int e = -1;
      for (int i = 0; i < path.length(); i++) {
        if (path.charAt(i) == '(') {
          s = i + 1;
          continue;
        }
        if (path.charAt(i) == ')') {
          // if not open parenthesis yet
          // this is a false end, continue looking
          if (s != -1) {
            e = i;
            break;
          }
        }
      }
      if (s != -1 && e != -1 && s < e) {
        return path.substring(s, e);
      }
    } else {
      // only pick the first cookie, when multiple sessions are used:
      // https://www.rfc-editor.org/rfc/rfc6265#section-5.4
      // The user agent SHOULD sort the cookie-list in the following order:
      // Cookies with longer paths are listed before cookies with shorter paths.
      Cookie cookie = context.request().getCookie(sessionCookieName);
      if (cookie != null) {
        // Look up sessionId
        if (Objects.nonNull(signature)) {
          // If we expect signed cookies parse the content, if it fails return null triggering a new session creation
          return signature.parse(cookie.getValue());
        }
        return cookie.getValue();
      }
    }

    return null;
  }

  private Future<Session> getSession(ContextInternal context, String sessionID) {
    return doGetSession(context, System.currentTimeMillis(), sessionID);
  }

  private Future<Session> doGetSession(ContextInternal context, long startTime, String sessionID) {
    return sessionStore.get(sessionID)
      .compose(session -> {
        if (session == null) {
          // no session was found (yet), we will retry as callback to avoid stackoverflow
          final Promise<Session> retry = context.promise();
          doGetSession(context.owner(), startTime, sessionID, retry);
          return retry.future();
        }
        return context.succeededFuture(session);
      });
  }
  private void doGetSession(Vertx vertx, long startTime, String sessionID, Completable<Session> resultHandler) {
    sessionStore.get(sessionID)
      .onComplete(res -> {
      if (res.succeeded()) {
        if (res.result() == null) {
          // Can't find it so retry. This is necessary for clustered sessions as it can take sometime for the session
          // to propagate across the cluster so if the next request for the session comes in quickly at a different
          // node there is a possibility it isn't available yet.
          if (System.currentTimeMillis() - startTime < sessionStore.retryTimeout()) {
            vertx.setTimer(5L, v -> doGetSession(vertx, startTime, sessionID, resultHandler));
            return;
          }
        }
      }
      resultHandler.complete(res.result(), res.cause());
    });
  }


  private void addStoreSessionHandler(RoutingContext context) {
    context.addHeadersEndHandler(v -> {
      // skip flush if we already flushed
      Boolean flushed = context.get(SESSION_FLUSHED_KEY);
      if (flushed == null || !flushed) {
        flush(context, true, false)
          .onFailure(err -> LOG.warn("Failed to flush the session to the underlying store", err));
      }
    });
  }

  private void createNewSession(RoutingContext context) {
    Session session = sessionStore.createSession(sessionTimeout, minLength);
    ((RoutingContextInternal) context).setSession(session);
    if (!cookieless) {
      context.response().removeCookie(sessionCookieName, false);
    }
    // it's a new session we must store the user too otherwise it won't be linked
    context.put(SESSION_STOREUSER_KEY, true);
    addStoreSessionHandler(context);
  }

  private Cookie sessionCookie(final RoutingContext context, final Session session) {
    // only pick the first cookie, when multiple sessions are used:
    // https://www.rfc-editor.org/rfc/rfc6265#section-5.4
    // The user agent SHOULD sort the cookie-list in the following order:
    // Cookies with longer paths are listed before cookies with shorter paths.
    Cookie cookie = context.request().getCookie(sessionCookieName);
    if (cookie != null) {
      return cookie;
    }
    String cookieValue = session.value();
    if (Objects.nonNull(signature)) {
      cookieValue = signature.sign(cookieValue);
    }
    cookie = Cookie.cookie(sessionCookieName, cookieValue);
    setCookieProperties(cookie, false);
    context.response().addCookie(cookie);
    return cookie;
  }
}
