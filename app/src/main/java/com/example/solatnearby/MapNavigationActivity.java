package com.example.solatnearby;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private EditText editMapSearch;
    private Button btnMapSearch;
    private View searchMapRow;

    private LatLng currentLatLng;
    private LatLng destinationLatLng;

    private Marker userMarker;
    private Marker destinationMarker;

    private String destinationName = "Selected Masjid";
    private String firstInstruction = "Follow the route.";

    private boolean routeLoaded = false;
    private boolean navigationStarted = false;
    private boolean navigationPaused = false;
    private boolean hasArrived = false;
    private boolean historySavedForThisRoute = false;

    private Handler cameraFollowHandler = new Handler(Looper.getMainLooper());
    private boolean cameraAutoFollow = false;
    private boolean userMovedMapManually = false;

    private ArrayList<LatLng> currentRoutePoints = new ArrayList<>();
    private ArrayList<NavigationStep> navigationSteps = new ArrayList<>();
    private int currentStepIndex = 0;
    private String lastSpokenInstruction = "";

    private final Runnable resumeCameraFollowRunnable = new Runnable() {
        @Override
        public void run() {
            if (navigationStarted && !navigationPaused && currentLatLng != null) {
                cameraAutoFollow = true;
                userMovedMapManually = false;

                textArrivalStatus.setText("Auto-follow resumed");
                moveCameraToUserNavigationView();
            }
        }
    };

    private static class NavigationStep {
        LatLng startPoint;
        LatLng endPoint;
        String distanceText;
        int distanceMeters;
        String simpleInstruction;
    }

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

        editMapSearch = findViewById(R.id.editMapSearch);
        btnMapSearch = findViewById(R.id.btnMapSearch);
        searchMapRow = findViewById(R.id.searchMapRow);

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

        if (btnMapSearch != null) {
            btnMapSearch.setOnClickListener(v -> {
                String query = editMapSearch.getText().toString().trim();

                if (query.isEmpty()) {
                    Toast.makeText(this, "Enter masjid name to search", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentLatLng == null) {
                    Toast.makeText(this, "GPS is still loading. Please wait.", Toast.LENGTH_SHORT).show();
                    return;
                }

                searchMasjidOnMap(query);
            });
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

        setupCameraGestureListener();
        checkLocationPermission();
    }

    private void setupCameraGestureListener() {
        if (googleMap == null) {
            return;
        }

        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
                    && navigationStarted
                    && !navigationPaused) {

                cameraAutoFollow = false;
                userMovedMapManually = true;

                textArrivalStatus.setText("Manual map view. Auto-follow in 10 seconds");

                cameraFollowHandler.removeCallbacks(resumeCameraFollowRunnable);
                cameraFollowHandler.postDelayed(resumeCameraFollowRunnable, 10000);
            }
        });
    }

    private void checkLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );
        } else {
            loadCurrentLocation();
        }
    }

    private void loadCurrentLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(
                        this,
                        "Unable to detect current location. Please turn on GPS.",
                        Toast.LENGTH_LONG
                ).show();
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
                        Toast.makeText(
                                this,
                                "Unable to find nearest masjid: " + status,
                                Toast.LENGTH_LONG
                        ).show();
                    });
                    return;
                }

                JSONArray results = jsonObject.optJSONArray("results");

                if (results == null || results.length() == 0) {
                    runOnUiThread(() -> {
                        textDestinationName.setText("No nearby masjid found");
                        textRouteInfo.setText("Try nearby masjid list instead");
                        Toast.makeText(
                                this,
                                "No nearby masjid found within 5 km.",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                    return;
                }

                JSONObject closestPlace = getClosestPlaceFromResults(results);

                if (closestPlace == null) {
                    runOnUiThread(() -> {
                        textDestinationName.setText("No nearby masjid found");
                        textRouteInfo.setText("Unable to choose nearest masjid");
                        Toast.makeText(this, "No valid masjid location found.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String name = closestPlace.optString("name", "Nearest Masjid");
                String address = closestPlace.optString("vicinity", "Nearby Masjid");

                JSONObject geometry = closestPlace.getJSONObject("geometry");
                JSONObject locationJson = geometry.getJSONObject("location");

                double lat = locationJson.getDouble("lat");
                double lng = locationJson.getDouble("lng");

                float[] distanceResult = new float[1];

                Location.distanceBetween(
                        currentLatLng.latitude,
                        currentLatLng.longitude,
                        lat,
                        lng,
                        distanceResult
                );

                runOnUiThread(() -> {
                    destinationName = name;
                    destinationLatLng = new LatLng(lat, lng);

                    getIntent().putExtra("address", address);

                    double km = distanceResult[0] / 1000.0;

                    textDestinationName.setText(destinationName);
                    textRouteInfo.setText(String.format(Locale.getDefault(), "%.1f km direct", km));
                    textArrivalStatus.setText("Closest masjid selected");

                    resetNavigationStateForNewRoute();
                    setupMapMarkers();
                    fetchRouteFromDirectionsAPI();
                });

            } catch (Exception e) {
                android.util.Log.e("PLACES_API", "MAP GUIDE EXCEPTION: " + e.getMessage());

                runOnUiThread(() -> {
                    textDestinationName.setText("Unable to load map guide");
                    textRouteInfo.setText("Error loading nearest masjid");
                    Toast.makeText(
                            this,
                            "Map Guide failed to find nearest masjid.",
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

    private void searchMasjidOnMap(String query) {
        textDestinationName.setText("Searching masjid...");
        textRouteInfo.setText("Searching for " + query);
        textNavigationInstruction.setText("Please wait while route is loading");
        textArrivalStatus.setText("Searching Google Maps");

        resetNavigationStateForNewRoute();

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String apiKey = getString(R.string.google_maps_key);
                String encodedQuery = URLEncoder.encode(query, "UTF-8");

                String urlText = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                        + "?location=" + currentLatLng.latitude + "," + currentLatLng.longitude
                        + "&radius=5000"
                        + "&type=mosque"
                        + "&keyword=" + encodedQuery
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

                android.util.Log.e("MAP_SEARCH", "STATUS: " + status);
                android.util.Log.e("MAP_SEARCH", "ERROR MESSAGE: " + errorMessage);

                if (!status.equals("OK")) {
                    runOnUiThread(() -> {
                        textDestinationName.setText("Search failed");
                        textRouteInfo.setText("No route loaded");
                        textArrivalStatus.setText("Try another masjid name");

                        Toast.makeText(
                                this,
                                "Search failed: " + status,
                                Toast.LENGTH_LONG
                        ).show();
                    });
                    return;
                }

                JSONArray results = jsonObject.optJSONArray("results");

                if (results == null || results.length() == 0) {
                    runOnUiThread(() -> {
                        textDestinationName.setText("No masjid found");
                        textRouteInfo.setText("Try another name");
                        textArrivalStatus.setText("No search result");

                        Toast.makeText(
                                this,
                                "No masjid found for: " + query,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                    return;
                }

                JSONObject closestPlace = getClosestPlaceFromResults(results);

                if (closestPlace == null) {
                    runOnUiThread(() -> {
                        textDestinationName.setText("No valid masjid found");
                        textRouteInfo.setText("Unable to get location");
                        textArrivalStatus.setText("Search failed");
                    });
                    return;
                }

                String name = closestPlace.optString("name", "Selected Masjid");
                String address = closestPlace.optString("vicinity", "Nearby Masjid");


                JSONObject geometry = closestPlace.getJSONObject("geometry");
                JSONObject locationJson = geometry.getJSONObject("location");

                double lat = locationJson.getDouble("lat");
                double lng = locationJson.getDouble("lng");

                getIntent().putExtra("address", address);

                float[] distanceResult = new float[1];

                Location.distanceBetween(
                        currentLatLng.latitude,
                        currentLatLng.longitude,
                        lat,
                        lng,
                        distanceResult
                );

                runOnUiThread(() -> {
                    destinationName = name;
                    destinationLatLng = new LatLng(lat, lng);

                    double km = distanceResult[0] / 1000.0;

                    textDestinationName.setText(destinationName);
                    textRouteInfo.setText(String.format(Locale.getDefault(), "%.1f km direct", km));
                    textNavigationInstruction.setText("Loading driving route...");
                    textArrivalStatus.setText("Masjid selected from search");

                    resetNavigationStateForNewRoute();
                    setupMapMarkers();
                    fetchRouteFromDirectionsAPI();
                });

            } catch (Exception e) {
                android.util.Log.e("MAP_SEARCH", "EXCEPTION: " + e.getMessage());

                runOnUiThread(() -> {
                    textDestinationName.setText("Search error");
                    textRouteInfo.setText("Unable to search masjid");
                    textArrivalStatus.setText("Try again");

                    Toast.makeText(
                            this,
                            "Error searching masjid.",
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

    private JSONObject getClosestPlaceFromResults(JSONArray results) {
        try {
            JSONObject closestPlace = null;
            float closestDistance = Float.MAX_VALUE;

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);

                JSONObject geometry = place.optJSONObject("geometry");
                if (geometry == null) continue;

                JSONObject locationJson = geometry.optJSONObject("location");
                if (locationJson == null) continue;

                double lat = locationJson.getDouble("lat");
                double lng = locationJson.getDouble("lng");

                float[] distanceResult = new float[1];

                Location.distanceBetween(
                        currentLatLng.latitude,
                        currentLatLng.longitude,
                        lat,
                        lng,
                        distanceResult
                );

                if (distanceResult[0] < closestDistance) {
                    closestDistance = distanceResult[0];
                    closestPlace = place;
                }
            }

            return closestPlace;

        } catch (Exception e) {
            android.util.Log.e("PLACES_API", "CLOSEST PLACE ERROR: " + e.getMessage());
            return null;
        }
    }

    private void resetNavigationStateForNewRoute() {
        routeLoaded = false;
        navigationStarted = false;
        navigationPaused = false;
        hasArrived = false;
        historySavedForThisRoute = false;

        cameraAutoFollow = false;
        userMovedMapManually = false;
        cameraFollowHandler.removeCallbacks(resumeCameraFollowRunnable);

        currentStepIndex = 0;
        lastSpokenInstruction = "";
        navigationSteps.clear();
        currentRoutePoints.clear();

        btnStartNavigation.setText("Start Navigation");

        if (btnStopNavigation != null) {
            btnStopNavigation.setVisibility(View.GONE);
        }

        if (searchMapRow != null) {
            searchMapRow.setVisibility(View.VISIBLE);
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    private void setupMapMarkers() {
        if (googleMap == null || currentLatLng == null || destinationLatLng == null) {
            return;
        }

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
        if (currentLatLng == null || destinationLatLng == null) {
            return;
        }

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

                if (!status.equals("OK")) {
                    runOnUiThread(() -> {
                        textRouteInfo.setText("Route failed: " + status);
                        textArrivalStatus.setText("Unable to load driving route");

                        Toast.makeText(
                                this,
                                "Directions API failed: " + status,
                                Toast.LENGTH_LONG
                        ).show();
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

                navigationSteps.clear();
                currentStepIndex = 0;

                for (int i = 0; i < steps.length(); i++) {
                    JSONObject stepJson = steps.getJSONObject(i);

                    NavigationStep step = new NavigationStep();

                    JSONObject startLocationJson = stepJson.getJSONObject("start_location");
                    JSONObject endLocationJson = stepJson.getJSONObject("end_location");

                    step.startPoint = new LatLng(
                            startLocationJson.getDouble("lat"),
                            startLocationJson.getDouble("lng")
                    );

                    step.endPoint = new LatLng(
                            endLocationJson.getDouble("lat"),
                            endLocationJson.getDouble("lng")
                    );

                    step.distanceText = stepJson.getJSONObject("distance").getString("text");
                    step.distanceMeters = stepJson.getJSONObject("distance").optInt("value", 0);

                    String htmlInstruction = stepJson.optString("html_instructions", "");
                    String cleanInstruction = cleanHtml(htmlInstruction);

                    step.simpleInstruction = makeSimpleRoadInstruction(cleanInstruction);

                    navigationSteps.add(step);
                }

                if (!navigationSteps.isEmpty()) {
                    firstInstruction = buildLiveInstruction(navigationSteps.get(0));
                } else {
                    firstInstruction = "Follow the route.";
                }

                ArrayList<LatLng> routePoints = decodePolyline(encodedPolyline);

                runOnUiThread(() -> drawRouteOnMap(routePoints, distance, duration));

            } catch (Exception e) {
                android.util.Log.e("DIRECTIONS_API", "EXCEPTION: " + e.getMessage());

                runOnUiThread(() -> {
                    textRouteInfo.setText("Unable to load route");
                    textArrivalStatus.setText("Route error");

                    Toast.makeText(
                            this,
                            "Error loading route.",
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

    private void drawRouteOnMap(ArrayList<LatLng> routePoints, String distance, String duration) {
        if (googleMap == null || routePoints == null || routePoints.isEmpty()) {
            return;
        }

        currentRoutePoints = routePoints;

        googleMap.addPolyline(new PolylineOptions()
                .addAll(routePoints)
                .width(12f)
                .color(Color.rgb(14, 143, 90))
                .jointType(JointType.ROUND));

        textRouteInfo.setText(distance + " route • Estimated " + duration);
        textNavigationInstruction.setText(firstInstruction);
        textArrivalStatus.setText("Route is ready");

        routeLoaded = true;

        zoomToRoute(routePoints);

        speak("Route ready to " + getShortDestinationName() + ".");
    }

    private void zoomToRoute(ArrayList<LatLng> routePoints) {
        if (routePoints == null || routePoints.isEmpty()) {
            return;
        }

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

        cameraAutoFollow = true;
        userMovedMapManually = false;
        cameraFollowHandler.removeCallbacks(resumeCameraFollowRunnable);

        if (!historySavedForThisRoute) {
            saveToHistory();
            historySavedForThisRoute = true;
        }

        btnStartNavigation.setText("Pause Navigation");

        if (btnStopNavigation != null) {
            btnStopNavigation.setVisibility(View.VISIBLE);
        }

        if (searchMapRow != null) {
            searchMapRow.setVisibility(View.GONE);
        }

        updateNavigationStepText(true);
        textArrivalStatus.setText("Navigation started inside SolatNearby");

        moveCameraToUserNavigationView();

        speak("Navigation started.");
    }

    private void pauseNavigationMode() {
        navigationPaused = true;
        navigationStarted = false;

        cameraAutoFollow = false;
        userMovedMapManually = false;
        cameraFollowHandler.removeCallbacks(resumeCameraFollowRunnable);

        btnStartNavigation.setText("Resume Navigation");

        textArrivalStatus.setText("Navigation paused");
        speak("Navigation paused.");
    }

    private void resumeNavigationMode() {
        navigationStarted = true;
        navigationPaused = false;

        cameraAutoFollow = true;
        userMovedMapManually = false;
        cameraFollowHandler.removeCallbacks(resumeCameraFollowRunnable);

        btnStartNavigation.setText("Pause Navigation");

        updateNavigationStepText(true);
        textArrivalStatus.setText("Navigation resumed");

        moveCameraToUserNavigationView();

        speak("Navigation resumed.");
    }

    private void stopNavigationAndGoNearby() {
        navigationStarted = false;
        navigationPaused = false;
        hasArrived = false;

        cameraAutoFollow = false;
        userMovedMapManually = false;
        cameraFollowHandler.removeCallbacks(resumeCameraFollowRunnable);

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
                if (locationResult == null || destinationLatLng == null) {
                    return;
                }

                Location location = locationResult.getLastLocation();

                if (location == null) {
                    return;
                }

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

                } else if (!hasArrived && navigationStarted && !navigationPaused) {
                    updateNavigationStepText(false);

                    textArrivalStatus.setText(String.format(
                            Locale.getDefault(),
                            "%.0f meters remaining",
                            distanceMeters
                    ));

                    if (cameraAutoFollow && !userMovedMapManually) {
                        moveCameraToUserNavigationView();
                    }
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

    private void updateNavigationStepText(boolean forceSpeak) {
        if (navigationSteps == null || navigationSteps.isEmpty() || currentLatLng == null) {
            textNavigationInstruction.setText(firstInstruction);
            return;
        }

        if (currentStepIndex >= navigationSteps.size()) {
            currentStepIndex = navigationSteps.size() - 1;
        }

        NavigationStep step = navigationSteps.get(currentStepIndex);

        float[] toStepEnd = new float[1];

        Location.distanceBetween(
                currentLatLng.latitude,
                currentLatLng.longitude,
                step.endPoint.latitude,
                step.endPoint.longitude,
                toStepEnd
        );

        if (toStepEnd[0] <= 25 && currentStepIndex < navigationSteps.size() - 1) {
            currentStepIndex++;
            step = navigationSteps.get(currentStepIndex);

            Location.distanceBetween(
                    currentLatLng.latitude,
                    currentLatLng.longitude,
                    step.endPoint.latitude,
                    step.endPoint.longitude,
                    toStepEnd
            );
        }

        String liveInstruction = buildLiveInstruction(step, toStepEnd[0]);

        textNavigationInstruction.setText(liveInstruction);

        if (forceSpeak || !liveInstruction.equals(lastSpokenInstruction)) {
            lastSpokenInstruction = liveInstruction;
            speak(liveInstruction);
        }
    }

    private String buildLiveInstruction(NavigationStep step) {
        if (step == null) {
            return "Follow the route.";
        }

        return "In " + step.distanceText + ", " + step.simpleInstruction;
    }

    private String buildLiveInstruction(NavigationStep step, float meters) {
        if (step == null) {
            return "Follow the route.";
        }

        return "In " + formatMeters(meters) + ", " + step.simpleInstruction;
    }

    private String formatMeters(float meters) {
        if (meters < 1000) {
            return String.format(Locale.getDefault(), "%.0f meters", meters);
        }

        return String.format(Locale.getDefault(), "%.1f km", meters / 1000.0);
    }

    private String makeSimpleRoadInstruction(String rawInstruction) {
        if (rawInstruction == null || rawInstruction.trim().isEmpty()) {
            return "follow the route.";
        }

        String cleaned = rawInstruction
                .replace("\n", " ")
                .replace("Restricted usage road", "")
                .replace("restricted usage road", "")
                .replace("Destination will be on the left", "destination is on the left")
                .replace("Destination will be on the right", "destination is on the right")
                .trim();

        cleaned = removeCardinalDirections(cleaned);

        String lower = cleaned.toLowerCase(Locale.ROOT);
        String roadName = extractRoadName(cleaned);

        if (lower.contains("u-turn") || lower.contains("uturn")) {
            return "make a U-turn.";
        }

        if (lower.contains("slight left")) {
            return roadName.isEmpty() ? "keep left." : "keep left onto " + roadName + ".";
        }

        if (lower.contains("slight right")) {
            return roadName.isEmpty() ? "keep right." : "keep right onto " + roadName + ".";
        }

        if (lower.contains("keep left")) {
            return roadName.isEmpty() ? "keep left." : "keep left onto " + roadName + ".";
        }

        if (lower.contains("keep right")) {
            return roadName.isEmpty() ? "keep right." : "keep right onto " + roadName + ".";
        }

        if (lower.contains("turn left") || lower.contains("left")) {
            return roadName.isEmpty() ? "turn left." : "turn left onto " + roadName + ".";
        }

        if (lower.contains("turn right") || lower.contains("right")) {
            return roadName.isEmpty() ? "turn right." : "turn right onto " + roadName + ".";
        }

        if (lower.contains("roundabout")) {
            return "enter the roundabout.";
        }

        if (lower.contains("continue") || lower.contains("straight")) {
            return roadName.isEmpty() ? "go straight." : "go straight on " + roadName + ".";
        }

        if (lower.contains("head") || lower.contains("go")) {
            return roadName.isEmpty() ? "go straight." : "go straight on " + roadName + ".";
        }

        if (lower.contains("destination")) {
            return "head to the destination.";
        }

        return roadName.isEmpty() ? "follow the route." : "go straight on " + roadName + ".";
    }

    private String removeCardinalDirections(String text) {
        return text
                .replaceAll("(?i)\\bnorthwest\\b", "")
                .replaceAll("(?i)\\bnortheast\\b", "")
                .replaceAll("(?i)\\bsouthwest\\b", "")
                .replaceAll("(?i)\\bsoutheast\\b", "")
                .replaceAll("(?i)\\bnorth\\b", "")
                .replaceAll("(?i)\\bsouth\\b", "")
                .replaceAll("(?i)\\beast\\b", "")
                .replaceAll("(?i)\\bwest\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractRoadName(String instruction) {
        if (instruction == null || instruction.trim().isEmpty()) {
            return "";
        }

        String text = instruction
                .replace(".", "")
                .replace(",", " ")
                .replace("Restricted usage road", "")
                .trim();

        String lower = text.toLowerCase(Locale.ROOT);

        String[] keywords = {
                " onto ",
                " on ",
                " toward ",
                " towards ",
                " to stay on ",
                " at "
        };

        for (String keyword : keywords) {
            int index = lower.indexOf(keyword);

            if (index != -1) {
                String road = text.substring(index + keyword.length()).trim();

                road = stopAtKeyword(road, " toward ");
                road = stopAtKeyword(road, " towards ");
                road = stopAtKeyword(road, " for ");
                road = stopAtKeyword(road, " then ");
                road = stopAtKeyword(road, " destination ");
                road = stopAtKeyword(road, " restricted ");
                road = stopAtKeyword(road, " continue ");
                road = stopAtKeyword(road, " turn ");

                road = removeCardinalDirections(road);

                return limitWords(road, 6);
            }
        }

        return "";
    }

    private String stopAtKeyword(String text, String keyword) {
        String lower = text.toLowerCase(Locale.ROOT);
        int index = lower.indexOf(keyword);

        if (index != -1) {
            return text.substring(0, index).trim();
        }

        return text;
    }

    private String limitWords(String text, int maxWords) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String[] words = text.trim().split("\\s+");

        if (words.length <= maxWords) {
            return text.trim();
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < maxWords; i++) {
            builder.append(words[i]);

            if (i < maxWords - 1) {
                builder.append(" ");
            }
        }

        return builder.toString().trim();
    }

    private void moveCameraToUserNavigationView() {
        if (googleMap == null || currentLatLng == null) {
            return;
        }

        float bearing = getNavigationBearing();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(currentLatLng)
                .zoom(18f)
                .tilt(45f)
                .bearing(bearing)
                .build();

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private float getNavigationBearing() {
        if (currentLatLng == null || destinationLatLng == null) {
            return 0f;
        }

        LatLng targetPoint = destinationLatLng;

        if (navigationSteps != null && !navigationSteps.isEmpty()) {
            if (currentStepIndex >= navigationSteps.size()) {
                currentStepIndex = navigationSteps.size() - 1;
            }

            targetPoint = navigationSteps.get(currentStepIndex).endPoint;
        } else if (currentRoutePoints != null && !currentRoutePoints.isEmpty()) {
            targetPoint = findNextRoutePoint();
        }

        Location currentLocation = new Location("current");
        currentLocation.setLatitude(currentLatLng.latitude);
        currentLocation.setLongitude(currentLatLng.longitude);

        Location targetLocation = new Location("target");
        targetLocation.setLatitude(targetPoint.latitude);
        targetLocation.setLongitude(targetPoint.longitude);

        return currentLocation.bearingTo(targetLocation);
    }

    private LatLng findNextRoutePoint() {
        if (currentRoutePoints == null || currentRoutePoints.isEmpty() || currentLatLng == null) {
            return destinationLatLng;
        }

        LatLng nextPoint = destinationLatLng;
        float nearestDistance = Float.MAX_VALUE;

        for (LatLng point : currentRoutePoints) {
            float[] result = new float[1];

            Location.distanceBetween(
                    currentLatLng.latitude,
                    currentLatLng.longitude,
                    point.latitude,
                    point.longitude,
                    result
            );

            if (result[0] > 20 && result[0] < nearestDistance) {
                nearestDistance = result[0];
                nextPoint = point;
            }
        }

        return nextPoint;
    }

    private void updateUserMarker() {
        if (googleMap == null || currentLatLng == null) {
            return;
        }

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
        if (html == null) {
            return "Follow the route.";
        }

        String cleaned;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            cleaned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            cleaned = Html.fromHtml(html).toString();
        }

        cleaned = cleaned.replace("\n", " ").trim();

        if (cleaned.isEmpty()) {
            return "Follow the route.";
        }

        return cleaned;
    }

    private String getShortDestinationName() {
        if (destinationName == null || destinationName.trim().isEmpty()) {
            return "the masjid";
        }

        String name = destinationName.replace("★", "").trim();
        String[] words = name.split("\\s+");

        if (words.length <= 5) {
            return name;
        }

        StringBuilder shortName = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            shortName.append(words[i]);

            if (i < 4) {
                shortName.append(" ");
            }
        }

        return shortName.toString();
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

    private void saveToHistory() {
        if (destinationName == null || destinationLatLng == null) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) return;

        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());


        String address = getIntent().getStringExtra("address");
        if (address == null || address.isEmpty() || address.equals("Nearby Masjid")) {
            address = textDestinationName.getText().toString();
            if (address == null || address.isEmpty()) {
                address = "Nearby Masjid";
            }
        }


        History history = new History();
        history.setUserId(user.getUid());
        history.setMasjidName(destinationName);
        history.setMasjidAddress(address);
        history.setMasjidLat(destinationLatLng.latitude);
        history.setMasjidLng(destinationLatLng.longitude);
        history.setDate(date);
        history.setTime(time);
        history.setUserNote("");
        history.setFavorite(false);

        DatabaseReference databaseHistory = FirebaseDatabase.getInstance()
                .getReference("history")
                .child(user.getUid())
                .push();

        databaseHistory.setValue(history);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cameraFollowHandler.removeCallbacks(resumeCameraFollowRunnable);

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

                loadCurrentLocation();

            } else {
                Toast.makeText(
                        this,
                        "Location permission is required for navigation.",
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }
        }
    }
}