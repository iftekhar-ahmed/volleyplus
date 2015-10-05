package com.iftekhar.volleyplus;

import android.os.Handler;
import android.os.Looper;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.HashMap;

/**
 * @author Iftekhar Ahmed
 */

/**
 * Base class for batch loading any data type with volley.
 * <p>Does the job of handling any data type requests from remote URL; from loading to caching and dispatching
 * response. It defers requesting identical data that are already in-flight, thus avoiding request overhead.
 * It uses a in-memory cache to sit in front of volley's {@link com.android.volley.toolbox.DiskBasedCache}.
 * These techniques allow for reducing request and load data faster. It also allows to coalesce responses
 * and dispatch them simultaneously, which improves performance. Any request can be canceled without
 * affecting other requests to the same resource.
 * </p>
 * <p>
 * It is advised to use a singleton of this class per data type.
 * </p>
 * <b>Important:</b> All requests for loading data must be made from the main/UI thread otherwise, an {@link IllegalAccessError}
 * will be thrown.
 *
 * @param <T> The data type to load
 */
public abstract class Loader<T> {

    /**
     * Amount of time to wait after first response arrives before delivering all responses.
     */
    private int mBatchResponseDelayMs = 100;

    /**
     * HashMap of Cache keys -> BatchedRequest used to track in-flight requests so
     * that we can coalesce multiple requests to the same URL into a single network request.
     */
    private final HashMap<String, BatchedRequest<T>> mInFlightRequests = new HashMap<>();

    /**
     * HashMap of the currently pending responses (waiting to be delivered).
     */
    private final HashMap<String, BatchedRequest<T>> mBatchedResponses = new HashMap<>();

    /**
     * Handler to the main thread.
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * RequestQueue for dispatching Data Requests onto.
     */
    private final RequestQueue mRequestQueue;

    /**
     * The cache implementation to be used as an L1 cache before calling into volley.
     */
    private final MemoryCache<T> mCache;

    /**
     * Runnable for in-flight response delivery.
     */
    private Runnable mRunnable;

    /**
     * Constructs a new Loader instance.
     *
     * @param requestQueue The preferred instance of a volley RequestQueue.
     * @param memoryCache  Any implementation of MemoryCache.
     */
    public Loader(RequestQueue requestQueue, MemoryCache<T> memoryCache) {
        mRequestQueue = requestQueue;
        mCache = memoryCache;
    }

