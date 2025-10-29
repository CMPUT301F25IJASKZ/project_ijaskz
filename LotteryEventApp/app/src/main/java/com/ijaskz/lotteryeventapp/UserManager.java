package com.ijaskz.lotteryeventapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.firebase.ui.auth.AuthUI;

public class UserManager {

    private static final String PREFS_NAME = "user_prefs";
    private SharedPreferences prefs;
    private Context context; // store context for Firebase sign out

    public UserManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Save logged-in user info
    public void saveUser(String userId, String userType, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userId", userId);
        editor.putString("userType", userType);
        editor.putString("email", email);
        editor.apply();
    }

    public String getUserType() {
        return prefs.getString("userType", "");
    }

    // Logout user
    public void logout() {
        // Clear local session
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Sign out from Firebase
        AuthUI.getInstance().signOut(context);
    }
}
