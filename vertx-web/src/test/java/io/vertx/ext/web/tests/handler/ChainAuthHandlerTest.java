package io.vertx.ext.web.tests.handler;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.htdigest.HtdigestAuth;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.tests.WebTestBase;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.junit.Test;

import java.util.List;

public class ChainAuthHandlerTest extends WebTestBase {

  private AuthenticationProvider authProvider;
  protected ChainAuthHandler chain;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    authProvider = PropertyFileAuthentication.create(vertx, "login/loginusers.properties");
    AuthenticationHandler redirectAuthHandler = RedirectAuthHandler.create(authProvider);

    // create a chain
    chain = ChainAuthHandler.any()
      .add(JWTAuthHandler.create(null))
      .add(BasicAuthHandler.create(authProvider))
      .add(redirectAuthHandler);

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(chain);
    router.route().handler(ctx -> ctx.response().end());
  }

  @Test
  public void testWithoutAuthorization() throws Exception {
    // since there is no authorization header the final status code will be 302 because it was
    // the last appended handler
    testRequest(HttpMethod.GET, "/", 302, "Found", "Redirecting to /loginpage.");
  }

  @Test
  public void testWithAuthorization() throws Exception {
    // there is an authorization header, so it should be handled properly
    testRequest(HttpMethod.GET, "/", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcw=="),200, "OK", "");
  }

  @Test
  public void testWithBadAuthorization() throws Exception {
    // there is an authorization header, but the token is invalid it should be processed by the last handler (redirect)
    testRequest(HttpMethod.GET, "/", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcX=="),302, "Found", null);
  }

  @Test
  public void testWithBasicAuthAsLastHandlerInChain() throws Exception {
    // after removing the RedirectAuthHandler, we check if the chain correctly returns the WWW-Authenticate Header, since now the BasicAuthHandler is the last handler in the chain
    router.clear();

    // create a chain
    chain = ChainAuthHandler.any()
      .add(JWTAuthHandler.create(null))
      .add(BasicAuthHandler.create(authProvider));

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(chain);
    router.route().handler(ctx -> ctx.response().end());

    testRequest(HttpMethod.GET, "/", req -> req.putHeader("Authorization", "Basic dGltOmRlbGljaW91czpzYXVzYWdlcX=="), resp -> assertEquals("Basic realm=\"vertx-web\"", resp.getHeader("WWW-Authenticate")),401, "Unauthorized", "Unauthorized");
  }

  @Test
  public void testWithMultipleWWWAuthenticate() throws Exception {
    router.clear();

    // create a chain
    chain = ChainAuthHandler.any()
      .add(BasicAuthHandler.create(authProvider))
      .add(DigestAuthHandler.create(vertx, HtdigestAuth.create(vertx)));

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(chain);
    router.route().handler(ctx -> ctx.response().end());

    testRequest(HttpMethod.GET, "/", null, resp -> {
      assertNotNull(resp.getHeader("WWW-Authenticate"));
      List<String> headers = resp.headers().getAll("WWW-Authenticate");
      assertNotNull(headers);
      assertEquals(2, headers.size());
      assertTrue(headers.get(0).startsWith("Basic realm=\"vertx-web\""));
      assertTrue(headers.get(1).startsWith("Digest realm=\"testrealm@host.com\""));
    },401, "Unauthorized", "Unauthorized");
  }

  @Test
  public void testWithPostAuthenticationAction() throws Exception {
    router.clear();

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

    chain = ChainAuthHandler.any()
      // Direct login is implemented as a post-authentication action
      .add(FormLoginHandler.create(authProvider).setDirectLoggedInOKURL("/welcome"));

    router.post("/login")
      .handler(BodyHandler.create())
      .handler(chain);

    testRequest(HttpMethod.POST, "/login", req -> {
      String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
      Buffer buffer = Buffer.buffer();
      String str =
        "--" + boundary + "\r\n" +
        "Content-Disposition: form-data; name=\"" + FormLoginHandler.DEFAULT_USERNAME_PARAM + "\"\r\n\r\ntim\r\n" +
        "--" + boundary + "\r\n" +
        "Content-Disposition: form-data; name=\"" + FormLoginHandler.DEFAULT_PASSWORD_PARAM + "\"\r\n\r\ndelicious:sausages\r\n" +
        "--" + boundary + "--\r\n";
      buffer.appendString(str);
      req.putHeader("content-length", String.valueOf(buffer.length()));
      req.putHeader("content-type", "multipart/form-data; boundary=" + boundary);
      req.write(buffer);
    }, resp -> {
      MultiMap headers = resp.headers();
      // session will be upgraded
      String setCookie = headers.get("set-cookie");
      assertNotNull(setCookie);
      // client will be redirected
      assertTrue(headers.contains(HttpHeaders.LOCATION, "/welcome", false));
    }, 302, "Found", null);
  }

}
