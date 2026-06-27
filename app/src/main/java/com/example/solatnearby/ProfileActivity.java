package com.example.solatnearby;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends Activity {

    TextView textProfileName;
    EditText editProfileName, editProfileEmail, editProfilePhone;
    EditText editOldPassword, editNewPassword;
    Button btnSaveProfile, btnLogout, btnChangePassword;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        databaseUsers = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getUid());

        textProfileName = findViewById(R.id.textProfileName);


        editProfileName = findViewById(R.id.editProfileName);
        editProfileEmail = findViewById(R.id.editProfileEmail);
        editProfilePhone = findViewById(R.id.editProfilePhone);

        editOldPassword = findViewById(R.id.editOldPassword);
        editNewPassword = findViewById(R.id.editNewPassword);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        //EMAIL CAN'T EDIT
        editProfileEmail.setEnabled(false);
        editProfileEmail.setFocusable(false);

        loadProfileData();

        //SAVE PROFILE
        btnSaveProfile.setOnClickListener(v -> {
            String name = editProfileName.getText().toString().trim();
            String phone = editProfilePhone.getText().toString().trim();

            if (name.isEmpty()) {
                editProfileName.setError("Please enter name");
                editProfileName.requestFocus();
                return;
            }

            saveProfileData(name, phone);
        });

        //CHANGE PASSWORD
        btnChangePassword.setOnClickListener(v -> {
            String oldPassword = editOldPassword.getText().toString().trim();
            String newPassword = editNewPassword.getText().toString().trim();

            if (oldPassword.isEmpty()) {
                editOldPassword.setError("Enter current password");
                editOldPassword.requestFocus();
                return;
            }

            if (newPassword.isEmpty()) {
                editNewPassword.setError("Enter new password");
                editNewPassword.requestFocus();
                return;
            }

            if (newPassword.equals(oldPassword)) {
                editNewPassword.setError("New password cannot be the same as current password");
                editNewPassword.requestFocus();
                return;
            }

            if (newPassword.length() < 6) {
                editNewPassword.setError("Minimum 6 characters");
                editNewPassword.requestFocus();
                return;
            }

            if (!newPassword.matches(".*\\d.*")) {
                editNewPassword.setError("Password must contain at least one number");
                editNewPassword.requestFocus();
                return;
            }

            if (!newPassword.matches(".*[A-Z].*")) {
                editNewPassword.setError("Password must contain at least one uppercase letter");
                editNewPassword.requestFocus();
                return;
            }
            editNewPassword.setError(null);



            changePassword(oldPassword, newPassword);
        });

        //LOGOUT
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfileData() {
        String email = currentUser.getEmail();
        if (email != null) {

            editProfileEmail.setText(email);
        }

        databaseUsers.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String name = snapshot.child("fullName").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);

                if (name != null) {
                    textProfileName.setText(name);
                    editProfileName.setText(name);
                }

                if (phone != null) {
                    editProfilePhone.setText(phone);
                }
            } else {
                String displayName = currentUser.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    textProfileName.setText(displayName);
                    editProfileName.setText(displayName);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProfileData(String name, String phone) {
        // Update Display Name
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    textProfileName.setText(name);
                    Toast.makeText(this, " Name updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, " Name update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Save to Realtime Database
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", name);
        userData.put("phone", phone);

        databaseUsers.setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, " Profile saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "  save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void changePassword(String oldPassword, String newPassword) {
        String email = currentUser.getEmail();

        AuthCredential credential = EmailAuthProvider.getCredential(email, oldPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {

                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, " Password changed successfully!", Toast.LENGTH_SHORT).show();
                                editOldPassword.setText("");
                                editNewPassword.setText("");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, " Password update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, " Incorrect current password", Toast.LENGTH_SHORT).show();
                });
    }
}