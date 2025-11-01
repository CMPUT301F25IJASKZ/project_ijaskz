package com.ijaskz.lotteryeventapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {
    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;
    private TextView errorText;
    private TextView loginLink;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        registerButton = findViewById(R.id.register_button);
        errorText = findViewById(R.id.error_text);
        loginLink = findViewById(R.id.login_link);

        registerButton.setOnClickListener(v -> attemptRegistration());
        loginLink.setOnClickListener(v -> navigateToLogin());
    }

    private void attemptRegistration() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (!email.contains("@")) {
            showError("Please enter a valid email");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        registerButton.setEnabled(false);
        errorText.setVisibility(View.GONE);

        // Check if email already exists
        db.collection("users")
                .whereEqualTo("user_email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            registerButton.setEnabled(true);
                            showError("Email already registered");
                        } else {
                            // Email doesn't exist, create new user
                            createUser(name, email, password);
                        }
                    } else {
                        registerButton.setEnabled(true);
                        showError("Registration failed: " + task.getException().getMessage());
                    }
                });
    }

    private void createUser(String name, String email, String password) {
        // Generate unique user ID
        String userId = UUID.randomUUID().toString();

        // Create user data map
        Map<String, Object> user = new HashMap<>();
        user.put("user_id", userId);
        user.put("user_name", name);
        user.put("user_email", email);
        user.put("user_password", password);
        user.put("user_type", "entrant"); // Default user type
        user.put("created_at", System.currentTimeMillis());

        // Add user to Firestore
        db.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    registerButton.setEnabled(true);
                    showSuccess("Registration successful! Please login.");

                    // Navigate to login after a short delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> navigateToLogin(), 1500);
                })
                .addOnFailureListener(e -> {
                    registerButton.setEnabled(true);
                    showError("Registration failed: " + e.getMessage());
                });
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        errorText.setVisibility(View.VISIBLE);
    }

    private void showSuccess(String message) {
        errorText.setText(message);
        errorText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        errorText.setVisibility(View.VISIBLE);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}