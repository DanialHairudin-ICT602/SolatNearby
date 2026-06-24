package com.example.solatnearby;

import android.content.Context;
import android.content.SharedPreferences;

public class ReviewManager {

    private static final String PREF_NAME = "masjid_review_pref";

    public static void saveReview(Context context, String placeId, float rating, String comment) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putFloat(placeId + "_rating", rating)
                .putString(placeId + "_comment", comment)
                .apply();
    }

    public static float getRating(Context context, String placeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(placeId + "_rating", 0f);
    }

    public static String getComment(Context context, String placeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(placeId + "_comment", "");
    }
}