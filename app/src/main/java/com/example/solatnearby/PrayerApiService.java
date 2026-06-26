package com.example.solatnearby;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PrayerApiService {
    // method=9 = JAKIM (Malaysia), city e.g. "Kuala Lumpur"
    @GET("timingsByCity")
    Call<PrayerResponse> getPrayerTimes(
            @Query("city") String city,
            @Query("country") String country,
            @Query("method") int method
    );
}