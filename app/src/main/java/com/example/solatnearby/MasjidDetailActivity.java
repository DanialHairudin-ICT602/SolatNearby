package com.example.solatnearby;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MasjidDetailActivity extends Activity {

    private TextView textDetailMasjidName, textDetailAddress, textDetailDistance, textDetailInfo;
    private TextView textMasjidDetailEmoji;
    private ImageView imageMasjidPhoto;
    private Button btnOpenMap, btnBackNearby;

    private double selectedLat = 0;
    private double selectedLng = 0;

    private String placeId = "";
    private String photoReference = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masjid_detail);

        imageMasjidPhoto = findViewById(R.id.imageMasjidPhoto);
        textMasjidDetailEmoji = findViewById(R.id.textMasjidDetailEmoji);

        textDetailMasjidName = findViewById(R.id.textDetailMasjidName);
        textDetailAddress = findViewById(R.id.textDetailAddress);
        textDetailDistance = findViewById(R.id.textDetailDistance);
        textDetailInfo = findViewById(R.id.textDetailInfo);

        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnBackNearby = findViewById(R.id.btnBackNearby);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        placeId = getIntent().getStringExtra("placeId");
        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");
        String distance = getIntent().getStringExtra("distance");
        String facilities = getIntent().getStringExtra("facilities");
        photoReference = getIntent().getStringExtra("photoReference");

        selectedLat = getIntent().getDoubleExtra("lat", 0);
        selectedLng = getIntent().getDoubleExtra("lng", 0);

        if (placeId == null) {
            placeId = "";
        }

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

        // Default view: show masjid emoji first
        imageMasjidPhoto.setVisibility(View.GONE);
        textMasjidDetailEmoji.setVisibility(View.VISIBLE);

        // If Google photo exists, load it and hide emoji
        if (photoReference != null && !photoReference.trim().isEmpty()) {
            loadMasjidPhoto(photoReference);
        }

        btnOpenMap.setOnClickListener(v -> {
            if (selectedLat == 0 || selectedLng == 0) {
                Toast.makeText(this, "Masjid location is not available.", Toast.LENGTH_SHORT).show();
                return;
            }


            Intent intent = new Intent(MasjidDetailActivity.this, MapNavigationActivity.class);

            intent.putExtra("placeId", placeId);
            intent.putExtra("name", textDetailMasjidName.getText().toString());
            intent.putExtra("address", textDetailAddress.getText().toString());
            intent.putExtra("lat", selectedLat);
            intent.putExtra("lng", selectedLng);

            startActivity(intent);
        });

        btnBackNearby.setOnClickListener(v -> finish());
    }


    private void loadMasjidPhoto(String photoReference) {
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String apiKey = getString(R.string.google_maps_key);

                String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                        + "?maxwidth=900"
                        + "&photo_reference=" + Uri.encode(photoReference)
                        + "&key=" + apiKey;

                URL url = new URL(photoUrl);

                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);

                InputStream inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                runOnUiThread(() -> {
                    if (bitmap != null) {
                        imageMasjidPhoto.setImageBitmap(bitmap);
                        imageMasjidPhoto.setVisibility(View.VISIBLE);
                        textMasjidDetailEmoji.setVisibility(View.GONE);
                    } else {
                        imageMasjidPhoto.setVisibility(View.GONE);
                        textMasjidDetailEmoji.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {
                android.util.Log.e("PLACE_PHOTO", "Photo load failed: " + e.getMessage());

                runOnUiThread(() -> {
                    imageMasjidPhoto.setVisibility(View.GONE);
                    textMasjidDetailEmoji.setVisibility(View.VISIBLE);
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}