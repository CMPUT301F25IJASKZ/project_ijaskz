package com.ijaskz.lotteryeventapp;

public class Event {
    // Existing fields
    private String event_description;
    private String event_id;
    private String location;
    private String event_name;
    private int max;
    private String event_time;

    // NEW fields (as requested)
    private String poster_url;          // Firebase Storage download URL
    private String status;              // "DRAFT" | "OPEN" | "CLOSED"
    private String organizer_id;        // uid of the creator
    private String registration_start;  // keep as String for now
    private String registration_end;    // keep as String for now
    private String created_at;          // optional
    private String updated_at;          // optional

    public Event() {}

    public Event(String event_description, String location, String event_name, int max, String event_time) {
        this.event_description = event_description;
        this.location = location;
        this.event_name = event_name;
        this.max = max;
        this.event_time = event_time;
    }

    // Getters (existing)
    public String getEvent_description() { return event_description; }
    public String getEvent_id() { return event_id; }
    public String getLocation() { return location; }
    public String getEvent_name() { return event_name; }
    public int getMax() { return max; }
    public String getEvent_time() { return event_time; }

    // Getters (new)
    public String getPoster_url() { return poster_url; }
    public String getStatus() { return status; }
    public String getOrganizer_id() { return organizer_id; }
    public String getRegistration_start() { return registration_start; }
    public String getRegistration_end() { return registration_end; }
    public String getCreated_at() { return created_at; }
    public String getUpdated_at() { return updated_at; }

    // Setters (existing)
    public void setEvent_description(String event_description) { this.event_description = event_description; }
    public void setEvent_id(String event_id) { this.event_id = event_id; }
    public void setLocation(String location) { this.location = location; }
    public void setEvent_name(String event_name) { this.event_name = event_name; }
    public void setMax(int max) { this.max = max; }
    public void setEvent_time(String event_time) { this.event_time = event_time; }

    // Setters (new)
    public void setPoster_url(String poster_url) { this.poster_url = poster_url; }
    public void setStatus(String status) { this.status = status; }
    public void setOrganizer_id(String organizer_id) { this.organizer_id = organizer_id; }
    public void setRegistration_start(String registration_start) { this.registration_start = registration_start; }
    public void setRegistration_end(String registration_end) { this.registration_end = registration_end; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}
