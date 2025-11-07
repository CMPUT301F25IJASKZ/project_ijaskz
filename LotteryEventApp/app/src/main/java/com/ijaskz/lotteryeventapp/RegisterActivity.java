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

/**
 * Defines the RegisterActivity where users can register a new account
 */
public class RegisterActivity extends AppCompatActivity {
    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;
    private TextView errorText;
    private TextView loginLink;
    private FirebaseFirestore db;

    /**
     * Creates Activity for Registation
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        phoneInput = findViewById(R.id.phone_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        registerButton = findViewById(R.id.register_button);
        errorText = findViewById(R.id.error_text);
        loginLink = findViewById(R.id.login_link);

        registerButton.setOnClickListener(v -> attemptRegistration());
        loginLink.setOnClickListener(v -> navigateToLogin());
    }

    /**
     * When user submits information, makes sure required fields are filled in
     */
    private void attemptRegistration() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validation (phone is optional)
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all required fields");
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
                            createUser(name, email, phone, password);
                        }
                    } else {
                        registerButton.setEnabled(true);
                        showError("Registration failed: " + task.getException().getMessage());
                    }
                });
    }

    /**
     * Creates the user and puts it in database
     * @param name The new users name
     * @param email The new users email
     * @param phone The new users phone #
     * @param password The new users password
     */
    private void createUser(String name, String email, String phone, String password) {
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
        
        // Add phone number if provided (optional)
        if (phone != null && !phone.isEmpty()) {
            user.put("user_phone", phone);
        }else{
            user.put("user_phone", null );
        }

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

    /**
     * Displays Error message if user makes mistake
     * @param message The error message to be displayed
     */
    private void showError(String message) {
        errorText.setText(message);
        errorText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        errorText.setVisibility(View.VISIBLE);
    }

    /**
     * Show user that registration was successful on a display
     * @param message The success message to be displayed
     */
    private void showSuccess(String message) {
        errorText.setText(message);
        errorText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        errorText.setVisibility(View.VISIBLE);
    }

    /**
     * Takes user to login fragment after registering
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}