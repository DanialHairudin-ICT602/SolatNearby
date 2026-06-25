package com.example.solatnearby;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class HomeActivity extends Activity {

    private Button btnFindNearby;
    private LinearLayout cardNearby, cardMap, cardPrayerTime, cardProfile, cardSettings, cardSavedMasjid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnFindNearby = findViewById(R.id.btnFindNearby);

        cardNearby = findViewById(R.id.cardNearby);
        cardMap = findViewById(R.id.cardMap);
        cardPrayerTime = findViewById(R.id.cardPrayerTime);
        cardProfile = findViewById(R.id.cardProfile);
        cardSettings = findViewById(R.id.cardSettings);
        cardSavedMasjid = findViewById(R.id.cardSavedMasjid);

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

        // ⭐ Saved Masjid / History Card
        if (cardSavedMasjid != null) {
            cardSavedMasjid.setClickable(true);
            cardSavedMasjid.setFocusable(true);
            cardSavedMasjid.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, HistoryActivity.class)));
        }
    }



    // ============ METHODS ============

    private void openNearbyMasjid() {
        startActivity(new Intent(HomeActivity.this, NearbyMasjidActivity.class));
    }

    private void openMapGuide() {
        Toast.makeText(this, "Opening nearest masjid route", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(HomeActivity.this, MapNavigationActivity.class);
        intent.putExtra("mode", "auto_nearest");
        startActivity(intent);
    }
}