package io.vertx.ext.web.handler;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.web.WebTestBase;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.junit.Test;

public class ChainAuthHandlerAndTest extends WebTestBase {

  private AuthenticationProvider authProvider;
  protected ChainAuthHandler chain;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    authProvider = PropertyFileAuthentication.create(vertx, "login/loginusers.properties");
    WebAuthenticationHandler redirectAuthHandler = RedirectAuthHandler.create(authProvider);

    // create a chain
    chain = ChainAuthHandler.all()
      .add(JWTAuthHandler.create(null))
      .add(BasicAuthHandler.create(authProvider))
      .add(redirectAuthHandler);

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(chain);
    router.route().handler(ctx -> ctx.response().end());
  }

  @Test
  public void testWithoutAuthorization() throws Exception {
    // since there is no authorization header the final status code will be 401 since all handlers are required it will
    // fail at the very first one.
    testRequest(HttpMethod.GET, "/", 401, "Unauthorized");
  }

  @Test
  public void testWithAuthorization() throws Exception {
    // there is an authorization header, so it should be handled properly
    // however it will not be accepted as first we required a jwt and that is enough to fail
    testRequest(HttpMethod.GET, "/", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcw=="),401, "Unauthorized", "Unauthorized");
  }

  @Test
  public void testWithBadAuthorization() throws Exception {
    // there is an authorization header, but the token is invalid it should be processed by the basic auth
    testRequest(HttpMethod.GET, "/", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcX=="),401, "Unauthorized", "Unauthorized");
  }

  @Test
  public void testWithBasicAuthAsLastHandlerInChain() throws Exception {
    // after removing the RedirectAuthHandler, we check if the chain correctly returns the WWW-Authenticate Header, since now the BasicAuthHandler is the last handler in the chain
    router.clear();

    // create a chain
    chain = ChainAuthHandler.all()
      .add(JWTAuthHandler.create(null))
      .add(BasicAuthHandler.create(authProvider));

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(chain);
    router.route().handler(ctx -> ctx.response().end());

    testRequest(HttpMethod.GET, "/", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcX=="), resp -> assertEquals("Basic realm=\"vertx-web\"", resp.getHeader("WWW-Authenticate")),401, "Unauthorized", "Unauthorized");
  }
}
