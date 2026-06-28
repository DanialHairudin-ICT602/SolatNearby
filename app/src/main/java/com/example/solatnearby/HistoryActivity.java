package com.example.solatnearby;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class HistoryActivity extends Activity implements HistoryAdapter.OnHistoryActionListener {

    private ListView listHistory;
    private HistoryAdapter adapter;

    //FIREBASE
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseHistory;

    private ArrayList<HashMap<String, String>> allHistoryList;
    private ArrayList<HashMap<String, String>> favoriteList;
    private ArrayList<HashMap<String, String>> currentDisplayList;

    private Button btnTabAll, btnTabFavorites;
    private TextView textEmptyState;
    private EditText editSearchHistory;
    private Button btnSearchHistory;

    private ArrayList<HashMap<String, String>> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Toast.makeText(this, "History", Toast.LENGTH_SHORT).show();

        Intent intent = getIntent();
        String action = intent.getStringExtra("action");

        if ("save".equals(action)) {
            String name = intent.getStringExtra("masjidName");
            String address = intent.getStringExtra("masjidAddress");
            double lat = intent.getDoubleExtra("masjidLat", 0);
            double lng = intent.getDoubleExtra("masjidLng", 0);

            if (name != null && lat != 0 && lng != 0) {
                saveToHistory(name, address, lat, lng);
            }
            finish();
            return;
        }

        listHistory = findViewById(R.id.listHistory);
        btnTabAll = findViewById(R.id.btnTabAll);
        btnTabFavorites = findViewById(R.id.btnTabFavorites);
        textEmptyState = findViewById(R.id.textEmptyState);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        editSearchHistory = findViewById(R.id.editSearchHistory);
        btnSearchHistory = findViewById(R.id.btnSearchHistory);


        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();


        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        databaseHistory = FirebaseDatabase.getInstance()
                .getReference("history")
                .child(currentUser.getUid());

        allHistoryList = new ArrayList<>();
        favoriteList = new ArrayList<>();
        currentDisplayList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new HistoryAdapter(this, currentDisplayList, this);
        listHistory.setAdapter(adapter);

        btnTabAll.setOnClickListener(v -> showAllHistory());
        btnTabFavorites.setOnClickListener(v -> showFavorites());

        btnTabAll.setBackgroundResource(R.drawable.bg_green_button);
        btnTabAll.setTextColor(getColor(android.R.color.white));
        btnTabFavorites.setBackgroundResource(R.drawable.bg_outline_green_button);
        btnTabFavorites.setTextColor(getColor(R.color.primary_green));

        btnSearchHistory.setOnClickListener(v -> {
            String query = editSearchHistory.getText().toString().trim();
            if (query.isEmpty()) {

                if (btnTabFavorites.isPressed()) {
                    showFavorites();
                } else {
                    showAllHistory();
                }
                return;
            }
            searchHistory(query);
        });

        loadHistoryFromFirebase();
    }

    //SEARCH
    private void searchHistory(String query) {
        String lowerQuery = query.toLowerCase(Locale.getDefault());
        filteredList.clear();

        ArrayList<HashMap<String, String>> sourceList;

        if (btnTabFavorites.isPressed()) {
            sourceList = favoriteList;
        } else {
            sourceList = allHistoryList;
        }

        for (HashMap<String, String> item : sourceList) {
            String name = item.get("masjidName");
            String address = item.get("masjidAddress");
            String date = item.get("date");
            String note = item.get("userNote");

            if (name != null && name.toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                    address != null && address.toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                    date != null && date.toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                    note != null && note.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {

                filteredList.add(item);
            }
        }

        currentDisplayList.clear();
        currentDisplayList.addAll(filteredList);
        adapter.notifyDataSetChanged();

        if (currentDisplayList.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            textEmptyState.setText(" No results found for: " + query);
        } else {
            textEmptyState.setVisibility(View.GONE);
            Toast.makeText(this, " Found " + currentDisplayList.size() + " results for: " + query, Toast.LENGTH_SHORT).show();
        }
    }

    //READ: LOAD FROM FIREBASE
    private void loadHistoryFromFirebase() {
        databaseHistory.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allHistoryList.clear();
                favoriteList.clear();

                if (!dataSnapshot.exists()) {
                    showAllHistory();
                    textEmptyState.setVisibility(View.VISIBLE);
                    textEmptyState.setText(" No history yet.\nNavigate to a masjid to save.");
                    return;
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Get History object from Firebase
                    History history = snapshot.getValue(History.class);

                    if (history != null) {
                        HashMap<String, String> item = new HashMap<>();
                        item.put("id", snapshot.getKey());
                        item.put("masjidName", history.getMasjidName());
                        item.put("masjidAddress", history.getMasjidAddress());
                        item.put("masjidLat", String.valueOf(history.getMasjidLat()));
                        item.put("masjidLng", String.valueOf(history.getMasjidLng()));
                        item.put("date", history.getDate());
                        item.put("time", history.getTime());
                        item.put("userNote", history.getUserNote());
                        item.put("favorite", String.valueOf(history.getFavorite()));

                        allHistoryList.add(0, item);

                        if (history.getFavorite()) {
                            favoriteList.add(item);
                        }
                    }
                }

                showAllHistory();

                if (allHistoryList.isEmpty()) {
                    textEmptyState.setVisibility(View.VISIBLE);
                    textEmptyState.setText(" No history yet.\nNavigate to a masjid to save.");
                } else {
                    textEmptyState.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HistoryActivity.this, "Error loading: " + error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    //SHOW ALL HISTORY
    private void showAllHistory() {
        btnTabAll.setBackgroundResource(R.drawable.bg_green_button);
        btnTabAll.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        btnTabFavorites.setBackgroundResource(R.drawable.bg_outline_green_button);
        btnTabFavorites.setTextColor(ContextCompat.getColor(this, R.color.primary_green));

        currentDisplayList.clear();
        currentDisplayList.addAll(allHistoryList);
        adapter.notifyDataSetChanged();

        if (currentDisplayList.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            textEmptyState.setText(" No history yet.\nNavigate to a masjid to save.");
        } else {
            textEmptyState.setVisibility(View.GONE);
        }
    }

    //SHOW FAVORITES
    private void showFavorites() {
        btnTabFavorites.setBackgroundResource(R.drawable.bg_green_button);
        btnTabFavorites.setTextColor(getResources().getColor(android.R.color.white));
        btnTabAll.setBackgroundResource(R.drawable.bg_outline_green_button);
        btnTabAll.setTextColor(getResources().getColor(R.color.primary_green));

        currentDisplayList.clear();
        currentDisplayList.addAll(favoriteList);
        adapter.notifyDataSetChanged();

        if (currentDisplayList.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            textEmptyState.setText(" No favorites yet.\nClick star on any masjid to add.");
        } else {
            textEmptyState.setVisibility(View.GONE);
        }
    }

    //CREATE SAVE TO FIREBASE
    public void saveToHistory(String masjidName, String masjidAddress,
                              double masjidLat, double masjidLng) {
        if (currentUser == null) {
            Toast.makeText(this, "Please login to save history", Toast.LENGTH_SHORT).show();
            return;
        }
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        String id = databaseHistory.push().getKey();

        History history = new History();
        history.setId(id);
        history.setUserId(currentUser.getUid());
        history.setMasjidName(masjidName);
        history.setMasjidAddress(masjidAddress);
        history.setMasjidLat(masjidLat);
        history.setMasjidLng(masjidLng);
        history.setDate(date);
        history.setTime(time);
        history.setUserNote("");
        history.setFavorite(false);

        if (id != null) {
            databaseHistory.child(id).setValue(history)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, " History saved: " + masjidName, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, " Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    //UPDATE FAVOURITE
    @Override

    public void onFavoriteClick(int position) {
        HashMap<String, String> item = currentDisplayList.get(position);
        String id = item.get("id");
        String currentFavorite = item.get("favorite");
        boolean newFavorite = !"true".equals(currentFavorite);

        databaseHistory.child(id).child("favorite").setValue(newFavorite)
                .addOnSuccessListener(aVoid -> {

                    for (HashMap<String, String> historyItem : allHistoryList) {
                        if (historyItem.get("id").equals(id)) {
                            historyItem.put("favorite", String.valueOf(newFavorite));
                            break;
                        }
                    }

                    favoriteList.clear();
                    for (HashMap<String, String> historyItem : allHistoryList) {
                        if ("true".equals(historyItem.get("favorite"))) {
                            favoriteList.add(historyItem);
                        }
                    }

                    if (btnTabFavorites.getBackground().getConstantState() ==
                            getDrawable(R.drawable.bg_green_button).getConstantState()) {
                        showFavorites();
                    } else {
                        showAllHistory();
                    }

                    Toast.makeText(this, newFavorite ? " Added to favorites" : " Removed from favorites", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, " Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    //UPDATE COMMENT
    @Override
    public void onCommentClick(int position) {
        HashMap<String, String> item = currentDisplayList.get(position);
        String id = item.get("id");

        if (id == null || id.isEmpty()) {
            Toast.makeText(this, "Error: History ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String masjidName = item.get("masjidName");
        String currentComment = item.get("userNote");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("💬 Personal Note for " + masjidName);

        final EditText input = new EditText(this);
        if (currentComment != null && !currentComment.isEmpty()) {
            input.setText(currentComment);
        }
        input.setHint("Write your personal note here.");
        input.setMinLines(3);
        builder.setView(input);

        builder.setPositiveButton("Save Note", (dialog, which) -> {
            String newComment = input.getText().toString().trim();

            // Save to Firebase
            databaseHistory.child(id).child("userNote").setValue(newComment)
                    .addOnSuccessListener(aVoid -> {

                        for (HashMap<String, String> historyItem : allHistoryList) {
                            if (historyItem.get("id").equals(id)) {
                                historyItem.put("userNote", newComment);
                                break;
                            }
                        }

                        for (HashMap<String, String> displayItem : currentDisplayList) {
                            if (displayItem.get("id").equals(id)) {
                                displayItem.put("userNote", newComment);
                                break;
                            }
                        }

                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, " Comment saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, " Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    //NAVIGATE
    @Override
    public void onNavigateClick(int position) {
        HashMap<String, String> item = currentDisplayList.get(position);
        String masjidName = item.get("masjidName");
        String address = item.get("masjidAddress");
        double lat = Double.parseDouble(item.get("masjidLat"));
        double lng = Double.parseDouble(item.get("masjidLng"));

        Intent intent = new Intent(HistoryActivity.this, MapNavigationActivity.class);
        intent.putExtra("name", masjidName);
        intent.putExtra("address", address);
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);
        startActivity(intent);
    }

    //DELETE
    @Override
    public void onDeleteClick(int position) {
        HashMap<String, String> item = currentDisplayList.get(position);
        String id = item.get("id");
        String masjidName = item.get("masjidName");

        new AlertDialog.Builder(this)
                .setTitle(" Delete History")
                .setMessage("Delete history for " + masjidName + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseHistory.child(id).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "History deleted", Toast.LENGTH_SHORT).show();
                                loadHistoryFromFirebase();  // Refresh the list
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }
}

