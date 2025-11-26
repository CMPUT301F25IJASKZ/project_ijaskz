package com.ijaskz.lotteryeventapp;

public class AppNotification {
    private String id;          // Firestore document ID (set when reading)
    private String userId;      // Who this notification is for
    private String title;       // Short title
    private String message;     // Body text
    private String eventId;     // Optional related event
    private long createdAt;     // When it was created (ms since epoch)
    private boolean read;       // Has the user seen it?

    public AppNotification() {
        // Needed for Firestore
    }

    public AppNotification(String userId, String title, String message, String eventId) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.eventId = eventId;
        this.createdAt = System.currentTimeMillis();
        this.read = false;
    }

    public String getId() {return id;}

    public void setId(String id) {this.id = id;}

    public String getUserId() {return userId;}

    public void setUserId(String userId) {this.userId = userId;}

    public String getTitle() {return title;}

    public void setTitle(String title) {this.title = title;}

    public String getMessage() {return message;}

    public void setMessage(String message) {this.message = message;}

    public String getEventId() {return eventId;}

    public void setEventId(String eventId) {this.eventId = eventId;}

    public long getCreatedAt() {return createdAt;}

    public void setCreatedAt(long createdAt) {this.createdAt = createdAt;}

    public boolean isRead() {return read;}

    public void setRead(boolean read) {this.read = read;}
}
