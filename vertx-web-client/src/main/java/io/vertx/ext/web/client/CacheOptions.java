package io.vertx.ext.web.client;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.ext.web.client.impl.CacheInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.vertx.ext.web.client.impl.CacheInterceptor.CacheKeyValue.*;
import static io.vertx.ext.web.client.impl.CacheInterceptor.CacheKeyValue.CONTENT_TYPE;
import static io.vertx.ext.web.client.impl.CacheInterceptor.CacheKeyValue.PARAMS;

/**
 * @author Alexey Soshin
 */
@DataObject
public class CacheOptions {

    private final List<CacheInterceptor.CacheKeyValue> DEFAULT_CACHE_KEY_STRUCTURE = Arrays.asList(
            METHOD, HOST, PORT, URI, PARAMS, CONTENT_TYPE
    );

    private int maxEntries = 0;
    private List<CacheInterceptor.CacheKeyValue> cacheKeyValue = DEFAULT_CACHE_KEY_STRUCTURE;

    public CacheOptions() {

    }

    public CacheOptions(CacheOptions other) {
        if (other != null) {
            this.maxEntries = other.getMaxEntries();
            if (other.cacheKeyValue != null) {
                this.cacheKeyValue = new ArrayList<>(other.cacheKeyValue);
            }
        }
    }

    /**
     * Passing zero or negative value disables the cache
     * @param maxEntries
     * @return
     */
    public CacheOptions setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
        return this;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public List<CacheInterceptor.CacheKeyValue> getCacheKeyValue() {
        return cacheKeyValue;
    }

    public CacheOptions setCacheKeyValue(List<CacheInterceptor.CacheKeyValue> cacheKeyValue) {
        this.cacheKeyValue = new ArrayList<>(cacheKeyValue);
        return this;
    }
}
