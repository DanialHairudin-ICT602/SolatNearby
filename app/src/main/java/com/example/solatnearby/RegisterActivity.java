package com.example.solatnearby;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    EditText e1, e2;
    FirebaseAuth mAuth;

    private EditText editFullName, editConfirmPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);



        editFullName = findViewById(R.id.editFullName);
        e1 = findViewById(R.id.editEmailRegister);   // Email
        e2 = findViewById(R.id.editPasswordRegister);  // Password
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        mAuth = FirebaseAuth.getInstance();

        Button btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(v -> createUser(v));

        // -------------------------------------
        // SIMPLE EMAIL VALIDATION (live check)
        // -------------------------------------
        e1.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String email = s.toString();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    e1.setError("Invalid email address");
                } else {
                    e1.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}


        });

        // -------------------------------------
        // SIMPLE PASSWORD VALIDATION (live check)
        // -------------------------------------
        e2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                if (s.length() < 6) {
                    e2.setError("Minimum 6 characters");
                } else {
                    e2.setError(null);
                }


                if (!password.matches(".*\\d.*")) {
                    e2.setError("Password must contain at least one number");
                    e2.requestFocus();
                    return;
                }

                if (!password.matches(".*[A-Z].*")) {
                    e2.setError("Password must contain at least one uppercase letter");
                    e2.requestFocus();
                    return;
                }
                e2.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });


        // =============================================
        // CONFIRM PASSWORD VALIDATION (live check)
        // =============================================
        editConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = e2.getText().toString();
                String confirm = s.toString();

                if (!confirm.equals(password)) {
                    editConfirmPassword.setError("Passwords do not match");
                } else {
                    editConfirmPassword.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }


    public void createUser(View v){

        String email = e1.getText().toString().trim();
        String password = e2.getText().toString().trim();

        // Final validation before register
        if(email.isEmpty()){
            e1.setError("Email cannot be empty");
            e1.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            e1.setError("Invalid email address");
            e1.requestFocus();
            return;
        }

        if(password.isEmpty()){
            e2.setError("Password cannot be empty");
            e2.requestFocus();
            return;
        }

        if(password.length() < 6){
            e2.setError("Minimum 6 characters");
            e2.requestFocus();
            return;
        }

        if (!password.matches(".*\\d.*")) {
            e2.setError("Password must contain at least one number");
            e2.requestFocus();
            return;
        }

        if (!password.matches(".*[A-Z].*")) {
            e2.setError("Password must contain at least one uppercase letter");
            e2.requestFocus();
            return;
        }

        String fullName = editFullName.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty()) {
            editFullName.setError("Full name is required");
            editFullName.requestFocus();
            return;
        }
        if (confirmPassword.isEmpty()) {
            editConfirmPassword.setError("Please confirm your password");
            editConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editConfirmPassword.setError("Passwords do not match");
            editConfirmPassword.requestFocus();
            return;
        }

        // Firebase create user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(getApplicationContext(),"User created successfully",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(),"Registration failed",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}