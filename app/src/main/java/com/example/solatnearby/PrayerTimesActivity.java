package com.example.solatnearby;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PrayerTimesActivity extends AppCompatActivity {

    private TextView textPrayerLocation, textNextPrayer, textNextPrayerTime,
            textReminderStatus, tvSubuh, tvZohor, tvAsar, tvMaghrib, tvIsyak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_times);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bind views
        textPrayerLocation  = findViewById(R.id.textPrayerLocation);
        textNextPrayer      = findViewById(R.id.textNextPrayer);
        textNextPrayerTime  = findViewById(R.id.textNextPrayerTime);
        textReminderStatus  = findViewById(R.id.textReminderStatus);

        // Your layout uses plain TextViews for each prayer row — add IDs to them (see note below)
        tvSubuh   = findViewById(R.id.tvSubuh);
        tvZohor   = findViewById(R.id.tvZohor);
        tvAsar    = findViewById(R.id.tvAsar);
        tvMaghrib = findViewById(R.id.tvMaghrib);
        tvIsyak   = findViewById(R.id.tvIsyak);

        fetchPrayerTimes("Shah Alam");
    }

    private void fetchPrayerTimes(String city) {
        PrayerApiService api = PrayerApiClient.getInstance().create(PrayerApiService.class);

        api.getPrayerTimes(city, "Malaysia", 9).enqueue(new Callback<PrayerResponse>() {
            @Override
            public void onResponse(Call<PrayerResponse> call, Response<PrayerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PrayerTimings t = response.body().data.timings;

                    runOnUiThread(() -> {
                        // Fill prayer rows
                        tvSubuh.setText("Subuh          " + formatTime(t.Fajr));
                        tvZohor.setText("Zohor          " + formatTime(t.Dhuhr));
                        tvAsar.setText("Asar           " + formatTime(t.Asr));
                        tvMaghrib.setText("Maghrib        " + formatTime(t.Maghrib));
                        tvIsyak.setText("Isyak          " + formatTime(t.Isha));

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
            String clean = raw.split(" ")[0]; // "05:43"
            SimpleDateFormat input  = new SimpleDateFormat("HH:mm", Locale.getDefault());
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
                {"Subuh",   t.Fajr},
                {"Zohor",   t.Dhuhr},
                {"Asar",    t.Asr},
                {"Maghrib", t.Maghrib},
                {"Isyak",   t.Isha}
        };

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date now = new Date();

            for (String[] prayer : prayers) {
                String cleanTime = prayer[1].split(" ")[0];
                Date prayerDate = sdf.parse(cleanTime);

                if (prayerDate != null && prayerDate.after(now)) {
                    textNextPrayer.setText("Next Prayer: " + prayer[0]);
                    textNextPrayerTime.setText(formatTime(prayer[1]));
                    textReminderStatus.setText("Push notification reminder enabled");
                    return;
                }
            }

            // All prayers passed for today
            textNextPrayer.setText("Next Prayer: Subuh");
            textNextPrayerTime.setText("Tomorrow");
            textReminderStatus.setText("All prayers done for today!");

        } catch (ParseException e) {
            Log.e("PrayerTimes", "Time parse error: " + e.getMessage());
        }
    }

    private void scheduleAllPrayers(PrayerTimings t) {
        PrayerScheduler scheduler = new PrayerScheduler(this);
        scheduler.schedule("Subuh",   t.Fajr,    1);
        scheduler.schedule("Zohor",   t.Dhuhr,   2);
        scheduler.schedule("Asar",    t.Asr,     3);
        scheduler.schedule("Maghrib", t.Maghrib, 4);
        scheduler.schedule("Isyak",   t.Isha,    5);
    }
}