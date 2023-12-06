/*
 * Copyright 2023 Red Hat, Inc.
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

package io.vertx.ext.web.handler.graphql.ws;

import graphql.GraphQL;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.handler.ProtocolUpgradeHandler;
import io.vertx.ext.web.handler.graphql.ExecutionInputBuilderWithContext;
import io.vertx.ext.web.handler.graphql.impl.ws.GraphQLWSHandlerBuilderImpl;
import io.vertx.ext.web.handler.graphql.impl.ws.GraphQLWSHandlerImpl;

import java.util.Objects;

import static io.vertx.codegen.annotations.GenIgnore.PERMITTED_TYPE;

/**
 * A handler for the <a href="https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md">GraphQL over WebSocket Protocol</a>.
 */
@VertxGen
public interface GraphQLWSHandler extends ProtocolUpgradeHandler {

  /**
   * Create a new {@link GraphQLWSHandlerBuilder} that will use the provided {@code graphQL} to build a {@link GraphQLWSHandler}.
   */
  @GenIgnore(PERMITTED_TYPE)
  static GraphQLWSHandlerBuilder builder(GraphQL graphQL) {
    return new GraphQLWSHandlerBuilderImpl(Objects.requireNonNull(graphQL, "graphQL instance is null"));
  }

  /**
   * Create a new {@link GraphQLWSHandler} that will use the provided {@code graphQL} object to execute requests.
   * <p>
   * The handler will be configured with the default {@link GraphQLWSOptions}.
   */
  @GenIgnore(PERMITTED_TYPE)
  static GraphQLWSHandler create(GraphQL graphQL) {
    return create(graphQL, null);
  }

  /**
   * Create a new {@link GraphQLWSHandler} that will use the provided {@code graphQL} object to execute requests.
   * <p>
   * The handler will be configured with the given {@code options}.
   *
   * @param options options for configuring the {@link GraphQLWSOptions}
   */
  @GenIgnore(PERMITTED_TYPE)
  static GraphQLWSHandler create(GraphQL graphQL, GraphQLWSOptions options) {
    return new GraphQLWSHandlerImpl(Objects.requireNonNull(graphQL, "graphQL instance is null"), options);
  }

  /**
   * Customize the connection init {@link Handler}.
   * This handler will be called when the {@link MessageType#CONNECTION_INIT} message is received.
   *
   * @return a reference to this, so the API can be used fluently
   * @deprecated as of 4.5.1, use {@link #builder(GraphQL)} instead
   */
  @Fluent
  @Deprecated
  GraphQLWSHandler connectionInitHandler(Handler<ConnectionInitEvent> connectionInitHandler);

  /**
   * Set a callback to invoke before executing a GraphQL query.
   *
   * @param config the callback to invoke
   * @return a reference to this, so the API can be used fluently
   * @deprecated as of 4.5.1, use {@link #builder(GraphQL)} instead
   */
  @Fluent
  @Deprecated
  GraphQLWSHandler beforeExecute(Handler<ExecutionInputBuilderWithContext<Message>> config);

  /**
   * Customize the message {@link Handler}.
   * This handler will be called for each {@link Message} received.
   *
   * @return a reference to this, so the API can be used fluently
   * @deprecated as of 4.5.1, use {@link #builder(GraphQL)} instead
   */
  @Fluent
  @Deprecated
  GraphQLWSHandler messageHandler(Handler<Message> messageHandler);

  /**
   * Customize the end {@link Handler}.
   * This handler will be called at the end of each websocket connection.
   *
   * @return a reference to this, so the API can be used fluently
   * @deprecated as of 4.5.1, use {@link #builder(GraphQL)} instead
   */
  @Fluent
  @Deprecated
  GraphQLWSHandler endHandler(Handler<ServerWebSocket> endHandler);
}
