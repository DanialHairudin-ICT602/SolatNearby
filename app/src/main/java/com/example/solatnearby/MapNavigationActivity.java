package com.example.solatnearby;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MapNavigationActivity extends Activity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_CODE = 100;
    private static final float ARRIVAL_RADIUS_METERS = 25f;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextToSpeech textToSpeech;

    private TextView textDestinationName, textRouteInfo, textNavigationInstruction, textArrivalStatus;
    private Button btnStartNavigation, btnStopNavigation;

    private LatLng currentLatLng;
    private LatLng destinationLatLng;

    private Marker userMarker;
    private Marker destinationMarker;

    private String destinationName = "Selected Masjid";
    private String firstInstruction = "Head towards the selected masjid";

    private boolean routeLoaded = false;
    private boolean navigationStarted = false;
    private boolean navigationPaused = false;
    private boolean hasArrived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_navigation);

        textDestinationName = findViewById(R.id.textDestinationName);
        textRouteInfo = findViewById(R.id.textRouteInfo);
        textNavigationInstruction = findViewById(R.id.textNavigationInstruction);
        textArrivalStatus = findViewById(R.id.textArrivalStatus);

        btnStartNavigation = findViewById(R.id.btnStartNavigation);
        btnStopNavigation = findViewById(R.id.btnStopNavigation);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.ENGLISH);
                textToSpeech.setSpeechRate(0.95f);
            }
        });

        getDestinationFromIntent();

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnStartNavigation.setText("Start Navigation");

        if (btnStopNavigation != null) {
            btnStopNavigation.setVisibility(View.GONE);
        }

        btnStartNavigation.setOnClickListener(v -> {
            if (!routeLoaded) {
                Toast.makeText(this, "Route is still loading. Please wait.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!navigationStarted && !navigationPaused) {
                startNavigationMode();
            } else if (navigationStarted && !navigationPaused) {
                pauseNavigationMode();
            } else if (navigationPaused) {
                resumeNavigationMode();
            }
        });

        if (btnStopNavigation != null) {
            btnStopNavigation.setOnClickListener(v -> stopNavigationAndGoNearby());
        }
    }

    private void getDestinationFromIntent() {
        String name = getIntent().getStringExtra("name");
        double lat = getIntent().getDoubleExtra("lat", 0);
        double lng = getIntent().getDoubleExtra("lng", 0);

        if (name != null && !name.trim().isEmpty()) {
            destinationName = name;
        }

        if (lat != 0 && lng != 0) {
            destinationLatLng = new LatLng(lat, lng);

            textDestinationName.setText(destinationName);
            textRouteInfo.setText("Waiting for GPS location...");
            textNavigationInstruction.setText("Route guide will appear here");
            textArrivalStatus.setText("Waiting for navigation status");

        } else {
            destinationLatLng = null;

            textDestinationName.setText("Finding nearest masjid...");
            textRouteInfo.setText("Waiting for GPS location...");
            textNavigationInstruction.setText("Map guide will auto-select nearest masjid");
            textArrivalStatus.setText("Detecting nearby masjid");
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );
        } else {
            loadCurrentLocation();
        }
    }

    private void loadCurrentLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                if (destinationLatLng == null) {
                    findNearestMasjidForMapGuide();
                } else {
                    setupMapMarkers();
                    fetchRouteFromDirectionsAPI();
                }

                startLiveLocationUpdates();

            } else {
                Toast.makeText(this, "Unable to detect current location. Please turn on GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void findNearestMasjidForMapGuide() {
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String apiKey = getString(R.string.google_maps_key);

                String urlText = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                        + "?location=" + currentLatLng.latitude + "," + currentLatLng.longitude
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

                android.util.Log.e("PLACES_API", "MAP GUIDE STATUS: " + status);
                android.util.Log.e("PLACES_API", "MAP GUIDE ERROR: " + errorMessage);

                if (!status.equals("OK")) {
                    runOnUiThread(() -> {
                        textDestinationName.setText("Unable to find nearest masjid");
                        textRouteInfo.setText("Places API error: " + status);
                        Toast.makeText(this, "Unable to find nearest masjid: " + status, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                JSONArray results = jsonObject.optJSONArray("results");

                if (results == null || results.length() == 0) {
                    runOnUiThread(() -> {
                        textDestinationName.setText("No nearby masjid found");
                        textRouteInfo.setText("Try nearby masjid list instead");
                        Toast.makeText(this, "No nearby masjid found within 5 km.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                JSONObject place = results.getJSONObject(0);

                String name = place.optString("name", "Nearest Masjid");

                JSONObject geometry = place.getJSONObject("geometry");
                JSONObject locationJson = geometry.getJSONObject("location");

                double lat = locationJson.getDouble("lat");
                double lng = locationJson.getDouble("lng");

                runOnUiThread(() -> {
                    destinationName = name;
                    destinationLatLng = new LatLng(lat, lng);

                    textDestinationName.setText(destinationName);
                    textArrivalStatus.setText("Nearest masjid selected");

                    setupMapMarkers();
                    fetchRouteFromDirectionsAPI();
                });

            } catch (Exception e) {
                android.util.Log.e("PLACES_API", "MAP GUIDE EXCEPTION: " + e.getMessage());

                runOnUiThread(() -> {
                    textDestinationName.setText("Unable to load map guide");
                    textRouteInfo.setText("Error loading nearest masjid");
                    Toast.makeText(this, "Map Guide failed to find nearest masjid.", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void setupMapMarkers() {
        if (googleMap == null || currentLatLng == null || destinationLatLng == null) return;

        googleMap.clear();

        userMarker = googleMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        destinationMarker = googleMap.addMarker(new MarkerOptions()
                .position(destinationLatLng)
                .title(destinationName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
    }

    private void fetchRouteFromDirectionsAPI() {
        if (currentLatLng == null || destinationLatLng == null) return;

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String apiKey = getString(R.string.google_maps_key);

                String urlText = "https://maps.googleapis.com/maps/api/directions/json"
                        + "?origin=" + currentLatLng.latitude + "," + currentLatLng.longitude
                        + "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude
                        + "&mode=driving"
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

                android.util.Log.e("DIRECTIONS_API", "STATUS: " + status);
                android.util.Log.e("DIRECTIONS_API", "ERROR MESSAGE: " + errorMessage);
                android.util.Log.e("DIRECTIONS_API", "FULL RESPONSE: " + response.toString());

                if (!status.equals("OK")) {
                    runOnUiThread(() -> {
                        textRouteInfo.setText("Route failed: " + status);
                        Toast.makeText(this, "Directions API failed: " + status, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                JSONArray routes = jsonObject.getJSONArray("routes");
                JSONObject route = routes.getJSONObject(0);

                String encodedPolyline = route
                        .getJSONObject("overview_polyline")
                        .getString("points");

                JSONArray legs = route.getJSONArray("legs");
                JSONObject leg = legs.getJSONObject(0);

                String distance = leg.getJSONObject("distance").getString("text");
                String duration = leg.getJSONObject("duration").getString("text");

                JSONArray steps = leg.getJSONArray("steps");

                if (steps.length() > 0) {
                    String htmlInstruction = steps.getJSONObject(0).optString("html_instructions", "");
                    firstInstruction = cleanHtml(htmlInstruction);
                }

                ArrayList<LatLng> routePoints = decodePolyline(encodedPolyline);

                runOnUiThread(() -> drawRouteOnMap(routePoints, distance, duration));

            } catch (Exception e) {
                android.util.Log.e("DIRECTIONS_API", "EXCEPTION: " + e.getMessage());

                runOnUiThread(() -> {
                    textRouteInfo.setText("Unable to load route");
                    Toast.makeText(this, "Error loading route.", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void drawRouteOnMap(ArrayList<LatLng> routePoints, String distance, String duration) {
        if (googleMap == null || routePoints == null || routePoints.isEmpty()) return;

        googleMap.addPolyline(new PolylineOptions()
                .addAll(routePoints)
                .width(12f)
                .color(Color.rgb(14, 143, 90))
                .jointType(JointType.ROUND));

        textRouteInfo.setText(distance + " • Estimated " + duration);
        textNavigationInstruction.setText(firstInstruction);
        textArrivalStatus.setText("Route is ready inside SolatNearby");

        routeLoaded = true;

        zoomToRoute(routePoints);

        speak("Route ready.");
    }

    private void zoomToRoute(ArrayList<LatLng> routePoints) {
        if (routePoints == null || routePoints.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (LatLng point : routePoints) {
            builder.include(point);
        }

        builder.include(currentLatLng);
        builder.include(destinationLatLng);

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
    }

    private void startNavigationMode() {
        navigationStarted = true;
        navigationPaused = false;

        btnStartNavigation.setText("Pause Navigation");

        if (btnStopNavigation != null) {
            btnStopNavigation.setVisibility(View.VISIBLE);
        }

        textNavigationInstruction.setText(firstInstruction);
        textArrivalStatus.setText("Navigation started inside SolatNearby");

        speak("Navigation started.");
    }

    private void pauseNavigationMode() {
        navigationPaused = true;
        navigationStarted = false;

        btnStartNavigation.setText("Resume Navigation");

        textArrivalStatus.setText("Navigation paused");
        speak("Navigation paused.");
    }

    private void resumeNavigationMode() {
        navigationStarted = true;
        navigationPaused = false;

        btnStartNavigation.setText("Pause Navigation");

        textArrivalStatus.setText("Navigation resumed");
        speak("Navigation resumed.");
    }

    private void stopNavigationAndGoNearby() {
        navigationStarted = false;
        navigationPaused = false;
        hasArrived = false;

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        Toast.makeText(this, "Navigation stopped", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MapNavigationActivity.this, NearbyMasjidActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void startLiveLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000
        )
                .setMinUpdateIntervalMillis(1500)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || destinationLatLng == null) return;

                Location location = locationResult.getLastLocation();
                if (location == null) return;

                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                updateUserMarker();

                float[] result = new float[1];

                Location.distanceBetween(
                        currentLatLng.latitude,
                        currentLatLng.longitude,
                        destinationLatLng.latitude,
                        destinationLatLng.longitude,
                        result
                );

                float distanceMeters = result[0];

                if (navigationStarted && !navigationPaused
                        && distanceMeters <= ARRIVAL_RADIUS_METERS
                        && !hasArrived) {

                    hasArrived = true;

                    textNavigationInstruction.setText("You have arrived");
                    textArrivalStatus.setText("You are near " + destinationName);

                    speak("You have arrived.");

                    try {
                        NotificationHelper.showArrivalNotification(MapNavigationActivity.this, destinationName);
                    } catch (Exception ignored) {
                    }

                } else if (!hasArrived && navigationStarted && !navigationPaused) {
                    textArrivalStatus.setText(String.format(Locale.getDefault(),
                            "%.0f meters remaining", distanceMeters));

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
                }
            }
        };

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    getMainLooper()
            );
        }
    }

    private void updateUserMarker() {
        if (googleMap == null || currentLatLng == null) return;

        if (userMarker == null) {
            userMarker = googleMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            userMarker.setPosition(currentLatLng);
        }
    }

    private String cleanHtml(String html) {
        if (html == null) return "Head towards the selected masjid";

        String cleaned;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            cleaned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            cleaned = Html.fromHtml(html).toString();
        }

        cleaned = cleaned.replace("\n", " ").trim();

        if (cleaned.isEmpty()) {
            return "Head towards the selected masjid";
        }

        return cleaned;
    }

    private ArrayList<LatLng> decodePolyline(String encoded) {
        ArrayList<LatLng> polyline = new ArrayList<>();

        int index = 0;
        int length = encoded.length();
        int lat = 0;
        int lng = 0;

        while (index < length) {
            int b;
            int shift = 0;
            int result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lat += dlat;

            shift = 0;
            result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lng += dlng;

            LatLng point = new LatLng(
                    lat / 1E5,
                    lng / 1E5
            );

            polyline.add(point);
        }

        return polyline;
    }

    private void speak(String message) {
        if (textToSpeech != null && message != null && !message.trim().isEmpty()) {
            textToSpeech.stop();
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission is required for navigation.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}