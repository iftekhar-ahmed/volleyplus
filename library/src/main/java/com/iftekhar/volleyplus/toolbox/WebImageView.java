package com.iftekhar.volleyplus.toolbox;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.iftekhar.volleyplus.DataContainer;
import com.iftekhar.volleyplus.VolleyPlus;
import com.iftekhar.volleyplus.Loader;

/**
 * @author Iftekhar Ahmed
 */

/**
 * An implementation of {@link ImageView} that uses {@link BitmapLoader} to load
 * bitmaps from the web, with options to customize the volley request. It makes
 * loading and managing bitmaps over network simple and more reliable. It also
 * allows to cancel bitmap requests any time and handles cancellation of old
 * request internally when a new request is issued. It also defers loading bitmap
 * during layout pass, thus preventing unnecessary call to {@link #requestLayout()}.
 */
public class WebImageView extends ImageView {

    /**
     * resource Id for drawable to set as placeholder.
     */
    private int mPlaceholderResId = 0;

    /**
     * resource Id for drawable to set on error.
     */
    private int mErrorResId = 0;

    /**
     * user supplied max width of the bitmap.
     */
    private int mRequestedWidth = 0;

    /**
     * user supplied max height of the bitmap.
     */
    private int mRequestedHeight = 0;

    /**
     * the URL to load the bitmap from.
     */
    private String mUrl;

    /**
     * reference to a BitmapLoader to load DataContainer for bitmap.
     */
    private BitmapLoader mBitmapLoader;

    /**
     * container object holding data for last requested bitmap.
     */
    private DataContainer<Bitmap> mBitmapContainer;

    public WebImageView(Context context) {
        super(context);
    }

    public WebImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the placeholder resource ID to be used for this view until the attempt to load it
     * completes.
     */
    private void setPlaceholderOrNull() {
        if (mPlaceholderResId != 0) {
            setImageResource(mPlaceholderResId);
        } else {
            setImageBitmap(null);
        }
    }

    /**
     * Loads the image for the view if it isn't already loaded.
     *
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */
    private void loadImageIfNecessary(final boolean isInLayoutPass) {
        int width = getWidth();
        int height = getHeight();
        ScaleType scaleType = getScaleType();

        boolean wrapWidth = false, wrapHeight = false;
        if (getLayoutParams() != null) {
            wrapWidth = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
            wrapHeight = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        boolean isFullyWrapContent = wrapWidth && wrapHeight;
        if (width == 0 && height == 0 && !isFullyWrapContent) {
            return;
        }

        // if the URL to be loaded in this view is empty, cancel any old requests and clear the
        // currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            if (mBitmapContainer != null) {
                mBitmapLoader.cancel(mBitmapContainer);
                mBitmapContainer = null;
            }
            setPlaceholderOrNull();
            return;
        }

        // if there was an old request in this view, check if it needs to be canceled.
        if (mBitmapContainer != null && mBitmapContainer.getRequestUrl() != null) {
            if (mBitmapContainer.getRequestUrl().equals(mUrl)) {
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mBitmapLoader.cancel(mBitmapContainer);
                setPlaceholderOrNull();
            }
        }

        // Calculate the max image width / height to use while ignoring WRAP_CONTENT dimens.
        int maxWidth = mRequestedWidth == 0 ? wrapWidth ? 0 : width : mRequestedWidth;
        int maxHeight = mRequestedHeight == 0 ? wrapHeight ? 0 : height : mRequestedHeight;

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        mBitmapLoader.newRequest().size(maxWidth, maxHeight).scaleType(scaleType).load(mUrl
                , new Loader.OnLoadListener<Bitmap>() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (mErrorResId != 0) {
                    setImageResource(mErrorResId);
                }
            }

            @Override
            public void onCacheMiss(DataContainer<Bitmap> container) {
                // Keep a reference to the container if later required to cancel request.
                mBitmapContainer = container;
                // Set a placeholder while our bitmap loads in the background.
                setPlaceholderOrNull();
            }

            @Override
            public void onSuccess(final DataContainer<Bitmap> bitmapContainer, boolean isFromCache) {
                // update the ImageContainer to be the new bitmap container.
                mBitmapContainer = bitmapContainer;
                // If this was an immediate response that was delivered inside of a layout
                // pass do not set the image immediately as it will trigger a requestLayout
                // inside of a layout. Instead, defer setting the image by posting back to
                // the main thread.
                if (isFromCache && isInLayoutPass) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            onSuccess(bitmapContainer, false);
                        }
                    });
                    return;
                }

                final Bitmap bitmap = bitmapContainer.get();
                if (bitmap != null) {
                    setImageBitmap(bitmap);
                } else {
                    setPlaceholderOrNull();
                }
            }
        });
    }

    /**
     * load image from the specified URL.
     *
     * @param url The URL to load from.
     */
    public void load(String url) {
        mUrl = url;
        if (mBitmapLoader == null) {
            mBitmapLoader = (BitmapLoader) VolleyPlus.getInstance(getContext()).getLoaderForClass(Bitmap.class);
        }
        loadImageIfNecessary(false);
    }

    /**
     * Sets the drawable resource id to be used for this view in the event that the image
     * requested is not found.
     *
     * @param resId The drawable resource id.
     * @return the ImageView object.
     */
    public WebImageView placeholder(int resId) {
        mPlaceholderResId = resId;
        return this;
    }

    /**
     * Sets the error image resource ID to be used for this view in the event that the image
     * requested fails to load.
     *
     * @param resId The drawable resource id.
     * @return the ImageView object.
     */
    public WebImageView error(int resId) {
        mErrorResId = resId;
        return this;
    }

    /**
     * Sets the preferred ScaleType enum value for the image when loaded.
     *
     * @param scaleType The ScaleType value.
     * @return the ImageView object.
     */
    public WebImageView scaleType(ScaleType scaleType) {
        setScaleType(scaleType);
        return this;
    }

    /**
     * Sets the size the requested image should be decoded into.
     *
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return the ImageView object.
     */
    public WebImageView resize(int width, int height) {
        mRequestedWidth = width;
        mRequestedHeight = height;
        return this;
    }

    /**
     * If any image loading is currently in-flight, cancels it.
     */
    public void cancelLoading() {
        if (mBitmapLoader != null && mBitmapContainer != null) {
            mBitmapLoader.cancel(mBitmapContainer);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mBitmapLoader != null && mBitmapContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mBitmapLoader.cancel(mBitmapContainer);
            setImageBitmap(null);
            // also clear out the container so we can reload the image if necessary.
            mBitmapContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
