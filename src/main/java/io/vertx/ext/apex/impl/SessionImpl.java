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

package io.vertx.ext.apex.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.shareddata.Shareable;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.apex.Session;
import io.vertx.ext.apex.sstore.SessionStore;
import io.vertx.ext.auth.AuthService;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class SessionImpl implements Session, ClusterSerializable, Shareable {

  private static final Logger log = LoggerFactory.getLogger(SessionImpl.class);
  private static final Charset UTF8 = Charset.forName("UTF-8");

  private static final byte TYPE_LONG = 1;
  private static final byte TYPE_INT = 2;
  private static final byte TYPE_SHORT = 3;
  private static final byte TYPE_BYTE = 4;
  private static final byte TYPE_DOUBLE = 5;
  private static final byte TYPE_FLOAT = 6;
  private static final byte TYPE_CHAR = 7;
  private static final byte TYPE_BOOLEAN = 8;
  private static final byte TYPE_STRING = 9;
  private static final byte TYPE_BUFFER = 10;
  private static final byte TYPE_BYTES = 11;
  private static final byte TYPE_SERIALIZABLE = 12;
  private static final byte TYPE_CLUSTER_SERIALIZABLE = 13;

  private String id;
  private SessionStore sessionStore;
  private long timeout;
  private Map<String, Object> data;

  private long lastAccessed;
  private boolean destroyed;
  private String loginID;
  private AuthService authService;
  private Set<String> roles = new HashSet<>();
  private Set<String> permissions = new HashSet<>();

  public SessionImpl()
  {
  }

  public SessionImpl(String id, long timeout, SessionStore sessionStore) {
    this.id = id;
    this.sessionStore = sessionStore;
    this.timeout = timeout;
    this.lastAccessed = System.currentTimeMillis();
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public long timeout() {
    return timeout;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    Object obj = getData().get(key);
    return (T)obj;
  }

  @Override
  public Session put(String key, Object obj) {
    getData().put(key, obj);
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T remove(String key) {
    Object obj = getData().remove(key);
    return (T)obj;
  }

  @Override
  public Map<String, Object> data() {
    return getData();
  }

  @Override
  public long lastAccessed() {
    return lastAccessed;
  }

  @Override
  public void setAccessed() {
    this.lastAccessed = System.currentTimeMillis();
    if (authService != null && loginID != null) {
      authService.refreshLoginSession(loginID, res -> {
        if (res.failed()) {
          log.error("Failed to refresh login", res.cause());
        }
      });
    }
  }

  @Override
  public void destroy() {
    destroyed = true;
    data = null;
    sessionStore.delete(id, res -> {
      if (!res.succeeded()) {
        log.error("Failed to delete session", res.cause());
      }
    });
  }

  @Override
  public boolean isDestroyed() {
    return destroyed;
  }

  @Override
  public SessionStore sessionStore() {
    return sessionStore;
  }

  @Override
  public boolean isLoggedIn() {
    return loginID != null;
  }

  @Override
  public void hasRole(String role, Handler<AsyncResult<Boolean>> resultHandler) {
    if (roles.contains(role)) {
      resultHandler.handle(Future.succeededFuture(true));
    } else {
      if (authService != null && loginID != null) {
        authService.hasRole(loginID, role, res -> {
          if (res.succeeded()) {
            boolean has = res.result();
            if (has) {
              roles.add(role);
              resultHandler.handle(Future.succeededFuture(true));
            } else {
              resultHandler.handle(Future.succeededFuture(false));
            }
          } else {
            log.error(res.cause());
          }
        });
      } else {
        resultHandler.handle(Future.succeededFuture(false));
      }
    }
  }

  @Override
  public void hasPermission(String permission, Handler<AsyncResult<Boolean>> resultHandler) {
    if (permissions.contains(permission)) {
      resultHandler.handle(Future.succeededFuture(true));
    } else {
      if (authService != null && loginID != null) {
        authService.hasPermission(loginID, permission, res -> {
          if (res.succeeded()) {
            boolean has = res.result();
            if (has) {
              permissions.add(permission);
              resultHandler.handle(Future.succeededFuture(true));
            } else {
              resultHandler.handle(Future.succeededFuture(false));
            }
          } else {
            log.error(res.cause());
          }
        });
      } else {
        resultHandler.handle(Future.succeededFuture(false));
      }
    }
  }

  @Override
  public void logout() {
    if (authService == null && loginID != null) {
      throw new IllegalStateException("No auth service");
    }
    if (authService != null && loginID != null) {
      authService.logout(loginID, res -> {
        if (res.failed()) {
          log.error("Failed to logout", res.cause());
        }
      });
    }
    this.loginID = null;
    roles.clear();
    permissions.clear();
  }

  @Override
  public void setLoginID(String loginID) {
    this.loginID = loginID;
  }

  @Override
  public String getLoginID() {
    return loginID;
  }

  @Override
  public void setAuthService(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public AuthService getAuthService() {
    return authService;
  }

  @Override
  public Buffer writeToBuffer() {
    Buffer buff = Buffer.buffer();
    buff.appendInt(id.length());
    buff.appendString(id);
    buff.appendLong(timeout);
    buff.appendLong(lastAccessed);
    Buffer dataBuf = writeDataToBuffer();
    buff.appendInt(dataBuf.length());
    buff.appendBuffer(dataBuf);
    return buff;
  }

  @Override
  public void readFromBuffer(Buffer buffer) {
    int pos = 0;
    int len = buffer.getInt(pos);
    pos += 4;
    id = buffer.getString(pos, pos + len);
    pos += len;
    timeout = buffer.getLong(pos);
    pos += 8;
    lastAccessed = buffer.getLong(pos);
    pos += 8;
    len = buffer.getInt(pos);
    pos += 4;
    data = readDataFromBuffer(buffer, pos);
  }

  private Map<String, Object> getData() {
    if (data == null) {
      data = new HashMap<>();
    }
    return data;
  }

  private Buffer writeDataToBuffer() {
    try {
      Map<String, Object> data = getData();
      Buffer buffer = Buffer.buffer();
      buffer.appendInt(data.size());
      for (Map.Entry<String, Object> entry : data.entrySet()) {
        String key  = entry.getKey();
        byte[] keyBytes = key.getBytes(UTF8);
        buffer.appendInt(keyBytes.length).appendBytes(keyBytes);
        Object val = entry.getValue();
        if (val instanceof Long) {
          buffer.appendByte(TYPE_LONG).appendLong((long) val);
        } else if (val instanceof Integer) {
          buffer.appendByte(TYPE_INT).appendInt((int) val);
        } else if (val instanceof Short) {
          buffer.appendByte(TYPE_SHORT).appendShort((short) val);
        } else if (val instanceof Byte) {
          buffer.appendByte(TYPE_BYTE).appendByte((byte) val);
        } else if (val instanceof Double) {
          buffer.appendByte(TYPE_DOUBLE).appendDouble((double) val);
        } else if (val instanceof Float) {
          buffer.appendByte(TYPE_FLOAT).appendFloat((float) val);
        } else if (val instanceof Character) {
          buffer.appendByte(TYPE_CHAR).appendShort((short)((Character)val).charValue());
        } else if (val instanceof Boolean) {
          buffer.appendByte(TYPE_BOOLEAN).appendByte((byte) ((boolean) val ? 1 : 0));
        } else if (val instanceof String) {
          byte[] bytes = ((String) val).getBytes(UTF8);
          buffer.appendByte(TYPE_STRING).appendInt(bytes.length).appendBytes(bytes);
        } else if (val instanceof Buffer) {
          Buffer buff = (Buffer) val;
          buffer.appendByte(TYPE_BUFFER).appendInt(buff.length()).appendBuffer(buff);
        } else if (val instanceof byte[]) {
          byte[] bytes = (byte[]) val;
          buffer.appendByte(TYPE_BYTES).appendInt(bytes.length).appendBytes(bytes);
        } else if (val instanceof Serializable) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(baos));
          oos.writeObject(val);
          oos.flush();
          byte[] bytes = baos.toByteArray();
          buffer.appendByte(TYPE_SERIALIZABLE).appendInt(bytes.length).appendBytes(bytes);
        } else if (val instanceof ClusterSerializable) {
          buffer.appendByte(TYPE_CLUSTER_SERIALIZABLE);
          String className = val.getClass().getName();
          byte[] classNameBytes = className.getBytes(UTF8);
          buffer.appendInt(classNameBytes.length).appendBytes(classNameBytes);
          Buffer buff = ((ClusterSerializable)val).writeToBuffer();
          buffer.appendBuffer(buff);
        } else {
          throw new IllegalStateException("Invalid type for data in session: " + val.getClass());
        }
      }
      return buffer;
    } catch (IOException e) {
      throw new VertxException(e);
    }
  }

  private Map<String, Object> readDataFromBuffer(Buffer buffer, int pos) {
    try {
      int entries = buffer.getInt(pos);
      pos +=4;
      Map<String, Object> data = new HashMap<>(entries);
      for (int i = 0; i < entries; i++) {
        int keylen = buffer.getInt(pos);
        pos += 4;
        byte[] keyBytes = buffer.getBytes(pos, pos + keylen);
        pos += keylen;
        String key = new String(keyBytes, UTF8);
        byte type = buffer.getByte(pos++);
        Object val;
        switch (type) {
          case TYPE_LONG:
            val = buffer.getLong(pos);
            pos += 8;
            break;
          case TYPE_INT:
            val = buffer.getInt(pos);
            pos += 4;
            break;
          case TYPE_SHORT:
            val = buffer.getShort(pos);
            pos += 2;
            break;
          case TYPE_BYTE:
            val = buffer.getByte(pos);
            pos ++;
            break;
          case TYPE_FLOAT:
            val = buffer.getFloat(pos);
            pos += 4;
            break;
          case TYPE_DOUBLE:
            val = buffer.getDouble(pos);
            pos += 8;
            break;
          case TYPE_CHAR:
            short s = buffer.getShort(pos);
            pos += 2;
            val = (char)s;
            break;
          case TYPE_BOOLEAN:
            byte b = buffer.getByte(pos);
            pos++;
            val = b == 1;
            break;
          case TYPE_STRING:
            int len = buffer.getInt(pos);
            pos += 4;
            byte[] bytes = buffer.getBytes(pos, pos + len);
            val = new String(bytes, UTF8);
            pos += len;
            break;
          case TYPE_BUFFER:
            len = buffer.getInt(pos);
            pos += 4;
            bytes = buffer.getBytes(pos, pos + len);
            val = Buffer.buffer(bytes);
            pos += len;
            break;
          case TYPE_BYTES:
            len = buffer.getInt(pos);
            pos += 4;
            val = buffer.getBytes(pos, pos + len);
            pos += len;
            break;
          case TYPE_SERIALIZABLE:
            len = buffer.getInt(pos);
            pos += 4;
            bytes = buffer.getBytes(pos, pos + len);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bais));
            val = ois.readObject();
            pos += len;
            break;
          case TYPE_CLUSTER_SERIALIZABLE:
            int classNameLen = buffer.getInt(pos);
            pos += 4;
            byte[] classNameBytes = buffer.getBytes(pos, pos + classNameLen);
            pos += classNameLen;
            String className = new String(classNameBytes, UTF8);
            Class clazz = Utils.getClassLoader().loadClass(className);
            ClusterSerializable obj = (ClusterSerializable)clazz.newInstance();
            len = buffer.getInt(pos);
            Buffer buff = buffer.slice(pos, pos + len + 4);
            pos += 4 + len;
            obj.readFromBuffer(buff);
            val = obj;
            break;
          default:
            throw new IllegalStateException("Invalid serialized type: " + type);
        }
        data.put(key, val);
      }
      return data;
    } catch (Exception e) {
      throw new VertxException(e);
    }
  }

}

