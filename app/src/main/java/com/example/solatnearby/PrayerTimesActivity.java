package com.example.solatnearby;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.Activity;

public class PrayerTimesActivity extends AppCompatActivity {

    private TextView textPrayerLocation, textNextPrayer, textNextPrayerTime,
            textReminderStatus, tvSubuh, tvZohor, tvAsar, tvMaghrib, tvIsyak;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_times);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bind views
        textPrayerLocation = findViewById(R.id.textPrayerLocation);
        textNextPrayer = findViewById(R.id.textNextPrayer);
        textNextPrayerTime = findViewById(R.id.textNextPrayerTime);
        textReminderStatus = findViewById(R.id.textReminderStatus);

        tvSubuh = findViewById(R.id.tvSubuh);
        tvZohor = findViewById(R.id.tvZohor);
        tvAsar = findViewById(R.id.tvAsar);
        tvMaghrib = findViewById(R.id.tvMaghrib);
        tvIsyak = findViewById(R.id.tvIsyak);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check permission and get location
        checkLocationPermission();

        requestNotificationPermission(); // notification
        checkLocationPermission();
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
                            textPrayerLocation.setText("Lat: " + currentLat + ", Lng: " + currentLng);
                            fetchPrayerTimesByCoordinates(currentLat, currentLng);
                        } else {
                            textPrayerLocation.setText("Unable to get location. Using default.");
                            currentLat = 3.1390; // Kuala Lumpur default
                            currentLng = 101.6869;
                            fetchPrayerTimesByCoordinates(currentLat, currentLng);
                        }
                    }
                });
    }

    private void fetchPrayerTimesByCoordinates(double lat, double lng) {
        PrayerApiService api = PrayerApiClient.getInstance().create(PrayerApiService.class);

        api.getPrayerTimesByCoordinates(lat, lng, 9).enqueue(new Callback<PrayerResponse>() {
            @Override
            public void onResponse(Call<PrayerResponse> call, Response<PrayerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PrayerTimings t = response.body().data.timings;

                    runOnUiThread(() -> {
                        // Fill prayer rows
                        tvSubuh.setText("Fajr           " + formatTime(t.Fajr));
                        tvZohor.setText("Dhuhr          " + formatTime(t.Dhuhr));
                        tvAsar.setText("Asr            " + formatTime(t.Asr));
                        tvMaghrib.setText("Maghrib        " + formatTime(t.Maghrib));
                        tvIsyak.setText("Isha           " + formatTime(t.Isha));

                        // Find and display next prayer
                        updateNextPrayer(t);
                    });

                    // Schedule notifications
                    scheduleAllPrayers(t);

                } else {
                    Log.e("PrayerTimes", "Response unsuccessful");
                    runOnUiThread(() ->
                            textReminderStatus.setText("Failed to load prayer times. Check connection."));
                }
            }

            @Override
            public void onFailure(Call<PrayerResponse> call, Throwable t) {
                Log.e("PrayerTimes", "API call failed: " + t.getMessage());
                runOnUiThread(() ->
                        textReminderStatus.setText("No internet connection."));
            }
        });
    }

    // Converts "05:43 (MYT)" → "5:43 AM"
    private String formatTime(String raw) {
        try {
            if (raw == null) return "--:--";
            String clean = raw.split(" ")[0]; // "05:43"
            SimpleDateFormat input = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = input.parse(clean);
            return date != null ? output.format(date) : raw;
        } catch (ParseException e) {
            return raw;
        }
    }

    // Figures out which prayer is next and updates the header card
    private void updateNextPrayer(PrayerTimings t) {
        String[][] prayers = {
                {"Fajr", t.Fajr},
                {"Dhuhr", t.Dhuhr},
                {"Asr", t.Asr},
                {"Maghrib", t.Maghrib},
                {"Isha", t.Isha}
        };

        try {
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
            int currentTotalMinutes = currentHour * 60 + currentMinute;

            String nextPrayerName = "";
            String nextPrayerTime = "";
            int smallestDiff = Integer.MAX_VALUE;

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            for (String[] prayer : prayers) {
                String prayerTime = prayer[1];
                if (prayerTime == null || prayerTime.isEmpty()) continue;

                String cleanTime = prayerTime.split(" ")[0];
                Date prayerDate = sdf.parse(cleanTime);

                if (prayerDate != null) {
                    Calendar prayerCal = Calendar.getInstance();
                    prayerCal.setTime(prayerDate);
                    int prayerMinutes = prayerCal.get(Calendar.HOUR_OF_DAY) * 60 + prayerCal.get(Calendar.MINUTE);

                    int diff = prayerMinutes - currentTotalMinutes;

                    if (diff < 0) {
                        diff += 1440;
                    }

                    if (diff < smallestDiff) {
                        smallestDiff = diff;
                        nextPrayerName = prayer[0];
                        nextPrayerTime = prayerTime;
                    }
                }
            }

            if (!nextPrayerName.isEmpty() && !nextPrayerTime.isEmpty()) {
                textNextPrayer.setText("Next Prayer: " + nextPrayerName);
                textNextPrayerTime.setText(formatTime(nextPrayerTime));
                textReminderStatus.setText("Push notification reminder enabled");
            } else {
                textNextPrayer.setText("Next Prayer: --");
                textNextPrayerTime.setText("--:--");
            }

        } catch (ParseException e) {
            Log.e("PrayerTimes", "Time parse error: " + e.getMessage());
        }
    }

    private void scheduleAllPrayers(PrayerTimings t) {
        PrayerScheduler scheduler = new PrayerScheduler(this);
        scheduler.schedule("Fajr", t.Fajr, 1);
        scheduler.schedule("Dhuhr", t.Dhuhr, 2);
        scheduler.schedule("Asr", t.Asr, 3);
        scheduler.schedule("Maghrib", t.Maghrib, 4);
        scheduler.schedule("Isha", t.Isha, 5);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                textPrayerLocation.setText("Location permission denied. Using default.");
                currentLat = 3.1390;
                currentLng = 101.6869;
                fetchPrayerTimesByCoordinates(currentLat, currentLng);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101);
            }
        }
    }

}