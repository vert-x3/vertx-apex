package io.vertx.ext.web.handler;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.WebTestBase;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.junit.Test;

import java.util.Arrays;

public class ChainAuthMixHandlerTest extends WebTestBase {

  private static final User USER = User.create(new JsonObject().put("id", "paulo"));

  private final WebAuthenticationHandler success = SimpleAuthenticationHandler.create()
    .authenticate(ctx -> Future.succeededFuture(USER));


  private final WebAuthenticationHandler failure = SimpleAuthenticationHandler.create()
    .authenticate(ctx -> Future.failedFuture(new HttpException(401)));

  @Test
  public void testFailOrFailOrSuccess() throws Exception {

    // (failure OR (failure OR success))
    ChainAuthHandler chain =
      ChainAuthHandler.any()
        .add(failure)
        .add(
          ChainAuthHandler.any()
            .add(failure)
            .add(success)
        );


    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(chain);
    router.route().handler(ctx -> ctx.response().end());

    testRequest(HttpMethod.GET, "/", 200, "OK");
  }

  @Test
  public void testFailOrSuccessAndFail() throws Exception {

    // (failure OR (sucess AND failure))
    ChainAuthHandler chain =
      ChainAuthHandler.any()
        .add(failure)
        .add(
          ChainAuthHandler.all()
            .add(success)
            .add(failure)
        );


    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(chain);
    router.route().handler(ctx -> ctx.response().end());

    testRequest(HttpMethod.GET, "/", 401, "Unauthorized");
  }

  @Test
  public void test2JWTIssuers() throws Exception {

    JsonObject key = new JsonObject()
      .put("kty", "oct")
      .put("use", "sig")
      .put("k", "wuSPxS64NYh4ohDpZWOMNtawhBLHVn8dhKuIxnsLLd-dfKzIb5FL7r-vXTJ3MjtqnBlh_piKjn6qvb8os00MXNEyJWhgbPsnZEfqj6wMsJiH3uDcEgDuBMVbsuMlVbyX3x0Cd6qn0qvF8JZaLxSR6JNEEOGnbkUXqF9ghcI2y8rooN6ivQJ0-SiCqtQSkVrSO4H65lHagUus0XjTErL4GypbcO6PBIZMtHBW4UZHVcl86IhDxj5v0xf3WSuDGxkrbw5rpM_eVUR1eu71XPoTXD4WgDRtq4CoQcIFeSpqJuKZvzDJ47zV3wgnqKZ6G-RkiSKLBUj5_4Ur_YWHw2h-CQ")
      .put("alg", "HS256");

    JWTAuth me = JWTAuth.create(vertx, new JWTAuthOptions().addJwk(key).setJWTOptions(new JWTOptions().setIssuer("me")));
    JWTAuth you = JWTAuth.create(vertx, new JWTAuthOptions().addJwk(key).setJWTOptions(new JWTOptions().setIssuer("you")));

    ChainAuthHandler chain =
      ChainAuthHandler.any()
        .add(JWTAuthHandler.create(me))
        .add(JWTAuthHandler.create(you));

    router.route().handler(chain);
    router.route().handler(ctx -> ctx.response().end());

    // Payload with right issuer
    final JsonObject payloadA = new JsonObject()
      .put("sub", "Paulo")
      .put("iss", "me");

    testRequest(HttpMethod.GET, "/", req -> req.putHeader("Authorization", "Bearer " + me.generateToken(payloadA)), 200, "OK", null);

    // Payload with right issuer
    final JsonObject payloadB = new JsonObject()
      .put("sub", "Paulo")
      .put("iss", "you");

    testRequest(HttpMethod.GET, "/", req -> req.putHeader("Authorization", "Bearer " + you.generateToken(payloadB)), 200, "OK", null);
  }

  @Test
  public void testRedirectAndHandler() throws Exception {
      ChainAuthHandler.all()
        .add(OAuth2AuthHandler.create(vertx, null))
        .add(BasicAuthHandler.create(null));
  }

  @Test(expected = IllegalStateException.class)
  public void testRedirectAndHandlerFail() {
    ChainAuthHandler.all()
      .add(OAuth2AuthHandler.create(vertx, null, "http://server.com/callback"))
      .add(BasicAuthHandler.create(null));
  }
}
