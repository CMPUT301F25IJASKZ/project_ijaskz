package com.ijaskz.lotteryeventapp;

/**
 * Defines class to enter a user into waiting list
 */
public class WaitingListEntry {
    private String id;              // Firestore document ID
    private String event_id;        // Which event
    private String entrant_id;      // Which user
    private String entrant_name;    // User's name (for display)
    private String entrant_email;   // User's email
    private String status;          // "waiting", "selected", "accepted", "declined", "cancelled", "enrolled"
    private long joined_at;         // When they joined
    private long updated_at;        // Last update time
    private Long selected_at;       // When selected in lottery (nullable)
    private Long responded_at;      // When responded to invite (nullable)
    private String decline_reason;  // Reason for declining (nullable)
    private Integer response_window_hours; // Per-entry response window override (nullable)

    private Double latitude;
    private Double longitude;

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
        this.selected_at = null;
        this.responded_at = null;
        this.decline_reason = null;
        this.response_window_hours = null;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
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

    /**
     * Gets the timestamp when this entry was selected in the lottery.
     * @return milliseconds since epoch, or null if not selected yet
     */
    public Long getSelected_at() { return selected_at; }

    /**
     * Sets the selection timestamp for this entry.
     * @param selected_at milliseconds since epoch
     */
    public void setSelected_at(Long selected_at) {
        this.selected_at = selected_at;
        this.updated_at = System.currentTimeMillis();
    }

    /**
     * Gets the timestamp when the entrant responded to the invitation.
     * @return milliseconds since epoch, or null if no response yet
     */
    public Long getResponded_at() { return responded_at; }

    /**
     * Sets the response timestamp for this entry.
     * @param responded_at milliseconds since epoch
     */
    public void setResponded_at(Long responded_at) {
        this.responded_at = responded_at;
        this.updated_at = System.currentTimeMillis();
    }

    /**
     * Gets the optional reason provided when declining.
     * @return reason string, or null
     */
    public String getDecline_reason() { return decline_reason; }

    /**
     * Sets the decline reason.
     * @param decline_reason reason string
     */
    public void setDecline_reason(String decline_reason) {
        this.decline_reason = decline_reason;
        this.updated_at = System.currentTimeMillis();
    }

    /**
     * Gets the per-entry response window (hours).
     * @return hours override, or null to use event default
     */
    public Integer getResponse_window_hours() { return response_window_hours; }

    /**
     * Sets the per-entry response window (hours) override.
     * @param response_window_hours hours to respond
     */
    public void setResponse_window_hours(Integer response_window_hours) {
        this.response_window_hours = response_window_hours;
        this.updated_at = System.currentTimeMillis();
    }
}
