package com.ijaskz.lotteryeventapp;

import java.io.Serializable;

/**
 * This class defines a User
 */
public class User implements Serializable {
    private String user_id;
    private String user_name;
    private String user_email;
    private String user_type;
    private String user_phone;
    // Required empty constructor for Firestore
    public User() {}

    public User(String userId, String name, String email, String userType, String userPhone) {
        this.user_id = userId;
        this.user_name = name;
        this.user_email = email;
        this.user_type = userType;
        this.user_phone = userPhone;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public String getUser_email() {
        return user_email;
    }

    public String getUser_type() {
        return user_type;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUser_phone() {
        return user_phone;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public void setUser_type(String user_type) {
        this.user_type = user_type;
    }
}
