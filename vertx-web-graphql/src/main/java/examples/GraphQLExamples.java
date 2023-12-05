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

package examples;

import graphql.GraphQL;
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport;
import graphql.execution.preparsed.persisted.PersistedQueryCache;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.WiringFactory;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.graphql.*;
import io.vertx.ext.web.handler.graphql.instrumentation.JsonObjectAdapter;
import io.vertx.ext.web.handler.graphql.instrumentation.VertxFutureAdapter;
import io.vertx.ext.web.handler.graphql.schema.VertxPropertyDataFetcher;
import io.vertx.ext.web.handler.graphql.ws.GraphQLWSHandler;
import org.dataloader.*;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author Thomas SEGISMONT
 */
@SuppressWarnings("unused")
public class GraphQLExamples {

  public void handlerSetup(Router router) {
    GraphQL graphQL = setupGraphQLJava();

    router.route("/graphql").handler(GraphQLHandler.create(graphQL));
  }

  public void persistedQueries(GraphQL.Builder graphQLBuilder, PersistedQueryCache queryCache) {
    graphQLBuilder.preparsedDocumentProvider(new ApolloPersistedQuerySupport(queryCache));
  }

  public void handlerSetupPost(Router router) {
    GraphQL graphQL = setupGraphQLJava();

    router.post("/graphql").handler(GraphQLHandler.create(graphQL));
  }

  public void handlerSetupGraphiQL(Vertx vertx, Router router) {
    GraphiQLHandlerOptions options = new GraphiQLHandlerOptions()
      .setEnabled(true);

    GraphiQLHandler handler = GraphiQLHandler.builder(vertx)
      .with(options)
      .build();

    router.route("/graphiql*").subRouter(handler.router());
  }

  public void handlerSetupGraphiQLAuthn(Vertx vertx, Router router) {
    GraphiQLHandlerOptions options = new GraphiQLHandlerOptions()
      .setEnabled(true);

    GraphiQLHandler handler = GraphiQLHandler.builder(vertx)
      .with(options)
      .withHeadersFactory(rc -> {
        String token = rc.get("token");
        return MultiMap.caseInsensitiveMultiMap().add("Authorization", "Bearer " + token);
      })
      .build();

    router.route("/graphiql*").subRouter(handler.router());
  }

  public void handlerSetupBatching(GraphQL graphQL) {
    GraphQLHandlerOptions options = new GraphQLHandlerOptions()
      .setRequestBatchingEnabled(true);

    GraphQLHandler handler = GraphQLHandler.create(graphQL, options);
  }

  public void setupGraphQLHandlerMultipart(Vertx vertx) {
    GraphQLHandler graphQLHandler = GraphQLHandler.create(
      setupGraphQLJava(),
      new GraphQLHandlerOptions().setRequestMultipartEnabled(true)
    );

    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route("/graphql").handler(graphQLHandler);
  }

  public void setRuntimeScalar() {
    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().scalar(UploadScalar.build()).build();
  }

  public void getFileUpload(DataFetchingEnvironment environment) {
    FileUpload file = environment.getArgument("myFile");
  }

  private GraphQL setupGraphQLJava() {
    return null;
  }

  static class Link {
  }

  public void completionStageDataFetcher() {
    DataFetcher<CompletionStage<List<Link>>> dataFetcher = environment -> {
      Future<List<Link>> future = retrieveLinksFromBackend(environment);
      return future.toCompletionStage();
    };

    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
      .type("Query", builder -> builder.dataFetcher("allLinks", dataFetcher))
      .build();
  }

  private Future<List<Link>> retrieveLinksFromBackend(DataFetchingEnvironment environment) {
    return null;
  }

  public void vertxFutureAdapter(GraphQL.Builder graphQLBuilder) {
    graphQLBuilder.instrumentation(VertxFutureAdapter.create());
  }

  public void futureDataFetcher() {
    DataFetcher<Future<List<Link>>> dataFetcher = environment -> {
      Future<List<Link>> future = retrieveLinksFromBackend(environment);
      return future;
    };
  }

  public void routingContextInDataFetchingEnvironment() {
    DataFetcher<CompletionStage<List<Link>>> dataFetcher = environment -> {

      RoutingContext routingContext = environment.getGraphQlContext().get(RoutingContext.class);

      User user = routingContext.user();

      Future<List<Link>> future = retrieveLinksPostedBy(user);
      return future.toCompletionStage();

    };
  }

  private Future<List<Link>> retrieveLinksPostedBy(User user) {
    return null;
  }

  public void jsonData() {
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

    builder.wiringFactory(new WiringFactory() {

      @Override
      public DataFetcher<Object> getDefaultDataFetcher(FieldWiringEnvironment environment) {

        return VertxPropertyDataFetcher.create(environment.getFieldDefinition().getName());

      }
    });
  }

  public void jsonObjectAdapter(GraphQL.Builder graphQLBuilder) {
    graphQLBuilder.instrumentation(new JsonObjectAdapter());
  }

  public void createBatchLoader() {
    BatchLoaderWithContext<String, Link> linksBatchLoader = (ids, env) -> {
      // retrieveLinksFromBackend takes a list of ids and returns a CompletionStage for a list of links
      return retrieveLinksFromBackend(ids, env);
    };
  }

  private CompletionStage<List<Link>> retrieveLinksFromBackend(List<String> ids, BatchLoaderEnvironment environment) {
    return null;
  }

  public void dataLoaderRegistry(GraphQL graphQL, BatchLoaderWithContext<String, Link> linksBatchLoader) {
    GraphQLHandler handler = GraphQLHandler.create(graphQL).beforeExecute(builderWithContext -> {

      DataLoader<String, Link> linkDataLoader = DataLoaderFactory.newDataLoader(linksBatchLoader);

      DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry().register("link", linkDataLoader);

      builderWithContext.builder().dataLoaderRegistry(dataLoaderRegistry);

    });
  }

  private Future<List<String>> findComments(List<Long> ids, BatchLoaderEnvironment env) {
    return null;
  }

  public void addApolloWsHandlerToRouter(Router router) {
    GraphQL graphQL = setupGraphQLJava();

    router.route("/graphql").handler(ApolloWSHandler.create(graphQL));
  }

  public void configureServerForApolloWs(Vertx vertx, Router router) {
    HttpServerOptions httpServerOptions = new HttpServerOptions()
      .addWebSocketSubProtocol("graphql-ws");
    vertx.createHttpServer(httpServerOptions)
      .requestHandler(router)
      .listen(8080);
  }

  public void configureWebSocketLinkAndHttpLinkSamePath(Router router) {
    GraphQL graphQL = setupGraphQLJava();

    router.route("/graphql").handler(ApolloWSHandler.create(graphQL));
    router.route("/graphql").handler(GraphQLHandler.create(graphQL));
  }

  public void addGraphQLWSHandlerToRouter(Router router, GraphQL graphQL) {
    router.route("/graphql").handler(GraphQLWSHandler.builder(graphQL).build());
  }

  public void configureServerForGraphQLWS() {
    HttpServerOptions httpServerOptions = new HttpServerOptions()
      .addWebSocketSubProtocol("graphql-transport-ws");
  }

  public void configureGraphQLWSAndHttpOnSamePath(Router router, GraphQL graphQL) {
    router.route("/graphql")
      .handler(GraphQLWSHandler.builder(graphQL).build())
      .handler(GraphQLHandler.create(graphQL));
  }
}
