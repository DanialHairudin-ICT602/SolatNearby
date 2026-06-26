package com.example.solatnearby;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private TextView textCurrentLocation;
    private TextView textCurrentPrayer;
    private Button btnFindNearby;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        textCurrentLocation = findViewById(R.id.textCurrentLocation);
        textCurrentPrayer = findViewById(R.id.textCurrentPrayer);
        btnFindNearby = findViewById(R.id.btnFindNearby);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        textCurrentPrayer.setText("Loading prayer times...");

        checkLocationPermission();

        btnFindNearby.setOnClickListener(v -> {
            if (currentLat != 0.0 && currentLng != 0.0) {
                // Navigate to NearbyMasjidActivity
            } else {
                Toast.makeText(this, "Please wait, detecting location...", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.cardNearby).setOnClickListener(v -> {
            // Navigate to NearbyMasjidActivity
        });

        findViewById(R.id.cardMap).setOnClickListener(v -> {
            // Navigate to MapNavigationActivity
        });

        findViewById(R.id.cardPrayerTime).setOnClickListener(v -> {
            // Navigate to PrayerTimesActivity
        });

        findViewById(R.id.cardProfile).setOnClickListener(v -> {
            // Navigate to ProfileActivity
        });

        findViewById(R.id.cardSavedMasjid).setOnClickListener(v -> {
            // Navigate to HistoryActivity
        });

        findViewById(R.id.cardSettings).setOnClickListener(v -> {
            // Navigate to SettingsActivity
        });
    }

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
                            currentLat = 3.1390;
                            currentLng = 101.6869;
                            fetchPrayerTimes(currentLat, currentLng);
                        }
                    }
                });
    }

    private void fetchPrayerTimes(double lat, double lng) {
        PrayerApiService apiService = PrayerApiClient.getInstance().create(PrayerApiService.class);

        // method=9 for JAKIM (Malaysia)
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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