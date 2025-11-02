package com.ijaskz.lotteryeventapp;

public class WaitingListEntry {
    private String id;              // Firestore document ID
    private String event_id;        // Which event
    private String entrant_id;      // Which user
    private String entrant_name;    // User's name (for display)
    private String entrant_email;   // User's email
    private String status;          // "waiting", "selected", "accepted", "declined", "cancelled", "enrolled"
    private long joined_at;         // When they joined
    private long updated_at;        // Last update time

    public WaitingListEntry(){}

    public WaitingListEntry(String event_id, String entrant_id,
                            String entrant_name, String entrant_email){
        this.event_id = event_id;
        this.entrant_id = entrant_id;
        this.entrant_name = entrant_name;
        this.entrant_email = entrant_email;
        this.status = "waiting";
        this.joined_at = System.currentTimeMillis();
        this.updated_at = System.currentTimeMillis();
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEvent_id() { return event_id; }
    public void setEvent_id(String event_id) { this.event_id = event_id; }

    public String getEntrant_id() { return entrant_id; }
    public void setEntrant_id(String entrant_id) { this.entrant_id = entrant_id; }

    public String getEntrant_name() { return entrant_name; }
    public void setEntrant_name(String entrant_name) { this.entrant_name = entrant_name; }

    public String getEntrant_email() { return entrant_email; }
    public void setEntrant_email(String entrant_email) { this.entrant_email = entrant_email; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updated_at = System.currentTimeMillis();
    }

    public long getJoined_at() { return joined_at; }
    public void setJoined_at(long joined_at) { this.joined_at = joined_at; }

    public long getUpdated_at() { return updated_at; }
    public void setUpdated_at(long updated_at) { this.updated_at = updated_at; }
}
