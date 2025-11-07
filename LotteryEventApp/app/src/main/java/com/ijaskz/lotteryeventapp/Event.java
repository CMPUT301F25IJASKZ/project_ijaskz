package com.ijaskz.lotteryeventapp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import com.google.firebase.Timestamp;

/**
 * Class that defines an Event 
 */
public class Event implements Serializable {
    private String event_description;
    private String event_id;
    private String location;
    private String event_name;

    private String image;
    private int max;
    private String event_time;
    private List<String> applied;
    private List<String> picked;
    private List<String> notPicked;

    private Timestamp registrationStart;
    private Timestamp registrationEnd;

    private String qrUrl;
    private String deeplink;

    public Event(){}


    public Event(String event_description, String location,  String event_name, int max, String event_time, String image){
        this.event_description = event_description;
        this.event_name = event_name;
        this.location = location;
        this.max = max;
        this.event_time = event_time;
        this.image = image;
    }

    public String getEvent_description() {
        return event_description;
    }

    public String getEvent_id() {
        return event_id;
    }

    public String getLocation() {
        return location;
    }

    public String getEvent_name() {
        return event_name;
    }

    public void setEvent_id(String arg) {
        this.event_id = arg;
    }

    public int getMax() {
        return max;
    }

    public String getImage() {
        return image;
    }

    public String getEvent_time() {
        return event_time;
    }

    public Timestamp getRegistrationStart() {
        return registrationStart;
    }

    public void setRegistrationStart(Timestamp registrationStart) {
        this.registrationStart = registrationStart;
    }

    public Timestamp getRegistrationEnd() {
        return registrationEnd;
    }

    public void setRegistrationEnd(Timestamp registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    public String getQrUrl() {
        return qrUrl;
    }

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    public String getDeeplink() {
        return deeplink;
    }

    public void setDeeplink(String deeplink) {
        this.deeplink = deeplink;
    }
}
