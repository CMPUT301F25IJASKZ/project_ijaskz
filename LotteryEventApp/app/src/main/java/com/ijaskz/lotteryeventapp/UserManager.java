package com.ijaskz.lotteryeventapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.firebase.ui.auth.AuthUI;

/**
 * defines a userManager to access the current logged in users information
 */
public class UserManager {

    private static final String PREFS_NAME = "user_prefs";
    private SharedPreferences prefs;
    private Context context; // store context for Firebase sign out

    public UserManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * saves the current logged in users information
     * @param userId The current users Id
     * @param userType The current users type
     * @param email The current users email
     */
    public void saveUser(String userId, String userType, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userId", userId);
        editor.putString("userType", userType);
        editor.putString("email", email);
        editor.apply();
    }

    /**
     * Saves the current logged in users information (with name)
     * @param userId The current users Id
     * @param userType The current users type
     * @param email The current users email
     * @param name The current users name
     */
    public void saveUser(String userId, String userType, String email, String name) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userId", userId);
        editor.putString("userType", userType);
        editor.putString("email", email);
        editor.putString("userName", name);
        editor.apply();
    }

    /**
     * Save the current logged in users information (with name and phone #)
     * @param userId The current users Id
     * @param userType The current users type
     * @param email The current users email
     * @param name The current users name
     * @param phone The current users #
     */
    public void saveUser(String userId, String userType, String email, String name, String phone) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userId", userId);
        editor.putString("userType", userType);
        editor.putString("email", email);
        editor.putString("userName", name);
        editor.putString("userPhone", phone);
        editor.apply();
    }

    /**
     * Turns the Current user into a user object
     * @return current user The current user information in an object
     */
    public User createUserClass(){
        return new User(getUserId(),getUserName() ,getUserEmail(), getUserType(), getUserPhone());
    }

    public String getUserType() {
        return prefs.getString("userType", "");
    }

    public String getUserId() {
        return prefs.getString("userId", null);
    }

    public String getUserEmail() {
        return prefs.getString("email", null);
    }
    
    public String getUserName() {
        return prefs.getString("userName", null);
    }
    public String getUserPhone() {
        return prefs.getString("userPhone", null);
    }
    public boolean isLoggedIn() {
        return prefs.getString("userId", null) != null;
    }

    /**
     * Logging out user
     */
    public void logout() {
        // Clear local session
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Sign out from Firebase
        AuthUI.getInstance().signOut(context);
    }
}