    /**
     * Throws an {@link IllegalStateException} if the loader is not called from the main thread.
     */
    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Loader must be invoked from the main thread.");
        }
    }

    /**
     * Make the actual volley request with specified url. Also get back the response here
     * to batch response data or error for delivery.
     *
     * @param url      The URL for data
     * @param cacheKey The cacheKey for the request.
     * @return the request to save in queue during flight.
     */
    private Request<T> makeDataRequest(String url, final String cacheKey) {
        return makeRequest(url, new Response.Listener<T>() {
            @Override
            public void onResponse(T response) {
                onGetData(cacheKey, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onError(cacheKey, error);
            }
        });
    }

    /**
     * Starts the runnable for batched delivery of responses if it is not already started.
     *
     * @param cacheKey The cacheKey of the response being delivered.
     * @param request  The BatchedRequest to be delivered.
     */
    private void batchResponse(String cacheKey, BatchedRequest<T> request) {
        mBatchedResponses.put(cacheKey, request);
        // If we don't already have a batch delivery runnable in flight, make a new one.
        // Note that this will be used to deliver responses to all callers in mBatchedResponses.
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    for (BatchedRequest<T> br : mBatchedResponses.values()) {
                        for (DataContainer<T> container : br.mContainers) {
                            // If one of the callers in the batched request canceled the request
                            // after the response was received but before it was delivered,
                            // skip them.
                            if (container.mListener == null) {
                                continue;
                            }
                            if (br.getError() == null) {
                                container.mData = br.mResponseData;
                                container.mListener.onSuccess(container, false);
                            } else {
                                container.mListener.onErrorResponse(br.getError());
                            }
                        }
                    }
                    mBatchedResponses.clear();
                    mRunnable = null;
                }

            };
            // Post the runnable.
            mHandler.postDelayed(mRunnable, mBatchResponseDelayMs);
        }
    }

    /**
     * Handler for when requested data was successfully loaded.
     *
     * @param cacheKey The cache key that is associated with the data request.
     * @param data     The data that was returned from the network.
     */
    protected void onGetData(String cacheKey, T data) {
        // cache the data that was fetched.
        mCache.put(cacheKey, data);

        // remove the request from the list of in-flight requests.
        BatchedRequest<T> request = mInFlightRequests.remove(cacheKey);

        if (request != null) {
            // Update the response data.
            request.mResponseData = data;

            // Send the batched response
            batchResponse(cacheKey, request);
        }
    }

    /**
     * Handler for when a requested data failed to load.
     *
     * @param cacheKey The cache key that is associated with the data request.
     */
    protected void onError(String cacheKey, VolleyError error) {
        // Notify the requestes that something failed via a null result.
        // Remove this request from the list of in-flight requests.
        BatchedRequest<T> request = mInFlightRequests.remove(cacheKey);

        if (request != null) {
            // Set the error for this request
            request.setError(error);

            // Send the batched response
            batchResponse(cacheKey, request);
        }
    }

    /**
     * Get the calculated cache key for the request.
     *
     * @param url The specified URL for the request.
     * @return the key in String
     */
    protected abstract String getCacheKey(String url);

    /**
     * Create the appropriate volley {@link Request} with specified parameters.
     *
     * @param url           The specified URL for the request
     * @param listener      An implementation of {@link com.android.volley.Response.Listener}
     * @param errorListener An implementation of {@link com.android.volley.Response.ErrorListener}
     * @return the newly created Request object
     */
    protected abstract Request<T> makeRequest(String url, Response.Listener<T> listener, Response.ErrorListener errorListener);

    /**
     * Creates a new builder object for volley request. This object is used to construct {@link Request}
     * by the loader if necessary. This method should be called at the Loader client side.
     *
     * @return an implementation of Builder interface.
     */
    public abstract Builder<T> newRequest();

    /**
     * Loads data of type {@link T} from the specified URL.
     *
     * @param url            The specified URL to load from.
     * @param onLoadListener An implementation of OnLoadListener to be called during the loading process.
     */
    public void load(String url, OnLoadListener<T> onLoadListener) {

        // only fulfill requests that were initiated from the main thread.
        throwIfNotOnMainThread();

        // Unfortunately, we have no listener to update.
        if (onLoadListener == null) {
            return;
        }

        final String cacheKey = getCacheKey(url);

        // Try to look up the request in the cache of previously loaded data.
        T cachedData = mCache.get(cacheKey);
        if (cachedData != null) {
            // Return the cached bitmap.
            DataContainer<T> container = new DataContainer<>(cachedData, url, null, null);
            onLoadListener.onSuccess(container, true);
            return;
        }

        // The bitmap did not exist in the cache, fetch it!
        DataContainer<T> dataContainer = new DataContainer<>(null, url, cacheKey, onLoadListener);

        // At this point, the caller should know that requested data was not found
        // in the cache. So they can do some intermediary task like load a placeholder
        // for Bitmap data requests.
        onLoadListener.onCacheMiss(dataContainer);

        // Check to see if a request is already in-flight.
        BatchedRequest<T> request = mInFlightRequests.get(cacheKey);
        if (request != null) {
            // If it is, add this request to the list of listeners.
            request.addContainer(dataContainer);
            return;
        }

        // The request is not already in flight. Send the new request to the network and
        // track it.
        Request<T> newRequest = makeDataRequest(url, cacheKey);

        mRequestQueue.add(newRequest);
        mInFlightRequests.put(cacheKey, new BatchedRequest<>(newRequest, dataContainer));
    }

    /**
     * Releases interest in the in-flight request identified from the container object
     * (and cancels it if no one else is listening).
     *
     * @param container The container to identify the in-flight request
     */
    public void cancel(DataContainer<T> container) {
        if (container == null || container.mListener == null) {
            return;
        }

        BatchedRequest<T> request = mInFlightRequests.get(container.mCacheKey);
        if (request != null) {
            boolean canceled = request.removeContainerAndCancelIfNecessary(container);
            if (canceled) {
                mInFlightRequests.remove(container.mCacheKey);
            }
        } else {
            // check to see if it is already batched for delivery.
            request = mBatchedResponses.get(container.mCacheKey);
            if (request != null) {
                boolean canceled = request.removeContainerAndCancelIfNecessary(container);
                if (canceled) {
                    mBatchedResponses.remove(container.mCacheKey);
                }
            }
        }
    }

    /**
     * Clears any previously cached data. If the data request is in-flight or awaiting
     * delivery, it is canceled first.
     *
     * @param dataContainer The container to use in identifying the data in cache.
     */
    public void clear(DataContainer<T> dataContainer) {
        // be sure to cancel any in-flight request or pending response with
        // the DataContainer.
        cancel(dataContainer);
        // clear it from the cache, if cached.
        if (isCached(dataContainer)) {
            mCache.remove(dataContainer.mCacheKey);
        }
    }

    /**
     * Calls resize on the {@link MemoryCache} implementation.
     *
     * @param newSize The new max size for the cache.
     */
    public void resizeCache(int newSize) {
        mCache.resize(newSize);
    }

    /**
     * Checks if data for a specific URL is already in cache.
     *
     * @param dataContainer The container object that holds the cache key to check against.
     * @return True if cached, false otherwise.
     */
    public boolean isCached(DataContainer<T> dataContainer) {
        throwIfNotOnMainThread();

        return dataContainer.mCacheKey != null && mCache.get(dataContainer.mCacheKey) != null;
    }

    public interface OnLoadListener<P> extends Response.ErrorListener {
        /**
         * Called immediately after the requested data is not found
         * in the cache. The request is either in-flight or to be placed in the
         * request queue soon.
         *
         * @param container holding identifiers of the request except the data. It
         *                  is important to hold on to a reference to it in order
         *                  to {@link #cancel(DataContainer)} the request later.
         */
        void onCacheMiss(DataContainer<P> container);

        /**
         * Called when the requested data becomes available.
         *
         * @param container   The container object holding the data.
         * @param isFromCache True for a cache hit, false otherwise.
         */
        void onSuccess(DataContainer<P> container, boolean isFromCache);
    }

    /**
     * <p>Implement this interface to pass {@link Request} specific data which are used by
     * the loader to construct a Request if necessary. A new instance of the Builder should be returned after
     * each call to {@link #newRequest()}.
     * </p>
     * Builder has one defined method {@link #load(String, OnLoadListener)}. All Loader implementations should keep a
     * reference to the Builder when this method is called, and use it to construct a volley
     * {@link Request} when {@link #makeRequest(String, Response.Listener, Response.ErrorListener)}
     * is called. Also, all Loader implementations should declare additional request-specific builder
     * methods in their respective Builder implementation.
     *
     * @param <T> The data type for request.
     */
    public interface Builder<T> {
        void load(String url, OnLoadListener<T> onLoadListener);
    }
}
