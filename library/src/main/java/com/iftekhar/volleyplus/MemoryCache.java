package com.iftekhar.volleyplus;

/**
 * @author Iftekhar Ahmed
 */

/**
 * An interface defining simple methods required by {@link Loader} for caching data
 * in application specific RAM.
 *
 * @param <T> The data type.
 */
public interface MemoryCache<T> {
    /**
     * Returns the cache entry if found against the specified key.
     *
     * @param cacheKey The key for entry
     * @return the data, or null if not found
     */
    T get(String cacheKey);

    /**
     * Caches data against the specified key.
     *
     * @param cacheKey The key for data.
     * @param t        The data object.
     */
    void put(String cacheKey, T t);

    /**
     * Removes a cache entry with the specified key, if found.
     *
     * @param cacheKey The key for entry
     */
    void remove(String cacheKey);

    /**
     * Sets a new maximum size for the cache implementation.
     *
     * @param size The new max size of the cache.
     */
    void resize(int size);
}
