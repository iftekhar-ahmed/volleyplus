package com.iftekhar.volleyplus;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.iftekhar.volleyplus.ext.InMemoryCache;
import com.iftekhar.volleyplus.toolbox.BitmapLoader;
import com.iftekhar.volleyplus.toolbox.JsonObjectLoader;

import org.json.JSONObject;

import java.util.Map;

/**
 * @author Iftekhar Ahmed
 */

/**
 * The global access point to a single volley RequestQueue and a pool of {@link Loader}s which are
 * optimized for simultaneous request and cancellation of data, request batching, response coalescing
 * and good in-memory caching. Using {@link Loader}s will also fasten the networking speed over volley.
 * <p>
 * VolleyPlus has two default implementations of {@link Loader} in the loader pool. They are
 * {@link JsonObjectLoader} and {@link BitmapLoader}. It is possible to create your own {@link Loader}
 * implementations for a data class and replace/add them to the pool.
 * </p>
 */
public class VolleyPlus {

    /**
     * Maximum available memory to the global single application, in bytes.
     */
    public static final int MAX_MEMORY = (int) (Runtime.getRuntime().maxMemory());

    /**
     * Default maximum memory usage in bytes
     */
    private static final int DEFAULT_MAX_MEMORY_BYTES = MAX_MEMORY / 8;

    /**
     * static instance of this class to be used throughout an application's lifecycle.
     */
    private static VolleyPlus instance = null;

    /**
     * an application context will make sure the static instance stays alive.
     */
    private static Context mContext;

    /**
     * the RequestQueue to use with volley requests and it is configurable.
     */
    private RequestQueue mRequestQueue;

    /**
     * a map of loaders to their corresponding data classes.
     */
    private Map<Class<?>, Object> mLoaderPool;

    /**
     * Creates a single global VolleyPlus instance.
     *
     * @param context The application context.
     */
    private VolleyPlus(Context context) {
        mContext = context;
        mLoaderPool = new ArrayMap<>(2);
        addToLoaderPool(Bitmap.class, getBitmapLoader());
        addToLoaderPool(JSONObject.class, getJsonObjectLoader());
    }

    /**
     * Gets the global single instance of VolleyPlus which contains a RequestQueue and a pool
     * of {@link Loader} objects for various data types.
     *
     * @param context The application context.
     * @return the VolleyPlus instance.
     */
    public static synchronized VolleyPlus getInstance(Context context) {
        if (instance == null) {
            instance = new VolleyPlus(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Creates the default implementation of {@link Loader} for Bitmaps.
     *
     * @return a new instance of BitmapLoader that uses a {@link LruCache} for caching bitmaps
     * and has a memory capacity of 1/8th of the max heap size.
     */
    private BitmapLoader getBitmapLoader() {
        return new BitmapLoader(getRequestQueue(), new MemoryCache<Bitmap>() {
            private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(DEFAULT_MAX_MEMORY_BYTES) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getRowBytes() * value.getHeight();
                }
            };

            @Override
            public Bitmap get(String cacheKey) {
                return cache.get(cacheKey);
            }

            @Override
            public void put(String cacheKey, Bitmap bitmap) {
                cache.put(cacheKey, bitmap);
            }

            @Override
            public void remove(String cacheKey) {
                cache.remove(cacheKey);
            }

            @Override
            public void resize(int size) {
                cache.resize(size);
            }
        });
    }

    /**
     * Creates the default implementation of {@link Loader} for JSON Objects.
     *
     * @return a new instance of JsonObjectLoader that uses a {@link LruCache} for caching JSON Objects
     * and has a memory capacity of 1/8th of the max heap size.
     */
    private JsonObjectLoader getJsonObjectLoader() {
        return new JsonObjectLoader(getRequestQueue(), new MemoryCache<JSONObject>() {
            private final LruCache<String, JSONObject> cache = new LruCache<String, JSONObject>(DEFAULT_MAX_MEMORY_BYTES) {
                @Override
                protected int sizeOf(String key, JSONObject value) {
                    return value.toString().getBytes().length;
                }
            };

            @Override
            public JSONObject get(String cacheKey) {
                return cache.get(cacheKey);
            }

            @Override
            public void put(String cacheKey, JSONObject jsonObject) {
                cache.put(cacheKey, jsonObject);
            }

            @Override
            public void remove(String cacheKey) {
                cache.remove(cacheKey);
            }

            @Override
            public void resize(int size) {
                cache.resize(size);
            }
        });
    }

    /**
     * Adds an implementation of {@link Loader} to the pool of loaders for specified data class.
     *
     * @param clazz  The class of the data.
     * @param loader The loader object for the data.
     * @param <T>    The data type associated with the loader.
     */
    public <T> void addToLoaderPool(Class<T> clazz, Loader<T> loader) {
        mLoaderPool.put(clazz, loader);
    }

    /**
     * Finds the matching {@link Loader} implementation for the specified data class.
     *
     * @param clazz The class of the data.
     * @param <T>   The data type associated with the loader.
     * @return the loader for the data type, or null if no loader found in the pool.
     */
    public <T> Loader<T> getLoaderForClass(Class<T> clazz) {
        return (Loader<T>) mLoaderPool.get(clazz);
    }

    /**
     * Adds standard volley request to the RequestQueue.
     *
     * @param request The volley request
     * @param <T>     The data type for the request.
     */
    public <T> void addToRequestQueue(Request<T> request) {
        getRequestQueue().add(request);
    }

    /**
     * Sets a preferred RequestQueue instance to use for standard volley requests
     * and VolleyPlus loaders.
     *
     * @param requestQueue The RequestQueue to use.
     */
    public void setRequestQueue(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
    }

    /**
     * Gets the current implementation of the volley RequestQueue. If not explicitly set, this
     * will return a default implementation of RequestQueue that uses an {@link InMemoryCache} for
     * http caching and a {@link BasicNetwork} with {@link HurlStack}.
     *
     * @return The RequestQueue instance.
     */
    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            //mRequestQueue = Volley.newRequestQueue(mContext);
            mRequestQueue = new RequestQueue(new InMemoryCache(), new BasicNetwork(new HurlStack()));
            mRequestQueue.start();
        }
        return mRequestQueue;
    }
}
