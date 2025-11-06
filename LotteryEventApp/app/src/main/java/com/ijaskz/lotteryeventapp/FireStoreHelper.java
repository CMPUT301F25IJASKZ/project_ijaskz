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

/**
 * Defines the FireStoreHelper to be used for interactions with database
 */
public class FireStoreHelper {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    public FireStoreHelper(){};

    /**
     * Takes in Event object then adds it to the database
     * @param event
     */
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

    /**
     * Grabs the list of all events from databse
     * @return List<Event> </Event>
     */
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

    //public void displayEvents(EventsAdapter adapter){
      //  db.collection("events")
        //        .get()
          //      .addOnSuccessListener(queryDocumentSnapshots -> {
            //        List<Event> list = new ArrayList<>();
              //      for (DocumentSnapshot doc : queryDocumentSnapshots) {
                //        Event e = doc.toObject(Event.class);
                  //      if (e != null) {
                    //        e.setEvent_id(doc.getId());
                      //      list.add(e);
                        //}
                    //}
                    //adapter.setEvents(list);
                //})
                //.addOnFailureListener(e -> {
                //    Log.e("EventsHome", "Failed to load events", e);
                //});
    //}

    /**
     * Delete a specific event from database
     * @param event
     */
    public void deleteEvent(Event event){
        db.collection("events").document(event.getEvent_id()).delete();
    }

    /**
     * gets a list of all users from database
     * @return List<User> </User>
     */
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

    /**
     * Used to grab users from database synchronously
     * @param callback
     * @return
     */
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

    /**
     * update user types when admin promote/demote
     * @param userId
     * @param newType
     */
    public void updateUserType(String userId, String newType) {
        db.collection("users")
                .document(userId)
                .update("user_type", newType);
    }

    /**
     * Deletes users from database
     * @param userId
     */
    public void deleteUser(String userId) {
        db.collection("users")
                .document(userId)
                .delete();
    }

    /**
     * interface to define classes for grabbing users from database synchronously
     */
    public interface ManageUsersCallback {
        void onUsersLoaded(List<User> users);
        void onError(Exception e);
    }

    /**
     * updates the adapter for events with a list of events from database
     * @param adapter
     * @return
     */
    public ListenerRegistration listenToEvents(EventsAdapter adapter) {
        return db.collection("events")
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


