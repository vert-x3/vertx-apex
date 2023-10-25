package io.vertx.ext.auth.common;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;

import java.util.Map;
import java.util.function.Function;

/**
 * Represents a browser session.
 * <p>
 * Sessions persist between HTTP requests for a single browser session. They are deleted when the browser is closed, or
 * they time-out. Session cookies are used to maintain sessions using a secure UUID.
 * <p>
 * Sessions can be used to maintain data for a browser session, e.g. a shopping basket.
 * <p>
 * The context must have first been routed to a {@link io.vertx.ext.web.handler.SessionHandler}
 * for sessions to be available.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@VertxGen
public interface Session {

  /**
   * @return The new unique ID of the session.
   */
  Session regenerateId();

  /**
   * @return The unique ID of the session. This is generated using a random secure UUID.
   */
  String id();

  /**
   * Put some data in a session
   *
   * @param key  the key for the data
   * @param obj  the data
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Session put(String key, Object obj);

  /**
   * Put some data in a session if absent
   *
   * @param key  the key for the data
   * @param obj  the data
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Session putIfAbsent(String key, Object obj);

  /**
   * Put some data in a session if absent.
   *
   * If the specified key is not already associated with a value (or is mapped
   * to {@code null}), attempts to compute its value using the given mapping
   * function and enters it into this map unless {@code null}.
   *
   * @param key  the key for the data
   * @param mappingFunction  a mapping function
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  Session computeIfAbsent(String key, Function<String, Object> mappingFunction);

  /**
   * Get some data from the session
   *
   * @param key  the key of the data
   * @return  the data
   */
  <T> T get(String key);

  /**
   * Remove some data from the session
   *
   * @param key  the key of the data
   * @return  the data that was there or null if none there
   */
  <T> T remove(String key);

  /**
   * @return the session data as a map
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  Map<String, Object> data();

  /**
   * @return true if the session has data
   */
  boolean isEmpty();

  /**
   * @return the time the session was last accessed
   */
  long lastAccessed();

  /**
   * Destroy the session
   */
  void destroy();

  /**
   * @return has the session been destroyed?
   */
  boolean isDestroyed();

  /**
   * @return has the session been renewed?
   */
  boolean isRegenerated();

  /**
   * @return old ID if renewed
   */
  String oldId();

  /**
   * @return the amount of time in ms, after which the session will expire, if not accessed.
   */
  long timeout();

  /**
   * Mark the session as being accessed.
   */
  void setAccessed();

  /**
   * The short representation of the session to be added to the session cookie. By default is the session id.
   *
   * @return short representation string.
   */
  default String value() {
    return id();
  }
}
