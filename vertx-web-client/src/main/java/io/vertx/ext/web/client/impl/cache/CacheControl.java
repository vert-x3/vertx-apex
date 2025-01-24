/*
 * Copyright 2021 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.web.client.impl.cache;

import io.netty.handler.codec.DateFormatter;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parse HTTP headers to determine if and how to cache a response.
 *
 * @author <a href="mailto:craigday3@gmail.com">Craig Day</a>
 */
public class CacheControl {

  private final Set<CacheControlDirective> directives;
  private final Map<CacheControlDirective, Long> timeDirectives;
  private final Instant expires;
  private final Instant date;
  private final String etag;
  private final String vary;
  private final long maxAge;

  public static CacheControl parse(MultiMap headers) {
    return new CacheControl(headers);
  }

  private CacheControl(MultiMap headers) {
    this.directives = new HashSet<>();
    this.timeDirectives = new HashMap<>();
    this.etag = headers.get(HttpHeaders.ETAG);
    this.vary = headers.get(HttpHeaders.VARY);

    if (headers.contains(HttpHeaders.DATE)) {
      Date date = DateFormatter.parseHttpDate(headers.get(HttpHeaders.DATE));
      this.date = date == null ? null : date.toInstant();
    } else {
      this.date = Instant.now();
    }

    if (headers.contains(HttpHeaders.EXPIRES)) {
      Date expiresDate = DateFormatter.parseHttpDate(headers.get(HttpHeaders.EXPIRES));
      this.expires = expiresDate == null ? null : expiresDate.toInstant();
    } else {
      this.expires = null;
    }

    // Order of operations matters here. We do plain assignment above, but then we must parse
    // the Cache-Control header before we can compute max age.
    parseAllCacheControl(headers);
    this.maxAge = computeMaxAge();
  }

  public Set<CacheControlDirective> getDirectives() {
    return directives;
  }

  public Map<CacheControlDirective, Long> getTimeDirectives() {
    return timeDirectives;
  }

  public String getEtag() {
    return etag;
  }

  public long getMaxAge() {
    return maxAge;
  }

  public Set<CharSequence> variations() {
    if (vary == null) {
      return Collections.emptySet();
    } else {
      Set<CharSequence> variations = new HashSet<>();
      String remaining = vary;
      int nextDelim = remaining.indexOf(',');

      while (nextDelim > 0) {
        variations.add(HttpHeaders.createOptimized(remaining.substring(0, nextDelim).trim().toLowerCase()));
        remaining = remaining.substring(nextDelim + 1);
        nextDelim = remaining.indexOf(',');
      }

      if (!remaining.isEmpty()) {
        variations.add(HttpHeaders.createOptimized(remaining.trim().toLowerCase()));
      }

      return variations;
    }
  }

  public boolean isCacheable() {
    if (directives.contains(CacheControlDirective.NO_STORE)) {
      return false;
    }
    if ("*".equals(vary)) {
      return false;
    }

    return maxAge >= 0;
  }

  public boolean isPublic() {
    // Technically, you cannot say `Cache-Control: public, private` but on the chance that we do,
    // default to private which is considered safer and more strict.
    return directives.contains(CacheControlDirective.PUBLIC) && !isPrivate();
  }

  public boolean isPrivate() {
    return directives.contains(CacheControlDirective.PRIVATE);
  }

  public boolean isVarying() {
    return !variations().isEmpty();
  }

  public boolean noStore() {
    return directives.contains(CacheControlDirective.NO_STORE);
  }

  public boolean noCache() {
    return !noStore() && directives.contains(CacheControlDirective.NO_CACHE);
  }

  public boolean mustRevalidate() {
    return directives.contains(CacheControlDirective.MUST_REVALIDATE);
  }

  private long computeMaxAge() {
    if (!isPrivate() && timeDirectives.containsKey(CacheControlDirective.SHARED_MAX_AGE)) {
      return timeDirectives.get(CacheControlDirective.SHARED_MAX_AGE);
    } else if (timeDirectives.containsKey(CacheControlDirective.MAX_AGE)) {
      return timeDirectives.get(CacheControlDirective.MAX_AGE);
    } else if (date != null && expires != null) {
      return Duration.between(date, expires).getSeconds();
    } else {
      return Long.MAX_VALUE;
    }
  }

  private void parseAllCacheControl(MultiMap headers) {
    headers.getAll(HttpHeaders.CACHE_CONTROL).forEach(value -> {
      for (String headerDirectives : value.split(",")) {
        String[] directiveParts = headerDirectives.split("=", 2);
        Optional<CacheControlDirective> directive =
          CacheControlDirective.fromHeader(directiveParts[0].trim().toLowerCase());

        if (!directive.isPresent()) {
          continue;
        }

        if (directiveParts.length == 1) {
          directives.add(directive.get());
        } else {
          try {
            timeDirectives.put(
              directive.get(),
              Long.parseLong(directiveParts[1].replaceAll("\"", "").trim().toLowerCase())
            );
          } catch (NumberFormatException e) {
            // The header contains unexpected data, ignore it
          }
        }
      }
    });
  }
}
