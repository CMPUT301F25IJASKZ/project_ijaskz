package com.ijaskz.lotteryeventapp;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Firestore operations for in-app notifications.
 *
 * Collection: "notifications"
 *
 * Common fields:
 *   userId        : String
 *   eventId       : String (optional)
 *   title         : String
 *   message       : String
 *   type          : String ("selection", "not_selected", "organizer_message", ...)
 *   createdAt     : long (ms since epoch)
 *   read          : boolean
 *   eventName     : String (optional)
 *   eventLocation : String (optional)
 *   eventTime     : String (optional)
 *   imageUrl      : String (optional)
 *   deeplink      : String (optional)
 */
public class NotificationManager {

    private static final String TAG = "AppNotificationMgr";

    private FirebaseFirestore db;

    public NotificationManager() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Testing-only constructor to avoid touching Firebase in unit tests.
     */
    protected NotificationManager(boolean skipInit) {
        if (!skipInit) {
            db = FirebaseFirestore.getInstance();
        }
    }

    // ------------------------------------------------------------------------
    // Creation helpers (used by WaitingListManager / organizers)
    // ------------------------------------------------------------------------

    /**
     * Creates a "you were selected" notification for a waiting list entry.
     * Uses entrant_id and event_id from the entry.
     *
     * Implements US 02.05.01 – includes event details and deeplink.
     */
    public void createSelectionNotification(WaitingListEntry entry) {
        if (entry == null) return;

        final String userId = entry.getEntrant_id();
        final String eventId = entry.getEvent_id();

        if (userId == null || userId.isEmpty() ||
                eventId == null || eventId.isEmpty()) {
            Log.w(TAG, "createSelectionNotification: missing userId or eventId");
            return;
        }

        // Fetch event to include name, location, time, image, deeplink
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    String eventName = null;
                    String location = null;
                    String time = null;
                    String imageUrl = null;
                    String deeplink = "lotteryevent://event/" + eventId;

                    if (eventDoc.exists()) {
                        eventName = firstNonNull(
                                eventDoc.getString("event_name"),
                                eventDoc.getString("name"));
                        location = eventDoc.getString("location");
                        time = firstNonNull(
                                eventDoc.getString("event_time"),
                                eventDoc.getString("time"));
                        imageUrl = firstNonNull(
                                eventDoc.getString("image"),
                                eventDoc.getString("imageUrl"));
                        String storedDeeplink = eventDoc.getString("deeplink");
                        if (storedDeeplink != null && !storedDeeplink.isEmpty()) {
                            deeplink = storedDeeplink;
                        }
                    }

                    String title = "You’ve been selected!";
                    if (eventName != null && !eventName.isEmpty()) {
                        title = "Selected for " + eventName;
                    }

                    StringBuilder msg = new StringBuilder();
                    msg.append("You have been selected to sign up for this event.");
                    if (eventName != null && !eventName.isEmpty()) {
                        msg.append(" Event: ").append(eventName).append(".");
                    }
                    if (time != null && !time.isEmpty()) {
                        msg.append(" Time: ").append(time).append(".");
                    }
                    if (location != null && !location.isEmpty()) {
                        msg.append(" Location: ").append(location).append(".");
                    }
                    msg.append(" Tap to view details and complete your registration.");

                    Map<String, Object> data = baseNotificationMap(
                            userId,
                            eventId,
                            title,
                            msg.toString(),
                            "selection"
                    );
                    putIfNotNull(data, "eventName", eventName);
                    putIfNotNull(data, "eventLocation", location);
                    putIfNotNull(data, "eventTime", time);
                    putIfNotNull(data, "imageUrl", imageUrl);
                    putIfNotNull(data, "deeplink", deeplink);

                    writeNotificationDocument(data);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to fetch event for selection notification", e));
    }

    /**
     * Creates a "you were not selected" notification for a waiting list entry.
     */
    public void createNotSelectedNotification(WaitingListEntry entry) {
        if (entry == null) return;

        final String userId = entry.getEntrant_id();
        final String eventId = entry.getEvent_id();

        if (userId == null || userId.isEmpty() ||
                eventId == null || eventId.isEmpty()) {
            Log.w(TAG, "createNotSelectedNotification: missing userId or eventId");
            return;
        }

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    String eventName = null;
                    if (eventDoc.exists()) {
                        eventName = firstNonNull(
                                eventDoc.getString("event_name"),
                                eventDoc.getString("name"));
                    }

                    String title = "Lottery result";
                    String message;
                    if (eventName != null && !eventName.isEmpty()) {
                        message = "Unfortunately, you were not selected for \"" +
                                eventName + "\" this time.";
                    } else {
                        message = "Unfortunately, you were not selected in the lottery.";
                    }

                    Map<String, Object> data = baseNotificationMap(
                            userId,
                            eventId,
                            title,
                            message,
                            "not_selected"
                    );
                    putIfNotNull(data, "eventName", eventName);

                    writeNotificationDocument(data);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to fetch event for not-selected notification", e));
    }

