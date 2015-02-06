package examples;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.apex.*;
import io.vertx.ext.apex.sstore.ClusteredSessionStore;
import io.vertx.ext.apex.sstore.LocalSessionStore;
import io.vertx.ext.apex.handler.*;
import io.vertx.ext.apex.sstore.SessionStore;
import io.vertx.ext.apex.templ.HandlebarsTemplateEngine;
import io.vertx.ext.apex.templ.TemplateEngine;
import io.vertx.ext.auth.AuthService;
import io.vertx.ext.auth.PropertiesAuthRealmConstants;

import java.util.Set;

/**
 *
 * These are the examples used in the documentation.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class Examples {

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

    router.route().handler(routingContext -> {

      // This handler will be called for every request
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/plain");

      // Write to the response and end it
      response.end("Hello World from Apex!");
    });

    server.requestHandler(router::accept).listen(8080);

  }

  public void example3(Router router) {

    Route route = router.route().path("/some/path/");

    route.handler(routingContext -> {
      // This handler will be called for any request with
      // a URI path that starts with `/some/path`
    });

  }

  public void example4(Router router) {

    Route route = router.route("/some/path/");

    route.handler(routingContext -> {
      // This handler will be called same as previous example
    });

  }

  public void example4_1(Router router) {

    Route route = router.route(HttpMethod.POST, "/catalogue/products/:productype/:productid/");

    route.handler(routingContext -> {

      String productType = routingContext.request().getParam("producttype");
      String productID = routingContext.request().getParam("productid");

      // Do something with them...
    });

  }


  public void example5(Router router) {

    // Matches any path ending with 'foo'
    Route route = router.route().pathRegex(".*foo");

    route.handler(routingContext -> {

      // This handler will be called for:

      // /some/path/foo
      // /foo
      // /foo/bar/wibble/foo
      // /foo/bar

      // But not:
      // /bar/wibble
    });

  }

  public void example6(Router router) {

    Route route = router.routeWithRegex(".*foo");

    route.handler(routingContext -> {

      // This handler will be called same as previous example

    });

  }

  public void example6_1(Router router) {

    Route route = router.routeWithRegex(".*foo");

    // This regular expression matches paths that start with something like:
    // "/foo/bar" - where the "foo" is captured into param0 and the "bar" is captured into
    // param1
    route.pathRegex("\\/([^\\/]+)\\/([^\\/]+)").handler(routingContext -> {

      String productType = routingContext.request().getParam("param0");
      String productID = routingContext.request().getParam("param1");

      // Do something with them...
    });

  }

  public void example7(Router router) {

    Route route = router.route().method(HttpMethod.POST);

    route.handler(routingContext -> {

      // This handler will be called for any POST request

    });

  }

  public void example8(Router router) {

    Route route = router.route(HttpMethod.POST, "/some/path/");

    route.handler(routingContext -> {

      // This handler will be called for any POST request to a URI path starting with /some/path/

    });

  }

  public void example8_1(Router router) {

    router.get().handler(routingContext -> {

      // Will be called for any GET request

    });

    router.get("/some/path/").handler(routingContext -> {

      // Will be called for any GET request to a path
      // starting with /some/path

    });

    router.getWithRegex(".*foo").handler(routingContext -> {

      // Will be called for any GET request to a path
      // ending with `foo`

    });

    // There are also equivalents to the above for PUT, POST, DELETE, HEAD and OPTIONS

  }

  public void example9(Router router) {

    Route route = router.route().method(HttpMethod.POST).method(HttpMethod.PUT);

    route.handler(routingContext -> {

      // This handler will be called for any POST or PUT request

    });

  }

  public void example10(Router router) {

    Route route1 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route1\n");

      // Now call the next matching route
      routingContext.next();
    });

    Route route2 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route2\n");

      // Now call the next matching route
      routingContext.next();
    });

    Route route3 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route3");

      // Now end the response
      routingContext.response().end();
    });

  }

  public void example11(Router router) {

    Route route1 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route1\n");

      // Now call the next matching route
      routingContext.next();
    });

    Route route2 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route2\n");

      // Now call the next matching route
      routingContext.next();
    });

    Route route3 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route3");

      // Now end the response
      routingContext.response().end();
    });

    // Change the order of route2 so it runs before route1
    route2.order(-1);
  }

  public void example12(Router router) {

    // Exact match
    router.route().consumes("text/html").handler(routingContext -> {

      // This handler will be called for any request with
      // content-type header set to `text/html`

    });
  }

  public void example13(Router router) {

    // Multiple exact matches
    router.route().consumes("text/html").consumes("text/plain").handler(routingContext -> {

      // This handler will be called for any request with
      // content-type header set to `text/html` or `text/plain`.

    });
  }

  public void example14(Router router) {

    // Sub-type wildcard match
    router.route().consumes("text/*").handler(routingContext -> {

      // This handler will be called for any request with top level type `text`
      // e.g. content-type header set to `text/html` or `text/plain` will both match

    });
  }

  public void example15(Router router) {

    // Top level type wildcard match
    router.route().consumes("*/json").handler(routingContext -> {

      // This handler will be called for any request with sub-type json
      // e.g. content-type header set to `text/json` or `application/json` will both match

    });
  }

  public void example16(Router router, String someJSON) {

    router.route().produces("application/json").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.write(someJSON).end();

    });
  }

  public void example17(Router router, String whatever) {

    // This route can produce two different MIME types
    router.route().produces("application/json").produces("text/html").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();

      // Get the actual MIME type acceptable
      String acceptableContentType = routingContext.getAcceptableContentType();

      response.putHeader("content-type", acceptableContentType);
      response.write(whatever).end();
    });
  }

  public void example18(Router router) {

    Route route = router.route(HttpMethod.PUT, "myapi/orders")
                        .consumes("application/json")
                        .produces("application/json");

    route.handler(routingContext -> {

      // This would be match for any PUT method to paths starting with "myapi/orders" with a
      // content-type of "application/json"
      // and an accept header matching "application/json"

    });

  }

  public void example20(Router router) {

    Route route1 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route1\n");

      // Call the next matching route after a 5 second delay
      routingContext.vertx().setTimer(5000, tid -> routingContext.next());
    });

    Route route2 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route2\n");

      // Call the next matching route after a 5 second delay
      routingContext.vertx().setTimer(5000, tid ->  routingContext.next());
    });

    Route route3 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route3");

      // Now end the response
      routingContext.response().end();
    });

  }

  public void example21(Router router) {

    router.get("/some/path").handler(routingContext -> {

      routingContext.put("foo", "bar");
      routingContext.next();

    });

    router.get("/some/path/other").handler(routingContext -> {

      String bar = routingContext.get("foo");
      // Do something with bar
      routingContext.response().end();

    });

  }

  public void example22(Vertx vertx, String productJSON) {

    Router restAPI = Router.router(vertx);

    restAPI.get("/products/:productID").handler(rc -> {

      // TODO Handle the lookup of the product....
      rc.response().write(productJSON);

    });

    restAPI.put("/products/:productID").handler(rc -> {

      // TODO Add a new product...
      rc.response().end();

    });

    restAPI.delete("/products/:productID").handler(rc -> {

      // TODO delete the product...
      rc.response().end();

    });
  }

  public void example23(Vertx vertx, Handler<RoutingContext> myStaticHandler, Handler<RoutingContext> myTemplateHandler) {
    Router mainRouter = Router.router(vertx);

    // Handle static resources
    mainRouter.route("/static").handler(myStaticHandler);

    mainRouter.route(".*\\.templ").handler(myTemplateHandler);
  }

  public void example24(Router mainRouter, Router restAPI) {

    mainRouter.mountSubRouter("/productsAPI", restAPI);

  }

  public void example25(Router router) {

    Route route = router.get("/somepath/");

    route.failureHandler(frc -> {

      // This will be called for failures that occur
      // when routing requests to paths starting with
      // '/somepath/'

    });
  }

  public void example26(Router router) {

    Route route1 = router.get("/somepath/path1/");

    route1.handler(routingContext -> {

      // Let's say this throws a RuntimeException
      throw new RuntimeException("something happened!");

    });

    Route route2 = router.get("/somepath/path2");

    route2.handler(routingContext -> {

      // This one deliberately fails the request passing in the status code
      // E.g. 403 - Forbidden
      routingContext.fail(403);

    });

    // Define a failure handler
    // This will get called for any failures in the above handlers
    Route route3 = router.get("/somepath/");

    route3.failureHandler(failureRoutingContext -> {

      int statusCode = failureRoutingContext.statusCode();

      // Status code will be 500 for the RuntimeException or 403 for the other failure
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

    router.post("/some/path/uploads").handler(routingContext -> {

      Set<FileUpload> uploads = routingContext.fileUploads();
      // Do something with uploads....

    });
  }

  public void example29(Router router) {

    // This cookie handler will be called for all routes
    router.route().handler(CookieHandler.create());

  }

  public void example30(Router router) {

    // This cookie handler will be called for all routes
    router.route().handler(CookieHandler.create());

    router.route("some/path/").handler(routingContext -> {

      Cookie someCookie = routingContext.getCookie("mycookie");
      String cookieValue = someCookie.getValue();

      // Do something with cookie...

      // Add a cookie - this will get written back in the response automatically
      routingContext.addCookie(Cookie.cookie("othercookie", "somevalue"));
    });
  }

  public void example31(Vertx vertx) {

    // Create a local session store using defaults
    SessionStore store1 = LocalSessionStore.create(vertx);

    // Create a local session store specifying the local shared map name to use
    // This might be useful if you have more than one application in the same
    // Vert.x instance and want to use different maps for different applications
    SessionStore store2 = LocalSessionStore.create(vertx, "myapp3.sessionmap");

    // Create a local session store specifying the local shared map name to use and
    // setting the reaper period for expired sessions to 10 seconds
    SessionStore store3 = LocalSessionStore.create(vertx, "myapp3.sessionmap", 10000);

  }

  public void example32() {

    // a clustered Vert.x
    Vertx.clusteredVertx(new VertxOptions().setClustered(true), res -> {

      Vertx vertx = res.result();

      // Create a clustered session store using defaults
      SessionStore store1 = ClusteredSessionStore.create(vertx);

      // Create a clustered session store specifying the distributed map name to use
      // This might be useful if you have more than one application in the cluster
      // and want to use different maps for different applications
      SessionStore store2 = ClusteredSessionStore.create(vertx, "myclusteredapp3.sessionmap");
    });

  }

  public void example33(Vertx vertx) {

    Router router = Router.router(vertx);

    // We need a cookie handler first
    router.route().handler(CookieHandler.create());

    // Create a clustered session store using defaults
    SessionStore store = ClusteredSessionStore.create(vertx);

    SessionHandler sessionHandler = SessionHandler.create(store);

    // Make sure all requests are routed through the session handler too
    router.route().handler(sessionHandler);

    // Now your application handlers
    router.route("/somepath/blah/").handler(routingContext -> {

      Session session = routingContext.session();
      session.put("foo", "bar");
      // etc

    });

  }

  public void example34(SessionHandler sessionHandler, Router router) {

    router.route().handler(CookieHandler.create());
    router.route().handler(sessionHandler);

    // Now your application handlers
    router.route("/somepath/blah").handler(routingContext -> {

      Session session = routingContext.session();

      // Put some data from the session
      session.put("foo", "bar");

      // Retrieve some data from a session
      int age = session.get("age");

      // Remove some data from a session
      JsonObject obj = session.remove("myobj");

    });

  }

  public void example35(Vertx vertx) {

    // Create a simple local auth service that gets user data from properties file
    // See the AuthService documentation for how to configure the auth service

    JsonObject config = new JsonObject();
    config.put(PropertiesAuthRealmConstants.PROPERTIES_PROPS_PATH_FIELD,
               "classpath:test-auth.properties");
    AuthService authService = AuthService.create(vertx, config);

    AuthHandler basicAuthHandler = BasicAuthHandler.create(authService);
  }

  public void example36(Vertx vertx) {

    // Let's say you already have an auth service somewhere on your network listening on event bus address `acme.authservice`.

    AuthService authService = AuthService.createEventBusProxy(vertx, "acme.authservice");

    AuthHandler basicAuthHandler = BasicAuthHandler.create(authService);
  }

  public void example37(Vertx vertx, Router router) {

    router.route().handler(CookieHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

    AuthService authService = AuthService.createEventBusProxy(vertx, "acme.authservice");
    AuthHandler basicAuthHandler = BasicAuthHandler.create(authService);

  }

  public void example38(Vertx vertx, Router router) {

    router.route().handler(CookieHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

    AuthService authService = AuthService.createEventBusProxy(vertx, "acme.authservice");
    AuthHandler basicAuthHandler = BasicAuthHandler.create(authService);

    // All requests to paths starting with '/private/' will be protected
    router.route("/private/").handler(basicAuthHandler);

    router.route("/someotherpath").handler(routingContext -> {

      // This will be public access - no login required

    });

    router.route("/private/somepath").handler(routingContext -> {

      // This will require a login

      // This will have the value true
      boolean isLoggedIn = routingContext.session().isLoggedIn();

    });
  }

  public void example39(Vertx vertx, Router router) {

    router.route().handler(CookieHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

    AuthService authService = AuthService.createEventBusProxy(vertx, "acme.authservice");
    AuthHandler redirectAuthHandler = RedirectAuthHandler.create(authService);

    // All requests to paths starting with '/private/' will be protected
    router.route("/private/").handler(redirectAuthHandler);

    // Handle the actual login
    router.route("/login").handler(FormLoginHandler.create(authService));

    // Set a static server to serve static resources, e.g. the login page
    router.route().handler(StaticHandler.create());

    router.route("/someotherpath").handler(routingContext -> {
      // This will be public access - no login required
    });

    router.route("/private/somepath").handler(routingContext -> {

      // This will require a login

      // This will have the value true
      boolean isLoggedIn = routingContext.session().isLoggedIn();

    });

  }

  public void example40(AuthService authService, Router router) {

    AuthHandler managerAuthHandler = RedirectAuthHandler.create(authService);
    managerAuthHandler.addRole("manager").addRole("admin");

    // Roles "manager" and "admin" have access to /private/managers
    router.route("/private/managers").handler(managerAuthHandler);

    AuthHandler settingsAuthHandler = RedirectAuthHandler.create(authService);
    settingsAuthHandler.addRole("admin");

    // Only "admin" has access to /private/settings
    router.route("/private/settings").handler(settingsAuthHandler);

  }

  public void example41(Router router) {

    router.route("/static/").handler(StaticHandler.create());

  }
  public void example41_0_1(Router router) {

    // Will only accept GET requests from origin "vertx.io"
    router.route().handler(CorsHandler.create("vertx\\.io").allowedMethod(HttpMethod.GET));

    router.route().handler(routingContext -> {

      // Your app handlers

    });
  }

  public void example41_1(Router router) {

    TemplateEngine engine = HandlebarsTemplateEngine.create();
    TemplateHandler handler = TemplateHandler.create(engine);

    // This will route all GET requests starting with /dynamic/ to the template handler
    // E.g. /dynamic/graph.hbs will look for a template in /templates/dynamic/graph.hbs
    router.get("/dynamic/").handler(handler);

    // Route all GET requests for resource ending in .hbs to the template handler
    router.getWithRegex(".+\\.hbs").handler(handler);

  }

  public void example41_2(Router router) {

    TemplateEngine engine = HandlebarsTemplateEngine.create();
    TemplateHandler handler = TemplateHandler.create(engine);

    router.get("/dynamic").handler(routingContext -> {

      routingContext.put("request_path", routingContext.request().path());
      routingContext.put("session_data", routingContext.session().data());

      routingContext.next();
    });

    router.get("/dynamic/").handler(handler);

  }

  public void example41_3(Router router) {

    // Any errors on paths beginning with '/somepath/' will be handled by this error handler
    router.route("/somepath/").failureHandler(ErrorHandler.create());

  }


  public void example42(Router router) {

    router.route("/foo/").handler(TimeoutHandler.create(5000));

  }





}

