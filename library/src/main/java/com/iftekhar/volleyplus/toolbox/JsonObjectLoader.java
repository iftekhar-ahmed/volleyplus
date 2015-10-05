package com.iftekhar.volleyplus.toolbox;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.iftekhar.volleyplus.Loader;
import com.iftekhar.volleyplus.MemoryCache;

import org.json.JSONObject;

/**
 * @author Iftekhar Ahmed
 */

/**
 * A concrete implementation of the {@link Loader} class for JSON Objects. It
 * uses standard volley {@link JsonObjectRequest}. Same json can be requested
 * for loading from multiple resources. Cancellation works per-request. This
 * class is not thread-safe. All requests for loading JSON Objects MUST be
 * called from the main thread otherwise an {@link IllegalStateException} will
 * be thrown.
 */
public class JsonObjectLoader extends Loader<JSONObject> {

    /**
     * A static container class for data required to construct a JSON Object request.
     */
    public static class RequestBuilder implements Builder<JSONObject> {
        /**
         * holds reference to a JsonObjectLoader object required in the {@link #load(String, OnLoadListener)}
         * method when data for the request is supplied.
         */
        private JsonObjectLoader mLoader;
        /**
         * Optional JSONObject to pass for POST methods.
         */
        private JSONObject mRequestBody = DEFAULT_REQUEST_BODY;

        /**
         * Volley Request method.
         */
        private int mMethod = DEFAULT_REQUEST_METHOD;

        /**
         * Creates a new instance of RequestBuilder.
         *
         * @param loader reference to a JsonObjectLoader object.
         */
        public RequestBuilder(JsonObjectLoader loader) {
            mLoader = loader;
        }

        /**
         * Pass in an instance of {@link JSONObject} as request body. Null is allowed.
         *
         * @param jsonObject The request body.
         * @return the RequestBuilder object.
         */
        public RequestBuilder requestBody(JSONObject jsonObject) {
            mRequestBody = jsonObject;
            return this;
        }

        /**
         * Pass in a supported volley request method.
         *
         * @param method One of the constants declared {@link com.android.volley.Request.Method}.
         * @return the RequestBuilder object.
         */
        public RequestBuilder requestMethod(int method) {
            mMethod = method;
            return this;
        }

        /**
         * load the JSON Object asynchronously with specified parameters.
         *
         * @param url      The URL for the JSON Object to load.
         * @param listener An implementation of OnLoadListener to be called during the loading process.
         */
        @Override
        public void load(String url, OnLoadListener<JSONObject> listener) {
            mLoader.loadWithRequestData(url, listener, this);
        }
    }

    /**
     * default request method to be used when no RequestBuilder is supplied.
     */
    public static final int DEFAULT_REQUEST_METHOD = Request.Method.GET;

    /**
     * default request body to be used when no RequestBuilder is supplied.
     */
    public static final JSONObject DEFAULT_REQUEST_BODY = null;

    /**
     * object containing user-supplied data to construct a JsonObjectRequest.
     */
    private RequestBuilder mRequestData;

    /**
     * Constructs a new JsonObjectLoader instance.
     *
     * @param requestQueue The preferred instance of a volley RequestQueue.
     * @param memoryCache  An implementation of MemoryCache interface.
     */
    public JsonObjectLoader(RequestQueue requestQueue, MemoryCache<JSONObject> memoryCache) {
        super(requestQueue, memoryCache);
    }

    /**
     * Start loading requested JSON Object with supplied RequestBuilder.
     *
     * @param url            The URL to load JSON Object from.
     * @param onLoadListener An implementation of OnLoadListener to be called during the loading process.
     * @param data           The supplied data to construct a {@link JsonObjectRequest}.
     */
    protected void loadWithRequestData(String url, OnLoadListener<JSONObject> onLoadListener, RequestBuilder data) {
        mRequestData = data;
        super.load(url, onLoadListener);
    }

    @Override
    public void load(String url, OnLoadListener<JSONObject> onLoadListener) {
        mRequestData = null;
        super.load(url, onLoadListener);
    }

    @Override
    protected String getCacheKey(String url) {
        if (mRequestData == null) {
            return new StringBuilder(url.length() + 12).append("#M").append(DEFAULT_REQUEST_METHOD)
                    .append(0).append(url).toString();
        }
        return new StringBuilder(url.length() + 12).append("#M").append(mRequestData.mMethod)
                .append(mRequestData.mRequestBody != null ? mRequestData.mRequestBody.toString().getBytes().length : 0)
                .append(url).toString();
    }

    @Override
    protected Request<JSONObject> makeRequest(String url, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        if (mRequestData == null) {
            return new JsonObjectRequest(DEFAULT_REQUEST_METHOD, url, DEFAULT_REQUEST_BODY, listener, errorListener);
        }
        return new JsonObjectRequest(mRequestData.mMethod, url, mRequestData.mRequestBody, listener, errorListener);
    }

    /**
     * Starts the loading process by returning a new instance of RequestBuilder.
     *
     * @return an instance of RequestBuilder.
     */
    @Override
    public RequestBuilder newRequest() {
        return new RequestBuilder(this);
    }
}
