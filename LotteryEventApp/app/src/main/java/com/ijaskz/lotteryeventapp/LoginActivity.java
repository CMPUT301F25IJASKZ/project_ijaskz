package com.ijaskz.lotteryeventapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        userManager = new UserManager(this);

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> attemptLogin());
        registerButton.setOnClickListener(v -> navigateToRegister());

    }
    private void attemptLogin(){
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if(TextUtils.isEmpty(email)){
            emailEditText.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)){
            passwordEditText.setError("Email is required");
            return;
        }
        if (password.length()<6){
            passwordEditText.setError("Password must be 6 characters long");
            return;
        }
        showProgress(true);

        //Sign in with email and password
        mAuth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, task -> {
                    if(task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                userManager.saveUser(user.getUid(), "entrant", user.getEmail());
                                updateUI(user);
                            } else {
                                Toast.makeText(LoginActivity.this, "Please verify email address",
                                        Toast.LENGTH_SHORT
                                ).show();
                                mAuth.signOut();
                            }
                        }
                    } else {

                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage():  "Authentication failed";
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed: "+ errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }
    private void navigateToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }


    // can be refactored if progressBar is passed in
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
        registerButton.setEnabled(!show);
    }
}
