/*
 * Copyright 2014 Red Hat, Inc.
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

package io.vertx.rxjava.ext.apex.handler;

import java.util.Map;
import io.vertx.lang.rxjava.InternalHelper;
import rx.Observable;
import io.vertx.rxjava.ext.apex.sstore.SessionStore;
import io.vertx.rxjava.ext.apex.RoutingContext;
import io.vertx.core.Handler;

/**
 * A handler that maintains a {@link  io.vertx.rxjava.ext.apex.Session} for each browser session.
 * <p>
 * It looks up the session for each request based on a session cookie which contains a session ID. It stores the session
 * when the response is ended in the session store.
 * <p>
 * The session is available on the routing context with {@link  io.vertx.rxjava.ext.apex.RoutingContext#session()}.
 * <p>
 * The session handler requires a {@link  io.vertx.rxjava.ext.apex.handler.CookieHandler} to be on the routing chain before it.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.apex.handler.SessionHandler original} non RX-ified interface using Vert.x codegen.
 */

public class SessionHandler implements Handler<RoutingContext> {

  final io.vertx.ext.apex.handler.SessionHandler delegate;

  public SessionHandler(io.vertx.ext.apex.handler.SessionHandler delegate) {
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  public void handle(RoutingContext arg0) { 
    this.delegate.handle((io.vertx.ext.apex.RoutingContext) arg0.getDelegate());
  }

  /**
   * Create a session handler
   * @param sessionStore the session store
   * @return the handler
   */
  public static SessionHandler create(SessionStore sessionStore) { 
    SessionHandler ret= SessionHandler.newInstance(io.vertx.ext.apex.handler.SessionHandler.create((io.vertx.ext.apex.sstore.SessionStore) sessionStore.getDelegate()));
    return ret;
  }

  /**
   * Set the session timeout
   * @param timeout the timeout, in ms.
   * @return a reference to this, so the API can be used fluently
   */
  public SessionHandler setSessionTimeout(long timeout) { 
    this.delegate.setSessionTimeout(timeout);
    return this;
  }

  /**
   * Set whether a nagging log warning should be written if the session handler is accessed over HTTP, not
   * HTTPS
   * @param nag true to nag
   * @return a reference to this, so the API can be used fluently
   */
  public SessionHandler setNagHttps(boolean nag) { 
    this.delegate.setNagHttps(nag);
    return this;
  }

  /**
   * Set the session cookie name
   * @param sessionCookieName the session cookie name
   * @return a reference to this, so the API can be used fluently
   */
  public SessionHandler setSessionCookieName(String sessionCookieName) { 
    this.delegate.setSessionCookieName(sessionCookieName);
    return this;
  }


  public static SessionHandler newInstance(io.vertx.ext.apex.handler.SessionHandler arg) {
    return new SessionHandler(arg);
  }
}
