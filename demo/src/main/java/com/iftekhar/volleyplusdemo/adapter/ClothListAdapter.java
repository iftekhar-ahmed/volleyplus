package com.iftekhar.volleyplusdemo.adapter;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.iftekhar.volleyplus.toolbox.WebImageView;
import com.iftekhar.volleyplusdemo.R;
import com.iftekhar.volleyplusdemo.model.Cloth;

import java.util.List;

/**
 * Created by Iftekhar on 8/23/2015.
 */
public class ClothListAdapter extends RecyclerView.Adapter<ClothListAdapter.ViewHolder> {

    private int mLayoutRes;
    private Context mContext;
    private List<Cloth> mCloths;

    public ClothListAdapter(Context context, int layoutResId, List<Cloth> cloths) {
        mContext = context;
        mLayoutRes = layoutResId;
        mCloths = cloths;
    }

    /**
     * Loads the thumbnail for the Cloth with WebView's default BitmapLoader.
     *
     * @param cloth        object containing the URL.
     * @param webImageView the view to load image.
     */
    private void loadThumbnail(Cloth cloth, WebImageView webImageView) {
        webImageView
                .placeholder(R.drawable.ic_image_grey600_48dp)
                .error(R.drawable.ic_texture_grey600_48dp)
                .resize(cloth.imageWidth, cloth.imageHeight)
                .load(cloth.imageUrl);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(mLayoutRes, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        viewHolder.itemView.setTag(position);
        final Cloth cloth = mCloths.get(position);
        viewHolder.name.setText(cloth.name);
        viewHolder.brand.setText(cloth.brandName);
        viewHolder.price.setText("$" + String.valueOf(cloth.price));
        // We want to set the thumbnail's height the same to its width, making it a square view.
        // We watch for thumbnail's layout to be ready with a ViewTreeObserver. When getWidth()
        // returns the actual thumbnail width, we set the height, invalidate layout and then
        // load the actual image.
        if (viewHolder.thumbnail.getWidth() == 0) {
            final ViewTreeObserver vto = viewHolder.thumbnail.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    viewHolder.thumbnail.getLayoutParams().height = viewHolder.thumbnail.getWidth();
                    viewHolder.thumbnail.requestLayout();
                    if (viewHolder.thumbnail.getHeight() == viewHolder.thumbnail.getWidth()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            viewHolder.thumbnail.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            viewHolder.thumbnail.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                        loadThumbnail(cloth, viewHolder.thumbnail);
                    }
                }
            });
        } else {
            loadThumbnail(cloth, viewHolder.thumbnail);
        }
    }

    @Override
    public int getItemCount() {
        return mCloths.size();
    }

    /**
     * Holds view items created by RecyclerView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView brand;
        TextView price;
        WebImageView thumbnail;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            brand = (TextView) itemView.findViewById(R.id.brand);
            price = (TextView) itemView.findViewById(R.id.price);
            thumbnail = (WebImageView) itemView.findViewById(R.id.thumbnail);
        }
    }
}
