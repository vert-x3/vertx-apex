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
package io.vertx.ext.web.client.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.impl.headers.HeadersAdaptor;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;
import io.vertx.ext.web.multipart.FormDataPart;
import io.vertx.ext.web.multipart.MultipartForm;

import java.io.File;

/**
 * A stream that sends a multipart form.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class MultipartFormUpload implements ReadStream<Buffer> {

  private static final UnpooledByteBufAllocator ALLOC = new UnpooledByteBufAllocator(false);

  private DefaultFullHttpRequest request;
  private HttpPostRequestEncoder encoder;
  private Handler<Throwable> exceptionHandler;
  private Handler<Buffer> dataHandler;
  private Handler<Void> endHandler;
  private InboundBuffer<Object> pending;
  private boolean ended;
  private final Context context;

  public MultipartFormUpload(Context context,
                             MultipartForm parts,
                             boolean multipart,
                             HttpPostRequestEncoder.EncoderMode encoderMode) throws Exception {
    this.context = context;
    this.pending = new InboundBuffer<>(context)
      .handler(this::handleChunk)
      .drainHandler(v -> run()).pause();
    this.request = new DefaultFullHttpRequest(
      HttpVersion.HTTP_1_1,
      io.netty.handler.codec.http.HttpMethod.POST,
      "/");
    this.encoder = new HttpPostRequestEncoder(
      new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE),
      request,
      multipart,
      HttpConstants.DEFAULT_CHARSET,
      encoderMode);
    for (FormDataPart formDataPart : parts) {
      if (formDataPart.isAttribute()) {
        encoder.addBodyAttribute(formDataPart.name(), formDataPart.value());
      } else {
        encoder.addBodyFileUpload(formDataPart.name(),
          formDataPart.filename(), new File(formDataPart.pathname()),
          formDataPart.mediaType(), formDataPart.isText());
      }
    }
    encoder.finalizeRequest();
  }

  private void handleChunk(Object item) {
    Handler handler;
    synchronized (MultipartFormUpload.this) {
      if (item instanceof Buffer) {
        handler = dataHandler;
      } else if (item instanceof Throwable) {
        handler = exceptionHandler;
      } else if (item == InboundBuffer.END_SENTINEL) {
        handler = endHandler;
        item = null;
      } else {
        return;
      }
    }
    handler.handle(item);
  }

  public void run() {
    if (Vertx.currentContext() != context) {
      context.runOnContext(v -> {
        run();
      });
      return;
    }
    while (!ended) {
      if (encoder.isChunked()) {
        try {
          HttpContent chunk = encoder.readChunk(ALLOC);
          ByteBuf content = chunk.content();
          Buffer buff = Buffer.buffer(content);
          boolean writable = pending.write(buff);
          if (encoder.isEndOfInput()) {
            ended = true;
            request = null;
            encoder = null;
            pending.write(InboundBuffer.END_SENTINEL);
          } else if (!writable) {
            break;
          }
        } catch (Exception e) {
          ended = true;
          request = null;
          encoder = null;
          pending.write(e);
          break;
        }
      } else {
        ByteBuf content = request.content();
        Buffer buffer = Buffer.buffer(content);
        request = null;
        encoder = null;
        pending.write(buffer);
        ended = true;
        pending.write(InboundBuffer.END_SENTINEL);
      }
    }
  }

  public MultiMap headers() {
    return new HeadersAdaptor(request.headers());
  }

  @Override
  public synchronized MultipartFormUpload exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  @Override
  public synchronized MultipartFormUpload handler(Handler<Buffer> handler) {
    dataHandler = handler;
    return this;
  }

  @Override
  public synchronized MultipartFormUpload pause() {
    pending.pause();
    return this;
  }

  @Override
  public ReadStream<Buffer> fetch(long amount) {
    pending.fetch(amount);
    return this;
  }

  @Override
  public synchronized MultipartFormUpload resume() {
    pending.resume();
    return this;
  }

  @Override
  public synchronized MultipartFormUpload endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }
}
