package io.vertx.ext.auth.common;

import java.util.List;

/**
 * Internal interface for scope aware Authentication handlers.
 *
 * @param <SELF>
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
public interface ScopedAuthentication<C extends AuthenticationContext, SELF extends AuthenticationHandler<C>> {

  /**
   * Return a new instance with the internal state copied from the caller but the scopes to be requested during a token request are unique to the instance.
   *
   * @param scope
   *          scope.
   * @return new instance of this interface.
   */
  SELF withScope(String scope);

  /**
   * Return a new instance with the internal state copied from the caller but the scopes to be requested during a token request are unique to the instance.
   *
   * @param scopes
   *          scopes.
   * @return new instance of this interface.
   */
  SELF withScopes(List<String> scopes);

  /**
   * Return the list of scopes provided as the 1st argument, unless the list is empty. In this case, the list of scopes is obtained from the routing context
   * metadata if possible. In case the metadata is not available, the list of scopes is always an empty list.
   */
  List<String> getScopesOrSearchMetadata(List<String> scopes, C ctx);

}
