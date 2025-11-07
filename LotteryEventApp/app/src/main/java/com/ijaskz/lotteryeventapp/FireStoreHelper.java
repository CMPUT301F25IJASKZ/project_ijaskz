package com.ijaskz.lotteryeventapp;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireStoreHelper {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FireStoreHelper() {}

    // 🔹 Add event with createdAt timestamp
    public void addEvent(Event event) {
        Map<String, Object> newEvent = new HashMap<>();
        newEvent.put("event_description", event.getEvent_description());
        newEvent.put("event_location", event.getLocation());
        newEvent.put("event_name", event.getEvent_name());
        newEvent.put("event_time", event.getEvent_time());
        newEvent.put("image", "/collection/document");
        newEvent.put("max", event.getMax());

        // 🔹 Add a timestamp for sorting
        newEvent.put("createdAt", FieldValue.serverTimestamp());

        db.collection("events").add(newEvent)
                .addOnSuccessListener(docRef -> {
                    String eventId = docRef.getId();
                    Log.d("Events", "Event created with ID: " + eventId);
                })
                .addOnFailureListener(e -> {
                    Log.e("Events", "Failed to create event", e);
                });
    }

    // 🔹 Optional direct fetch (not live)
    public List<Event> getEventList() {
        List<Event> list = new ArrayList<>();
        db.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setEvent_id(doc.getId());
                            list.add(e);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Events", "Failed to load events", e));
        return list;
    }

    public void deleteEvent(Event event) {
        db.collection("events").document(event.getEvent_id()).delete();
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) users.add(user);
                    }
                })
                .addOnFailureListener(e -> Log.e("Users", "Failed to load users", e));
        return users;
    }

    // 🔹 Live listener for organizers + entrants
    public ListenerRegistration listenToManageableUsers(ManageUsersCallback callback) {
        return db.collection("users")
                .whereIn("user_type", Arrays.asList("organizer", "entrant"))
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        callback.onError(e);
                        return;
                    }
                    List<User> users = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap) {
                            User u = doc.toObject(User.class);
                            if (u != null) {
                                u.setUser_id(doc.getId());
                                users.add(u);
                            }
                        }
                    }
                    callback.onUsersLoaded(users);
                });
    }

    public void updateUserType(String userId, String newType) {
        db.collection("users")
                .document(userId)
                .update("user_type", newType);
    }

    public void deleteUser(String userId) {
        db.collection("users")
                .document(userId)
                .delete();
    }

    public interface ManageUsersCallback {
        void onUsersLoaded(List<User> users);
        void onError(Exception e);
    }

    // 🔹 Realtime listener with events ordered by newest first
    public ListenerRegistration listenToEvents(EventsAdapter adapter) {
        return db.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING) // ✅ Sort by newest
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("EventsHome", "Failed to listen for events", e);
                        return;
                    }

                    List<Event> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap) {
                            Event ev = doc.toObject(Event.class);
                            if (ev != null) {
                                ev.setEvent_id(doc.getId());
                                list.add(ev);
                            }
                        }
                    }

                    adapter.setEvents(list);
                });
    }
}
