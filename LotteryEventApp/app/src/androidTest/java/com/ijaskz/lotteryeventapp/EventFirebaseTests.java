package com.ijaskz.lotteryeventapp;



import static com.google.common.base.Verify.verify;
import static com.ijaskz.lotteryeventapp.OrganizerFlowTest.organizerIntent;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Firebase integration tests for creating and deleting events.
 */
public class EventFirebaseTests {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FireStoreHelper helper = new FireStoreHelper();
    @Rule
    public ActivityScenarioRule<MainActivity> mainRule =
            new ActivityScenarioRule<>(organizerIntent());

    /**
     * Creates and saves a temporary test event in Firestore.
     *
     * @return event with generated id set
     * @throws InterruptedException if the Firestore call is interrupted
     * @throws ExecutionException   if the Firestore call fails
     */
    private Event createtestevent() throws InterruptedException, ExecutionException {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        Event e = new Event("test", "Ken", "my house", "test_event" + uniqueSuffix, 5, "2025-12-15 19:00", "");
        Timestamp now = Timestamp.now();
        e.setRegistrationStart(new Timestamp(new Date(now.toDate().getTime() - 86400000))); // Started yesterday
        e.setRegistrationEnd(new Timestamp(new Date(now.toDate().getTime() + 86400000))); // Ends tomorrow
        helper.addEvent(e);
        Thread.sleep(500);
        QuerySnapshot snap = Tasks.await(
                db.collection("events")
                        .whereEqualTo("event_name", "test_event")
                        .whereEqualTo("event_location", "my house")
                        .get()
        );
        Assert.assertFalse("Event should exist after addEvent", snap.isEmpty());

        DocumentSnapshot doc = snap.getDocuments().get(0);
        String generatedId = doc.getId();
        e.setEvent_id(generatedId);
        return e;
    }

    /**
     * Verifies that an event can be added and then deleted from Firestore.
     */
    @Test
    public void testAdd_DeleteEvent() throws ExecutionException, InterruptedException {
        Event e = createtestevent();
        DocumentSnapshot before = Tasks.await(
                db.collection("events").document(e.getEvent_id()).get()
        );
        Assert.assertTrue(before.exists());
        helper.deleteEvent(e);
        DocumentSnapshot after = Tasks.await(
                db.collection("events").document(e.getEvent_id()).get()
        );
        Assert.assertFalse(after.exists());

    }
}



