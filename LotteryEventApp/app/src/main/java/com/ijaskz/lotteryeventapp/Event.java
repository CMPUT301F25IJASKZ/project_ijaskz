package com.ijaskz.lotteryeventapp;

public class Event {
    private String event_description;
    private String event_id;
    private String location;
    private String event_name;
    private int max;
    private String event_time;
    public Event(){}

    public Event(String event_description, String location,  String event_name, int max, String event_time){
        this.event_description = event_description;
        //this.event_id = event_id;
        this.location = location;
        this.event_name = event_name;
        this.max = max;
        this.event_time = event_time;
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

    public int getMax() {
        return max;
    }

    public String getEvent_time() {
        return event_time;
    }
}