    /**
     * Organizer/admin sends a custom message
     * to a specific user, optionally tied to an event.
     *
     * These notifications use type "organizer_message".
     */
    public void createOrganizerNotificationForUser(String userId,
                                                   @Nullable String eventId,
                                                   String title,
                                                   String message) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "createOrganizerNotificationForUser: userId is null");
            return;
        }

        Map<String, Object> data = baseNotificationMap(
                userId,
                eventId,
                title,
                message,
                "organizer_message"
        );

        // If eventId is present, enrich with event name/image/deeplink
        if (eventId != null && !eventId.isEmpty()) {
            db.collection("events")
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(eventDoc -> {
                        String eventName = null;
                        String imageUrl = null;
                        String deeplink = "lotteryevent://event/" + eventId;

                        if (eventDoc.exists()) {
                            eventName = firstNonNull(
                                    eventDoc.getString("event_name"),
                                    eventDoc.getString("name"));
                            imageUrl = firstNonNull(
                                    eventDoc.getString("image"),
                                    eventDoc.getString("imageUrl"));
                            String storedDeeplink = eventDoc.getString("deeplink");
                            if (storedDeeplink != null && !storedDeeplink.isEmpty()) {
                                deeplink = storedDeeplink;
                            }
                        }

                        putIfNotNull(data, "eventName", eventName);
                        putIfNotNull(data, "imageUrl", imageUrl);
                        putIfNotNull(data, "deeplink", deeplink);

                        writeNotificationDocument(data);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch event for organizer notification", e);
                        writeNotificationDocument(data);
                    });
        } else {
            writeNotificationDocument(data);
        }
    }

    // ------------------------------------------------------------------------
    // Read helpers used by NotificationsFragment
    // ------------------------------------------------------------------------

    /**
     * Fetches all notifications for a given user, newest first.
     * Sorting is done in memory to avoid needing a composite index.
     */
    public void getNotificationsForUser(
            String userId,
            OnNotificationsLoadedListener listener
    ) {
        if (userId == null || userId.isEmpty()) {
            listener.onLoaded(new ArrayList<>());
            return;
        }

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AppNotification> notifications = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        AppNotification n = doc.toObject(AppNotification.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            notifications.add(n);
                        }
                    }

                    // Sort newest first by createdAt
                    notifications.sort(
                            (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt())
                    );

                    listener.onLoaded(notifications);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Marks a notification document as read.
     */
    public void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) return;

        db.collection("notifications")
                .document(notificationId)
                .update("read", true);
    }

    /**
     * Callback interface used when loading notifications for a user.
     */
    public interface OnNotificationsLoadedListener {
        void onLoaded(List<AppNotification> notifications);
        void onError(Exception e);
    }

    // ------------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------------

    /** Builds the minimal notification map shared by all types. */
    private Map<String, Object> baseNotificationMap(String userId,
                                                    @Nullable String eventId,
                                                    String title,
                                                    String message,
                                                    String type) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        if (eventId != null) {
            map.put("eventId", eventId);
        }
        map.put("title", title);
        map.put("message", message);
        map.put("type", type);
        map.put("createdAt", System.currentTimeMillis());
        map.put("read", false);
        return map;
    }

    /** Writes a notification document into the "notifications" collection. */
    private void writeNotificationDocument(Map<String, Object> data) {
        db.collection("notifications")
                .add(data)
                .addOnSuccessListener(
                        (OnSuccessListener<? super com.google.firebase.firestore.DocumentReference>) documentReference ->
                                Log.d(TAG, "Notification created: " + documentReference.getId()))
                .addOnFailureListener(
                        (OnFailureListener) e ->
                                Log.e(TAG, "Failed to create notification", e));
    }

    /**
     * Puts a key-value pair into the map only if the value is not null or empty.
     */
    private void putIfNotNull(Map<String, Object> map, String key, @Nullable String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    /**
     * Returns the first non-null and non-empty string among the two.
     * Can return null if both are empty.
     */
    private String firstNonNull(String a, String b) {
        return (a != null && !a.isEmpty()) ? a : (b != null ? b : null);
    }
}