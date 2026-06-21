package com.example.solatnearby;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MapNavigationActivity extends Activity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_CODE = 102;

    // 1 meter is too strict for normal phone GPS. Use 20m for demo stability.
    // You can change this to 1f if your lecturer specifically wants 1 meter.
    private static final float ARRIVAL_RADIUS_METERS = 20f;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextToSpeech textToSpeech;

    private TextView textDestinationName, textRouteInfo, textNavigationInstruction, textArrivalStatus;
    private Button btnStartNavigation;

    private LatLng currentLatLng;
    private LatLng destinationLatLng;
    private String destinationName = "Nearest Masjid";

    private boolean hasSpokenRouteReady = false;
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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.ENGLISH);
            }
        });

        requestNotificationPermission();

        MapFragment mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnStartNavigation.setOnClickListener(v -> openGoogleMapsNavigation());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        checkLocationPermissionAndStart();
    }

    private void checkLocationPermissionAndStart() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );
            return;
        }

        startLocationProcess();
    }

    private void startLocationProcess() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                googleMap.addMarker(new MarkerOptions()
                        .position(currentLatLng)
                        .title("Your current location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                loadDestination();
                startLiveLocationUpdates();
            } else {
                Toast.makeText(this, "Unable to detect location. Please turn on GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadDestination() {
        String nameExtra = getIntent().getStringExtra("name");
        double latExtra = getIntent().getDoubleExtra("lat", 0);
        double lngExtra = getIntent().getDoubleExtra("lng", 0);

        if (nameExtra != null && latExtra != 0 && lngExtra != 0) {
            destinationName = nameExtra;
            destinationLatLng = new LatLng(latExtra, lngExtra);

            textDestinationName.setText(destinationName);
            drawDestinationAndRoute();
        } else {
            findNearestMasjidFromGooglePlaces();
        }
    }

    private void findNearestMasjidFromGooglePlaces() {
        textDestinationName.setText("Finding nearest masjid...");
        textRouteInfo.setText("Using GPS and Google Places API");

        new Thread(() -> {
            try {
                String apiKey = getString(R.string.google_maps_key);
                URL url = new URL("https://places.googleapis.com/v1/places:searchNearby");

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("X-Goog-Api-Key", apiKey);
                connection.setRequestProperty(
                        "X-Goog-FieldMask",
                        "places.displayName,places.formattedAddress,places.location"
                );
                connection.setDoOutput(true);

                String body =
                        "{"
                                + "\"includedTypes\":[\"mosque\"],"
                                + "\"maxResultCount\":5,"
                                + "\"locationRestriction\":{"
                                + "\"circle\":{"
                                + "\"center\":{"
                                + "\"latitude\":" + currentLatLng.latitude + ","
                                + "\"longitude\":" + currentLatLng.longitude
                                + "},"
                                + "\"radius\":5000.0"
                                + "}"
                                + "}"
                                + "}";

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(body.getBytes());
                outputStream.flush();
                outputStream.close();

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
                JSONArray places = jsonObject.optJSONArray("places");

                if (places == null || places.length() == 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No nearby masjid found.", Toast.LENGTH_SHORT).show();
                        useFallbackMasjid();
                    });
                    return;
                }

                JSONObject nearestPlace = places.getJSONObject(0);

                String foundName = nearestPlace
                        .getJSONObject("displayName")
                        .getString("text");

                JSONObject location = nearestPlace.getJSONObject("location");
                double lat = location.getDouble("latitude");
                double lng = location.getDouble("longitude");

                runOnUiThread(() -> {
                    destinationName = foundName;
                    destinationLatLng = new LatLng(lat, lng);

                    textDestinationName.setText(destinationName);
                    drawDestinationAndRoute();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Places API failed. Using sample masjid.", Toast.LENGTH_SHORT).show();
                    useFallbackMasjid();
                });
            }
        }).start();
    }

    private void useFallbackMasjid() {
        destinationName = "Masjid Sultan Salahuddin Abdul Aziz Shah";
        destinationLatLng = new LatLng(3.0789, 101.5199);

        textDestinationName.setText(destinationName);
        drawDestinationAndRoute();
    }

    private void drawDestinationAndRoute() {
        if (googleMap == null || currentLatLng == null || destinationLatLng == null) return;

        googleMap.clear();

        googleMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title("Your location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        googleMap.addMarker(new MarkerOptions()
                .position(destinationLatLng)
                .title(destinationName)
                .snippet("Selected masjid / surau")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        fetchRouteFromDirectionsAPI();
    }

    private void fetchRouteFromDirectionsAPI() {
        new Thread(() -> {
            try {
                String apiKey = getString(R.string.google_maps_key);

                String urlText = "https://maps.googleapis.com/maps/api/directions/json?"
                        + "origin=" + currentLatLng.latitude + "," + currentLatLng.longitude
                        + "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude
                        + "&mode=driving"
                        + "&key=" + apiKey;

                URL url = new URL(urlText);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

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
                JSONArray routes = jsonObject.getJSONArray("routes");

                if (routes.length() == 0) {
                    runOnUiThread(() -> textRouteInfo.setText("Route not available"));
                    return;
                }

                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");

                JSONArray legs = route.getJSONArray("legs");
                JSONObject leg = legs.getJSONObject(0);

                String distance = leg.getJSONObject("distance").getString("text");
                String duration = leg.getJSONObject("duration").getString("text");

                ArrayList<LatLng> polylinePoints = decodePolyline(encodedPolyline);

                runOnUiThread(() -> {
                    googleMap.addPolyline(new PolylineOptions()
                            .addAll(polylinePoints)
                            .width(12f)
                            .color(Color.rgb(14, 143, 90))
                            .jointType(JointType.ROUND));

                    textRouteInfo.setText(distance + " • Estimated " + duration);
                    textNavigationInstruction.setText("Head towards the selected masjid");
                    textArrivalStatus.setText("You are not near the destination yet");

                    zoomToRoute(polylinePoints);

                    if (!hasSpokenRouteReady) {
                        speak("Route ready. Head towards " + destinationName);
                        hasSpokenRouteReady = true;
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    textRouteInfo.setText("Unable to load route");
                    Toast.makeText(this, "Directions API failed.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void zoomToRoute(ArrayList<LatLng> points) {
        if (points == null || points.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (LatLng point : points) {
            builder.include(point);
        }

        builder.include(currentLatLng);
        builder.include(destinationLatLng);

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
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

                float[] result = new float[1];
                Location.distanceBetween(
                        currentLatLng.latitude,
                        currentLatLng.longitude,
                        destinationLatLng.latitude,
                        destinationLatLng.longitude,
                        result
                );

                float distanceMeters = result[0];

                if (distanceMeters <= ARRIVAL_RADIUS_METERS && !hasArrived) {
                    hasArrived = true;

                    textNavigationInstruction.setText("You have arrived");
                    textArrivalStatus.setText("You are near " + destinationName);

                    speak("You have arrived near " + destinationName);
                    NotificationHelper.showArrivalNotification(MapNavigationActivity.this, destinationName);

                } else if (!hasArrived) {
                    textArrivalStatus.setText(String.format(Locale.getDefault(),
                            "%.0f meters remaining", distanceMeters));
                }
            }
        };

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    getMainLooper()
            );
        }
    }

    private void openGoogleMapsNavigation() {
        if (destinationLatLng == null) {
            Toast.makeText(this, "Destination not ready yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        speak("Opening Google Maps navigation to " + destinationName);

        Uri uri = Uri.parse("google.navigation:q="
                + destinationLatLng.latitude
                + ","
                + destinationLatLng.longitude
                + "&mode=d");

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                    + destinationLatLng.latitude
                    + ","
                    + destinationLatLng.longitude);

            startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
        }
    }

    private void speak(String message) {
        if (textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
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
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationProcess();
            } else {
                Toast.makeText(this, "Location permission is required for GPS navigation.", Toast.LENGTH_LONG).show();
            }
        }
    }
}