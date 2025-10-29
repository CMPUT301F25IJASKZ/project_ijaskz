package com.ijaskz.lotteryeventapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private Button loginButton;
    private UserManager userManager;

    private ActivityResultLauncher<Intent> firebaseAuthLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userManager = new UserManager(this);
        loginButton = findViewById(R.id.login_button); // your button id

        // Modern ActivityResultLauncher
        firebaseAuthLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();
                    IdpResponse response = IdpResponse.fromResultIntent(data);

                    if (resultCode == RESULT_OK) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            userManager.saveUser(user.getUid(), "entrant", user.getEmail());
                            navigateToMain();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Sign-in failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // If user already signed in, go to main
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
        } else {
            Toast.makeText(this, "Firebase Auth connected successfully!", Toast.LENGTH_SHORT).show();
        }

        // Launch FirebaseUI login on button click
        loginButton.setOnClickListener(v -> startFirebaseLogin());
    }

    private void startFirebaseLogin() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build()
        );

        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)

                .build();

        firebaseAuthLauncher.launch(signInIntent);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
