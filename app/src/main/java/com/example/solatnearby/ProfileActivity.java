package com.example.solatnearby;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileActivity extends Activity {

    TextView textProfileName, textProfileEmail;
    EditText editProfileName, editProfileEmail, editProfilePhone;
    Button btnSaveProfile, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        textProfileName = findViewById(R.id.textProfileName);
        textProfileEmail = findViewById(R.id.textProfileEmail);

        editProfileName = findViewById(R.id.editProfileName);
        editProfileEmail = findViewById(R.id.editProfileEmail);
        editProfilePhone = findViewById(R.id.editProfilePhone);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSaveProfile.setOnClickListener(v -> {
            String name = editProfileName.getText().toString().trim();
            String email = editProfileEmail.getText().toString().trim();

            if (name.isEmpty()) {
                editProfileName.setError("Please enter name");
                return;
            }

            if (email.isEmpty()) {
                editProfileEmail.setError("Please enter email");
                return;
            }

            textProfileName.setText(name);
            textProfileEmail.setText(email);

            Toast.makeText(this, "Profile updated for demo", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}