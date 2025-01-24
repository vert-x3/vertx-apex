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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import io.netty.handler.codec.http.multipart.MemoryFileUpload;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.impl.headers.HeadersAdaptor;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;
import io.vertx.ext.web.multipart.FormDataPart;
import io.vertx.ext.web.multipart.MultipartForm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

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
  private final InboundBuffer<Object> pending;
  private boolean ended;
  private final Context context;
  private final HttpVersion version;

  public MultipartFormUpload(Context context,
                             MultipartForm parts,
                             boolean multipart,
                             HttpPostRequestEncoder.EncoderMode encoderMode) throws Exception {
    this(context, parts, multipart, HttpVersion.HTTP_1_1, encoderMode);
  }

  public MultipartFormUpload(Context context,
                             MultipartForm parts,
                             boolean multipart,
                             HttpVersion version,
                             HttpPostRequestEncoder.EncoderMode encoderMode) throws Exception {
    this.context = context;
    this.version = version;
    this.pending = new InboundBuffer<>(context)
      .handler(this::handleChunk)
      .drainHandler(v -> run()).pause();
    this.request = new DefaultFullHttpRequest(
      io.netty.handler.codec.http.HttpVersion.HTTP_1_1, //this version is not used in any way
      io.netty.handler.codec.http.HttpMethod.POST,
      "/");
    parts.getCharset();
    Charset charset = parts.getCharset() != null ? parts.getCharset() : HttpConstants.DEFAULT_CHARSET;
    this.encoder = new HttpPostRequestEncoder(
      new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE, charset) {
        @Override
        public Attribute createAttribute(HttpRequest request, String name, String value) {
          try {
            return new MemoryAttribute(name, value, charset);
          } catch (IOException e) {
            throw new IllegalArgumentException(e);
          }
        }

        @Override
        public FileUpload createFileUpload(HttpRequest request, String name, String filename, String contentType, String contentTransferEncoding, Charset _charset, long size) {
          if (_charset == null) {
            _charset = charset;
          }
          return super.createFileUpload(request, name, filename, contentType, contentTransferEncoding, _charset, size);
        }
      },
      request,
      multipart,
      charset,
      encoderMode);
    for (FormDataPart formDataPart : parts) {
      if (formDataPart.isAttribute()) {
        encoder.addBodyAttribute(formDataPart.name(), formDataPart.value());
      } else {
        String pathname = formDataPart.pathname();
        if (pathname != null) {
          encoder.addBodyFileUpload(formDataPart.name(),
            formDataPart.filename(), new File(formDataPart.pathname()),
            formDataPart.mediaType(), formDataPart.isText());
        } else {
          String contentType = formDataPart.mediaType();
          if (formDataPart.mediaType() == null) {
            if (formDataPart.isText()) {
              contentType = "text/plain";
            } else {
              contentType = "application/octet-stream";
            }
          }
          String transferEncoding = formDataPart.isText() ? null : "binary";
          MemoryFileUpload fileUpload = new MemoryFileUpload(
            formDataPart.name(),
            formDataPart.filename(),
            contentType, transferEncoding, null, formDataPart.content().length());
          fileUpload.setContent(formDataPart.content().getByteBuf());
          encoder.addBodyHttpData(fileUpload);
        }
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
      throw new IllegalArgumentException();
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

  /**
   * Remove the transfer encoding header if the version is not HTTP/1.0 or HTTP/1.1 as indicated in the
   * <a href="https://datatracker.ietf.org/doc/html/rfc7540#section-8.1.2.2">RFC 7540 Section 8.1.2.2</a>.
   * Currently, it is verbose, but making sure this does not need to be fixed for HTTP/3 again.
   *
   * @return the headers appropriate to include with the form.
   */
  public MultiMap headers() {
    HeadersAdaptor adaptor = new HeadersAdaptor(request.headers());
    // Currently it is verbose, but making sure this does not need to be fixed for HTTP/3 again.
    if (this.encoder.isChunked() && !(this.version == HttpVersion.HTTP_1_1 || this.version == HttpVersion.HTTP_1_0)) {
      final long realSize = this.encoder.length();
      adaptor.remove(io.vertx.core.http.HttpHeaders.TRANSFER_ENCODING);
      adaptor.set(io.vertx.core.http.HttpHeaders.CONTENT_LENGTH, String.valueOf(realSize));
    }
    return adaptor;
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
