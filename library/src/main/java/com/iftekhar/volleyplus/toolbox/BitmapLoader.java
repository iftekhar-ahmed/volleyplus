package com.iftekhar.volleyplus.toolbox;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.iftekhar.volleyplus.Loader;
import com.iftekhar.volleyplus.MemoryCache;

/**
 * @author Iftekhar Ahmed
 */

/**
 * A concrete implementation of the {@link Loader} class for bitmap data. It
 * uses standard volley {@link ImageRequest} for loading bitmaps. Same
 * bitmaps can be requested for loading from multiple resources. Cancellation
 * works per-request. This class is not thread-safe. All requests for loading
 * bitmaps MUST be called from the main thread otherwise an {@link IllegalStateException}
 * will be thrown.
 */
public class BitmapLoader extends Loader<Bitmap> {

    /**
     * A static container class for data required to construct a bitmap request.
     */
    public static final class RequestBuilder implements Builder<Bitmap> {
        /**
         * holds reference to a BitmapLoader object required in the {@link #load(String, OnLoadListener)}
         * method when data for the request is supplied.
         */
        private BitmapLoader mLoader;

        /**
         * max width of the bitmap.
         */
        private int maxWidth = DEFAULT_MAX_WIDTH;

        /**
         * max height of the bitmap.
         */
        private int maxHeight = DEFAULT_MAX_HEIGHT;

        /**
         * bitmap config to be used for bitmap decoding.
         */
        private Bitmap.Config mConfig = DEFAULT_BITMAP_CONFIG;

        /**
         * bitmap scale type to be used for bitmap decoding.
         */
        private ImageView.ScaleType mScaleType = DEFAULT_SCALE_TYPE;

        /**
         * Creates a new instance of RequestBuilder.
         *
         * @param loader reference to a BitmapLoader object.
         */
        public RequestBuilder(BitmapLoader loader) {
            mLoader = loader;
        }

        /**
         * Pass in custom bitmap config for the request.
         *
         * @param config The bitmap config.
         * @return the RequestBuilder object.
         */
        public RequestBuilder config(Bitmap.Config config) {
            mConfig = config;
            return this;
        }

        /**
         * Pass in preferred bitmap size for the request.
         *
         * @param width  The bitmap width.
         * @param height The bitmap height.
         * @return the RequestBuilder object.
         */
        public RequestBuilder size(int width, int height) {
            maxWidth = width;
            maxHeight = height;
            return this;
        }

        /**
         * Pass in preferred bitmap ScaleType for the request.
         *
         * @param scaleType The ScaleType
         * @return the RequestBuilder object.
         */
        public RequestBuilder scaleType(ImageView.ScaleType scaleType) {
            mScaleType = scaleType;
            return this;
        }

        /**
         * load the bitmap with specified parameters.
         *
         * @param url      The URL for the bitmap to load.
         * @param listener An implementation of OnLoadListener to be called during the loading process.
         */
        @Override
        public void load(String url, OnLoadListener<Bitmap> listener) {
            mLoader.loadWithRequestData(url, listener, this);
        }
    }

    /**
     * default max width of the bitmap.
     */
    public static final int DEFAULT_MAX_WIDTH = 0;

    /**
     * default max height of the bitmap.
     */
    public static final int DEFAULT_MAX_HEIGHT = 0;

    /**
     * default bitmap config to be passed with the request if no request data found.
     */
    public static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.RGB_565;

    /**
     * default bitmap scale type to be passed with the request if no request data found.
     */
    public static final ImageView.ScaleType DEFAULT_SCALE_TYPE = ImageView.ScaleType.CENTER_INSIDE;

    /**
     * object containing user-supplied data to construct a ImageRequest.
     */
    private RequestBuilder mRequestBuilder;

    /**
     * Creates a new BitmapLoader instance.
     *
     * @param requestQueue The preferred instance of a volley RequestQueue.
     * @param memoryCache  An implementation of MemoryCache interface.
     */
    public BitmapLoader(RequestQueue requestQueue, MemoryCache<Bitmap> memoryCache) {
        super(requestQueue, memoryCache);
    }

    /**
     * Start loading requested bitmap with supplied RequestBuilder.
     *
     * @param url            The URL to load bitmap from.
     * @param onLoadListener An implementation of OnLoadListener to be called during the loading process.
     * @param data           The supplied data to construct a {@link ImageRequest}.
     */
    protected void loadWithRequestData(String url, OnLoadListener<Bitmap> onLoadListener, RequestBuilder data) {
        mRequestBuilder = data;
        super.load(url, onLoadListener);
    }

    @Override
    public void load(String url, OnLoadListener<Bitmap> onLoadListener) {
        mRequestBuilder = null;
        super.load(url, onLoadListener);
    }

    @Override
    protected String getCacheKey(String url) {
        if (mRequestBuilder == null) {
            return new StringBuilder(url.length() + 12).append("#W").append(DEFAULT_MAX_WIDTH)
                    .append("#H").append(DEFAULT_MAX_HEIGHT).append("#S").append(DEFAULT_SCALE_TYPE.ordinal()).append(url)
                    .toString();
        }
        return new StringBuilder(url.length() + 12).append("#W").append(mRequestBuilder.maxWidth)
                .append("#H").append(mRequestBuilder.maxHeight).append("#S").append(mRequestBuilder.mScaleType.ordinal()).append(url)
                .toString();
    }

    @Override
    protected Request<Bitmap> makeRequest(String url, Response.Listener<Bitmap> listener, Response.ErrorListener errorListener) {
        if (mRequestBuilder == null) {
            return new ImageRequest(url, listener, DEFAULT_MAX_WIDTH, DEFAULT_MAX_HEIGHT, DEFAULT_SCALE_TYPE
                    , DEFAULT_BITMAP_CONFIG, errorListener);
        }
        return new ImageRequest(url, listener, mRequestBuilder.maxWidth, mRequestBuilder.maxHeight, mRequestBuilder.mScaleType
                , mRequestBuilder.mConfig, errorListener);
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
