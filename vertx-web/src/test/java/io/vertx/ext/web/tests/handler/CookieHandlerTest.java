/*
 * Copyright 2014 Red Hat, Inc.
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

package io.vertx.ext.web.tests.handler;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.impl.Utils;
import io.vertx.ext.web.tests.WebTestBase;
import org.junit.Test;

import java.util.*;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class CookieHandlerTest extends WebTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testSimpleCookie() throws Exception {
    router.route().handler(rc -> {
      assertEquals(1, rc.request().cookieCount());
      Cookie cookie = rc.request().getCookie("foo");
      assertNotNull(cookie);
      assertEquals("bar", cookie.getValue());
      rc.response().end();
    });
    testRequestWithCookies(HttpMethod.GET, "/", "foo=bar", 200, "OK");
  }

  @Test
  public void testGetCookies() throws Exception {
    router.route().handler(rc -> {
      assertEquals(3, rc.request().cookieCount());
      Map<String, Cookie> cookies = rc.request().cookieMap();
      assertTrue(cookies.containsKey("foo"));
      assertTrue(cookies.containsKey("wibble"));
      assertTrue(cookies.containsKey("plop"));
      Cookie removed = rc.response().removeCookie("foo");
      cookies = rc.request().cookieMap();
      // removed cookies, need to be sent back with an expiration date
      assertTrue(cookies.containsKey("foo"));
      assertTrue(cookies.containsKey("wibble"));
      assertTrue(cookies.containsKey("plop"));
      rc.response().end();
    });
    testRequest(HttpMethod.GET, "/", req -> req.headers().set("Cookie", "foo=bar; wibble=blibble; plop=flop"), resp -> {
      List<String> cookies = resp.headers().getAll("set-cookie");
      // the expired cookie must be sent back
      assertEquals(1, cookies.size());
      assertTrue(cookies.get(0).contains("Max-Age=0"));
      assertTrue(cookies.get(0).contains("Expires="));
    }, 200, "OK", null);
  }

  @Test
  public void testCookiesChangedInHandler() throws Exception {
    router.route().handler(rc -> {
      assertEquals(3, rc.request().cookieCount());
      assertEquals("bar", rc.request().getCookie("foo").getValue());
      assertEquals("blibble", rc.request().getCookie("wibble").getValue());
      assertEquals("flop", rc.request().getCookie("plop").getValue());
      rc.response().removeCookie("plop");
      // the expected number of elements should remain the same as we're sending an invalidate cookie back
      assertEquals(3, rc.request().cookieCount());
      rc.next();
    });
    router.route().handler(rc -> {
      assertEquals("bar", rc.request().getCookie("foo").getValue());
      assertEquals("blibble", rc.request().getCookie("wibble").getValue());
      assertNotNull(rc.request().getCookie("plop"));
      rc.response().addCookie(Cookie.cookie("fleeb", "floob"));
      assertEquals(4, rc.request().cookieCount());
      assertNull(rc.response().removeCookie("blarb"));
      assertEquals(4, rc.request().cookieCount());
      Cookie foo = rc.request().getCookie("foo");
      foo.setValue("blah");
      rc.response().end();
    });
    testRequest(HttpMethod.GET, "/", req -> req.headers().set("Cookie", "foo=bar; wibble=blibble; plop=flop"), resp -> {
      List<String> cookies = resp.headers().getAll("set-cookie");
      assertEquals(3, cookies.size());
      assertTrue(cookies.contains("foo=blah"));
      assertTrue(cookies.contains("fleeb=floob"));
      boolean found = false;
      for (String s : cookies) {
        if (s.startsWith("plop")) {
          found = true;
          assertTrue(s.contains("Max-Age=0"));
          assertTrue(s.contains("Expires="));
          break;
        }
      }
      assertTrue(found);
    }, 200, "OK", null);
  }

  @Test
  public void testCookieFields() throws Exception {
    Cookie cookie = Cookie.cookie("foo", "bar");
    assertEquals("foo", cookie.getName());
    assertEquals("bar", cookie.getValue());
    assertEquals("foo=bar", cookie.encode());
    assertNull(cookie.getPath());
    cookie.setPath("/somepath");
    assertEquals("/somepath", cookie.getPath());
    assertEquals("foo=bar; Path=/somepath", cookie.encode());
    assertNull(cookie.getDomain());
    cookie.setDomain("foo.com");
    assertEquals("foo.com", cookie.getDomain());
    assertEquals("foo=bar; Path=/somepath; Domain=foo.com", cookie.encode());
    long maxAge = 30 * 60;
    cookie.setMaxAge(maxAge);


    long now = System.currentTimeMillis();
    String encoded = cookie.encode();
    int startPos = encoded.indexOf("Expires=");
    int endPos = encoded.indexOf(';', startPos);
    String expiresDate = encoded.substring(startPos + 8, endPos);
    Date d = new Date(Utils.parseRFC1123DateTime(expiresDate));
    assertTrue(d.getTime() - now >= maxAge);

    cookie.setMaxAge(Long.MIN_VALUE);
    cookie.setSecure(true);
    assertEquals("foo=bar; Path=/somepath; Domain=foo.com; Secure", cookie.encode());
    cookie.setHttpOnly(true);
    assertEquals("foo=bar; Path=/somepath; Domain=foo.com; Secure; HTTPOnly", cookie.encode());
  }
}
