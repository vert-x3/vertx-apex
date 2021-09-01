/*
 * Copyright 2021 Red Hat, Inc.
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

package io.vertx.ext.web.handler.graphql.impl;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.ApolloWSConnectionInitEvent;
import io.vertx.ext.web.handler.graphql.ApolloWSMessage;
import io.vertx.ext.web.handler.graphql.ApolloWSMessageType;
import org.dataloader.DataLoaderRegistry;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.vertx.ext.web.handler.graphql.ApolloWSMessageType.*;
import static io.vertx.ext.web.handler.graphql.impl.ErrorUtil.toJsonObject;

/**
 * @author Rogelio Orts
 */
class ApolloWSConnectionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApolloWSConnectionHandler.class);
  private static final short WS_INTERNAL_ERROR = 1011;

  private final ApolloWSHandlerImpl apolloWSHandler;
  private final ServerWebSocket serverWebSocket;
  private final ContextInternal context;
  private final Executor executor;
  private final ConcurrentMap<String, Subscription> subscriptions;
  private final Promise<Object> connectionPromise;
  private final AtomicBoolean connectionInitialized;

  ApolloWSConnectionHandler(ApolloWSHandlerImpl apolloWSHandler, ContextInternal context, ServerWebSocket serverWebSocket) {
    this.apolloWSHandler = apolloWSHandler;
    this.context = context;
    this.serverWebSocket = serverWebSocket;
    this.executor = task -> context.runOnContext(v -> task.run());
    subscriptions = new ConcurrentHashMap<>();
    connectionPromise = context.promise();
    connectionInitialized = new AtomicBoolean(false);
  }

  void handleConnection() {
    Handler<ServerWebSocket> ch = apolloWSHandler.getConnectionHandler();
    if (ch != null) {
      ch.handle(serverWebSocket);
    }

    serverWebSocket.binaryMessageHandler(this::handleBinaryMessage);
    serverWebSocket.textMessageHandler(this::handleTextMessage);
    serverWebSocket.closeHandler(this::close);
  }

  private void handleBinaryMessage(Buffer buffer) {
    handleMessage(new JsonObject(buffer));
  }

  private void handleTextMessage(String text) {
    handleMessage(new JsonObject(text));
  }

  private void handleMessage(JsonObject jsonObject) {
    String opId = jsonObject.getString("id");
    ApolloWSMessageType type = from(jsonObject.getString("type"));

    if (type == null) {
      sendMessage(opId, ERROR, "Unknown message type: " + jsonObject.getString("type"));
      return;
    }

    ApolloWSMessageImpl message = new ApolloWSMessageImpl(serverWebSocket, type, jsonObject);

    Handler<ApolloWSMessage> mh = apolloWSHandler.getMessageHandler();
    if (mh != null) {
      mh.handle(message);
    }

    Handler<ApolloWSConnectionInitEvent> connectionInitHandler = apolloWSHandler.getConnectionInitHandler();

    switch (type) {
      case CONNECTION_INIT:
        if (!connectionInitialized.compareAndSet(false, true)) {
          sendMessage(opId, ERROR, "CONNECTION_INIT can only be sent once")
            .onComplete(v -> serverWebSocket.close(WS_INTERNAL_ERROR));
          break;
        }
        if (connectionInitHandler != null) {
          connectionInitHandler.handle(new ApolloWSConnectionInitEvent() {
            @Override
            public ApolloWSMessage message() {
              return message;
            }

            @Override
            public boolean tryComplete(Object o) {
              return connectionPromise.tryComplete(o);
            }

            @Override
            public boolean tryFail(Throwable throwable) {
              return connectionPromise.tryFail(throwable);
            }

            @Override
            public Future<Object> future() {
              return connectionPromise.future();
            }
          });
        } else {
          connectionPromise.complete();
        }
        connectionPromise.future().onComplete(ar -> {
            if (ar.succeeded()) {
              connect();
            } else {
              sendMessage(opId, CONNECTION_ERROR, ar.cause().getMessage())
                .onComplete(v -> serverWebSocket.close(WS_INTERNAL_ERROR));
            }
          });
        break;
      case CONNECTION_TERMINATE:
        serverWebSocket.close();
        break;
      case START:
        if (!connectionInitialized.get()) {
          sendMessage(opId, ERROR, "CONNECTION_INIT has to be sent before START")
            .onComplete(v -> serverWebSocket.close(WS_INTERNAL_ERROR));
          break;
        }
        connectionPromise.future().onComplete(ar -> {
          if (ar.succeeded()) {
            ApolloWSMessage messageWithParams = new ApolloWSMessageImpl(serverWebSocket, type, jsonObject, ar.result());
            start(messageWithParams);
          } else {
            sendMessage(opId, ERROR, ar.cause().getMessage());
            stop(opId);
          }
        });
        break;
      case STOP:
        stop(opId);
        break;
      default:
        sendMessage(opId, ERROR, "Unsupported message type: " + type);
        break;
    }
  }

  private void connect() {
    sendMessage(null, CONNECTION_ACK, null);

    long keepAlive = apolloWSHandler.getKeepAlive();
    if (keepAlive > 0) {
      sendMessage(null, CONNECTION_KEEP_ALIVE, null);
      context.setPeriodic(keepAlive, timerId -> {
        if (serverWebSocket.isClosed()) {
          context.owner().cancelTimer(timerId);
        } else {
          sendMessage(null, CONNECTION_KEEP_ALIVE, null);
        }
      });
    }
  }

  private void start(ApolloWSMessage message) {
    String opId = message.content().getString("id");

    // Unsubscribe if it's subscribed
    if (subscriptions.containsKey(opId)) {
      stop(opId);
    }

    GraphQLQuery payload = new GraphQLQuery(message.content().getJsonObject("payload"));
    ExecutionInput.Builder builder = ExecutionInput.newExecutionInput();
    builder.query(payload.getQuery());

    builder.context(apolloWSHandler.getQueryContext().apply(message));

    DataLoaderRegistry registry = apolloWSHandler.getDataLoaderRegistry().apply(message);
    if (registry != null) {
      builder.dataLoaderRegistry(registry);
    }

    Locale locale = apolloWSHandler.getLocale().apply(message);
    if (locale != null) {
      builder.locale(locale);
    }

    String operationName = payload.getOperationName();
    if (operationName != null) {
      builder.operationName(operationName);
    }
    Map<String, Object> variables = payload.getVariables();
    if (variables != null) {
      builder.variables(variables);
    }
    Object initialValue = payload.getInitialValue();
    if (initialValue != null) {
      builder.root(initialValue);
    }

    apolloWSHandler.getGraphQL().executeAsync(builder).whenCompleteAsync((executionResult, throwable) -> {
      if (throwable == null) {
        if (executionResult.getData() instanceof Publisher) {
          subscribe(opId, executionResult);
        } else {
          sendMessage(opId, DATA, new JsonObject(executionResult.toSpecification()));
          sendMessage(opId, COMPLETE, null);
        }
      } else {
        if (log.isDebugEnabled()) {
          log.debug("Failed to execute GraphQL query, opId=" + opId, throwable);
        }
        sendMessage(opId, ERROR, toJsonObject(throwable));
      }
    }, executor);
  }

  private void subscribe(String opId, ExecutionResult executionResult) {
    Publisher<ExecutionResult> publisher = executionResult.getData();

    AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
    publisher.subscribe(new Subscriber<ExecutionResult>() {
      @Override
      public void onSubscribe(Subscription s) {
        subscriptionRef.set(s);
        subscriptions.put(opId, s);
        s.request(1);
      }

      @Override
      public void onNext(ExecutionResult er) {
        sendMessage(opId, DATA, new JsonObject(er.toSpecification()));
        subscriptionRef.get().request(1);
      }

      @Override
      public void onError(Throwable t) {
        if (log.isDebugEnabled()) {
          log.debug("GraphQL subscription terminated with error, opId=" + opId, t);
        }
        sendMessage(opId, ERROR, toJsonObject(t));
        subscriptions.remove(opId);
      }

      @Override
      public void onComplete() {
        sendMessage(opId, COMPLETE, null);
        subscriptions.remove(opId);
      }
    });
  }

  private void stop(String opId) {
    Subscription subscription = subscriptions.get(opId);
    if (subscription != null) {
      subscription.cancel();
      subscriptions.remove(opId);
    }
  }

  private Future<Void> sendMessage(String opId, ApolloWSMessageType type, Object payload) {
    Objects.requireNonNull(type, "type is null");
    JsonObject message = new JsonObject();
    if (opId != null) {
      message.put("id", opId);
    }
    message.put("type", type.getText());
    if (payload != null) {
      message.put("payload", payload);
    }
    return serverWebSocket.writeTextMessage(message.toString());
  }

  private void close(Void v) {
    subscriptions.values().forEach(Subscription::cancel);

    Handler<ServerWebSocket> eh = apolloWSHandler.getEndHandler();
    if (eh != null) {
      eh.handle(serverWebSocket);
    }
  }
}
