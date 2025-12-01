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

/**
 * Defines the FireStoreHelper to be used for interactions with database
 */
public class FireStoreHelper {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    public FireStoreHelper(){};

    public FireStoreHelper(FirebaseFirestore db) {
        this.db = db;
    }


    /**
     * Takes in Event object then adds it to the database
     * @param event The event to be added
     */
    public void addEvent(Event event){
        Map<String,Object> newEvent = new HashMap<>();
        newEvent.put("event_description", event.getEvent_description());
        newEvent.put("event_location", event.getLocation());
        newEvent.put("event_name", event.getEvent_name());
        newEvent.put("event_time", event.getEvent_time());
        newEvent.put("image", event.getImage());
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

    /**
     * Grabs the list of all events from database
     * @return List<Event> </Event>
     */
    // EVERYONE USE THIS!!!
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
     * @param event The event to be deleted
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
                })
                .addOnFailureListener(e -> Log.e("Users", "Failed to load users", e));
        return users;
    }

    /**
     * Used to grab users from database synchronously
     * @param callback The callback to make sure list is filled before returning
     * @return The listener reg that makes sure user list is populated
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
     * Listen to events filtered by organizer name
     * @param adapter The adapter for events to update
     * @param organizerName The organizer name to filter by
     * @return Listener reg for events
     */
    public ListenerRegistration listenToEventsFiltered(EventsAdapter adapter, String organizerName) {
        return db.collection("events")
                .whereEqualTo("organizer_name", organizerName)
                .orderBy("createdAt", Query.Direction.DESCENDING)
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

    /**
     * update user types when admin promote/demote
     * @param userId Id of user update type of
     * @param newType new type for user
     */
    public void updateUserType(String userId, String newType) {
        db.collection("users")
                .document(userId)
                .update("user_type", newType);
    }

    /**
     * Deletes users from database
     * @param userId id of user to be deleted
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
     * @param adapter The adapter for events to update
     * @return Listener reg for events
     */
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
