package io.vertx.ext.web.impl;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.WebServerRequest;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Wraps the source {@link HttpServerRequestInternal}. It updates the method, path and query of the original request and
 * resumes the request if a caller explicitly sets a handler to any callback that processes the request body.
 */
class HttpServerRequestWrapper extends io.vertx.core.http.impl.HttpServerRequestWrapper implements WebServerRequest {

  private final ForwardedParser forwardedParser;

  private boolean modified;

  private HttpMethod method;
  private String path;
  private String query;
  private String uri;
  private String absoluteURI;
  private MultiMap params;
  private RoutingContext ctx;

  HttpServerRequestWrapper(HttpServerRequest request, AllowForwardHeaders allowForward) {
    this(request, allowForward, null);
  }

  HttpServerRequestWrapper(HttpServerRequest request, AllowForwardHeaders allowForward, RoutingContext ctx) {
    super((HttpServerRequestInternal) request);
    forwardedParser = new ForwardedParser(request, allowForward);
    this.ctx = ctx;
  }

  void changeTo(HttpMethod method, String uri) {
    modified = true;
    this.method = method;
    this.uri = uri;
    // lazy initialization
    this.query = null;
    this.absoluteURI = null;

    // parse
    int queryIndex = uri.indexOf('?');

    // there's a query
    if (queryIndex != -1) {
      int fragmentIndex = uri.indexOf('#', queryIndex);
      path = uri.substring(0, queryIndex);
      // there's a fragment
      if (fragmentIndex != -1) {
        query = uri.substring(queryIndex + 1, fragmentIndex);
      } else {
        query = uri.substring(queryIndex + 1);
      }
    } else {
      int fragmentIndex = uri.indexOf('#');
      // there's a fragment
      if (fragmentIndex != -1) {
        path = uri.substring(0, fragmentIndex);
      } else {
        path = uri;
      }
    }
  }

  @Override
  public HttpMethod method() {
    if (!modified) {
      return delegate.method();
    }
    return method;
  }

  @Override
  public String uri() {
    if (!modified) {
      return delegate.uri();
    }
    return uri;
  }

  @Override
  public String path() {
    if (!modified) {
      return delegate.path();
    }
    return path;
  }

  @Override
  public String query() {
    if (!modified) {
      return delegate.query();
    }
    return query;
  }

  @Override
  public MultiMap params() {
    if (!modified) {
      return delegate.params();
    }
    if (params == null) {
      params = MultiMap.caseInsensitiveMultiMap();
      // if there is no query it's not really needed to parse it
      if (query != null) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri, Charset.forName(delegate.getParamsCharset()));
        Map<String, List<String>> prms = queryStringDecoder.parameters();
        if (!prms.isEmpty()) {
          for (Map.Entry<String, List<String>> entry : prms.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
          }
        }
      }
    }

    return params;
  }

  @Override
  public String getParam(String param) {
    if (!modified) {
      return delegate.getParam(param);
    }

    return params().get(param);
  }

  @Override
  public HttpServerRequest setParamsCharset(String s) {
    String old = delegate.getParamsCharset();
    delegate.setParamsCharset(s);
    if (!s.equals(old)) {
      params = null;
    }
    return this;
  }

  @Override
  public SocketAddress remoteAddress() {
    return forwardedParser.remoteAddress();
  }

  @Override
  public String absoluteURI() {
    if (!modified) {
      return forwardedParser.absoluteURI();
    } else {
      if (absoluteURI == null) {
        String scheme = forwardedParser.scheme();
        HostAndPort host = forwardedParser.authority();

        // if both are not null we can rebuild the uri
        if (scheme != null && host != null) {
          absoluteURI = scheme + "://" + host + uri;
        } else {
          absoluteURI = uri;
        }
      }

      return absoluteURI;
    }
  }

  @Override
  public String scheme() {
    return forwardedParser.scheme();
  }

  @Override
  public @Nullable HostAndPort authority() {
    return forwardedParser.authority();
  }

  @Override
  public Future<ServerWebSocket> toWebSocket() {
    return delegate
      .toWebSocket()
      .map(ws -> new ServerWebSocketWrapper(ws, authority(), scheme(), isSSL(), remoteAddress()));
  }

  @Override
  public boolean isSSL() {
    return forwardedParser.isSSL();
  }

  @Override
  public RoutingContext routingContext() {
    return ctx;
  }

}
