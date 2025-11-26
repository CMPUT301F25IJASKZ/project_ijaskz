package com.ijaskz.lotteryeventapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.firebase.ui.auth.AuthUI;

/**
 * defines a userManager to access the current logged in users information
 */
public class UserManager {

    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notificationsEnabled";
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

    /**
     * Enables or disables in-app notifications from organizers/admins
     * for this device/user session.
     *
     * @param enabled true to receive notifications, false to opt out
     */
    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
                .apply();
    }

    /**
     * Returns whether notifications are enabled for this user on this device.
     * Defaults to true if the preference has never been set.
     *
     * @return true if notifications are enabled, false if opted out
     */
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }
}
