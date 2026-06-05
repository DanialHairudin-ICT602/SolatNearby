package com.example.solatnearby;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    Switch switchPrayerNotification;
    Switch switchArrivalAlert;
    Switch switchVoiceNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchPrayerNotification = findViewById(R.id.switchPrayerNotification);
        switchArrivalAlert = findViewById(R.id.switchArrivalAlert);
        switchVoiceNavigation = findViewById(R.id.switchVoiceNavigation);

        switchPrayerNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, isChecked ? "Prayer notification enabled" : "Prayer notification disabled", Toast.LENGTH_SHORT).show();
        });

        switchArrivalAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, isChecked ? "Arrival alert enabled" : "Arrival alert disabled", Toast.LENGTH_SHORT).show();
        });

        switchVoiceNavigation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, isChecked ? "Voice navigation enabled" : "Voice navigation disabled", Toast.LENGTH_SHORT).show();
        });
    }
}