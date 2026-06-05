package com.example.solatnearby;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MasjidDetailActivity extends Activity {

    Button btnOpenMap, btnBackNearby;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masjid_detail);

        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnBackNearby = findViewById(R.id.btnBackNearby);

        btnOpenMap.setOnClickListener(v -> {
            startActivity(new Intent(MasjidDetailActivity.this, MapNavigationActivity.class));
        });

        btnBackNearby.setOnClickListener(v -> {
            finish();
        });
    }
}