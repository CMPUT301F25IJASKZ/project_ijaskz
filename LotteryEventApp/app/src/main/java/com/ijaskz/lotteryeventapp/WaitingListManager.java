package com.ijaskz.lotteryeventapp;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * WaitingListManager
 * Handles all Firestore operations for waiting lists
 */
public class WaitingListManager {

    private FirebaseFirestore db;

    public WaitingListManager() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Join a waiting list for an event
     */
    public void joinWaitingList(String eventId, String userId, String userName,
                                String userEmail, OnCompleteListener listener) {

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
}