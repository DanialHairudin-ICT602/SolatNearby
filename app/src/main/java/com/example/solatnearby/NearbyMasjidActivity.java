package com.example.solatnearby;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class NearbyMasjidActivity extends Activity {

    private static final int LOCATION_PERMISSION_CODE = 201;

    private ListView listMasjid;
    private TextView textNearbySubtitle;

    private FusedLocationProviderClient fusedLocationClient;
    private final ArrayList<HashMap<String, String>> masjidList = new ArrayList<>();

    private double currentLat = 0;
    private double currentLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_masjid);

        listMasjid = findViewById(R.id.listMasjid);
        textNearbySubtitle = findViewById(R.id.textNearbySubtitle);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        checkPermissionAndLoadMasjid();
    }

    private void checkPermissionAndLoadMasjid() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );
            return;
        }

        getCurrentLocationAndSearch();
    }

    private void getCurrentLocationAndSearch() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        textNearbySubtitle.setText("Detecting your GPS location...");

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();

                textNearbySubtitle.setText("Searching nearby masjid from Google Maps...");
                searchNearbyMasjidFromPlacesAPI();
            } else {
                textNearbySubtitle.setText("Unable to detect GPS location.");
                Toast.makeText(
                        this,
                        "Please turn on GPS/location and try again.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }).addOnFailureListener(e -> {
            textNearbySubtitle.setText("GPS detection failed.");
            Toast.makeText(
                    this,
                    "Unable to get current location.",
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private void searchNearbyMasjidFromPlacesAPI() {
        masjidList.clear();

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String apiKey = getString(R.string.google_maps_key);

                String urlText = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                        + "?location=" + currentLat + "," + currentLng
                        + "&radius=5000"
                        + "&type=mosque"
                        + "&key=" + apiKey;

                URL url = new URL(urlText);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject jsonObject = new JSONObject(response.toString());

                String status = jsonObject.optString("status", "");
                String errorMessage = jsonObject.optString("error_message", "");

                android.util.Log.e("PLACES_API", "STATUS: " + status);
                android.util.Log.e("PLACES_API", "ERROR MESSAGE: " + errorMessage);
                android.util.Log.e("PLACES_API", "FULL RESPONSE: " + response.toString());

                if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                    runOnUiThread(() -> {
                        textNearbySubtitle.setText("Places API error: " + status);

                        Toast.makeText(
                                this,
                                "Places API failed: " + status,
                                Toast.LENGTH_LONG
                        ).show();
                    });
                    return;
                }

                JSONArray results = jsonObject.optJSONArray("results");

                if (results == null || results.length() == 0) {
                    runOnUiThread(() -> {
                        textNearbySubtitle.setText("No nearby masjid found within 5 km.");

                        Toast.makeText(
                                this,
                                "No nearby masjid found.",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                    return;
                }

                for (int i = 0; i < results.length(); i++) {
                    JSONObject place = results.getJSONObject(i);

                    String placeId = place.optString("place_id", "");
                    String name = place.optString("name", "Unknown Masjid");
                    String address = place.optString("vicinity", "Address not available");

                    JSONObject geometry = place.getJSONObject("geometry");
                    JSONObject locationJson = geometry.getJSONObject("location");

                    double lat = locationJson.getDouble("lat");
                    double lng = locationJson.getDouble("lng");

                    float[] distanceResult = new float[1];

                    Location.distanceBetween(
                            currentLat,
                            currentLng,
                            lat,
                            lng,
                            distanceResult
                    );

                    double km = distanceResult[0] / 1000.0;
                    String distanceText = String.format(java.util.Locale.getDefault(), "%.1f km away", km);

                    HashMap<String, String> masjid = new HashMap<>();
                    masjid.put("placeId", placeId);
                    masjid.put("name", name);
                    masjid.put("address", address);
                    masjid.put("distance", distanceText);
                    masjid.put("facilities", "Information gathered from Google Places API.");
                    masjid.put("lat", String.valueOf(lat));
                    masjid.put("lng", String.valueOf(lng));

                    masjidList.add(masjid);
                }

                runOnUiThread(() -> {
                    textNearbySubtitle.setText("Showing nearby masjid from Google Maps");
                    displayMasjidList();
                });

            } catch (Exception e) {
                android.util.Log.e("PLACES_API", "EXCEPTION: " + e.getMessage());

                runOnUiThread(() -> {
                    textNearbySubtitle.setText("Unable to load nearby masjid.");

                    Toast.makeText(
                            this,
                            "Error loading Google Maps masjid data.",
                            Toast.LENGTH_LONG
                    ).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void addMasjid(
            String name,
            String address,
            String distance,
            String facilities,
            double lat,
            double lng
    ) {
        HashMap<String, String> masjid = new HashMap<>();

        masjid.put("name", name);
        masjid.put("address", address);
        masjid.put("distance", distance);
        masjid.put("facilities", facilities);
        masjid.put("lat", String.valueOf(lat));
        masjid.put("lng", String.valueOf(lng));

        masjidList.add(masjid);
    }

    private void displayMasjidList() {
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                masjidList,
                R.layout.item_masjid,
                new String[]{"name", "address", "distance"},
                new int[]{
                        R.id.textMasjidName,
                        R.id.textMasjidAddress,
                        R.id.textMasjidDistance
                }
        );

        listMasjid.setAdapter(adapter);

        listMasjid.setOnItemClickListener((parent, view, position, id) -> {
            HashMap<String, String> selectedMasjid = masjidList.get(position);

            Intent intent = new Intent(NearbyMasjidActivity.this, MasjidDetailActivity.class);

            intent.putExtra("name", selectedMasjid.get("name"));
            intent.putExtra("address", selectedMasjid.get("address"));
            intent.putExtra("distance", selectedMasjid.get("distance"));
            intent.putExtra("facilities", selectedMasjid.get("facilities"));
            intent.putExtra("lat", Double.parseDouble(selectedMasjid.get("lat")));
            intent.putExtra("lng", Double.parseDouble(selectedMasjid.get("lng")));

            startActivity(intent);
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndSearch();
            } else {
                textNearbySubtitle.setText("Location permission denied.");
                Toast.makeText(
                        this,
                        "Location permission is required to search nearby masjid.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}