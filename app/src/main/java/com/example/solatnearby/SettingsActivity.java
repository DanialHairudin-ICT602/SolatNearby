package com.example.solatnearby;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private Switch switchPrayerNotification;
    private Switch switchArrivalAlert;
    private Switch switchVoiceNavigation;
    private TextView btnBack3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize views
        switchPrayerNotification = findViewById(R.id.switchPrayerNotification);
        switchArrivalAlert = findViewById(R.id.switchArrivalAlert);
        switchVoiceNavigation = findViewById(R.id.switchVoiceNavigation);
        btnBack3 = findViewById(R.id.btnBack3);

        // Back button
        btnBack3.setOnClickListener(v -> finish());

        // SharedPreferences
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);

        // Restore saved switch states
        switchPrayerNotification.setChecked(
                prefs.getBoolean("prayer_notification", false));

        switchArrivalAlert.setChecked(
                prefs.getBoolean("arrival_alert", false));

        switchVoiceNavigation.setChecked(
                prefs.getBoolean("voice_navigation", false));

        // Prayer Notification Switch
        switchPrayerNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("prayer_notification", isChecked).apply();

            Toast.makeText(
                    SettingsActivity.this,
                    isChecked ? "Prayer notification enabled"
                            : "Prayer notification disabled",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Arrival Alert Switch
        switchArrivalAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("arrival_alert", isChecked).apply();

            Toast.makeText(
                    SettingsActivity.this,
                    isChecked ? "Arrival alert enabled"
                            : "Arrival alert disabled",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Voice Navigation Switch
        switchVoiceNavigation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("voice_navigation", isChecked).apply();

            Toast.makeText(
                    SettingsActivity.this,
                    isChecked ? "Voice navigation enabled"
                            : "Voice navigation disabled",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}