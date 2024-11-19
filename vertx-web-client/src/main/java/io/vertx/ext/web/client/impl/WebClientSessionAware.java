/*
 * Copyright (c) 2011-2018 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.ext.web.client.impl;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.internal.ContextInternal;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.client.spi.CacheStore;
import io.vertx.ext.web.client.spi.CookieStore;

/**
 * @author <a href="mailto:tommaso.nolli@gmail.com">Tommaso Nolli</a>
 */
public class WebClientSessionAware extends WebClientBase<WebClientSessionAware> implements WebClientSession {

  private final CookieStore cookieStore;
  private final CacheStore cacheStore;
  private MultiMap headers;

  public WebClientSessionAware(WebClient webClient, CookieStore cookieStore) {
    super((WebClientBase) webClient);
    this.cookieStore = cookieStore;
    this.cacheStore = CacheStore.localStore();
    addInterceptor(new SessionAwareInterceptor(this));
  }

  public CookieStore cookieStore() {
    return cookieStore;
  }

  protected MultiMap headers() {
    if (headers == null) {
      headers = HttpHeaders.headers();
    }
    return headers;
  }

  @Override
  public WebClientSession addHeader(CharSequence name, CharSequence value) {
    headers().add(name, value);
    return this;
  }

  @Override
  public WebClientSession addHeader(String name, String value) {
    headers().add(name, value);
    return this;
  }

  @Override
  public WebClientSession addHeader(CharSequence name, Iterable<CharSequence> values) {
    headers().add(name, values);
    return this;
  }

  @Override
  public WebClientSession addHeader(String name, Iterable<String> values) {
    headers().add(name, values);
    return this;
  }

  @Override
  public WebClientSession removeHeader(CharSequence name) {
    headers().remove(name);
    return this;
  }

  @Override
  public WebClientSession removeHeader(String name) {
    headers().remove(name);
    return this;
  }

  @Override
  public <T> HttpContext<T> createContext(ContextInternal context) {
    return super.<T>createContext(context).privateCacheStore(cacheStore);
  }
}
