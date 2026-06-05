package com.example.solatnearby;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity {

    Button btnRegister;
    TextView textGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnRegister = findViewById(R.id.btnRegister);
        textGoLogin = findViewById(R.id.textGoLogin);

        btnRegister.setOnClickListener(v -> {
            Toast.makeText(this, "Account registered for demo", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
            finish();
        });

        textGoLogin.setOnClickListener(v -> {
            finish();
        });
    }
}