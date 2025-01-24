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

package io.vertx.ext.web.handler;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.CookieSameSite;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.impl.SessionHandlerImpl;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * A handler that maintains a {@link io.vertx.ext.web.Session} for each browser
 * session.
 * <p>
 * It looks up the session for each request based on a session cookie which
 * contains a session ID. It stores the session when the response is ended in
 * the session store.
 * <p>
 * The session is available on the routing context with
 * {@link RoutingContext#session()}.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface SessionHandler extends PlatformHandler {

	/**
	 * Default name of session cookie
	 */
	String DEFAULT_SESSION_COOKIE_NAME = "vertx-web.session";

  /**
   * Default domain of session cookie i.e. no domain is set. More info:
   * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie#domaindomain-value
   */
  String DEFAULT_SESSION_COOKIE_DOMAIN = null;

	/**
	 * Default path of session cookie
	 */
	String DEFAULT_SESSION_COOKIE_PATH = "/";

	/**
	 * Default time, in ms, that a session lasts for without being accessed before
	 * expiring.
	 */
	long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

	/**
	 * Default of whether a nagging log warning should be written if the session
	 * handler is accessed over HTTP, not HTTPS
	 */
	boolean DEFAULT_NAG_HTTPS = true;

	/**
	 * Default of whether the cookie has the HttpOnly flag set More info:
	 * https://www.owasp.org/index.php/HttpOnly
	 */
	boolean DEFAULT_COOKIE_HTTP_ONLY_FLAG = false;

	/**
	 * Default of whether the cookie has the 'secure' flag set to allow transmission
	 * over https only. More info: https://www.owasp.org/index.php/SecureFlag
	 */
	boolean DEFAULT_COOKIE_SECURE_FLAG = false;

	/**
	 * Default min length for a session id. More info:
	 * https://www.owasp.org/index.php/Session_Management_Cheat_Sheet
	 */
	int DEFAULT_SESSIONID_MIN_LENGTH = 16;

  /**
   * Default of whether the session should be created lazily.
   */
	boolean DEFAULT_LAZY_SESSION = false;

	/**
	 * Create a session handler
	 *
	 * @param sessionStore the session store
	 * @return the handler
	 */
	static SessionHandler create(SessionStore sessionStore) {
		return new SessionHandlerImpl(sessionStore);
	}

	/**
	 * Set the session timeout
	 *
	 * @param timeout the timeout, in ms.
	 * @return a reference to this, so the API can be used fluently
	 */
	@Fluent
	SessionHandler setSessionTimeout(long timeout);

	/**
	 * Set whether a nagging log warning should be written if the session handler is
	 * accessed over HTTP, not HTTPS
	 *
	 * @param nag true to nag
	 * @return a reference to this, so the API can be used fluently
	 */
	@Fluent
	SessionHandler setNagHttps(boolean nag);

	/**
	 * Sets whether the 'secure' flag should be set for the session cookie. When set
	 * this flag instructs browsers to only send the cookie over HTTPS. Note that
	 * this will probably stop your sessions working if used without HTTPS (e.g. in
	 * development).
	 *
	 * @param secure true to set the secure flag on the cookie
	 * @return a reference to this, so the API can be used fluently
	 */
	@Fluent
	SessionHandler setCookieSecureFlag(boolean secure);

	/**
	 * Sets whether the 'HttpOnly' flag should be set for the session cookie. When
	 * set this flag instructs browsers to prevent Javascript access to the the
	 * cookie. Used as a line of defence against the most common XSS attacks.
	 *
	 * @param httpOnly true to set the HttpOnly flag on the cookie
	 * @return a reference to this, so the API can be used fluently
	 */
	@Fluent
	SessionHandler setCookieHttpOnlyFlag(boolean httpOnly);

	/**
	 * Set the session cookie name
	 *
	 * @param sessionCookieName the session cookie name
	 * @return a reference to this, so the API can be used fluently
	 */
	@Fluent
	SessionHandler setSessionCookieName(String sessionCookieName);

  /**
   * Set the session cookie domain. Only the current domain can be set as the
   * value, or a domain of a higher order, unless it is a public suffix. Setting
   * the domain will make the cookie available to it, as well as to all its
   * subdomains. If omitted, this attribute defaults to the host of the current
   * document URL, not including subdomains.
   *
   * @param sessionCookieDomain the session cookie domain
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  SessionHandler setSessionCookieDomain(String sessionCookieDomain);

	/**
	 * Set the session cookie path
	 *
	 * @param sessionCookiePath the session cookie path
	 * @return a reference to this, so the API can be used fluently
	 */
	@Fluent
	SessionHandler setSessionCookiePath(String sessionCookiePath);

	/**
	 * Set expected session id minimum length.
	 *
	 * @param minLength the session id minimal length
	 * @return a reference to this, so the API can be used fluently
	 */
	@Fluent
	SessionHandler setMinLength(int minLength);

  /**
   * Set the session cookie SameSite policy to use.
   * @param policy to use, {@code null} for no policy.
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
	SessionHandler setCookieSameSite(CookieSameSite policy);

  /**
   * Use a lazy session creation mechanism. The session will only be created when accessed from the context. Thus the
   * session cookie is set only if the session was accessed.
   *
   * @param lazySession true to have a lazy session creation.
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
	SessionHandler setLazySession(boolean lazySession);

  /**
   * Set a Cookie max-age to the session cookie. When doing this the Cookie will be persistent across browser restarts.
   * This can be dangerous as closing a browser windows does not invalidate the session. For more information refer to
   * https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html#Expire_and_Max-Age_Attributes
   *
   * @param cookieMaxAge a non negative max-age, note that 0 means expire now.
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  SessionHandler setCookieMaxAge(long cookieMaxAge);

  /**
   * Flush a context session earlier to the store, this will allow the end user to have full control on the event of
   * a failure at the store level. Once a session is flushed no automatic save will be performed at end of request.
   *
   * @param ctx the current context
   * @param handler the event handler to signal a asynchronous response.
   * @return fluent self
   */
	@Fluent
  default SessionHandler flush(RoutingContext ctx, Handler<AsyncResult<Void>> handler) {
    flush(ctx, false)
      .onComplete(handler);

    return this;
  }

  /**
   * Flush a context session earlier to the store, this will allow the end user to have full control on the event of
   * a failure at the store level. Once a session is flushed no automatic save will be performed at end of request.
   *
   * @param ctx the current context
   * @param ignoreStatus flush regardless of response status code
   * @param handler the event handler to signal a asynchronous response.
   * @return fluent self
   */
  @Fluent
  default SessionHandler flush(RoutingContext ctx, boolean ignoreStatus, Handler<AsyncResult<Void>> handler) {
    flush(ctx, ignoreStatus)
      .onComplete(handler);

    return this;
  }

  /**
   * Promisified flush. See {@link #flush(RoutingContext, Handler)}.
   */
	default Future<Void> flush(RoutingContext ctx) {
    return flush(ctx, false);
  }

  /**
   * Promisified flush. See {@link #flush(RoutingContext, boolean, Handler)}.
   */
  Future<Void> flush(RoutingContext ctx, boolean ignoreStatus);

  /**
   * Use sessions based on url paths instead of cookies. This is an potential less safe alternative to cookies
   * but offers an alternative when Cookies are not desired, for example, to avoid showing banners on a website
   * due to cookie laws, or doing machine to machine operations where state is required to maintain.
   *
   * @param cookieless true if a cookieless session should be used
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  SessionHandler setCookieless(boolean cookieless);

  /**
   * Create a new session
   *
   * @param context the routing context
   * @return the session
   */
  Session newSession(RoutingContext context);

  /**
   * Set the user for the session
   *
   * @param context the routing context
   * @param user the user
   * @return future that will be called when complete, or a failure
   */
  Future<Void> setUser(RoutingContext context, User user);

  /**
   * Set the user for the session
   *
   * @param context the routing context
   * @param user the user
   * @param handler the event handler to signal a asynchronous response.
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  default SessionHandler setUser(RoutingContext context, User user, Handler<AsyncResult<Void>> handler) {
    setUser(context, user).onComplete(handler);
    return this;
  }
}
