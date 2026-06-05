package com.example.solatnearby;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public class HomeActivity extends Activity {

    private Button btnFindNearby;
    private LinearLayout cardNearby, cardMap, cardPrayerTime, cardNotification, cardProfile, cardSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnFindNearby = findViewById(R.id.btnFindNearby);

        cardNearby = findViewById(R.id.cardNearby);
        cardMap = findViewById(R.id.cardMap);
        cardPrayerTime = findViewById(R.id.cardPrayerTime);
        cardNotification = findViewById(R.id.cardNotification);
        cardProfile = findViewById(R.id.cardProfile);
        cardSettings = findViewById(R.id.cardSettings);

        btnFindNearby.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, NearbyMasjidActivity.class)));

        cardNearby.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, NearbyMasjidActivity.class)));

        cardMap.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MapNavigationActivity.class)));

        cardPrayerTime.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, PrayerTimesActivity.class)));

        cardNotification.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, PrayerTimesActivity.class)));

        cardProfile.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));

        cardSettings.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));
    }
}