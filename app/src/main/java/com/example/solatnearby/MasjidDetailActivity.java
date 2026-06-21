package com.example.solatnearby;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MasjidDetailActivity extends Activity {

    TextView textDetailMasjidName, textDetailAddress, textDetailDistance, textDetailInfo;
    Button btnOpenMap, btnBackNearby;

    private double selectedLat = 0;
    private double selectedLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masjid_detail);

        textDetailMasjidName = findViewById(R.id.textDetailMasjidName);
        textDetailAddress = findViewById(R.id.textDetailAddress);
        textDetailDistance = findViewById(R.id.textDetailDistance);
        textDetailInfo = findViewById(R.id.textDetailInfo);

        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnBackNearby = findViewById(R.id.btnBackNearby);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");
        String distance = getIntent().getStringExtra("distance");
        String facilities = getIntent().getStringExtra("facilities");

        selectedLat = getIntent().getDoubleExtra("lat", 0);
        selectedLng = getIntent().getDoubleExtra("lng", 0);

        if (name != null) {
            textDetailMasjidName.setText(name);
        }

        if (address != null) {
            textDetailAddress.setText(address);
        }

        if (distance != null) {
            textDetailDistance.setText(distance);
        }

        if (facilities != null) {
            textDetailInfo.setText(facilities);
        }

        btnOpenMap.setOnClickListener(v -> {
            Intent intent = new Intent(MasjidDetailActivity.this, MapNavigationActivity.class);

            intent.putExtra("name", textDetailMasjidName.getText().toString());
            intent.putExtra("lat", selectedLat);
            intent.putExtra("lng", selectedLng);

            startActivity(intent);
        });

        btnBackNearby.setOnClickListener(v -> finish());
    }
}