package io.vertx.ext.auth.jwt;

import java.util.List;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.ext.auth.common.AuthenticationContext;
import io.vertx.ext.auth.common.AuthenticationHandler;

public interface JWTAuthHandler<C extends AuthenticationContext> extends AuthenticationHandler<C> {

  /**
   * Set the scope delimiter. By default this is a space character.
   *
   * @param delimiter
   *          scope delimiter.
   * @return fluent self.
   */
  @Fluent
  JWTAuthHandler<C> scopeDelimiter(String delimiter);

  /**
   * Return a new instance with the internal state copied from the caller but the scopes to be requested during a token request are unique to the instance. When
   * scopes are applied to the handler, the default scopes from the route metadata will be ignored.
   *
   * @param scope
   *          scope.
   * @return new instance of this interface.
   */
  JWTAuthHandler<C> withScope(String scope);

  /**
   * Return a new instance with the internal state copied from the caller but the scopes to be requested during a token request are unique to the instance. When
   * scopes are applied to the handler, the default scopes from the route metadata will be ignored.
   *
   * @param scopes
   *          scopes.
   * @return new instance of this interface.
   */
  JWTAuthHandler<C> withScopes(List<String> scopes);
}
