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

package io.vertx.rxjava.ext.apex.sstore;

import java.util.Map;
import io.vertx.lang.rxjava.InternalHelper;
import rx.Observable;
import io.vertx.rxjava.core.Vertx;

/**
 * A session store which is only available on a single node.
 * <p>
 * Can be used when sticky sessions are being used.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.apex.sstore.LocalSessionStore original} non RX-ified interface using Vert.x codegen.
 */

public class LocalSessionStore extends SessionStore {

  final io.vertx.ext.apex.sstore.LocalSessionStore delegate;

  public LocalSessionStore(io.vertx.ext.apex.sstore.LocalSessionStore delegate) {
    super(delegate);
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  /**
   * Create a session store
   * @param vertx the Vert.x instance
   * @return the session store
   */
  public static LocalSessionStore create(Vertx vertx) { 
    LocalSessionStore ret= LocalSessionStore.newInstance(io.vertx.ext.apex.sstore.LocalSessionStore.create((io.vertx.core.Vertx) vertx.getDelegate()));
    return ret;
  }

  /**
   * Create a session store
   * @param vertx the Vert.x instance
   * @param sessionMapName name for map used to store sessions
   * @return the session store
   */
  public static LocalSessionStore create(Vertx vertx, String sessionMapName) { 
    LocalSessionStore ret= LocalSessionStore.newInstance(io.vertx.ext.apex.sstore.LocalSessionStore.create((io.vertx.core.Vertx) vertx.getDelegate(), sessionMapName));
    return ret;
  }

  /**
   * Create a session store
   * @param vertx the Vert.x instance
   * @param sessionMapName name for map used to store sessions
   * @param reaperPeriod how often, in ms, to check for expired sessions
   * @return the session store
   */
  public static LocalSessionStore create(Vertx vertx, String sessionMapName, long reaperPeriod) { 
    LocalSessionStore ret= LocalSessionStore.newInstance(io.vertx.ext.apex.sstore.LocalSessionStore.create((io.vertx.core.Vertx) vertx.getDelegate(), sessionMapName, reaperPeriod));
    return ret;
  }


  public static LocalSessionStore newInstance(io.vertx.ext.apex.sstore.LocalSessionStore arg) {
    return new LocalSessionStore(arg);
  }
}
