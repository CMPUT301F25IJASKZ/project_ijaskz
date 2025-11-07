package com.ijaskz.lotteryeventapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Defines Activity for logging in
 */
public class LoginActivity extends AppCompatActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView registerButton;
    private FirebaseFirestore db;
    private UserManager userManager;

    /**
     * Creates Activity to be displayed when new user opens app
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userManager = new UserManager(this);

        // Check if user is already logged in
        if (userManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();
        emailInput = findViewById(R.id.emailEditText);
        passwordInput = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(v -> attemptLogin());
        registerButton.setOnClickListener(v -> navigateToRegister());
    }

    /**
     * make sure user enters both email and password and that they match in database
     */
    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password");
            return;
        }

        loginButton.setEnabled(false);

        db.collection("users")
                .whereEqualTo("user_email", email)
                .get()
                .addOnCompleteListener(task -> {
                    loginButton.setEnabled(true);

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String dbPassword = document.getString("user_password");

                            if (password.equals(dbPassword)) {
                                String userId = document.getString("user_id");
                                String userType = document.getString("user_type");
                                String userName = document.getString("user_name");
                                String userPhone = document.getString("user_phone");
                                if(userPhone != null){
                                    userManager.saveUser(userId, userType, email, userName, userPhone );
                                }else {
                                    userManager.saveUser(userId, userType, email, userName);
                                }
                                navigateToMain();
                                return;
                            }
                        }
                        showError("Invalid password");
                    } else {
                        showError("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    loginButton.setEnabled(true);
                    showError("Login failed: " + e.getMessage());
                });
    }

    /**
     * Displays error to user
     * @param message The error to be displayed
     */
    private void showError(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * Sends user to main activity if login is successful
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Sends new user to register activity if new to app
     */
    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}