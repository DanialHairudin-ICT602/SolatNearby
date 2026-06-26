package com.example.solatnearby;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PrayerApiService {
    // For city-based
    @GET("timingsByCity")
    Call<PrayerResponse> getPrayerTimes(
            @Query("city") String city,
            @Query("country") String country,
            @Query("method") int method
    );

    // For coordinates
    @GET("timings")
    Call<PrayerResponse> getPrayerTimesByCoordinates(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("method") int method
    );
}