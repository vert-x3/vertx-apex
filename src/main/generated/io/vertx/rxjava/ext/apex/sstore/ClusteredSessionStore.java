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
 * A session store which stores sessions in a distributed map so they are available across the cluster.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.apex.sstore.ClusteredSessionStore original} non RX-ified interface using Vert.x codegen.
 */

public class ClusteredSessionStore extends SessionStore {

  final io.vertx.ext.apex.sstore.ClusteredSessionStore delegate;

  public ClusteredSessionStore(io.vertx.ext.apex.sstore.ClusteredSessionStore delegate) {
    super(delegate);
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  /**
   * Create a session store
   * @param vertx the Vert.x instance
   * @param sessionMapName the session map name
   * @return the session store
   */
  public static ClusteredSessionStore create(Vertx vertx, String sessionMapName) { 
    ClusteredSessionStore ret= ClusteredSessionStore.newInstance(io.vertx.ext.apex.sstore.ClusteredSessionStore.create((io.vertx.core.Vertx) vertx.getDelegate(), sessionMapName));
    return ret;
  }

  /**
   * Create a session store
   * @param vertx the Vert.x instance
   * @return the session store
   */
  public static ClusteredSessionStore create(Vertx vertx) { 
    ClusteredSessionStore ret= ClusteredSessionStore.newInstance(io.vertx.ext.apex.sstore.ClusteredSessionStore.create((io.vertx.core.Vertx) vertx.getDelegate()));
    return ret;
  }


  public static ClusteredSessionStore newInstance(io.vertx.ext.apex.sstore.ClusteredSessionStore arg) {
    return new ClusteredSessionStore(arg);
  }
}
