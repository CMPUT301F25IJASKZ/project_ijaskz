package com.ijaskz.lotteryeventapp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * WaitingListManager
 * Handles all Firestore operations for waiting lists
 */
public class WaitingListManager {

    private FirebaseFirestore db;
    private NotificationManager notificationManager;

    public WaitingListManager() {
        db = FirebaseFirestore.getInstance();
        notificationManager = new NotificationManager();
    }

    /**
     * Testing-only constructor that skips Firebase initialization.
     * Subclasses in JVM unit tests can call this to avoid touching Android/Firebase.
     * When using this constructor, overridden methods must avoid accessing {@code db}.
     * @param skipInit when true, do not initialize the Firestore instance
     */
    protected WaitingListManager(boolean skipInit) {
        if (!skipInit) {
            db = FirebaseFirestore.getInstance();
            notificationManager = new NotificationManager();
        } else {
            notificationManager= null;
        }
    }

    /**
     * Updates the status for multiple waiting list entries with an optional
     * response window (hours). If newStatus is "selected", selected_at is set.
     * When hours is not null, response_window_hours is written on each entry.
     * @param entryIds List of entry IDs to update
     * @param newStatus Status value to apply
     * @param hours Optional response window hours to set (nullable)
     * @param listener Callback for completion or error
     */
    public void updateEntriesStatus(List<String> entryIds, String newStatus, Integer hours, OnCompleteListener listener) {
        if (entryIds == null || entryIds.isEmpty()) {
            listener.onSuccess();
            return;
        }

        long now = System.currentTimeMillis();
        WriteBatch batch = db.batch();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updated_at", now);
        if ("selected".equals(newStatus)) {
            updates.put("selected_at", now);
        }
        if (hours != null) {
            updates.put("response_window_hours", hours);
        }

        for (String id : entryIds) {
            DocumentReference ref = db.collection("waiting_list").document(id);
            batch.update(ref, updates);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // AFTER updating status, send notifications to selected entrants
                    if ("selected".equals(newStatus) && notificationManager != null) {
                        // Notifications for selected entrants
                        for (String id : entryIds) {
                            db.collection("waiting_list")
                                    .document(id)
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                                        if (entry != null) {
                                            entry.setId(doc.getId());
                                            notificationManager.createSelectionNotification(entry);
                                        }
                                    });
                        }
                        // Determine event id and notify non-selected entrants
                        String firstId = entryIds.get(0);
                        db.collection("waiting_list")
                                .document(firstId)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                                    if (entry != null) {
                                        String eventId = entry.getEvent_id();
                                        notifyNotSelectedEntrants(eventId, entryIds);
                                    }
                                });
                    }
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Join a waiting list for an event
     */
    public void joinWaitingList(String eventId, String userId, String userName,
                                String userEmail,Double lat, Double lon, OnCompleteListener listener) {

        // Check if already joined
        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("entrant_id", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        listener.onFailure(new Exception("Already on waiting list"));
                        return;
                    }

                    // Create new entry
                    WaitingListEntry entry = new WaitingListEntry(
                            eventId, userId, userName, userEmail
                    );

                    // Add location if provided
                    if (lat != null && lon != null) {
                        entry.setLatitude(lat);
                        entry.setLongitude(lon);
                    }

                    // Add to Firestore
                    db.collection("waiting_list")
                            .add(entry)
                            .addOnSuccessListener(docRef -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onFailure(e));
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    /**
     * Leave a waiting list
     */
    public void leaveWaitingList(String eventId, String userId, OnCompleteListener listener) {

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("entrant_id", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onFailure(new Exception("Not on waiting list"));
                        return;
                    }

                    // Delete the entry
                    querySnapshot.getDocuments().get(0).getReference().delete()
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onFailure(e));
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    /**
     * Get all waiting lists for a user
     */
    public void getMyWaitingLists(String userId, OnWaitingListLoadedListener listener) {

        db.collection("waiting_list")
                .whereEqualTo("entrant_id", userId)
                // Removed .orderBy() to avoid needing a composite index
                // Sorting is done in memory instead
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<WaitingListEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    
                    // Sort by joined_at in descending order (newest first)
                    entries.sort((a, b) -> Long.compare(b.getJoined_at(), a.getJoined_at()));
                    
                    listener.onLoaded(entries);
                })
                .addOnFailureListener(e -> listener.onError(e));
    }

    /**
     * Check if user is on waiting list for an event
     */
    public void isOnWaitingList(String eventId, String userId, OnCheckListener listener) {

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("entrant_id", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listener.onResult(!querySnapshot.isEmpty());
                })
                .addOnFailureListener(e -> listener.onResult(false));
    }

    /**
     * Get status of user's waiting list entry
     */
    public void getWaitingListStatus(String eventId, String userId, OnStatusListener listener) {

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("entrant_id", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onStatus(null);
                    } else {
                        WaitingListEntry entry = querySnapshot.getDocuments()
                                .get(0).toObject(WaitingListEntry.class);
                        listener.onStatus(entry != null ? entry.getStatus() : null);
                    }
                })
                .addOnFailureListener(e -> listener.onStatus(null));
    }

    /**
     * Accept invitation when selected from lottery
     * Changes status from "selected" to "accepted"
     */
    public void acceptInvitation(String eventId, String userId, OnCompleteListener listener) {
        
        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("entrant_id", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onFailure(new Exception("Not on waiting list"));
                        return;
                    }

                    // Update status to "accepted"
                    querySnapshot.getDocuments().get(0).getReference()
                            .update("status", "accepted", 
                                   "updated_at", System.currentTimeMillis())
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onFailure(e));
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    /**
     * Decline invitation when selected from lottery
     * Changes status from "selected" to "declined"
     */
    public void declineInvitation(String eventId, String userId, OnCompleteListener listener) {
        
        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("entrant_id", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onFailure(new Exception("Not on waiting list"));
                        return;
                    }

                    // Update status to "declined"
                    querySnapshot.getDocuments().get(0).getReference()
                            .update("status", "declined", 
                                   "updated_at", System.currentTimeMillis())
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onFailure(e));
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    /**
     * Gets all entries for an event with a specific status.
     * @param eventId The event ID to query entries for
     * @param status The status value to filter by (e.g., "waiting", "selected")
     * @param listener Callback receiving the loaded entries or an error
     */
    public void getEntriesByStatus(String eventId, String status, OnEntriesLoadedListener listener) {
        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<WaitingListEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    listener.onEntriesLoaded(entries);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Updates the status for multiple waiting list entries in a single batch.
     * If newStatus is "selected", this will also set the selected_at timestamp
     * and create notifications for both selected and non-selected entrants.
     * @param entryIds List of entry document IDs to update
     * @param newStatus The status to write to each entry
     * @param listener Callback for completion or error
     */
    public void updateEntriesStatus(List<String> entryIds, String newStatus, OnCompleteListener listener) {
        if (entryIds == null || entryIds.isEmpty()) {
            listener.onSuccess();
            return;
        }

        long now = System.currentTimeMillis();
        WriteBatch batch = db.batch();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updated_at", now);
        if ("selected".equals(newStatus)) {
            updates.put("selected_at", now);
        }

        for (String id : entryIds) {
            DocumentReference ref = db.collection("waiting_list").document(id);
            batch.update(ref, updates);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if ("selected".equals(newStatus) && notificationManager != null) {
                        // Notifications for selected entrants
                        for (String id : entryIds) {
                            db.collection("waiting_list")
                                    .document(id)
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                                        if (entry != null) {
                                            entry.setId(doc.getId());
                                            notificationManager.createSelectionNotification(entry);
                                        }
                                    });
                        }
                        // Determine event id and notify non-selected entrants
                        String firstId = entryIds.get(0);
                        db.collection("waiting_list")
                                .document(firstId)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                                    if (entry != null) {
                                        String eventId = entry.getEvent_id();
                                        notifyNotSelectedEntrants(eventId, entryIds);
                                    }
                                });
                    }
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Notify all selected entrants for an event
     * Sends a custom organizer/admin message to every waiting-list entry
     * with status "selected" for the given event.
     *
     * @param eventId  id of the event
     * @param title    notification title (e.g., "Event Update")
     * @param message  body text
     * @param listener callback for completion / error
     */
    public void notifySelectedEntrants(String eventId,
                                       String title,
                                       String message,
                                       OnCompleteListener listener) {
        if (eventId == null || eventId.isEmpty()) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("eventId is required"));
            }
            return;
        }

        if (notificationManager == null) {
            if (listener != null) {
                listener.onFailure(new IllegalStateException("NotificationManager not initialized"));
            }
            return;
        }

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", "selected")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                        if (entry != null) {
                            String userId = entry.getEntrant_id();
                            notificationManager.createOrganizerNotificationForUser(
                                    userId,
                                    eventId,
                                    title,
                                    message
                            );
                        }
                    }
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Notify all NOT SELECTED entrants for an event
     * Sends a custom organizer/admin message to every waiting-list entry
     * with status "not_selected" for the given event.
     *
     * Uses NotificationManager.createOrganizerNotificationForUser(...) with type "organizer_message".
     *
     * @param eventId  id of the event
     * @param title    notification title (e.g., "Lottery Result")
     * @param message  body text
     * @param listener callback for completion / error
     */
    public void notifyNotSelectedEntrants(String eventId,
                                          String title,
                                          String message,
                                          OnCompleteListener listener) {
        if (eventId == null || eventId.isEmpty()) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("eventId is required"));
            }
            return;
        }

        if (notificationManager == null) {
            if (listener != null) {
                listener.onFailure(new IllegalStateException("NotificationManager not initialized"));
            }
            return;
        }

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", "not_selected")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                        if (entry != null) {
                            String userId = entry.getEntrant_id();
                            notificationManager.createOrganizerNotificationForUser(
                                    userId,
                                    eventId,
                                    title,
                                    message
                            );
                        }
                    }
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Notify ALL entrants on the waiting list for an event.
     * Sends a custom organizer/admin message to every waiting-list entry
     * with the given eventId (regardless of status).
     *
     * Uses NotificationManager.createOrganizerNotificationForUser(...) with type "organizer_message".
     *
     * @param eventId  id of the event
     * @param title    notification title (e.g., "Event Update")
     * @param message  body text
     * @param listener callback for completion / error
     */
    public void notifyAllWaitingListEntrants(String eventId,
                                             String title,
                                             String message,
                                             OnCompleteListener listener) {
        if (eventId == null || eventId.isEmpty()) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("eventId is required"));
            }
            return;
        }

        if (notificationManager == null) {
            if (listener != null) {
                listener.onFailure(new IllegalStateException("NotificationManager not initialized"));
            }
            return;
        }

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                        if (entry != null) {
                            String userId = entry.getEntrant_id();
                            notificationManager.createOrganizerNotificationForUser(
                                    userId,
                                    eventId,
                                    title,
                                    message
                            );
                        }
                    }
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Handles a user's response to a selection by updating status, responded_at,
     * and decline_reason if applicable.
     * @param entryId The waiting list entry document ID
     * @param accepted True if the user accepts the invitation; false if declined
     * @param declineReason Optional reason for declining (used when accepted is false)
     * @param listener Callback for completion or error
     */
    public void handleLotteryResponse(String entryId, boolean accepted, String declineReason, OnCompleteListener listener) {
        String newStatus = accepted ? "accepted" : "declined";
        long now = System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("responded_at", now);
        updates.put("updated_at", now);
        if (!accepted && declineReason != null && !declineReason.isEmpty()) {
            updates.put("decline_reason", declineReason);
        }

        db.collection("waiting_list").document(entryId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Sends "not selected" notifications to all entrants on the waiting list
     * for a given event, excluding the entries whose ids are in selectedEntryIds.
     * This is used automatically when the lottery is run and winners are chosen.
     */
    private void notifyNotSelectedEntrants(String eventId, List<String> selectedEntryIds) {
        if (notificationManager == null || eventId == null || eventId.isEmpty()) {
            return;
        }

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    long now = System.currentTimeMillis();
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : querySnapshot) {
                        String id = doc.getId();

                        // Winners are already "selected" – skip them here
                        if (selectedEntryIds.contains(id)) {
                            continue;
                        }

                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                        if (entry == null) continue;

                        entry.setId(id);

                        // 1) Update status to "not_selected"
                        batch.update(doc.getReference(),
                                "status", "not_selected",
                                "updated_at", now);

                        // 2) Send the automatic "you were not selected" notification
                        notificationManager.createNotSelectedNotification(entry);
                    }

                    // Commit status updates (fire-and-forget)
                    batch.commit();
                });
    }

    /**Callback used when counting entrants for an event. */
    public interface OnCountListener {
        /**
         * Called when the count query completes successfully.
         * @param count number of documents in <code>waiting_list</code>
         *              for the given event id; guaranteed non-negative
         */
        void onCount(int count);

        /**
         * Called when the count query fails (e.g., network error,
         * permission denied, or unexpected server issue).
         * @param e the originating exception with diagnostic details
         */
        void onError(Exception e);
    }

    /**
     * Asynchronously counts how many entrants are on the waiting list
     * for a given event.
     * <p>
     * Implementation detail: this executes a Firestore aggregation
     * (count) on the <code>waiting_list</code> collection filtered by
     * <code>event_id</code>. The result is delivered on the supplied
     * {@link OnCountListener} callback.
     * </p>
     *
     * <h4>Contract</h4>
     * <ul>
     *   <li>If <code>eventId</code> is <code>null</code> or blank, the
     *       listener receives <code>onCount(0)</code>.</li>
     *   <li>On success, exactly one of <code>onCount(int)</code> is called.</li>
     *   <li>On failure (e.g., networking / permission), exactly one of
     *       <code>onError(Exception)</code> is called.</li>
     * </ul>
     *
     * @param eventId Firestore event document id to count entrants for
     * @param listener callback invoked with either the count or an error
     */

    public void getWaitingListCount(String eventId, OnCountListener listener) {
        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(snap -> listener.onCount(snap.size()))
                .addOnFailureListener(listener::onError);
    }

    // Callback Interfaces
    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnWaitingListLoadedListener {
        void onLoaded(List<WaitingListEntry> entries);
        void onError(Exception e);
    }

    public interface OnCheckListener {
        void onResult(boolean isOnList);
    }

    public interface OnStatusListener {
        void onStatus(String status);
    }

    /**
     * Callback invoked when a list of entries is loaded or an error occurs.
     */
    public interface OnEntriesLoadedListener {
        void onEntriesLoaded(List<WaitingListEntry> entries);
        void onError(Exception e);
    }
}