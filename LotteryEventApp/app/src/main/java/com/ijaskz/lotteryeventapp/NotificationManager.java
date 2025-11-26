package com.ijaskz.lotteryeventapp;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Firestore operations for in-app notifications.
 */
public class NotificationManager {

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

    /**
     * Creates a "you were selected" notification for a waiting list entry.
     * Uses entrant_id and event_id from the entry.
     */
    public void createSelectionNotification(WaitingListEntry entry) {
        if (entry == null) return;

        String userId = entry.getEntrant_id();
        if (userId == null || userId.isEmpty()) return;

        String eventId = entry.getEvent_id();
        String title = "You were selected!";
        String message;

        if (entry.getEntrant_name() != null && !entry.getEntrant_name().isEmpty()) {
            message = "You have been selected from the waiting list for event "
                    + eventId + ".";
        } else {
            message = "You have been selected from the waiting list for an event.";
        }

        AppNotification notification =
                new AppNotification(userId, title, message, eventId);

        // Fire-and-forget; we don't need to handle success/failure here.
        db.collection("notifications").add(notification);
    }

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
                // No orderBy here; we'll sort in memory
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
     * Marks a notification as read.
     */
    public void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) return;

        db.collection("notifications")
                .document(notificationId)
                .update("read", true);
    }

    public interface OnNotificationsLoadedListener {
        void onLoaded(List<AppNotification> notifications);
        void onError(Exception e);
    }
}
