package com.example.solatnearby;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends Activity {

    private Button btnFindNearby;
    private LinearLayout cardNearby, cardMap, cardPrayerTime, cardProfile, cardSettings, cardSavedMasjid;
    private TextView textCurrentLocation, textCurrentPrayer;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize views
        btnFindNearby = findViewById(R.id.btnFindNearby);
        cardNearby = findViewById(R.id.cardNearby);
        cardMap = findViewById(R.id.cardMap);
        cardPrayerTime = findViewById(R.id.cardPrayerTime);
        cardProfile = findViewById(R.id.cardProfile);
        cardSettings = findViewById(R.id.cardSettings);
        cardSavedMasjid = findViewById(R.id.cardSavedMasjid);
        textCurrentLocation = findViewById(R.id.textCurrentLocation);
        textCurrentPrayer = findViewById(R.id.textCurrentPrayer);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set default text
        textCurrentPrayer.setText("Loading prayer times...");

        // Check permission and get location
        checkLocationPermission();

        // Top button - Start Map Guide
        btnFindNearby.setText("Start Map Guide");
        btnFindNearby.setOnClickListener(v -> openMapGuide());

        // Nearby Masjid Card
        if (cardNearby != null) {
            cardNearby.setClickable(true);
            cardNearby.setFocusable(true);
            cardNearby.setOnClickListener(v -> openNearbyMasjid());
        }

        // Map Guide Card
        if (cardMap != null) {
            cardMap.setClickable(true);
            cardMap.setFocusable(true);
            cardMap.setOnClickListener(v -> openMapGuide());
        }

        // Prayer Times Card
        if (cardPrayerTime != null) {
            cardPrayerTime.setClickable(true);
            cardPrayerTime.setFocusable(true);
            cardPrayerTime.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, PrayerTimesActivity.class)));
        }

        // Profile Card
        if (cardProfile != null) {
            cardProfile.setClickable(true);
            cardProfile.setFocusable(true);
            cardProfile.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        }

        // Settings Card
        if (cardSettings != null) {
            cardSettings.setClickable(true);
            cardSettings.setFocusable(true);
            cardSettings.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));
        }

        // Saved Masjid / History Card
        if (cardSavedMasjid != null) {
            cardSavedMasjid.setClickable(true);
            cardSavedMasjid.setFocusable(true);
            cardSavedMasjid.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, HistoryActivity.class)));
        }
    }

    // ============ LOCATION METHODS ============

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            currentLat = location.getLatitude();
                            currentLng = location.getLongitude();
                            textCurrentLocation.setText("Lat: " + currentLat + ", Lng: " + currentLng);
                            fetchPrayerTimes(currentLat, currentLng);
                        } else {
                            textCurrentLocation.setText("Unable to get location. Using default.");
                            currentLat = 3.1390; // Kuala Lumpur default
                            currentLng = 101.6869;
                            fetchPrayerTimes(currentLat, currentLng);
                        }
                    }
                });
    }

    // ============ PRAYER TIME API ============

    private void fetchPrayerTimes(double lat, double lng) {
        PrayerApiService apiService = PrayerApiClient.getInstance().create(PrayerApiService.class);

        Call<PrayerResponse> call = apiService.getPrayerTimesByCoordinates(lat, lng, 9);
        call.enqueue(new Callback<PrayerResponse>() {
            @Override
            public void onResponse(Call<PrayerResponse> call, Response<PrayerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PrayerResponse prayerResponse = response.body();
                    if (prayerResponse.data != null && prayerResponse.data.timings != null) {
                        PrayerTimings timings = prayerResponse.data.timings;
                        updateNextPrayer(timings);
                    } else {
                        textCurrentPrayer.setText("Prayer times data unavailable");
                    }
                } else {
                    textCurrentPrayer.setText("Failed to load prayer times");
                }
            }

            @Override
            public void onFailure(Call<PrayerResponse> call, Throwable t) {
                textCurrentPrayer.setText("Error: " + t.getMessage());
            }
        });
    }

    private void updateNextPrayer(PrayerTimings timings) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentHour = now.get(java.util.Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(java.util.Calendar.MINUTE);
        int currentTotalMinutes = currentHour * 60 + currentMinute;

        String[] prayerNames = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
        String[] prayerTimes = {
                timings.Fajr,
                timings.Sunrise,
                timings.Dhuhr,
                timings.Asr,
                timings.Maghrib,
                timings.Isha
        };

        String nextPrayerName = "";
        String nextPrayerTime = "";
        int smallestDiff = Integer.MAX_VALUE;

        for (int i = 0; i < prayerNames.length; i++) {
            String timeStr = prayerTimes[i];
            if (timeStr == null || timeStr.isEmpty()) continue;

            try {
                int prayerMinutes = parseTimeToMinutes(timeStr);
                int diff = prayerMinutes - currentTotalMinutes;

                if (diff < 0) {
                    diff += 1440;
                }

                if (diff < smallestDiff) {
                    smallestDiff = diff;
                    nextPrayerName = prayerNames[i];
                    nextPrayerTime = timeStr;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!nextPrayerName.isEmpty()) {
            textCurrentPrayer.setText("Next prayer: " + nextPrayerName + " • " + nextPrayerTime);
        } else {
            textCurrentPrayer.setText("Prayer times loaded");
        }
    }

    private int parseTimeToMinutes(String timeStr) {
        int hour = 0, minute = 0;

        try {
            String cleanTime = timeStr;
            if (timeStr.contains("(")) {
                cleanTime = timeStr.substring(0, timeStr.indexOf("(")).trim();
            }
            cleanTime = cleanTime.replace("AM", "").replace("PM", "").trim();

            String[] hm = cleanTime.split(":");
            hour = Integer.parseInt(hm[0]);
            minute = Integer.parseInt(hm[1]);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return hour * 60 + minute;
    }

    // ============ NAVIGATION METHODS ============

    private void openNearbyMasjid() {
        startActivity(new Intent(HomeActivity.this, NearbyMasjidActivity.class));
    }

    private void openMapGuide() {
        Toast.makeText(this, "Opening nearest masjid route", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(HomeActivity.this, MapNavigationActivity.class);
        intent.putExtra("mode", "auto_nearest");
        startActivity(intent);
    }

    // ============ PERMISSION RESULT ============

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                textCurrentLocation.setText("Location permission denied. Using default.");
                currentLat = 3.1390;
                currentLng = 101.6869;
                fetchPrayerTimes(currentLat, currentLng);
            }
        }
    }
}