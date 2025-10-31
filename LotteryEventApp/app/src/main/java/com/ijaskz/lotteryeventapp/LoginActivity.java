package com.ijaskz.lotteryeventapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView errorText;
    private TextView signUpText;
    private FirebaseFirestore db;
    private UserManager userManager;

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
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        errorText = findViewById(R.id.error_text);
        signUpText = findViewById(R.id.sign_up_text);

        loginButton.setOnClickListener(v -> attemptLogin());
        signUpText.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password");
            return;
        }

        loginButton.setEnabled(false);
        errorText.setVisibility(View.GONE);

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

                                userManager.saveUser(userId, userType, email);
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

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}