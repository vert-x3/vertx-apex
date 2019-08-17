/*
 * Copyright 2019 Red Hat, Inc.
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

package examples;

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.WiringFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.*;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author Thomas SEGISMONT
 */
public class GraphQLExamples {

  public void handlerSetup(Router router) {
    GraphQL graphQL = setupGraphQLJava();

    router.route("/graphql").handler(GraphQLHandler.create(graphQL));
  }

  public void handlerSetupPost(Router router) {
    GraphQL graphQL = setupGraphQLJava();

    router.post("/graphql").handler(GraphQLHandler.create(graphQL));
  }

  public void handlerSetupGraphiQL(GraphQL graphQL, Router router) {
    GraphQLHandlerOptions options = new GraphQLHandlerOptions()
      .setGraphiQLOptions(new GraphiQLOptions()
        .setEnabled(true)
      );

    router.route("/graphql").handler(GraphQLHandler.create(graphQL, options));
  }

  public void handlerSetupGraphiQLAuthn(GraphQL graphQL, Router router) {
    GraphQLHandlerOptions options = new GraphQLHandlerOptions()
      .setGraphiQLOptions(new GraphiQLOptions()
        .setEnabled(true)
      );

    GraphQLHandler graphQLHandler = GraphQLHandler.create(graphQL, options)
      .graphiQLRequestHeaders(rc -> {
        String token = rc.get("token");
        return MultiMap.caseInsensitiveMultiMap().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
      });
  }

  public void handlerSetupBatching(GraphQL graphQL) {
    GraphQLHandlerOptions options = new GraphQLHandlerOptions()
      .setRequestBatchingEnabled(true);

    GraphQLHandler handler = GraphQLHandler.create(graphQL, options);
  }

  private GraphQL setupGraphQLJava() {
    return null;
  }

  class Link {}

  private void completionStageDataFetcher() {
    DataFetcher<CompletionStage<List<Link>>> dataFetcher = environment -> {

      CompletableFuture<List<Link>> completableFuture = new CompletableFuture<>();

      retrieveLinksFromBackend(environment, ar -> {
        if (ar.succeeded()) {
          completableFuture.complete(ar.result());
        } else {
          completableFuture.completeExceptionally(ar.cause());
        }
      });

      return completableFuture;
    };

    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
      .type("Query", builder -> builder.dataFetcher("allLinks", dataFetcher))
      .build();
  }

  private void retrieveLinksFromBackend(DataFetchingEnvironment environment, Handler<AsyncResult<List<Link>>> handler) {
  }

  private void vertxDataFetcher() {
    VertxDataFetcher<List<Link>> dataFetcher = new VertxDataFetcher<>((environment, future) -> {

      retrieveLinksFromBackend(environment, future);

    });

    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
      .type("Query", builder -> builder.dataFetcher("allLinks", dataFetcher))
      .build();
  }

  class User {}

  private void routingContextInDataFetchingEnvironment() {
    VertxDataFetcher<List<Link>> dataFetcher = new VertxDataFetcher<>((environment, future) -> {

      RoutingContext routingContext = environment.getContext();

      User user = routingContext.get("user");

      retrieveLinksPostedBy(user, future);

    });
  }

  private void retrieveLinksPostedBy(User user, Handler<AsyncResult<List<Link>>> future) {
  }

  private void customContextInDataFetchingEnvironment(Router router) {
    VertxDataFetcher<List<Link>> dataFetcher = new VertxDataFetcher<>((environment, future) -> {

      // User as custom context object
      User user = environment.getContext();

      retrieveLinksPostedBy(user, future);

    });

    GraphQL graphQL = setupGraphQLJava(dataFetcher);

    // Customize the query context object when setting up the handler
    GraphQLHandler handler = GraphQLHandler.create(graphQL).queryContext(routingContext -> {

      return routingContext.get("user");

    });

    router.route("/graphql").handler(handler);
  }

  private GraphQL setupGraphQLJava(DataFetcher<CompletionStage<List<Link>>> dataFetcher) {
    return null;
  }

  private void jsonData() {
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

    builder.wiringFactory(new WiringFactory() {

      @Override
      public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {

        return new VertxPropertyDataFetcher(environment.getFieldDefinition().getName());

      }
    });
  }

  public void createBatchLoader() {
    BatchLoaderWithContext<String, Link> linksBatchLoader = (keys, environment) -> {

      return retrieveLinksFromBackend(keys, environment);

    };
  }

  private CompletionStage<List<Link>> retrieveLinksFromBackend(List<String> ids, BatchLoaderEnvironment environment) {
    return null;
  }

  public void dataLoaderRegistry(GraphQL graphQL, BatchLoaderWithContext<String, Link> linksBatchLoader) {
    GraphQLHandler handler = GraphQLHandler.create(graphQL).dataLoaderRegistry(rc -> {

      DataLoader<String, Link> linkDataLoader = DataLoader.newDataLoader(linksBatchLoader);

      return new DataLoaderRegistry().register("link", linkDataLoader);

    });
  }
}
