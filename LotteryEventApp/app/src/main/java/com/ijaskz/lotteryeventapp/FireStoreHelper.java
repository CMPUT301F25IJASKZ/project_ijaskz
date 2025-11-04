package com.ijaskz.lotteryeventapp;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireStoreHelper {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FireStoreHelper() {}

    /** Create event document. Field names match Event.java exactly. */
    public void addEvent(Event event) {
        Map<String, Object> newEvent = new HashMap<>();
        newEvent.put("event_description", event.getEvent_description());
        newEvent.put("location",           event.getLocation());
        newEvent.put("event_name",         event.getEvent_name());
        newEvent.put("event_time",         event.getEvent_time());
        newEvent.put("max",                event.getMax());

        // keep optional extras only if present (no schema change if nulls)
        if (event.getPoster_url() != null)      newEvent.put("poster_url", event.getPoster_url());
        if (event.getStatus() != null)          newEvent.put("status", event.getStatus());
        if (event.getOrganizer_id() != null)    newEvent.put("organizer_id", event.getOrganizer_id());
        if (event.getRegistration_start() != null) newEvent.put("registration_start", event.getRegistration_start());
        if (event.getRegistration_end() != null)   newEvent.put("registration_end", event.getRegistration_end());
        if (event.getCreated_at() != null)      newEvent.put("created_at", event.getCreated_at());
        if (event.getUpdated_at() != null)      newEvent.put("updated_at", event.getUpdated_at());

        db.collection("events")
                .add(newEvent)
                .addOnSuccessListener(docRef -> {
                    String eventId = docRef.getId();
                    // write event_id back into the document so future reads have it
                    docRef.update("event_id", eventId);
                    Log.d("Events", "Event created with ID: " + eventId);
                })
                .addOnFailureListener(e -> Log.e("Events", "Failed to create event", e));
    }

    /** WARNING: returns before Firestore finishes — prefer displayEvents(). */
    public List<Event> getEventList() {
        List<Event> list = new ArrayList<>();
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            // ensure in-memory object has id for delete/update
                            e.setEvent_id(doc.getId());
                            list.add(e);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("EventsHome", "Failed to load events", e));
        return list;
    }

    /** Populate adapter with events; also set event_id on each Event object. */
    public void displayEvents(EventsAdapter adapter) {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setEvent_id(doc.getId()); // important for later deletes/edits
                            list.add(e);
                        }
                    }
                    adapter.setEvents(list);
                })
                .addOnFailureListener(e ->
                        Log.e("EventsHome", "Failed to load events", e));
    }

    public void deleteEvent(Event event) {
        if (event.getEvent_id() == null || event.getEvent_id().isEmpty()) {
            Log.e("Events", "deleteEvent: missing event_id");
            return;
        }
        db.collection("events")
                .document(event.getEvent_id())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Events", "Event deleted: " + event.getEvent_id()))
                .addOnFailureListener(e -> Log.e("Events", "Failed to delete event", e));
    }
}
