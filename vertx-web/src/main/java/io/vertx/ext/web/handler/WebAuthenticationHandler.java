package io.vertx.ext.web.handler;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.auth.common.AuthenticationHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * Base interface for Vert.x Web authentication handlers.
 * <p>
 * An auth handler allows your application to provide authentication support.
 * <p>
 * An Auth handler may require a {@link SessionHandler} to be on the routing chain before it.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
@VertxGen(concrete = false)
public interface WebAuthenticationHandler extends AuthenticationHandler<RoutingContext> {

}
