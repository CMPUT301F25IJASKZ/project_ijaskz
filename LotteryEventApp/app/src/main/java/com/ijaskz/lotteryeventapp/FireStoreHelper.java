package com.ijaskz.lotteryeventapp;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireStoreHelper {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    public FireStoreHelper(){};
    public void addEvent(Event event){
        Map<String,Object> newEvent = new HashMap<>();
        newEvent.put("event_description", event.getEvent_description());
        newEvent.put("event_location", event.getLocation());
        newEvent.put("event_name", event.getEvent_name());
        newEvent.put("event_time", event.getEvent_time());
        newEvent.put("image", "/collection/document");
        newEvent.put("max", event.getMax());
        db.collection("events").add(newEvent).addOnSuccessListener(docRef->{
            String eventId = docRef.getId();
            Log.d("Events", "Event created with ID: " + eventId);
        }).addOnFailureListener(e -> {
            Log.e("Events", "Failed to create event", e);
        });
    }

    // EVERYONE USE THIS!!!
    public List<Event> getEventList() {
        List<Event> list = new ArrayList<>();
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            list.add(e);
                        }
                    }
                });
        return list;
    }
// THIS IS USEFUL!

    public void displayEvents(EventsAdapter adapter){
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setEvent_id(doc.getId());
                            list.add(e);
                        }
                    }
                    adapter.setEvents(list);
                })
                .addOnFailureListener(e -> {
                    Log.e("EventsHome", "Failed to load events", e);
                });
    }

    public void deleteEvent(Event event){
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

                });
        return users;
    }

    // Get all organizers + entrants
    public ListenerRegistration listenToManageableUsers(ManageUsersCallback callback) {
        // whereIn supports up to 10 values, so 2 is fine
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
                                // ensure we also keep the doc id as userId
                                u.setUser_id(doc.getId());
                                users.add(u);
                            }
                        }
                    }
                    callback.onUsersLoaded(users);
                });
    }

    //  Promote / demote
    public void updateUserType(String userId, String newType) {
        db.collection("users")
                .document(userId)
                .update("user_type", newType);
    }

    // Delete profile
    public void deleteUser(String userId) {
        db.collection("users")
                .document(userId)
                .delete();
    }

    public interface ManageUsersCallback {
        void onUsersLoaded(List<User> users);
        void onError(Exception e);
    }
}


