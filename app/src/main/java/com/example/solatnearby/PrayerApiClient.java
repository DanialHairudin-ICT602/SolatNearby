package com.example.solatnearby;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PrayerApiClient {
    private static final String BASE_URL = "https://api.aladhan.com/v1/";
    private static Retrofit instance;

    public static Retrofit getInstance() {
        if (instance == null) {
            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instance;
    }
}