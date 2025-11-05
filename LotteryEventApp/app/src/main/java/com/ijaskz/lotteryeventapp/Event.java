package com.ijaskz.lotteryeventapp;

import java.io.Serializable;
import java.util.List;

public class Event implements Serializable {
    private String event_description;
    private String event_id;
    private String location;
    private String event_name;
    private int max;
    private String event_time;

    // from main
    private String image;
    private List<String> applied;
    private List<String> picked;
    private List<String> notPicked;

    // required by Firestore
    public Event() {}

    // your constructor (no image)
    public Event(String event_description, String location, String event_name, int max, String event_time) {
        this.event_description = event_description;
        this.location = location;
        this.event_name = event_name;
        this.max = max;
        this.event_time = event_time;
    }

    // main's constructor (with image)
    public Event(String event_description, String location, String event_name, int max, String event_time, String image) {
        this.event_description = event_description;
        this.location = location;
        this.event_name = event_name;
        this.max = max;
        this.event_time = event_time;
        this.image = image;
    }

    // getters
    public String getEvent_description() { return event_description; }
    public String getEvent_id() { return event_id; }
    public String getLocation() { return location; }
    public String getEvent_name() { return event_name; }
    public int getMax() { return max; }
    public String getEvent_time() { return event_time; }
    public String getImage() { return image; }
    public List<String> getApplied() { return applied; }
    public List<String> getPicked() { return picked; }
    public List<String> getNotPicked() { return notPicked; }

    // minimal setters (handy for Firestore writes)
    public void setEvent_id(String event_id) { this.event_id = event_id; }
    public void setImage(String image) { this.image = image; }
    public void setApplied(List<String> applied) { this.applied = applied; }
    public void setPicked(List<String> picked) { this.picked = picked; }
    public void setNotPicked(List<String> notPicked) { this.notPicked = notPicked; }
}
