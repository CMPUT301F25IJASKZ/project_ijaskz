package com.ijaskz.lotteryeventapp;

public class User {
    private String user_id;
    private String user_name;
    private String email;
    private String user_type;

    // Required empty constructor for Firestore
    public User() {}

    public User(String userId, String name, String email, String userType) {
        this.user_id = userId;
        this.user_name = name;
        this.email = email;
        this.user_type = userType;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public String getEmail() {
        return email;
    }

    public String getUser_type() {
        return user_type;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}
