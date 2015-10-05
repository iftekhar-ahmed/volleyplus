package com.iftekhar.volleyplus;

/**
 * @author Iftekhar Ahmed
 */

/**
 * Container object for all of the data surrounding an image request.
 *
 * @param <T> The type of data
 */
public class DataContainer<T> {

    /**
     * The data of specified type.
     */
    protected T mData;

    /**
     * The request URL that was specified
     */
    protected final String mRequestUrl;

    /**
     * helps to find if data loading is being observed or should this should be purged.
     */
    protected final Loader.OnLoadListener<T> mListener;

    /**
     * The cache key that was associated with the request
     */
    protected final String mCacheKey;

    /**
     * Constructs a DataContainer object.
     *
     * @param data         The final data (if it exists).
     * @param requestUrl   The requested URL for this container.
     * @param cacheKey     The cache key that identifies the requested URL for this container.
     * @param loadListener The listener to call upon loading.
     */
    public DataContainer(T data, String requestUrl, String cacheKey, Loader.OnLoadListener<T> loadListener) {
        this.mData = data;
        mCacheKey = cacheKey;
        mRequestUrl = requestUrl;
        mListener = loadListener;
    }

    /**
     * Returns the data associated with the request URL if it has been loaded, null otherwise.
     */
    public T get() {
        return mData;
    }

    /**
     * Returns the requested URL for this container.
     */
    public String getRequestUrl() {
        return mRequestUrl;
    }
}
