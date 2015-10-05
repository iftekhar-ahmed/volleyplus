package com.iftekhar.volleyplusdemo;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.iftekhar.volleyplus.DataContainer;
import com.iftekhar.volleyplus.Loader;
import com.iftekhar.volleyplus.VolleyPlus;
import com.iftekhar.volleyplus.toolbox.BitmapLoader;
import com.iftekhar.volleyplus.toolbox.JsonObjectLoader;
import com.iftekhar.volleyplusdemo.adapter.ClothListAdapter;
import com.iftekhar.volleyplusdemo.model.Cloth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Iftekhar on 8/23/2015.
 */

/**
 * A fragment with RecyclerView to show a list of cloth items. Each item has a name, brand name,
 * price and an image. It also allows to mock refresh items with pull gesture on the RecyclerView.
 */
public class ClothListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    /**
     * The URL to load JSON that contains all of our data.
     */
    private final String JSON_URL = "https://www.zalora.com.my/mobile-api/women/clothing/";

    public static final String TAG = "ClothListFragment";

    private boolean mClearCacheBeforeLoading = false;
    private boolean mIsLoading = false;
    private ClothListAdapter mAdapter;
    private JsonObjectLoader mJSONLoader;
    private BitmapLoader mBitmapLoader;
    private DataContainer<JSONObject> mContainer;
    private SwipeRefreshLayout mRefreshLayout;
    private Snackbar mSnack;
    private RecyclerView mListView;
    private List<Cloth> mCloths;

    public static ClothListFragment findOrGetInstance(FragmentManager fm) {
        ClothListFragment fragment = (ClothListFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            return new ClothListFragment();
        }
        return fragment;
    }

    public ClothListFragment() {
        // Required empty public ctor
    }

    /**
     * Setup Grid layout manager for RecyclerView. The span count for grid layout is calculated
     * after RecyclerView is laid out so that we know the measured width of it.
     */
    private void setupMultiSpanLayout() {
        final RecyclerView recyclerView = mListView;
        if (recyclerView.getWidth() == 0) {
            final ViewTreeObserver vto = recyclerView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        recyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                    setupMultiSpanLayout();
                }
            });
        } else {
            int measured_width = recyclerView.getMeasuredWidth();
            int grid_item_width = getResources().getDimensionPixelSize(R.dimen.multi_span_list_item_width);
            int span_count = (int) Math.floor((measured_width / grid_item_width));
            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), span_count));
        }
    }

    /**
     * Does the loading and parsing of cloth items. Our adapter is notified from here.
     *
     * @param loader The loader to load JSONObject. We're using JSONObjectLoader in this fragment.
     */
    private void loadCloths(Loader<JSONObject> loader) {
        JsonObjectLoader jsonObjectLoader = (JsonObjectLoader) loader;
        jsonObjectLoader.newRequest().requestMethod(Request.Method.GET).load(JSON_URL, new Loader.OnLoadListener<JSONObject>() {
            @Override
            public void onCacheMiss(DataContainer<JSONObject> container) {
                // We have a cache miss. The loading will get in flight soon.
                // Hold on to the container reference so that we can use it to cancel requests if required.
                mContainer = container;
                mIsLoading = true;
                if (mSnack != null) {
                    mSnack.setText("Data not found in cache").setDuration(Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onSuccess(DataContainer<JSONObject> container, boolean isFromCache) {
                mIsLoading = false;
                if (isFromCache) {
                    if (mSnack != null) {
                        mSnack.setText("Data already cached").setDuration(Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    mContainer = container;
                    if (mSnack != null) {
                        mSnack.setText("Data loaded from network").show();
                    }
                }
                mCloths.clear();
                try {
                    JSONArray results = container.get().getJSONObject("metadata").getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        final JSONObject object = results.getJSONObject(i);
                        final List<Cloth> cloths = Cloth.createFrom(object);
                        mCloths.addAll(cloths);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
                if (mRefreshLayout != null) {
                    mRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {
                mIsLoading = false;
                String error = volleyError.getMessage();
                if (error != null) {
                    Log.e("Volley Error", error);
                    if (mSnack != null) {
                        mSnack.setText("Volley Error: " + error).show();
                    }
                } else {
                    volleyError.printStackTrace();
                }
                if (mRefreshLayout != null) {
                    mRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    /**
     * A creepy reflection-based hack to force the overflow menu to show up in the action bar.
     */
    private void makeActionOverflowMenuShown() {
        try {
            ViewConfiguration config = ViewConfiguration.get(getContext());
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_cache:
                item.setChecked(!item.isChecked());
                mClearCacheBeforeLoading = item.isChecked();
                break;
        }
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        makeActionOverflowMenuShown();
        VolleyPlus volleyPlus = VolleyPlus.getInstance(getContext());
        mBitmapLoader = (BitmapLoader) volleyPlus.getLoaderForClass(Bitmap.class);
        mJSONLoader = (JsonObjectLoader) volleyPlus.getLoaderForClass(JSONObject.class);
        mCloths = new ArrayList<>();
        mAdapter = new ClothListAdapter(getContext(), R.layout.grid_list_item_cloth, mCloths);
        loadCloths(mJSONLoader);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cloth_list, container, false);
        mListView = (RecyclerView) root.findViewById(R.id.cloths);
        setupMultiSpanLayout();
        mListView.setAdapter(mAdapter);
        mRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_container);
        mRefreshLayout.setOnRefreshListener(ClothListFragment.this);
        mRefreshLayout.setColorSchemeColors(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW);
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.setRefreshing(mIsLoading);
            }
        });
        mListView.addOnScrollListener(new RecyclerScrollListener(mRefreshLayout));
        mSnack = Snackbar.make(root.findViewById(R.id.coordinator), "", Snackbar.LENGTH_LONG);
        return root;
    }

    /**
     * Cancel any loading in progress and restart the loader.
     */
    @Override
    public void onRefresh() {
        // if selected from thee options menu to clear the cache before loading.
        if (mClearCacheBeforeLoading && mContainer != null) {
            mJSONLoader.clear(mContainer);
        }
        loadCloths(mJSONLoader);
    }

    /**
     * Handles a problem with SwipeRefreshLayout kicking off load progress before the first item
     * in the list becomes visible when scrolling up.
     */
    private static class RecyclerScrollListener extends RecyclerView.OnScrollListener {
        private SwipeRefreshLayout mRefreshLayout;

        public RecyclerScrollListener(SwipeRefreshLayout refreshLayout) {
            mRefreshLayout = refreshLayout;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            GridLayoutManager gridLayoutManager = ((GridLayoutManager) recyclerView.getLayoutManager());
            mRefreshLayout.setEnabled(gridLayoutManager.findFirstCompletelyVisibleItemPosition() == 0);
        }
    }
}
