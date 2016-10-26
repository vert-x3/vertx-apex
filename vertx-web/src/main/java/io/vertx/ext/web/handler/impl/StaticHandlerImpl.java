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

import io.vertx.core.*;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.impl.LRUCache;
import io.vertx.ext.web.impl.Utils;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Static web server
 * Parts derived from Yoke
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://pmlopes@gmail.com">Paulo Lopes</a>
 */
public class StaticHandlerImpl implements StaticHandler {

  private static final Logger log = LoggerFactory.getLogger(StaticHandlerImpl.class);
  private static final String defaultContentEncoding = Charset.defaultCharset().name();

  private final DateFormat dateTimeFormatter = Utils.createRFC1123DateTimeFormatter();
  private Map<String, CacheEntry> propsCache;
  private String webRoot = DEFAULT_WEB_ROOT;
  private long maxAgeSeconds = DEFAULT_MAX_AGE_SECONDS; // One day
  private boolean directoryListing = DEFAULT_DIRECTORY_LISTING;
  private String directoryTemplateResource = DEFAULT_DIRECTORY_TEMPLATE;
  private String directoryTemplate;
  private boolean includeHidden = DEFAULT_INCLUDE_HIDDEN;
  private boolean filesReadOnly = DEFAULT_FILES_READ_ONLY;
  private boolean cachingEnabled = DEFAULT_CACHING_ENABLED;
  private long cacheEntryTimeout = DEFAULT_CACHE_ENTRY_TIMEOUT;
  private String indexPage = DEFAULT_INDEX_PAGE;
  private int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;
  private boolean rangeSupport = DEFAULT_RANGE_SUPPORT;
  private JsonObject http2PushMapping = null;
  private boolean allowRootFileSystemAccess = DEFAULT_ROOT_FILESYSTEM_ACCESS;
  private boolean sendVaryHeader = DEFAULT_SEND_VARY_HEADER;

  // These members are all related to auto tuning of synchronous vs asynchronous file system access
  private static int NUM_SERVES_TUNING_FS_ACCESS = 1000;
  private boolean alwaysAsyncFS = DEFAULT_ALWAYS_ASYNC_FS;
  private long maxAvgServeTimeNanoSeconds = DEFAULT_MAX_AVG_SERVE_TIME_NS;
  private boolean tuning = DEFAULT_ENABLE_FS_TUNING;
  private long totalTime;
  private long numServesBlocking;
  private boolean useAsyncFS;
  private long nextAvgCheck = NUM_SERVES_TUNING_FS_ACCESS;

  private final ClassLoader classLoader;

  public StaticHandlerImpl(String root, ClassLoader classLoader) {
    this.classLoader = classLoader;
    setRoot(root);
  }

  public StaticHandlerImpl() {
    classLoader = null;
  }

  private String directoryTemplate(Vertx vertx) {
    if (directoryTemplate == null) {
      directoryTemplate = Utils.readFileToString(vertx, directoryTemplateResource);
    }
    return directoryTemplate;
  }

