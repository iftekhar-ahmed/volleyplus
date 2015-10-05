package com.iftekhar.volleyplusdemo.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Iftekhar Ahmed
 */

/**
 * Model object containing cloth data.
 */
public class Cloth {

    public String id;

    public String name;

    public String brandName;

    public float price;

    public String imageUrl;

    public int imageWidth;

    public int imageHeight;

    /**
     * Create a list of Cloths for each images found in the supplied JSONObject.
     *
     * @param jsonObject The JSONObject to crete Cloths from.
     * @return the list of cloths created.
     * @throws JSONException if any parse error occured.
     */
    public static List<Cloth> createFrom(JSONObject jsonObject) throws JSONException {
        List<Cloth> cloths = new ArrayList<>();
        String id = jsonObject.getString("id");
        JSONObject data = jsonObject.getJSONObject("data");
        String name = data.getString("name");
        String brandName = data.getString("brand");
        float price = Float.parseFloat(data.getString("price"));
        JSONArray images = jsonObject.getJSONArray("images");
        for (int i = 0; i < images.length(); i++) {
            JSONObject image = images.getJSONObject(i);
            Cloth cloth = new Cloth();
            cloth.id = id;
            cloth.name = name;
            cloth.brandName = brandName;
            cloth.price = price;
            cloth.imageUrl = image.getString("path");
            cloth.imageWidth = Integer.parseInt(image.getString("width"));
            cloth.imageHeight = Integer.parseInt(image.getString("height"));
            cloths.add(cloth);
        }
        return cloths;
    }
}
