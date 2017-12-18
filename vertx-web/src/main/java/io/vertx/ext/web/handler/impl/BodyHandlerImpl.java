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

package io.vertx.ext.web.handler.impl;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.impl.FileUploadImpl;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class BodyHandlerImpl implements BodyHandler {

  private static final Logger log = LoggerFactory.getLogger(BodyHandlerImpl.class);

  private static final String BODY_HANDLED = "__body-handled";
  private static final String WEBSOCKET_HEADER = HttpHeaders.WEBSOCKET.toString();

  private long bodyLimit = DEFAULT_BODY_LIMIT;
  private String uploadsDir;
  private boolean mergeFormAttributes = DEFAULT_MERGE_FORM_ATTRIBUTES;
  private boolean deleteUploadedFilesOnEnd = DEFAULT_DELETE_UPLOADED_FILES_ON_END;

  public BodyHandlerImpl() {
    setUploadsDirectory(DEFAULT_UPLOADS_DIRECTORY);
  }

  public BodyHandlerImpl(String uploadDirectory) {
    setUploadsDirectory(uploadDirectory);
  }

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    // FIXME when MultiMap has a contains(name,value) method, remove the constant and simplify this test
    String upgradeHeader = request.getHeader(HttpHeaders.UPGRADE);
    if (upgradeHeader != null && upgradeHeader.equalsIgnoreCase(WEBSOCKET_HEADER)) {
      context.next();
      return;
    }

    // we need to keep state since we can be called again on reroute
    Boolean handled = context.get(BODY_HANDLED);
    if (handled == null || !handled) {
      BHandler handler = new BHandler(context);
      request.handler(handler);
      request.endHandler(v -> handler.end());
      context.put(BODY_HANDLED, true);
    } else {
      // on reroute we need to re-merge the form params if that was desired
      if (mergeFormAttributes && request.isExpectMultipart()) {
        request.params().addAll(request.formAttributes());
      }

      context.next();
    }
  }

  @Override
  public BodyHandler setBodyLimit(long bodyLimit) {
    this.bodyLimit = bodyLimit;
    return this;
  }

  @Override
  public BodyHandler setUploadsDirectory(String uploadsDirectory) {
    this.uploadsDir = uploadsDirectory;
    return this;
  }

  @Override
  public BodyHandler setMergeFormAttributes(boolean mergeFormAttributes) {
    this.mergeFormAttributes = mergeFormAttributes;
    return this;
  }

  @Override
  public BodyHandler setDeleteUploadedFilesOnEnd(boolean deleteUploadedFilesOnEnd) {
    this.deleteUploadedFilesOnEnd = deleteUploadedFilesOnEnd;
    return this;
  }

  private class BHandler implements Handler<Buffer> {

    RoutingContext context;
    Buffer body = Buffer.buffer();
    boolean failed;
    AtomicInteger uploadCount = new AtomicInteger();
    boolean ended;
    long uploadSize = 0L;

    final boolean isMultipart;
    final boolean isUrlEncoded;

    public BHandler(RoutingContext context) {
      this.context = context;
      this.context.request().connection().closeHandler(event -> deleteFileUploads());

      Set<FileUpload> fileUploads = context.fileUploads();

      final String contentType = context.request().getHeader(HttpHeaders.CONTENT_TYPE);
      if (contentType == null) {
        isMultipart = false;
        isUrlEncoded = false;
      } else {
        final String lowerCaseContentType = contentType.toLowerCase();
        isMultipart = lowerCaseContentType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString());
        isUrlEncoded = lowerCaseContentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString());
      }

      if (isMultipart || isUrlEncoded) {
        makeUploadDir(context.vertx().fileSystem());
        context.request().setExpectMultipart(true);
        context.request().uploadHandler(upload -> {
          if (bodyLimit != -1 && upload.isSizeAvailable()) {
            // we can try to abort even before the upload starts
            long size = uploadSize + upload.size();
            if (size > bodyLimit) {
              failed = true;
              context.fail(413);
              return;
            }
          }
          // we actually upload to a file with a generated filename
          uploadCount.incrementAndGet();
          String uploadedFileName = new File(uploadsDir, UUID.randomUUID().toString()).getPath();
          upload.streamToFileSystem(uploadedFileName);
          FileUploadImpl fileUpload = new FileUploadImpl(uploadedFileName, upload);
          fileUploads.add(fileUpload);
          upload.exceptionHandler(context::fail);
          upload.endHandler(v -> uploadEnded());
        });
      }
      context.request().exceptionHandler(context::fail);
    }

    private void makeUploadDir(FileSystem fileSystem) {
      if (!fileSystem.existsBlocking(uploadsDir)) {
        fileSystem.mkdirsBlocking(uploadsDir);
      }
    }

    @Override
    public void handle(Buffer buff) {
      if (failed) {
        return;
      }
      uploadSize += buff.length();
      if (bodyLimit != -1 && uploadSize > bodyLimit) {
        failed = true;
        context.fail(413);
        // enqueue a delete for the error uploads
        context.vertx().runOnContext(v -> deleteFileUploads());
      } else {
        // multipart requests will not end up in the request body
        // url encoded should also not, however jQuery by default
        // post in urlencoded even if the payload is something else
        if (!isMultipart /* && !isUrlEncoded */) {
          body.appendBuffer(buff);
        }
      }
    }

    void uploadEnded() {
      int count = uploadCount.decrementAndGet();
      // only if parsing is done and count is 0 then all files have been processed
      if (ended && count == 0) {
        doEnd();
      }
    }

    void end() {
      // this marks the end of body parsing, calling doEnd should
      // only be possible from this moment onwards
      ended = true;

      // only if parsing is done and count is 0 then all files have been processed
      if (uploadCount.get() == 0) {
        doEnd();
      }
    }

    void doEnd() {

      if (failed) {
        deleteFileUploads();
        return;
      }

      if (deleteUploadedFilesOnEnd) {
        context.addBodyEndHandler(x -> deleteFileUploads());
      }

      HttpServerRequest req = context.request();
      if (mergeFormAttributes && req.isExpectMultipart()) {
        req.params().addAll(req.formAttributes());
      }
      context.setBody(body);
      context.next();
    }

    private void deleteFileUploads() {
      for (FileUpload fileUpload : context.fileUploads()) {
        FileSystem fileSystem = context.vertx().fileSystem();
        String uploadedFileName = fileUpload.uploadedFileName();
        fileSystem.exists(uploadedFileName, existResult -> {
          if (existResult.failed()) {
            log.warn("Could not detect if uploaded file exists, not deleting: " + uploadedFileName, existResult.cause());
          } else if (existResult.result()) {
            fileSystem.delete(uploadedFileName, deleteResult -> {
              if (deleteResult.failed()) {
                log.warn("Delete of uploaded file failed: " + uploadedFileName, deleteResult.cause());
              }
            });
          }
        });
      }
    }
  }

}
