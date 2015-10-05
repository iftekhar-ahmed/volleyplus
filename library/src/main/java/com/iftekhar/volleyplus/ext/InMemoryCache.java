package com.iftekhar.volleyplus.ext;

import android.support.v4.util.LruCache;

import com.android.volley.Cache;

import java.util.Collection;
import java.util.Set;

/**
 * An implementation of {@link Cache} that stores per-request response data
 * ({@link com.android.volley.Cache.Entry}) to a {@link LruCache}. The default
 * memory size is 1/8 th of the maximum number of bytes heap size can extend,
 * but this can be configured.
 */
public class InMemoryCache implements Cache {

    /**
     * Maximum available memory to the global single application, in bytes.
     */
    public static final int MAX_MEMORY = (int) (Runtime.getRuntime().maxMemory());

    /**
     * Default maximum memory usage in bytes
     */
    private static final int DEFAULT_MAX_MEMORY_BYTES = MAX_MEMORY / 8;

    /**
     * Maximum size of the in-memory cache in bytes
     */
    private final int mMaxCacheSizeInBytes;

    /**
     * The memory-based cache implementation.
     */
    private LruCache<String, Entry> mCache;

    public InMemoryCache() {
        this(DEFAULT_MAX_MEMORY_BYTES);
    }

    /**
     * Constructs an instance of the InMemoryCache.
     *
     * @param maxCacheSizeInBytes maximum size of the in-memory cache in bytes
     */
    public InMemoryCache(int maxCacheSizeInBytes) {
        mMaxCacheSizeInBytes = maxCacheSizeInBytes <= MAX_MEMORY ? maxCacheSizeInBytes : DEFAULT_MAX_MEMORY_BYTES;
    }

    /**
     * Approximates the size of an Entry object
     *
     * @param entry the object to calculate size
     * @return the collective size of public properties in the entry object in bytes
     */
    private int getEntrySizeInBytes(Entry entry) {
        int bytes = entry.data.length;
        bytes += entry.etag != null ? entry.etag.getBytes().length : 0;
        // calculate byte count of headers
        if (entry.responseHeaders != null && !entry.responseHeaders.isEmpty()) {
            Set<String> keys = entry.responseHeaders.keySet();
            for (String key : keys) {
                bytes += key.getBytes().length;
            }
            Collection<String> values = entry.responseHeaders.values();
            for (String value : values) {
                bytes += value.getBytes().length;
            }
        }
        // calculate sizes of entry.serverDate, entry.ttl & entry.softTtl
        bytes += 3 * (Long.SIZE / 8);
        return bytes;
    }

    @Override
    public Entry get(String key) {
        return mCache.get(key);
    }

    @Override
    public void put(String key, Entry entry) {
        mCache.put(key, entry);
    }

    @Override
    public void initialize() {
        mCache = new LruCache<String, Entry>(mMaxCacheSizeInBytes) {
            @Override
            protected int sizeOf(String key, Entry entry) {
                return getEntrySizeInBytes(entry);
            }
        };
    }

    @Override
    public void invalidate(String key, boolean fullExpire) {
        Entry entry = get(key);
        if (entry != null) {
            entry.softTtl = 0;
            if (fullExpire) {
                entry.ttl = 0;
            }
            put(key, entry);
        }
    }

    @Override
    public void remove(String key) {
        mCache.remove(key);
    }

    @Override
    public void clear() {
        mCache.evictAll();
    }
}
