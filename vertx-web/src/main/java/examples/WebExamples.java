package examples;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.auth.otp.totp.TotpAuth;
import io.vertx.ext.auth.webauthn.*;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.*;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

import java.util.List;
import java.util.function.Function;

/**
 * These are the examples used in the documentation.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@SuppressWarnings("unused")
public class WebExamples {

  public void example1(Vertx vertx) {
    HttpServer server = vertx.createHttpServer();

    server.requestHandler(request -> {

      // This handler gets called for each request that arrives on the server
      HttpServerResponse response = request.response();
      response.putHeader("content-type", "text/plain");

      // Write to the response and end it
      response.end("Hello World!");
    });

    server.listen(8080);
  }

  public void example2(Vertx vertx) {
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    router.route().handler(ctx -> {

      // This handler will be called for every request
      HttpServerResponse response = ctx.response();
      response.putHeader("content-type", "text/plain");

      // Write to the response and end it
      response.end("Hello World from Vert.x-Web!");
    });

    server.requestHandler(router).listen(8080);

  }

  public void example3(Router router) {

    Route route = router.route().path("/some/path/");

    route.handler(ctx -> {
      // This handler will be called for the following request paths:

      // `/some/path/`
      // `/some/path//`
      //
      // but not:
      // `/some/path` the end slash in the path makes it strict
      // `/some/path/subdir`
    });

    // paths that do not end with slash are not strict
    // this means that the trailing slash is optional
    // and they match regardless
    Route route2 = router.route().path("/some/path");

    route2.handler(ctx -> {
      // This handler will be called for the following request paths:

      // `/some/path`
      // `/some/path/`
      // `/some/path//`
      //
      // but not:
      // `/some/path/subdir`
    });


  }

  public void example3_1(Router router) {

    Route route = router.route().path("/some/path/*");

    route.handler(ctx -> {
      // This handler will be called for any path that starts with
      // `/some/path/`, e.g.

      // `/some/path/`
      // `/some/path/subdir`
      // `/some/path/subdir/blah.html`
      //
      // but **NOT**:
      // `/some/path` the final slash is never optional with a wildcard to
      //              handle sub routing and composition without risking wrong
      //              configuration like bellow:
      // `/some/patha`
      // `/some/patha/`
      // etc...
    });

  }

  public void example4(Router router) {

    Route route = router.route("/some/path/*");

    route.handler(ctx -> {
      // This handler will be called same as previous example
    });

  }

  public void example4_1(Router router) {

    router
      .route(HttpMethod.POST, "/catalogue/products/:productType/:productID/")
      .handler(ctx -> {

        String productType = ctx.pathParam("productType");
        String productID = ctx.pathParam("productID");

        // Do something with them...
      });

  }

  public void example4_2(Router router) {

    router
      .route(HttpMethod.GET, "/flights/:from-:to")
      .handler(ctx -> {
        // when handling requests to /flights/AMS-SFO will set:
        String from = ctx.pathParam("from"); // AMS
        String to = ctx.pathParam("to"); // SFO
        // remember that this will not work as expected when the parameter
        // naming pattern in use is not the "extended" one. That is because in
        // that case "-" is considered to be part of the variable name and
        // not a separator.
      });

  }

  public void example5(Router router) {

    // Matches any path ending with 'foo'
    Route route = router.route().pathRegex(".*foo");

    route.handler(ctx -> {

      // This handler will be called for:

      // /some/path/foo
      // /foo
      // /foo/bar/wibble/foo
      // /bar/foo

      // But not:
      // /bar/wibble
    });

  }

  public void example6(Router router) {

    Route route = router.routeWithRegex(".*foo");

    route.handler(ctx -> {

      // This handler will be called same as previous example

    });

  }

  public void example6_1(Router router) {

    Route route = router.routeWithRegex(".*foo");

    // This regular expression matches paths that start with something like:
    // "/foo/bar" - where the "foo" is captured into param0 and the "bar" is
    // captured into param1
    route.pathRegex("\\/([^\\/]+)\\/([^\\/]+)").handler(ctx -> {

      String productType = ctx.pathParam("param0");
      String productID = ctx.pathParam("param1");

      // Do something with them...
    });

  }

  public void example6_2(Router router) {

    // This regular expression matches paths that start with
    // something like: "/foo/bar". It uses named regex groups
    // to capture path params
    router
      .routeWithRegex("\\/(?<productType>[^\\/]+)\\/(?<productID>[^\\/]+)")
      .handler(ctx -> {

        String productType = ctx.pathParam("productType");
        String productID = ctx.pathParam("productID");

        // Do something with them...
      });

  }

  public void example7(Router router) {

    Route route = router.route().method(HttpMethod.POST);

    route.handler(ctx -> {

      // This handler will be called for any POST request

    });

  }

  public void example8(Router router) {

    Route route = router.route(HttpMethod.POST, "/some/path/");

    route.handler(ctx -> {
      // This handler will be called for any POST request
      // to a URI path starting with /some/path/
    });

  }

  public void example8_1(Router router) {

    router.get().handler(ctx -> {

      // Will be called for any GET request

    });

    router.get("/some/path/").handler(ctx -> {

      // Will be called for any GET request to a path
      // starting with /some/path

    });

    router.getWithRegex(".*foo").handler(ctx -> {

      // Will be called for any GET request to a path
      // ending with `foo`

    });

    // There are also equivalents to the above for:
    // PUT, POST, DELETE, HEAD and OPTIONS

  }

  public void example9(Router router) {

    Route route = router.route().method(HttpMethod.POST).method(HttpMethod.PUT);

    route.handler(ctx -> {

      // This handler will be called for any POST or PUT request

    });

  }

  public void example9_1(Router router) {

    Route route = router.route()
      .method(HttpMethod.valueOf("MKCOL"))
      .handler(ctx -> {
        // This handler will be called for any MKCOL request
      });

  }

  public void example10(Router router) {

    router
      .route("/some/path/")
      .handler(ctx -> {

        HttpServerResponse response = ctx.response();
        // enable chunked responses because we will be adding data as
        // we execute over other handlers. This is only required once and
        // only if several handlers do output.
        response.setChunked(true);

        response.write("route1\n");

        // Now call the next matching route
        ctx.next();
      });

    router
      .route("/some/path/")
      .handler(ctx -> {

        HttpServerResponse response = ctx.response();
        response.write("route2\n");

        // Now call the next matching route
        ctx.next();
      });

    router
      .route("/some/path/")
      .handler(ctx -> {

        HttpServerResponse response = ctx.response();
        response.write("route3");

        // Now end the response
        ctx.response().end();
      });

  }

  public void example11(Router router) {

    router
      .route("/some/path/")
      .order(1)
      .handler(ctx -> {

        HttpServerResponse response = ctx.response();
        response.write("route1\n");

        // Now call the next matching route
        ctx.next();
      });

    router
      .route("/some/path/")
      .order(0)
      .handler(ctx -> {

        HttpServerResponse response = ctx.response();
        // enable chunked responses because we will be adding data as
        // we execute over other handlers. This is only required once and
        // only if several handlers do output.
        response.setChunked(true);

        response.write("route2\n");

        // Now call the next matching route
        ctx.next();
      });

    router
      .route("/some/path/")
      .order(2)
      .handler(ctx -> {

        HttpServerResponse response = ctx.response();
        response.write("route3");

        // Now end the response
        ctx.response().end();
      });
  }

  public void example12(Router router) {

    // Exact match
    router.route()
      .consumes("text/html")
      .handler(ctx -> {

        // This handler will be called for any request with
        // content-type header set to `text/html`

      });
  }

  public void example13(Router router) {

    // Multiple exact matches
    router.route()
      .consumes("text/html")
      .consumes("text/plain")
      .handler(ctx -> {

        // This handler will be called for any request with
        // content-type header set to `text/html` or `text/plain`.

      });
  }

  public void example14(Router router) {

    // Sub-type wildcard match
    router.route()
      .consumes("text/*")
      .handler(ctx -> {

        // This handler will be called for any request
        // with top level type `text` e.g. content-type
        // header set to `text/html` or `text/plain`
        // will both match

      });
  }

  public void example15(Router router) {

    // Top level type wildcard match
    router.route()
      .consumes("*/json")
      .handler(ctx -> {

        // This handler will be called for any request with sub-type json
        // e.g. content-type header set to `text/json` or
        // `application/json` will both match

      });
  }

  public void example16(Router router, String someJSON) {

    router.route()
      .produces("application/json")
      .handler(ctx -> {

        HttpServerResponse response = ctx.response();
        response.putHeader("content-type", "application/json");
        response.end(someJSON);

      });
  }

  public void example17(Router router, String whatever) {

    // This route can produce two different MIME types
    router.route()
      .produces("application/json")
      .produces("text/html")
      .handler(ctx -> {

        HttpServerResponse response = ctx.response();

        // Get the actual MIME type acceptable
        String acceptableContentType = ctx.getAcceptableContentType();

        response.putHeader("content-type", acceptableContentType);
        response.end(whatever);
      });
  }

  public void example18(Router router) {

    router.route(HttpMethod.PUT, "myapi/orders")
      .consumes("application/json")
      .produces("application/json")
      .handler(ctx -> {

        // This would be match for any PUT method to paths starting
        // with "myapi/orders" with a content-type of "application/json"
        // and an accept header matching "application/json"

      });

  }

  public void example20(Router router) {

    Route route = router.route("/some/path/");
    route.handler(ctx -> {

      HttpServerResponse response = ctx.response();
      // enable chunked responses because we will be adding data as
      // we execute over other handlers. This is only required once and
      // only if several handlers do output.
      response.setChunked(true);

      response.write("route1\n");

      // Call the next matching route after a 5 second delay
      ctx.vertx().setTimer(5000, tid -> ctx.next());
    });

    route.handler(ctx -> {

      HttpServerResponse response = ctx.response();
      response.write("route2\n");

      // Call the next matching route after a 5 second delay
      ctx.vertx().setTimer(5000, tid -> ctx.next());
    });

    route.handler(ctx -> {

      HttpServerResponse response = ctx.response();
      response.write("route3");

      // Now end the response
      ctx.response().end();
    });

  }

  public void example20_1(Router router, SomeLegacyService service) {

    router.route().blockingHandler(ctx -> {

      // Do something that might take some time synchronously
      service.doSomethingThatBlocks();

      // Now call the next handler
      ctx.next();

    });
  }

  public void example20_2(Router router) {
    router.post("/some/endpoint").handler(ctx -> {
      ctx.request().setExpectMultipart(true);
      ctx.next();
    }).blockingHandler(ctx -> {
      // ... Do some blocking operation
    });
  }

  interface SomeLegacyService {

    void doSomethingThatBlocks();
  }

  public void example21(Router router) {

    router.get("/some/path").handler(ctx -> {

      ctx.put("foo", "bar");
      ctx.next();

    });

    router.get("/some/path/other").handler(ctx -> {

      String bar = ctx.get("foo");
      // Do something with bar
      ctx.response().end();

    });

  }

  public void example22(Vertx vertx, String productJSON) {

    Router restAPI = Router.router(vertx);

    restAPI.get("/products/:productID").handler(ctx -> {

      // TODO Handle the lookup of the product....
      ctx.response().write(productJSON);

    });

    restAPI.put("/products/:productID").handler(ctx -> {

      // TODO Add a new product...
      ctx.response().end();

    });

    restAPI.delete("/products/:productID").handler(ctx -> {

      // TODO delete the product...
      ctx.response().end();

    });
  }

  public void example23(Vertx vertx, Handler<RoutingContext> myStaticHandler, Handler<RoutingContext> myTemplateHandler) {
    Router mainRouter = Router.router(vertx);

    // Handle static resources
    mainRouter.route("/static/*").handler(myStaticHandler);

    mainRouter.route(".*\\.templ").handler(myTemplateHandler);
  }

  public void example24(Router mainRouter, Router restAPI) {

    mainRouter.route("/productsAPI/*")
      .subRouter(restAPI);
  }

  public void example25(Router router) {

    Route route = router.get("/somepath/*");

    route.failureHandler(ctx -> {

      // This will be called for failures that occur
      // when routing requests to paths starting with
      // '/somepath/'

    });
  }

  public void example26(Router router) {

    Route route1 = router.get("/somepath/path1/");

    route1.handler(ctx -> {

      // Let's say this throws a RuntimeException
      throw new RuntimeException("something happened!");

    });

    Route route2 = router.get("/somepath/path2");

    route2.handler(ctx -> {

      // This one deliberately fails the request passing in the status code
      // E.g. 403 - Forbidden
      ctx.fail(403);

    });

    // Define a failure handler
    // This will get called for any failures in the above handlers
    Route route3 = router.get("/somepath/*");

    route3.failureHandler(failureRoutingContext -> {

      int statusCode = failureRoutingContext.statusCode();

      // Status code will be 500 for the RuntimeException
      // or 403 for the other failure
      HttpServerResponse response = failureRoutingContext.response();
      response.setStatusCode(statusCode).end("Sorry! Not today");

    });

  }

  public void example27(Router router) {

    // This body handler will be called for all routes
    router.route().handler(BodyHandler.create());

  }

  public void example28(Router router) {

    router.route().handler(BodyHandler.create());

    router.post("/some/path/uploads").handler(ctx -> {

      List<FileUpload> uploads = ctx.fileUploads();
      // Do something with uploads....

    });
  }

  public void example30(RoutingContext ctx) {

    Cookie someCookie = ctx.request().getCookie("mycookie");
    String cookieValue = someCookie.getValue();

    // Do something with cookie...

    // Add a cookie - this will get written back in the response automatically
    ctx.response().addCookie(Cookie.cookie("othercookie", "somevalue"));
  }

  public void example31(Vertx vertx) {

    // Create a local session store using defaults
    SessionStore store1 = LocalSessionStore.create(vertx);

    // Create a local session store specifying the local shared map name to use
    // This might be useful if you have more than one application in the same
    // Vert.x instance and want to use different maps for different applications
    SessionStore store2 = LocalSessionStore.create(
      vertx,
      "myapp3.sessionmap");

    // Create a local session store specifying the local shared map name to use and
    // setting the reaper interval for expired sessions to 10 seconds
    SessionStore store3 = LocalSessionStore.create(
      vertx,
      "myapp3.sessionmap",
      10000);

  }

  public void example32() {

    // a clustered Vert.x
    Vertx.clusteredVertx(new VertxOptions())
      .onSuccess(vertx -> {
      // Create a clustered session store using defaults
      SessionStore store1 = ClusteredSessionStore.create(vertx);

      // Create a clustered session store specifying the distributed map name to use
      // This might be useful if you have more than one application in the cluster
      // and want to use different maps for different applications
      SessionStore store2 = ClusteredSessionStore.create(
        vertx,
        "myclusteredapp3.sessionmap");
    });

  }

  public void example33(Vertx vertx) {

    Router router = Router.router(vertx);

    // Create a clustered session store using defaults
    SessionStore store = ClusteredSessionStore.create(vertx);

    SessionHandler sessionHandler = SessionHandler.create(store);

    // the session handler controls the cookie used for the session
    // this includes configuring, for example, the same site policy
    // like this, for strict same site policy.
    sessionHandler.setCookieSameSite(CookieSameSite.STRICT);

    // Make sure all requests are routed through the session handler too
    router.route().handler(sessionHandler);

    // Now your application handlers
    router.route("/somepath/blah/").handler(ctx -> {

      Session session = ctx.session();
      session.put("foo", "bar");
      // etc

    });

  }

  public void example34(SessionHandler sessionHandler, Router router) {

    router.route().handler(sessionHandler);

    // Now your application handlers
    router.route("/somepath/blah").handler(ctx -> {

      Session session = ctx.session();

      // Put some data from the session
      session.put("foo", "bar");

      // Retrieve some data from a session
      int age = session.get("age");

      // Remove some data from a session
      JsonObject obj = session.remove("myobj");

    });

  }


  public void example37(Vertx vertx, AuthenticationProvider authProvider, Router router) {

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

    WebAuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);
  }

  public void example38(Vertx vertx, AuthenticationProvider authProvider, Router router) {

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

    WebAuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);

    // All requests to paths starting with '/private/' will be protected
    router.route("/private/*").handler(basicAuthHandler);

    router.route("/someotherpath").handler(ctx -> {

      // This will be public access - no login required

    });

    router.route("/private/somepath").handler(ctx -> {

      // This will require a login

      // This will have the value true
      boolean isAuthenticated = ctx.user().authenticated();

    });
  }

  public void example39(Vertx vertx, AuthenticationProvider authProvider, Router router) {

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

    // All requests to paths starting with '/private/' will be protected
    router
      .route("/private/*")
      .handler(RedirectAuthHandler.create(authProvider));

    // Handle the actual login
    // One of your pages must POST form login data
    router.post("/login").handler(FormLoginHandler.create(authProvider));

    // Set a static server to serve static resources, e.g. the login page
    router.route().handler(StaticHandler.create());

    router
      .route("/someotherpath")
      .handler(ctx -> {
        // This will be public access - no login required
      });

    router
      .route("/private/somepath")
      .handler(ctx -> {

        // This will require a login

        // This will have the value true
        boolean isAuthenticated = ctx.user().authenticated();

      });

  }

  public void example40_a(AuthorizationProvider authProvider, Router router) {
    // attest that all requests on the route match the required authorization
    router.route().handler(
      // create the handler that will perform the attestation
      AuthorizationHandler.create(
          // what to attest
          PermissionBasedAuthorization.create("can-do-work"))
        // where to lookup the authorizations for the user
        .addAuthorizationProvider(authProvider));
  }


  public void example40(AuthorizationProvider authProvider, Router router) {
    // Need "list_products" authorization to list products
    router.route("/listproducts/*").handler(
      // create the handler that will perform the attestation
      AuthorizationHandler.create(
          // what to attest
          PermissionBasedAuthorization.create("list_products"))
        // where to lookup the authorizations for the user
        .addAuthorizationProvider(authProvider));

    // Only "admin" has access to /private/settings
    router.route("/private/settings/*").handler(
      // create the handler that will perform the attestation
      AuthorizationHandler.create(
          // what to attest
          RoleBasedAuthorization.create("admin"))
        .addAuthorizationProvider(authProvider));
  }

  public void example41(Router router) {

    router.route("/static/*").handler(StaticHandler.create());

  }

  public void example41_0_1(Router router) {

    // Will only accept GET requests from origin "vertx.io"
    router.route()
      .handler(
        CorsHandler.create()
          .addRelativeOrigin("vertx\\.io")
          .allowedMethod(HttpMethod.GET));

    router.route().handler(ctx -> {

      // Your app handlers

    });
  }

  public void example41_2(Router router, TemplateEngine engine) {

    TemplateHandler handler = TemplateHandler.create(engine);

    router.get("/dynamic").handler(ctx -> {

      ctx.put("request_path", ctx.request().path());
      ctx.put("session_data", ctx.session().data());

      ctx.next();
    });

    router.get("/dynamic/").handler(handler);

  }

  public void example41_3(Vertx vertx, Router router) {

    // Any errors on paths beginning with '/somepath/' will
    // be handled by this error handler
    router.route("/somepath/").failureHandler(ErrorHandler.create(vertx));

  }


  public void example42(Router router) {

    router.route("/foo/").handler(TimeoutHandler.create(5000));

  }

  public void example43(Vertx vertx) {

    Router router = Router.router(vertx);

    SockJSHandlerOptions options = new SockJSHandlerOptions()
      .setHeartbeatInterval(2000);

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);

  }

  public void example44(Vertx vertx) {

    Router router = Router.router(vertx);

    SockJSHandlerOptions options = new SockJSHandlerOptions()
      .setHeartbeatInterval(2000);

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);

    router.route("/myapp/*")
      .subRouter(sockJSHandler.socketHandler(sockJSSocket -> {

        // Just echo the data back
        sockJSSocket.handler(sockJSSocket::write);

      }));

  }

  public void sockJsWriteHandler(Vertx vertx) {
    Router router = Router.router(vertx);

    SockJSHandlerOptions options = new SockJSHandlerOptions()
      .setRegisterWriteHandler(true);

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);

    router.route("/myapp/*")
      .subRouter(sockJSHandler.socketHandler(sockJSSocket -> {

        // Retrieve the writeHandlerID and store it (e.g. in a local map)
        String writeHandlerID = sockJSSocket.writeHandlerID();

      }));

  }

  public void sockJsSendBufferEventBus(EventBus eventBus, String writeHandlerID) {

    // Send buffers directly to the SockJSHandler socket

    eventBus.send(writeHandlerID, Buffer.buffer("foo"));

  }


  public void example45(Vertx vertx) {

    Router router = Router.router(vertx);

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    SockJSBridgeOptions options = new SockJSBridgeOptions();
    // mount the bridge on the router
    router
      .route("/eventbus/*")
      .subRouter(sockJSHandler.bridge(options));
  }

  public void example46(Vertx vertx) {

    Router router = Router.router(vertx);

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);


    // Let through any messages sent to 'demo.orderMgr' from the client
    PermittedOptions inboundPermitted1 = new PermittedOptions()
      .setAddress("demo.orderMgr");

    // Allow calls to the address 'demo.persistor' from the client as
    // long as the messages have an action field with value 'find'
    // and a collection field with value 'albums'
    PermittedOptions inboundPermitted2 = new PermittedOptions()
      .setAddress("demo.persistor")
      .setMatch(new JsonObject().put("action", "find")
        .put("collection", "albums"));

    // Allow through any message with a field `wibble` with value `foo`.
    PermittedOptions inboundPermitted3 = new PermittedOptions()
      .setMatch(new JsonObject().put("wibble", "foo"));

    // First let's define what we're going to allow from server -> client

    // Let through any messages coming from address 'ticker.mystock'
    PermittedOptions outboundPermitted1 = new PermittedOptions()
      .setAddress("ticker.mystock");

    // Let through any messages from addresses starting with "news."
    // (e.g. news.europe, news.usa, etc)
    PermittedOptions outboundPermitted2 = new PermittedOptions()
      .setAddressRegex("news\\..+");

    // Let's define what we're going to allow from client -> server
    SockJSBridgeOptions options = new SockJSBridgeOptions().
      addInboundPermitted(inboundPermitted1).
      addInboundPermitted(inboundPermitted1).
      addInboundPermitted(inboundPermitted3).
      addOutboundPermitted(outboundPermitted1).
      addOutboundPermitted(outboundPermitted2);

    // mount the bridge on the router
    router
      .route("/eventbus/*")
      .subRouter(sockJSHandler.bridge(options));
  }

  public void example47() {

    // Let through any messages sent to 'demo.orderService' from the client
    PermittedOptions inboundPermitted = new PermittedOptions()
      .setAddress("demo.orderService");

    // But only if the user is logged in and has the authority "place_orders"
    inboundPermitted.setRequiredAuthority("place_orders");

    SockJSBridgeOptions options = new SockJSBridgeOptions()
      .addInboundPermitted(inboundPermitted);
  }

  public void example48(Vertx vertx, AuthenticationProvider authProvider) {

    Router router = Router.router(vertx);

    // Let through any messages sent to 'demo.orderService' from the client
    PermittedOptions inboundPermitted = new PermittedOptions()
      .setAddress("demo.orderService");

    // But only if the user is logged in and has the authority "place_orders"
    inboundPermitted.setRequiredAuthority("place_orders");

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);

    // Now set up some basic auth handling:

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

    WebAuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);

    router.route("/eventbus/*").handler(basicAuthHandler);

    // mount the bridge on the router
    router
      .route("/eventbus/*")
      .subRouter(sockJSHandler.bridge(new SockJSBridgeOptions()
        .addInboundPermitted(inboundPermitted)));
  }

  public void example48_1(Vertx vertx) {

    Router router = Router.router(vertx);

    // Let through any messages sent to 'demo.orderService' from the client
    PermittedOptions inboundPermitted = new PermittedOptions()
      .setAddress("demo.orderService");

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    SockJSBridgeOptions options = new SockJSBridgeOptions()
      .addInboundPermitted(inboundPermitted);

    // mount the bridge on the router
    router
      .route("/eventbus/*")
      .subRouter(sockJSHandler.bridge(options, be -> {
        if (
          be.type() == BridgeEventType.PUBLISH ||
            be.type() == BridgeEventType.SEND) {

            // Add some headers
            JsonObject headers = new JsonObject()
              .put("header1", "val")
              .put("header2", "val2");

            JsonObject rawMessage = be.getRawMessage();
            rawMessage.put("headers", headers);
            be.setRawMessage(rawMessage);
          }
          be.complete(true);
        }));
  }

  public void example49(Vertx vertx) {

    Router router = Router.router(vertx);

    // Let through any messages sent to 'demo.orderMgr' from the client
    PermittedOptions inboundPermitted = new PermittedOptions()
      .setAddress("demo.someService");

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    SockJSBridgeOptions options = new SockJSBridgeOptions()
      .addInboundPermitted(inboundPermitted);

    // mount the bridge on the router
    router
      .route("/eventbus/*")
      .subRouter(sockJSHandler
        .bridge(options, be -> {
          if (be.type() == BridgeEventType.PUBLISH ||
            be.type() == BridgeEventType.RECEIVE) {

              if (be.getRawMessage().getString("body").equals("armadillos")) {
                // Reject it
                be.complete(false);
                return;
              }
            }
            be.complete(true);
          }));
  }

  public void handleSocketIdle(Vertx vertx, PermittedOptions inboundPermitted) {
    Router router = Router.router(vertx);

    // Initialize SockJSHandler handler
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    SockJSBridgeOptions options = new SockJSBridgeOptions()
      .addInboundPermitted(inboundPermitted)
      .setPingTimeout(5000);

    // mount the bridge on the router
    router
      .route("/eventbus/*")
      .subRouter(sockJSHandler.bridge(options, be -> {
        if (be.type() == BridgeEventType.SOCKET_IDLE) {
          // Do some custom handling...
        }

          be.complete(true);
        }));
  }

  public void example50(Vertx vertx) {

    Router router = Router.router(vertx);

    JWTAuthOptions authConfig = new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setType("jceks")
        .setPath("keystore.jceks")
        .setPassword("secret"));

    JWTAuth jwt = JWTAuth.create(vertx, authConfig);

    router.route("/login").handler(ctx -> {
      // this is an example, authentication should be done with another provider...
      if (
        "paulo".equals(ctx.request().getParam("username")) &&
          "secret".equals(ctx.request().getParam("password"))) {
        ctx.response()
          .end(jwt.generateToken(new JsonObject().put("sub", "paulo")));
      } else {
        ctx.fail(401);
      }
    });
  }

  public void example51(Vertx vertx) {

    Router router = Router.router(vertx);

    JWTAuthOptions authConfig = new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setType("jceks")
        .setPath("keystore.jceks")
        .setPassword("secret"));

    JWTAuth authProvider = JWTAuth.create(vertx, authConfig);

    router.route("/protected/*").handler(JWTAuthHandler.create(authProvider));

    router.route("/protected/somepage").handler(ctx -> {
      // some handle code...
    });
  }

  public void example52(Vertx vertx) {

    JWTAuthOptions authConfig = new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setType("jceks")
        .setPath("keystore.jceks")
        .setPassword("secret"));

    JWTAuth authProvider = JWTAuth.create(vertx, authConfig);

    authProvider
      .generateToken(
        new JsonObject()
          .put("sub", "paulo")
          .put("someKey", "some value"),
        new JWTOptions());
  }

  public void example53(Vertx vertx) {

    Handler<RoutingContext> handler = ctx -> {
      String theSubject = ctx.user().get().principal().getString("sub");
      String someKey = ctx.user().get().principal().getString("someKey");
    };
  }

  public void example54(Vertx vertx, Router router) {

    router.route().handler(CSRFHandler.create(vertx, "abracadabra"));
    router.route().handler(ctx -> {

    });
  }

  public void example55(Router router) {

    router.get("/some/path").handler(ctx -> {

      ctx.put("foo", "bar");
      ctx.next();

    });

    router
      .get("/some/path/B")
      .handler(ctx -> ctx.response().end());

    router
      .get("/some/path")
      .handler(ctx -> ctx.reroute("/some/path/B"));

  }

  public void example55b(Router router) {

    router.get("/my-pretty-notfound-handler").handler(ctx -> ctx.response()
      .setStatusCode(404)
      .end("NOT FOUND fancy html here!!!"));

    router.get().failureHandler(ctx -> {
      if (ctx.statusCode() == 404) {
        ctx.reroute("/my-pretty-notfound-handler");
      } else {
        ctx.next();
      }
    });
  }

  public void example55c(Router router) {

    router.get("/final-target").handler(ctx -> {
      // continue from here...
    });

    // (Will reroute to /final-target including the query string)
    router.get().handler(ctx -> ctx.reroute("/final-target?variable=value"));

    // A safer way would be to add the variable to the context
    router.get().handler(ctx -> ctx
      .put("variable", "value")
      .reroute("/final-target"));
  }

  public void example56(Router router) {
    router.route().virtualHost("*.vertx.io").handler(ctx -> {
      // do something if the request is for *.vertx.io
    });
  }

  public void example57(Router router) {

    Route route = router.get("/localized").handler(ctx -> {
      // although it might seem strange by running a loop with a switch we
      // make sure that the locale order of preference is preserved when
      // replying in the users language.
      for (LanguageHeader language : ctx.acceptableLanguages()) {
        switch (language.tag()) {
          case "en":
            ctx.response().end("Hello!");
            return;
          case "fr":
            ctx.response().end("Bonjour!");
            return;
          case "pt":
            ctx.response().end("Olá!");
            return;
          case "es":
            ctx.response().end("Hola!");
            return;
        }
      }
      // we do not know the user language so lets just inform that back:
      ctx.response().end("Sorry we don't speak: " + ctx.preferredLanguage());
    });
  }

  public void example58(Vertx vertx, Router router) {

    // create an OAuth2 provider, clientID and clientSecret
    // should be requested to github
    OAuth2Auth authProvider = GithubAuth
      .create(vertx, "CLIENT_ID", "CLIENT_SECRET");

    // create a oauth2 handler on our running server
    // the second argument is the full url to the
    // callback as you entered in your provider management console.
    OAuth2AuthHandler oauth2 = OAuth2AuthHandler
      .create(vertx, authProvider, "https://myserver.com/callback");

    // setup the callback handler for receiving the GitHub callback
    oauth2.setupCallback(router.route("/callback"));

    // protect everything under /protected
    router.route("/protected/*").handler(oauth2);
    // mount some handler under the protected zone
    router
      .route("/protected/somepage")
      .handler(ctx -> ctx.response().end("Welcome to the protected resource!"));

    // welcome page
    router
      .get("/")
      .handler(ctx -> ctx.response()
        .putHeader("content-type", "text/html")
        .end("Hello<br><a href=\"/protected/somepage\">Protected by Github</a>"));
  }

  public void example59(Vertx vertx, Router router) {

    // create an OAuth2 provider, clientID and clientSecret
    // should be requested to Google
    OAuth2Auth authProvider = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("CLIENT_ID")
      .setClientSecret("CLIENT_SECRET")
      .setSite("https://accounts.google.com")
      .setTokenPath("https://www.googleapis.com/oauth2/v3/token")
      .setAuthorizationPath("/o/oauth2/auth"));

    // create a oauth2 handler on our domain: "http://localhost:8080"
    OAuth2AuthHandler oauth2 = OAuth2AuthHandler
      .create(vertx, authProvider, "http://localhost:8080");

    // these are the scopes
    oauth2.withScope("profile");

    // setup the callback handler for receiving the Google callback
    oauth2.setupCallback(router.get("/callback"));

    // protect everything under /protected
    router.route("/protected/*").handler(oauth2);
    // mount some handler under the protected zone
    router
      .route("/protected/somepage")
      .handler(ctx -> ctx.response().end("Welcome to the protected resource!"));

    // welcome page
    router
      .get("/")
      .handler(ctx -> ctx.response()
        .putHeader("content-type", "text/html")
        .end("Hello<br><a href=\"/protected/somepage\">Protected by Google</a>"));
  }

  public void example61(Vertx vertx, Router router, OAuth2Auth provider) {
    // create a oauth2 handler pinned to
    // myserver.com: "https://myserver.com:8447/callback"
    OAuth2AuthHandler oauth2 = OAuth2AuthHandler
      .create(vertx, provider, "https://myserver.com:8447/callback");

    // now allow the handler to setup the callback url for you
    oauth2.setupCallback(router.route("/callback"));
  }

  public void example62(Vertx vertx, Router router) {
    // To simplify the development of the web components
    // we use a Router to route all HTTP requests
    // to organize our code in a reusable way.

    // Simple auth service which uses a GitHub to
    // authenticate the user
    OAuth2Auth authProvider =
      GithubAuth
        .create(vertx, "CLIENTID", "CLIENT SECRET");
    // We need a user session handler too to make sure
    // the user is stored in the session between requests
    router.route()
      .handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    // we now protect the resource under the path "/protected"
    router.route("/protected").handler(
      OAuth2AuthHandler.create(
          vertx,
          authProvider,
          "http://localhost:8080/callback")
        // we now configure the oauth2 handler, it will
        // setup the callback handler
        // as expected by your oauth2 provider.
        .setupCallback(router.route("/callback"))
        // for this resource we require that users have
        // the authority to retrieve the user emails
        .withScope("user:email")
    );
    // Entry point to the application, this will render
    // a custom template.
    router.get("/").handler(ctx -> ctx.response()
      .putHeader("Content-Type", "text/html")
      .end(
        "<html>\n" +
          "  <body>\n" +
          "    <p>\n" +
          "      Well, hello there!\n" +
          "    </p>\n" +
          "    <p>\n" +
          "      We're going to the protected resource, if there is no\n" +
          "      user in the session we will talk to the GitHub API. Ready?\n" +
          "      <a href=\"/protected\">Click here</a> to begin!</a>\n" +
          "    </p>\n" +
          "    <p>\n" +
          "      <b>If that link doesn't work</b>, remember to provide your\n" +
          "      own <a href=\"https://github.com/settings/applications/new\">\n" +
          "      Client ID</a>!\n" +
          "    </p>\n" +
          "  </body>\n" +
          "</html>"));
    // The protected resource
    router.get("/protected").handler(ctx -> {
      // at this moment your user object should contain the info
      // from the Oauth2 response, since this is a protected resource
      // as specified above in the handler config the user object is never null
      User user = ctx.user().get();
      // just dump it to the client for demo purposes
      ctx.response().end(user.toString());
    });
  }

  public void manualContentType(Router router) {
    router
      .get("/api/books")
      .produces("application/json")
      .handler(ctx -> findBooks()
        .onSuccess(books -> ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(toJson(books))).onFailure(ctx::fail));
  }

  public void contentTypeHandler(Router router) {
    router.route("/api/*").handler(ResponseContentTypeHandler.create());
    router
      .get("/api/books")
      .produces("application/json")
      .handler(ctx -> findBooks()
        .onSuccess(books -> ctx.response()
          .end(toJson(books))).onFailure(ctx::fail));
  }

  private Future<List<Book>> findBooks() {
    throw new UnsupportedOperationException();
  }

  static class Book {
  }

  Buffer toJson(List<Book> books) {
    throw new UnsupportedOperationException();
  }

  Buffer toXML(List<Book> books) {
    throw new UnsupportedOperationException();
  }

  public void mostAcceptableContentTypeHandler(Router router) {
    router.route("/api/*").handler(ResponseContentTypeHandler.create());

    router
      .get("/api/books")
      .produces("text/xml")
      .produces("application/json")
      .handler(ctx -> findBooks()
        .onSuccess(books -> {
          if (ctx.getAcceptableContentType().equals("text/xml")) {
            ctx.response().end(toXML(books));
          } else {
            ctx.response().end(toJson(books));
          }
        })
        .onFailure(ctx::fail));
  }

  public void example63(Router router, AuthenticationProvider provider) {

    ChainAuthHandler chain = ChainAuthHandler.any();

    // add http basic auth handler to the chain
    chain.add(BasicAuthHandler.create(provider));
    // add form redirect auth handler to the chain
    chain.add(RedirectAuthHandler.create(provider));

    // secure your route
    router.route("/secure/resource").handler(chain);
    // your app
    router.route("/secure/resource").handler(ctx -> {
      // do something...
    });
  }

  public void example64(Router router) {
    router.route().handler(MethodOverrideHandler.create());

    router.route(HttpMethod.GET, "/").handler(ctx -> {
      // do GET stuff...
    });

    router.route(HttpMethod.POST, "/").handler(ctx -> {
      // do POST stuff...
    });
  }

  public void example65(Router router) {
    router.route().handler(MethodOverrideHandler.create(false));

    router.route(HttpMethod.GET, "/").handler(ctx -> {
      // do GET stuff...
    });

    router.route(HttpMethod.POST, "/").handler(ctx -> {
      // do POST stuff...
    });
  }

  public void example66(RoutingContext ctx, Buffer pdfBuffer) {
    ctx
      .attachment("weekly-report.pdf")
      .end(pdfBuffer);
  }

  public void example67(RoutingContext ctx) {

    ctx.redirect("https://securesite.com/");

    // there is a special handling for the target "back".
    // In this case the redirect would send the user to the
    // referrer url or "/" if there's no referrer.

    ctx.redirect("back");
  }

  public void example68(RoutingContext ctx, Object someObject) {
    // no need to specify the content type headers
    ctx.json(new JsonObject().put("hello", "vert.x"));
    // also applies to arrays
    ctx.json(new JsonArray().add("vertx").add("web"));
    // or any object that will be converted according
    // to the json encoder available at runtime.
    ctx.json(someObject);
  }

  public void example69(RoutingContext ctx) {
    // Check if the incoming request contains the "Content-Type"
    // get field, and it contains the give mime `type`.
    // If there is no request body, `false` is returned.
    // If there is no content type, `false` is returned.
    // Otherwise, it returns true if the `type` that matches.

    // With Content-Type: text/html; getCharset=utf-8
    ctx.is("html"); // => true
    ctx.is("text/html"); // => true

    // When Content-Type is application/json
    ctx.is("application/json"); // => true
    ctx.is("html"); // => false
  }

  public void example70(RoutingContext ctx) {
    // set the response resource meta data
    ctx.lastModified("Wed, 13 Jul 2011 18:30:00 GMT");
    // this will now be used to verify the freshness of the request
    if (ctx.isFresh()) {
      // client cache value is fresh perhaps we
      // can stop and return 304?
    }
  }

  public void example71(RoutingContext ctx, Buffer buffer) {
    // this response etag with a given value
    ctx.etag("W/123456789");

    // set the last modified value
    ctx.lastModified("Wed, 13 Jul 2011 18:30:00 GMT");

    // quickly end
    ctx.end();
    ctx.end("body");
    ctx.end(buffer);
  }

  public void example72(Router router) {
    router.route().handler(MultiTenantHandler.create("X-Tenant"));
  }

  public void example73() {
    MultiTenantHandler.create("X-Tenant")
      .addTenantHandler("tenant-A", ctx -> {
        // do something for tenant A...
      })
      .addTenantHandler("tenant-B", ctx -> {
        // do something for tenant B...
      })
      // optionally
      .addDefaultHandler(ctx -> {
        // do something when no tenant matches...
      });

  }

  public void example74(Vertx vertx, Router router) {
    // create an OAuth2 provider, clientID and clientSecret
    // should be requested to github
    OAuth2Auth gitHubAuthProvider = GithubAuth
      .create(vertx, "CLIENT_ID", "CLIENT_SECRET");

    // create a oauth2 handler on our running server
    // the second argument is the full url to the callback
    // as you entered in your provider management console.
    OAuth2AuthHandler githubOAuth2 = OAuth2AuthHandler.create(
      vertx,
      gitHubAuthProvider,
      "https://myserver.com/github-callback");

    // setup the callback handler for receiving the GitHub callback
    githubOAuth2.setupCallback(router.route("/github-callback"));

    // create an OAuth2 provider, clientID and clientSecret
    // should be requested to Google
    OAuth2Auth googleAuthProvider = OAuth2Auth.create(vertx, new OAuth2Options()
      .setClientId("CLIENT_ID")
      .setClientSecret("CLIENT_SECRET")
      .setSite("https://accounts.google.com")
      .setTokenPath("https://www.googleapis.com/oauth2/v3/token")
      .setAuthorizationPath("/o/oauth2/auth"));

    // create a oauth2 handler on our domain: "http://localhost:8080"
    OAuth2AuthHandler googleOAuth2 = OAuth2AuthHandler.create(
      vertx,
      googleAuthProvider,
      "https://myserver.com/google-callback");

    // setup the callback handler for receiving the Google callback
    googleOAuth2.setupCallback(router.route("/google-callback"));

    // At this point the 2 callbacks endpoints are registered:

    // /github-callback -> handle github Oauth2 callbacks
    // /google-callback -> handle google Oauth2 callbacks

    // As the callbacks are made by the IdPs there's no header
    // to identify the source, hence the need of custom URLs

    // However for out Application we can control it so later
    // we can add the right handler for the right tenant

    router.route().handler(
      MultiTenantHandler.create("X-Tenant")
        // tenants using github should go this way:
        .addTenantHandler("github", githubOAuth2)
        // tenants using google should go this way:
        .addTenantHandler("google", googleOAuth2)
        // all other should be forbidden
        .addDefaultHandler(ctx -> ctx.fail(401)));

    // Proceed using the router as usual.
  }

  public void example81(Router router) {

    router.route().handler(ctx -> {
      // the default key is "tenant" as defined in
      // MultiTenantHandler.TENANT but this value can be
      // modified at creation time in the factory method
      String tenant = ctx.get(MultiTenantHandler.TENANT);

      switch (tenant) {
        case "google":
          // do something for google users
          break;
        case "github":
          // so something for github users
          break;
      }
    });
  }

  public void example75(Vertx vertx, Router router, Function<Authenticator, Future<List<Authenticator>>> fetcher, Function<Authenticator, Future<Void>> updater) {
    // create the webauthn security object
    WebAuthn webAuthn = WebAuthn.create(
        vertx,
        new WebAuthnOptions()
          .setRelyingParty(new RelyingParty().setName("Vert.x WebAuthN Demo"))
          // What kind of authentication do you want? do you care?
          // # security keys
          .setAuthenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
          // # fingerprint
          .setAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM)
          .setUserVerification(UserVerification.REQUIRED))
      // where to load the credentials from?
      .authenticatorFetcher(fetcher)
      // update the state of an authenticator
      .authenticatorUpdater(updater);

    // parse the BODY
    router.post()
      .handler(BodyHandler.create());
    // add a session handler
    router.route()
      .handler(SessionHandler
        .create(LocalSessionStore.create(vertx)));

    // security handler
    WebAuthnHandler webAuthNHandler = WebAuthnHandler.create(webAuthn)
      .setOrigin("https://192.168.178.74.xip.io:8443")
      // required callback
      .setupCallback(router.post("/webauthn/response"))
      // optional register callback
      .setupCredentialsCreateCallback(router.post("/webauthn/register"))
      // optional login callback
      .setupCredentialsGetCallback(router.post("/webauthn/login"));

    // secure the remaining routes
    router.route().handler(webAuthNHandler);
  }

  public void example76(Vertx vertx, Router router) {
    // we can now allow forward header parsing
    // and in this case only the "Forward" header will be considered
    router.allowForward(AllowForwardHeaders.FORWARD);

    // we can now allow forward header parsing
    // and in this case only the "X-Forward" headers will be considered
    router.allowForward(AllowForwardHeaders.X_FORWARD);

    // we can now allow forward header parsing
    // and in this case both the "Forward" header and "X-Forward" headers
    // will be considered, yet the values from "Forward" take precedence
    // this means if case of a conflict (2 headers for the same value)
    // the "Forward" value will be taken and the "X-Forward" ignored.
    router.allowForward(AllowForwardHeaders.ALL);
  }

  public void example77(Vertx vertx, Router router) {
    // we explicitly not allow forward header parsing
    // of any kind
    router.allowForward(AllowForwardHeaders.NONE);
  }

  public void example78(Router router, WebAuthenticationHandler authNHandlerA, WebAuthenticationHandler authNHandlerB, WebAuthenticationHandler authNHandlerC) {

    // Chain will verify (A Or (B And C))
    ChainAuthHandler chain =
      ChainAuthHandler.any()
        .add(authNHandlerA)
        .add(ChainAuthHandler.all()
          .add(authNHandlerB)
          .add(authNHandlerC));

    // secure your route
    router.route("/secure/resource").handler(chain);
    // your app
    router.route("/secure/resource").handler(ctx -> {
      // do something...
    });
  }

  public void example78(Router router, SessionHandler sessionHandler) {

    router.route().handler(ctx -> sessionHandler.flush(ctx)
      .onSuccess(v -> ctx.end("Success!"))
      .onFailure(err -> {
        // session wasn't saved...
        // go for plan B
      }));
  }

  public void example79(Router router, SessionStore store) {

    router.route()
      .handler(SessionHandler.create(store).setCookieless(true));
  }

  public void example80(Router router) {

    // all responses will then include the right
    // Strict-Transport-Security header if the
    // connection is secure (using TLS/SSL, or
    // the forwarding parsing is enabled
    router.route().handler(HSTSHandler.create());
  }

  @DataObject
  static class Pojo {
  }

  public void example82(Router router) {

    router
      .get("/some/path")
      // this handler will ensure that the response is serialized to json
      // the content type is set to "application/json"
      .respond(
        ctx -> Future.succeededFuture(new JsonObject().put("hello", "world")));

    router
      .get("/some/path")
      // this handler will ensure that the Pojo is serialized to json
      // the content type is set to "application/json"
      .respond(
        ctx -> Future.succeededFuture(new Pojo()));
  }

  public void example83(Router router) {

    router
      .get("/some/path")
      .respond(
        ctx -> ctx
          .response()
          .putHeader("Content-Type", "text/plain")
          .end("hello world!"));

    router
      .get("/some/path")
      // in this case, the handler ensures that the connection is ended
      .respond(
        ctx -> ctx
          .response()
          .setChunked(true)
          .write("Write some text..."));
  }

  public void example84(Router router) {

    // all responses will then include the right
    // Content-Security-Policy header allowing sub-domain
    // sources to be fetched from the parent "trusted.com" domain
    router.route().handler(
      CSPHandler.create()
        .addDirective("default-src", "*.trusted.com"));
  }

  public void example85(Router router) {

    // all responses will then include the right
    // X-Frame-Options header with the value "DENY"
    router.route().handler(XFrameHandler.create(XFrameHandler.DENY));
  }

  public void example86(Vertx vertx, Router router, BasicAuthHandler basicAuthHandler) {

    // parse the BODY
    router.post()
      .handler(BodyHandler.create());
    // add a session handler (OTP requires state)
    router.route()
      .handler(SessionHandler
        .create(LocalSessionStore.create(vertx))
        .setCookieSameSite(CookieSameSite.STRICT));

    // add the first authentication mode, for example HTTP Basic Authentication
    router.route()
      .handler(basicAuthHandler);

    final OtpAuthHandler otp = OtpAuthHandler
      .create(TotpAuth.create()
        .authenticatorFetcher(authr -> {
          // fetch authenticators from a database
          // ...
          return Future.succeededFuture(new io.vertx.ext.auth.otp.Authenticator());
        })
        .authenticatorUpdater(authr -> {
          // update or insert authenticators from a database
          // ...
          return Future.succeededFuture();
        }));

    otp
      // the issuer for the application
      .issuer("Vert.x Demo")
      // handle code verification responses
      .verifyUrl("/verify-otp.html")
      // handle registration of authenticators
      .setupRegisterCallback(router.post("/otp/register"))
      // handle verification of authenticators
      .setupCallback(router.post("/otp/verify"));

    // secure the rest of the routes
    router.route()
      .handler(otp);

    // To view protected details, user must be authenticated and
    // using 2nd factor authentication
    router.get("/protected")
      .handler(ctx -> ctx.end("Super secret content"));
  }

  public void example87(Router router) {
    router
      .route("/metadata/route")
      .putMetadata("metadata-key", "123")
      .handler(ctx -> {
        Route route = ctx.currentRoute();
        String value = route.getMetadata("metadata-key"); // 123
        // will end the request with the value 123
        ctx.end(value);
      });
  }

  public void example89(Router router) {
    router
      .route("/high/security/route/check")
      .handler(ctx -> {
        // if the user isn't admin, we ask the user to login again as admin
        ctx
          .user()
          .loginHint("admin")
          .impersonate();
      });
  }

  public void example90(Router router) {
    router
      .route("/high/security/route/back/to/me")
      .handler(ctx -> {
        ctx
          .user()
          .restore();
      });
  }

  public void example91(Router router) {
    router
      .route("/high/security/route/refresh/me")
      .handler(ctx -> {
        ctx
          .user()
          .refresh();
      });
  }

  public void example88(Router router) {
    router.route()
      .handler(SecurityAuditLoggerHandler.create());

    // By default, nothing seems to happen when the handler is called
    // in order to get it to log, you need to set the logger:
    // io.vertx.ext.auth.audit.SecurityAudit
    // to level INFO
  }

  public void exposeHealthChecks(Router router, HealthChecks healthChecks) {
    HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(healthChecks);
    router.get("/health*").handler(healthCheckHandler);
  }

  public void registerHealthCheckProcedureDirectly(Vertx vertx, Router router) {
    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);

    // Register procedures
    // It can be done after the route registration, or even at runtime
    healthCheckHandler.register("my-procedure-name", promise -> {
      // Do the check ...
      // Upon success ...
      promise.complete(Status.OK());
      // Or, in case of failure ...
      promise.complete(Status.KO());
    });

    router.get("/health*").handler(healthCheckHandler);
  }

  public void exposeHealthChecksWithAuth(Vertx vertx, Router router, AuthenticationProvider auth) {
    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx, auth);
    router.get("/health*").handler(healthCheckHandler);
  }
}
