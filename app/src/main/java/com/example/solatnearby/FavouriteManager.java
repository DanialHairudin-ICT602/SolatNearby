package com.example.solatnearby;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class FavouriteManager {

    private static final String PREF_NAME = "favourite_masjid_pref";
    private static final String KEY_FAVOURITES = "favourite_masjid_list";

    public static void addFavourite(Context context,
                                    String placeId,
                                    String name,
                                    String address,
                                    String distance,
                                    String facilities,
                                    double lat,
                                    double lng) {

        try {
            JSONArray array = getFavouriteArray(context);

            if (placeId == null || placeId.trim().isEmpty()) {
                placeId = name + "_" + lat + "_" + lng;
            }

            if (isFavourite(context, placeId)) {
                return;
            }

            JSONObject obj = new JSONObject();
            obj.put("placeId", placeId);
            obj.put("name", name);
            obj.put("address", address);
            obj.put("distance", distance);
            obj.put("facilities", facilities);
            obj.put("lat", lat);
            obj.put("lng", lng);

            array.put(obj);

            saveFavouriteArray(context, array);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeFavourite(Context context, String placeId) {
        try {
            JSONArray oldArray = getFavouriteArray(context);
            JSONArray newArray = new JSONArray();

            for (int i = 0; i < oldArray.length(); i++) {
                JSONObject obj = oldArray.getJSONObject(i);

                if (!obj.optString("placeId").equals(placeId)) {
                    newArray.put(obj);
                }
            }

            saveFavouriteArray(context, newArray);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isFavourite(Context context, String placeId) {
        try {
            JSONArray array = getFavouriteArray(context);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                if (obj.optString("placeId").equals(placeId)) {
                    return true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static ArrayList<HashMap<String, String>> getFavourites(Context context) {
        ArrayList<HashMap<String, String>> list = new ArrayList<>();

        try {
            JSONArray array = getFavouriteArray(context);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                HashMap<String, String> masjid = new HashMap<>();
                masjid.put("placeId", obj.optString("placeId"));
                masjid.put("name", "★ " + obj.optString("name"));
                masjid.put("address", obj.optString("address"));
                masjid.put("distance", "Favourite • " + obj.optString("distance"));
                masjid.put("facilities", obj.optString("facilities"));
                masjid.put("lat", String.valueOf(obj.optDouble("lat")));
                masjid.put("lng", String.valueOf(obj.optDouble("lng")));

                list.add(masjid);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private static JSONArray getFavouriteArray(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(KEY_FAVOURITES, "[]");

        try {
            return new JSONArray(data);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void saveFavouriteArray(Context context, JSONArray array) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FAVOURITES, array.toString()).apply();
    }
}