  /**
   * Create all required header so content can be cache by Caching servers or Browsers
   *
   * @param response base HttpServerResponse
   * @param props   file properties
   */
  private void writeCacheHeaders(HttpServerResponse response, FileProps props) {

    MultiMap headers = response.headers();

    if (cachingEnabled) {
      // We use cache-control and last-modified
      // We *do not use* etags and expires (since they do the same thing - redundant)
      headers.set("cache-control", "public, max-age=" + maxAgeSeconds);
      headers.set("last-modified", dateTimeFormatter.format(props.lastModifiedTime()));
      // We send the vary header (for intermediate caches)
      // (assumes that most will turn on compression when using static handler)
      if (sendVaryHeader && request.headers().contains("accept-encoding")) {
        headers.set("vary", "accept-encoding");
      }
    }

    // date header is mandatory
    headers.set("date", dateTimeFormatter.format(new Date()));
  }

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
      if (log.isTraceEnabled()) log.trace("Not GET or HEAD so ignoring request");
      context.next();
    } else {
      String path = context.normalisedPath();
      // if the normalized path is null it cannot be resolved
      if (path == null) {
        log.warn("Invalid path: " + context.request().path() + " so returning 404");
        context.fail(NOT_FOUND.code());
        return;
      }

      // only root is known for sure to be a directory. all other directories must be identified as such.
      if (!directoryListing && "/".equals(path)) {
        path = indexPage;
      }

      // can be called recursive for index pages
      sendStatic(context, path);

    }
  }

  private void sendStatic(RoutingContext context, String path) {

    String file = null;

    if (!includeHidden) {
      file = getFile(path, context);
      int idx = file.lastIndexOf('/');
      String name = file.substring(idx + 1);
      if (name.length() > 0 && name.charAt(0) == '.') {
        context.fail(NOT_FOUND.code());
        return;
      }
    }

    // Look in cache
    CacheEntry entry;
    if (cachingEnabled) {
      entry = propsCache().get(path);
      if (entry != null) {
        HttpServerRequest request = context.request();
        if ((filesReadOnly || !entry.isOutOfDate()) && entry.shouldUseCached(request)) {
          context.response().setStatusCode(NOT_MODIFIED.code()).end();
          return;
        }
      }
    }

    if (file == null) {
      file = getFile(path, context);
    }

    String sfile = file;

    // Need to read the props from the filesystem
    getFileProps(context.vertx().fileSystem(), file, res -> {
      if (res.succeeded()) {
        FileProps fprops = res.result();
        if (fprops == null) {
          // File does not exist
          context.fail(NOT_FOUND.code());
        } else if (fprops.isDirectory()) {
          sendDirectory(context, path, sfile);
        } else {
          propsCache().put(path, new CacheEntry(fprops, System.currentTimeMillis()));
          sendFile(context, sfile, fprops);
        }
      } else {
        if (res.cause() instanceof NoSuchFileException || (res.cause().getCause() != null && res.cause().getCause() instanceof NoSuchFileException)) {
          context.fail(NOT_FOUND.code());
        } else {
          context.fail(res.cause());
        }
      }
    });
  }

  private void sendDirectory(RoutingContext context, String path, String file) {
    if (directoryListing) {
      sendDirectoryListing(file, context);
    } else if (indexPage != null) {
      // send index page
      String indexPath;
      if (path.endsWith("/") && indexPage.startsWith("/")) {
        indexPath = path + indexPage.substring(1);
      } else if (!path.endsWith("/") && !indexPage.startsWith("/")) {
        indexPath = path + "/" + indexPage.substring(1);
      } else {
        indexPath = path + indexPage;
      }
      // recursive call
      sendStatic(context, indexPath);

    } else {
      // Directory listing denied
      context.fail(FORBIDDEN.code());
    }
  }

  private <T> T wrapInTCCLSwitch(Callable<T> callable) {
    try {
      if (classLoader == null) {
        return callable.call();
      } else {
        final ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
          Thread.currentThread().setContextClassLoader(classLoader);
          return callable.call();
        } finally {
          Thread.currentThread().setContextClassLoader(original);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private synchronized void getFileProps(FileSystem fs, String file, Handler<AsyncResult<FileProps>> resultHandler) {
    if (alwaysAsyncFS || useAsyncFS) {
      wrapInTCCLSwitch(() -> fs.props(file, resultHandler));
    } else {
      // Use synchronous access - it might well be faster!
      long start = 0;
      if (tuning) {
        start = System.nanoTime();
      }
      try {
        FileProps props = wrapInTCCLSwitch(() -> fs.propsBlocking(file));

        if (tuning) {
          long end = System.nanoTime();
          long dur = end - start;
          totalTime += dur;
          numServesBlocking++;
          if (numServesBlocking == Long.MAX_VALUE) {
            // Unlikely.. but...
            resetTuning();
          } else if (numServesBlocking == nextAvgCheck) {
            double avg = (double) totalTime / numServesBlocking;
            if (avg > maxAvgServeTimeNanoSeconds) {
              useAsyncFS = true;
              log.info("Switching to async file system access in static file server as fs access is slow! (Average access time of " + avg + " ns)");
              tuning = false;
            }
            nextAvgCheck += NUM_SERVES_TUNING_FS_ACCESS;
          }
        }
        resultHandler.handle(Future.succeededFuture(props));
      } catch (RuntimeException e) {
        resultHandler.handle(Future.failedFuture(e.getCause()));
      }
    }
  }

  private void resetTuning() {
    // Reset
    nextAvgCheck = NUM_SERVES_TUNING_FS_ACCESS;
    totalTime = 0;
    numServesBlocking = 0;
  }

  private static final Pattern RANGE = Pattern.compile("^bytes=(\\d+)-(\\d*)$");

  private void sendFile(RoutingContext context, String file, FileProps fileProps) {
    final HttpServerRequest request = context.request();
    final HttpServerResponse response = request.response();
    final Vertx vertx = context.vertx();

    Long offset = null;
    Long end = null;
    MultiMap headers = null;

    if (rangeSupport) {
      // check if the client is making a range request
      String range = request.getHeader("Range");
      // end byte is length - 1
      end = fileProps.size() - 1;

      if (range != null) {
        Matcher m = RANGE.matcher(range);
        if (m.matches()) {
          try {
            String part = m.group(1);
            // offset cannot be empty
            offset = Long.parseLong(part);
            // offset must fall inside the limits of the file
            if (offset < 0 || offset >= fileProps.size()) {
              throw new IndexOutOfBoundsException();
            }
            // length can be empty
            part = m.group(2);
            if (part != null && part.length() > 0) {
              // ranges are inclusive
              end = Long.parseLong(part);
              // offset must fall inside the limits of the file
              if (end < offset || end >= fileProps.size()) {
                throw new IndexOutOfBoundsException();
              }
            }
          } catch (NumberFormatException | IndexOutOfBoundsException e) {
            context.response().putHeader("Content-Range", "bytes */" + fileProps.size());
            context.fail(REQUESTED_RANGE_NOT_SATISFIABLE.code());
            return;
          }
        }
      }

      // notify client we support range requests
      headers = response.headers();
      headers.set("Accept-Ranges", "bytes");
      // send the content length even for HEAD requests
      headers.set("Content-Length", Long.toString(end + 1 - (offset == null ? 0 : offset)));
    }

    writeCacheHeaders(response, fileProps);

    if (request.method() == HttpMethod.HEAD) {
      response.end();
    } else {
      if (rangeSupport && offset != null) {
        // must return content range
        headers.set("Content-Range", "bytes " + offset + "-" + end + "/" + fileProps.size());
        // return a partial response
        response.setStatusCode(PARTIAL_CONTENT.code());

        // Wrap the sendFile operation into a TCCL switch, so the file resolver would find the file from the set
        // classloader (if any).
        final Long finalOffset = offset;
        final Long finalEnd = end;
        wrapInTCCLSwitch(() -> {
          // guess content type
          String contentType = MimeMapping.getMimeTypeForFilename(file);
          if (contentType != null) {
            if (contentType.startsWith("text")) {
              response.putHeader("Content-Type", contentType + ";charset=" + defaultContentEncoding);
            } else {
              response.putHeader("Content-Type", contentType);
            }
          }

          return response.sendFile(file, finalOffset, finalEnd + 1, res2 -> {
            if (res2.failed()) {
              context.fail(res2.cause());
            }
          });
        });
      } else {
        // Wrap the sendFile operation into a TCCL switch, so the file resolver would find the file from the set
        // classloader (if any).
        wrapInTCCLSwitch(() -> {
          // guess content type
          String contentType = MimeMapping.getMimeTypeForFilename(file);
          if (contentType != null) {
            if (contentType.startsWith("text")) {
              response.putHeader("Content-Type", contentType + ";charset=" + defaultContentEncoding);
            } else {
              response.putHeader("Content-Type", contentType);
            }
          }

          // http2 pushing support
          if (context.request().version() == HttpVersion.HTTP_2 && http2PushMapping != null) {
            System.out.println("looking up " + file.substring(webRoot.length() + 1));
            JsonArray dependencies = http2PushMapping.getJsonArray(file.substring(webRoot.length() + 1));
            if (dependencies != null) {
//              final AtomicInteger counter = new AtomicInteger(dependencies.size());
//
//              new Handler<Integer>() {
//                @Override
//                public void handle(Integer i) {
//                  if (i == -1) {
//                    // terminal state
//                    response.sendFile(file, res2 -> {
//                      if (res2.failed()) {
//                        context.fail(res2.cause());
//                      }
//                    });
//                    return;
//                  }
//
//                  final Handler<Integer> self = this;
//                  final String dep = webRoot + "/" + dependencies.getString(i);
//                  // get the file props
//                  System.out.println("looking up props for " + dep);
//                  getFileProps(vertx.fileSystem(), dep, filePropsAsyncResult -> {
//                    if (filePropsAsyncResult.succeeded()) {
//                      // push
//                      System.out.println("pushing /" + dependencies.getString(i));
//                      response.push(HttpMethod.GET, "/" + dependencies.getString(i), pushAsyncResult -> {
//                        if (pushAsyncResult.succeeded()) {
//                          final HttpServerResponse res = pushAsyncResult.result();
//
//                          writeCacheHeaders(res, filePropsAsyncResult.result());
//                          // guess content type
//                          final String depContentType = MimeMapping.getMimeTypeForFilename(file);
//
//                          if (depContentType != null) {
//                            if (depContentType.startsWith("text")) {
//                              res.putHeader("Content-Type", contentType + ";charset=" + defaultContentEncoding);
//                            } else {
//                              res.putHeader("Content-Type", contentType);
//                            }
//                          }
//                          // start pushing data
//                          res.sendFile(dep, sendFileAsyncResult -> {
//                            if (sendFileAsyncResult.failed()) {
//                              // skip the rest
//                              self.handle(-1);
//                              return;
//                            }
//                            // push the next one
//                            self.handle(counter.decrementAndGet());
//                          });
//                        }
//                      });
//                    }
//                  });
//                }
//              }.handle(counter.decrementAndGet());

              for (Object dependency : dependencies) {
                final String dep = webRoot + "/" + dependency;
                // get the file props
                System.out.println("looking up props for " + dep);
                getFileProps(vertx.fileSystem(), dep, filePropsAsyncResult -> {
                  if (filePropsAsyncResult.succeeded()) {
                    // push
                    System.out.println("pushing /" + dependency);
                    response.push(HttpMethod.GET, "/" + dependency, pushAsyncResult -> {
                      if (pushAsyncResult.succeeded()) {
                        final HttpServerResponse res = pushAsyncResult.result();

                        writeCacheHeaders(res, filePropsAsyncResult.result());
                        // guess content type
                        final String depContentType = MimeMapping.getMimeTypeForFilename(file);

                        if (depContentType != null) {
                          if (depContentType.startsWith("text")) {
                            res.putHeader("Content-Type", contentType + ";charset=" + defaultContentEncoding);
                          } else {
                            res.putHeader("Content-Type", contentType);
                          }
                        }
                        // start pushing data
                        res.sendFile(dep);
                      }
                    });
                  }
                });
              }
            }
          }

          return response.sendFile(file, res2 -> {
            if (res2.failed()) {
              context.fail(res2.cause());
            }
          });
        });
      }
    }
  }

  @Override
  public StaticHandler setAllowRootFileSystemAccess(boolean allowRootFileSystemAccess) {
    this.allowRootFileSystemAccess = allowRootFileSystemAccess;
    return this;
  }

  @Override
  public StaticHandler setWebRoot(String webRoot) {
    setRoot(webRoot);
    return this;
  }

  @Override
  public StaticHandler setFilesReadOnly(boolean readOnly) {
    this.filesReadOnly = readOnly;
    return this;
  }

  @Override
  public StaticHandler setMaxAgeSeconds(long maxAgeSeconds) {
    if (maxAgeSeconds < 0) {
      throw new IllegalArgumentException("timeout must be >= 0");
    }
    this.maxAgeSeconds = maxAgeSeconds;
    return this;
  }

  @Override
  public StaticHandler setMaxCacheSize(int maxCacheSize) {
    if (maxCacheSize < 1) {
      throw new IllegalArgumentException("maxCacheSize must be >= 1");
    }
    this.maxCacheSize = maxCacheSize;
    return this;
  }

  @Override
  public StaticHandler setCachingEnabled(boolean enabled) {
    this.cachingEnabled = enabled;
    return this;
  }

  @Override
  public StaticHandler setDirectoryListing(boolean directoryListing) {
    this.directoryListing = directoryListing;
    return this;
  }

  @Override
  public StaticHandler setDirectoryTemplate(String directoryTemplate) {
    this.directoryTemplateResource = directoryTemplate;
    this.directoryTemplate = null;
    return this;
  }

  @Override
  public StaticHandler setEnableRangeSupport(boolean enableRangeSupport) {
    this.rangeSupport = enableRangeSupport;
    return this;
  }

  @Override
  public StaticHandler setHTTP2PushMapping(JsonObject http2PushMapping) {
    this.http2PushMapping = http2PushMapping;
    return this;
  }

  @Override
  public StaticHandler setIncludeHidden(boolean includeHidden) {
    this.includeHidden = includeHidden;
    return this;
  }

  @Override
  public StaticHandler setCacheEntryTimeout(long timeout) {
    if (timeout < 1) {
      throw new IllegalArgumentException("timeout must be >= 1");
    }
    this.cacheEntryTimeout = timeout;
    return this;
  }

  @Override
  public StaticHandler setIndexPage(String indexPage) {
    Objects.requireNonNull(indexPage);
    if (!indexPage.startsWith("/")) {
      indexPage = "/" + indexPage;
    }
    this.indexPage = indexPage;
    return this;
  }

  @Override
  public StaticHandler setAlwaysAsyncFS(boolean alwaysAsyncFS) {
    this.alwaysAsyncFS = alwaysAsyncFS;
    return this;
  }

  @Override
  public synchronized StaticHandler setEnableFSTuning(boolean enableFSTuning) {
    this.tuning = enableFSTuning;
    if (!tuning) {
      resetTuning();
    }
    return this;
  }

  @Override
  public StaticHandler setMaxAvgServeTimeNs(long maxAvgServeTimeNanoSeconds) {
    this.maxAvgServeTimeNanoSeconds = maxAvgServeTimeNanoSeconds;
    return this;
  }
  
  @Override
  public StaticHandler setSendVaryHeader(boolean sendVaryHeader) {
    this.sendVaryHeader = sendVaryHeader;
    return this;
  }

  private Map<String, CacheEntry> propsCache() {
    if (propsCache == null) {
      propsCache = new LRUCache<>(maxCacheSize);
    }
    return propsCache;
  }

  private Date parseDate(String header) {
    try {
      return dateTimeFormatter.parse(header);
    } catch (ParseException e) {
      throw new VertxException(e);
    }
  }

  private String getFile(String path, RoutingContext context) {
    String file = webRoot + Utils.pathOffset(path, context);
    if (log.isTraceEnabled()) log.trace("File to serve is " + file);
    return file;
  }

  private void setRoot(String webRoot) {
    Objects.requireNonNull(webRoot);
    if (!allowRootFileSystemAccess) {
      for (File root : File.listRoots()) {
        if (webRoot.startsWith(root.getAbsolutePath())) {
          throw new IllegalArgumentException("root cannot start with '" + root.getAbsolutePath() + "'");
        }
      }
    }
    this.webRoot = webRoot;
  }

  private void sendDirectoryListing(String dir, RoutingContext context) {
    FileSystem fileSystem = context.vertx().fileSystem();
    HttpServerRequest request = context.request();

    fileSystem.readDir(dir, asyncResult -> {
      if (asyncResult.failed()) {
        context.fail(asyncResult.cause());
      } else {

        String accept = request.headers().get("accept");
        if (accept == null) {
          accept = "text/plain";
        }

        if (accept.contains("html")) {
          String normalizedDir = context.normalisedPath();
          if (!normalizedDir.endsWith("/")) {
            normalizedDir += "/";
          }

          String file;
          StringBuilder files = new StringBuilder("<ul id=\"files\">");

          List<String> list = asyncResult.result();
          Collections.sort(list);

          for (String s : list) {
            file = s.substring(s.lastIndexOf(File.separatorChar) + 1);
            // skip dot files
            if (!includeHidden && file.charAt(0) == '.') {
              continue;
            }
            files.append("<li><a href=\"");
            files.append(normalizedDir);
            files.append(file);
            files.append("\" title=\"");
            files.append(file);
            files.append("\">");
            files.append(file);
            files.append("</a></li>");
          }

          files.append("</ul>");

          // link to parent dir
          int slashPos = 0;
          for (int i = normalizedDir.length() - 2; i > 0; i--) {
            if (normalizedDir.charAt(i) == '/') {
              slashPos = i;
              break;
            }
          }

          String parent = "<a href=\"" + normalizedDir.substring(0, slashPos + 1) + "\">..</a>";

          request.response().putHeader("content-type", "text/html");
          request.response().end(
              directoryTemplate(context.vertx()).replace("{directory}", normalizedDir)
                  .replace("{parent}", parent)
                  .replace("{files}", files.toString()));
        } else if (accept.contains("json")) {
          String file;
          JsonArray json = new JsonArray();

          for (String s : asyncResult.result()) {
            file = s.substring(s.lastIndexOf(File.separatorChar) + 1);
            // skip dot files
            if (!includeHidden && file.charAt(0) == '.') {
              continue;
            }
            json.add(file);
          }
          request.response().putHeader("content-type", "application/json");
          request.response().end(json.encode());
        } else {
          String file;
          StringBuilder buffer = new StringBuilder();

          for (String s : asyncResult.result()) {
            file = s.substring(s.lastIndexOf(File.separatorChar) + 1);
            // skip dot files
            if (!includeHidden && file.charAt(0) == '.') {
              continue;
            }
            buffer.append(file);
            buffer.append('\n');
          }

          request.response().putHeader("content-type", "text/plain");
          request.response().end(buffer.toString());
        }
      }
    });
  }

  // TODO make this static and use Java8 DateTimeFormatter
  private final class CacheEntry {
    final FileProps props;
    long createDate;

    private CacheEntry(FileProps props, long createDate) {
      this.props = props;
      this.createDate = createDate;
    }

    // return true if there are conditional headers present and they match what is in the entry
    boolean shouldUseCached(HttpServerRequest request) {
      String ifModifiedSince = request.headers().get("if-modified-since");
      if (ifModifiedSince == null) {
        // Not a conditional request
        return false;
      }
      Date ifModifiedSinceDate = parseDate(ifModifiedSince);
      boolean modifiedSince = Utils.secondsFactor(props.lastModifiedTime()) > ifModifiedSinceDate.getTime();
      return !modifiedSince;
    }

    boolean isOutOfDate() {
      return System.currentTimeMillis() - createDate > cacheEntryTimeout;
    }

  }


}
