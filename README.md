# volleyplus
**volleyplus** is a wrapper library over [volley](https://android.googlesource.com/platform/frameworks/volley/) for doing easier network IO in android. It offers batch issuing and cancellation 
of request, caching and coalesching of response and request lifecycle management for any data type; all on top of the flexibility and robustness of volley.
It also allows to go back to 'volley as usual' where you create custom or out-of-the-box volley Requests and place them in a RequestQueue. 
In that case, you will be missing the features and ease of using **volleyplus** `Loader`.
# Using volleyplus
The `Loader` class in volleplus allows to load data without dealing with `Request` and `RequestQueue` for same data type over and over again. 
Your application needs only to use one instance of this class per data type for the entire lifecycle. That's why, when creating a custom `Loader`, 
it is required to add the instance to the `VolleyPlus` loader pool and use it from there.

The library includes 2 out-of-the-box implementations of this class for `Bitmap`
([BitmapLoader](https://github.com/iftekhar-ahmed/volleyplus/blob/master/library/src/main/java/com/iftekhar/volleyplus/toolbox/BitmapLoader.java)) and
 `JsonObject`
([JsonObjectLoader](https://github.com/iftekhar-ahmed/volleyplus/blob/master/library/src/main/java/com/iftekhar/volleyplus/toolbox/JsonObjectLoader.java)).
Using these default `Loader` implementations and creating custom `Loader` for your own data types are both fairly simple. 
## Using volleplus Loaders
Here is an example of using the `JsonObjectLoader`,
```
VolleyPlus volleyPlus = VolleyPlus.getInstance(getContext());
JsonObjectLoader mJSONLoader = (JsonObjectLoader) volleyPlus.getLoaderForClass(JSONObject.class);
jsonObjectLoader.newRequest().requestMethod(Request.Method.GET).load(JSON_URL, new Loader.OnLoadListener<JSONObject>() {
            @Override
            public void onCacheMiss(DataContainer<JSONObject> container) {
                // data not found in cache. request should be in flight very soon.
            }

            @Override
            public void onSuccess(DataContainer<JSONObject> container, boolean isFromCache) {
                // data loaded either from cache or over network
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // handle volley error
            }
        });
```
The `BitmapLoader` can be used in a similar way shown above. However, it is best used with the 
[WebImageView](https://github.com/iftekhar-ahmed/volleyplus/blob/master/library/src/main/java/com/iftekhar/volleyplus/toolbox/WebImageView.java)
component included in the toolbox which loads and fits bitmap nicely into ImageView. `WebImageView` offers builder style methods to tailor request for bitmaps. An example of using it is here,
```
WebImageView thumbnail = (WebImageView) itemView.findViewById(R.id.thumbnail);
thumbnail.placeholder(R.drawable.ic_placeholder)
         .error(R.drawable.ic_error)
         .resize(100, 80)
         .load(IMAGE_URL);
```
`WebImageView` allows to cancel bitmap request at any time. It also handles cancellation of requests soon as a new request is issued. This makes it safe to use inside list adapters.
## Custom Loader
Creating a custom **volleyplus** `Loader` involves 2 steps,
* Extending the `Loader<T>` class where `T` is the data type. (Check the [toolbox](https://github.com/iftekhar-ahmed/volleyplus/tree/master/library/src/main/java/com/iftekhar/volleyplus/toolbox) to see how the two default `Loader`s do it.)
* Adding an instance of the `Loader` to [VolleyPlus](https://github.com/iftekhar-ahmed/volleyplus/blob/master/library/src/main/java/com/iftekhar/volleyplus/VolleyPlus.java)
which is the global single access point to a pool of `Loader`s and a `RequestQueue` object

Creating an instance of `Loader` takes two parameters, a `RequestQueue` and a [MemoryCache](https://github.com/iftekhar-ahmed/volleyplus/blob/master/library/src/main/java/com/iftekhar/volleyplus/MemoryCache.java) 
instance. Have a look at the `VollePlus` singleton class for examples of creating and adding `Loader` instances to pool.
# License
```
Copyright 2015 Iftekhar Ahmed
Copyright (C) 2011 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
