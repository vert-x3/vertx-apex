package io.vertx.ext.auth.jwt;

import io.vertx.ext.auth.common.AuthenticationContext;
import io.vertx.ext.auth.common.handler.impl.HTTPAuthorizationHandler;

public abstract class AbstractJWTHandler<C extends AuthenticationContext> extends HTTPAuthorizationHandler<C, JWTAuth>
  implements JWTAuthHandler<C>, io.vertx.ext.auth.common.ScopedAuthentication<C, JWTAuthHandler<C>> {

  public AbstractJWTHandler(JWTAuth authProvider, Type bearer, String realm) {
    super(authProvider, bearer, realm);
  }

}
