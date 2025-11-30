package com.ijaskz.lotteryeventapp;

import static org.junit.Assert.*;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Firebase integration tests for Event Lottery operations
 * Tests: Lottery status, registration windows, waitlist limits
 */
@RunWith(AndroidJUnit4.class)
public class EventLotteryFirebaseTest {

    private FirebaseFirestore db;
    private String testEventId;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        testEventId = "test_lottery_event_" + System.currentTimeMillis();
    }

    @After
    public void tearDown() throws Exception {
        cleanupEvent();
    }

    // Test updating lottery run status
    @Test
    public void testUpdateLotteryRunStatus() throws Exception {
        createTestEvent();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        db.collection("events").document(testEventId)
                .update("lotteryRun", true)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Lottery status should be updated", updated[0]);

        // Verify the update
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final Boolean[] lotteryRun = {false};

        db.collection("events").document(testEventId)
                .get()
                .addOnSuccessListener(doc -> {
                    lotteryRun[0] = doc.getBoolean("lotteryRun");
                    verifyLatch.countDown();
                });

        assertTrue("Verify timed out", verifyLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Lottery should be marked as run", lotteryRun[0]);
    }

    // Test setting registration start time
    @Test
    public void testSetRegistrationStartTime() throws Exception {
        createTestEvent();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        Timestamp startTime = new Timestamp(new Date());

        db.collection("events").document(testEventId)
                .update("registrationStart", startTime)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Registration start should be set", updated[0]);
    }

    // Test setting registration end time
    @Test
    public void testSetRegistrationEndTime() throws Exception {
        createTestEvent();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        Timestamp endTime = new Timestamp(new Date(System.currentTimeMillis() + 86400000)); // +1 day

        db.collection("events").document(testEventId)
                .update("registrationEnd", endTime)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Registration end should be set", updated[0]);
    }

    // Test setting waitlist limit
    @Test
    public void testSetWaitlistLimit() throws Exception {
        createTestEvent();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        db.collection("events").document(testEventId)
                .update("waitlistLimit", 50)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Waitlist limit should be set", updated[0]);

        // Verify the limit
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final Long[] limit = {null};

        db.collection("events").document(testEventId)
                .get()
                .addOnSuccessListener(doc -> {
                    limit[0] = doc.getLong("waitlistLimit");
                    verifyLatch.countDown();
                });

        assertTrue("Verify timed out", verifyLatch.await(5, TimeUnit.SECONDS));
        assertEquals("Waitlist limit should be 50", Long.valueOf(50), limit[0]);
    }

    // Test removing waitlist limit (unlimited)
    @Test
    public void testRemoveWaitlistLimit() throws Exception {
        createTestEventWithLimit(30);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        db.collection("events").document(testEventId)
                .update("waitlistLimit", null)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Waitlist limit should be removed", updated[0]);
    }

    // Test setting response window hours
    @Test
    public void testSetResponseWindowHours() throws Exception {
        createTestEvent();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        db.collection("events").document(testEventId)
                .update("responseWindowHours", 24)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Response window should be set", updated[0]);
    }

    // Test querying events with lottery not run
    @Test
    public void testQueryEventsWithLotteryNotRun() throws Exception {
        createTestEvent();

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("events")
                .whereEqualTo("lotteryRun", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    count[0] = querySnapshot.size();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Query timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should find events with lottery not run", count[0] >= 1);
    }

    // Test updating event status
    @Test
    public void testUpdateEventStatus() throws Exception {
        createTestEvent();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        db.collection("events").document(testEventId)
                .update("status", "completed")
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Event status should be updated", updated[0]);
    }

    // Helper method to create test event
    private void createTestEvent() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event_id", testEventId);
        eventData.put("event_name", "Test Lottery Event");
        eventData.put("event_description", "Test Description");
        eventData.put("location", "Test Location");
        eventData.put("max", 100);
        eventData.put("organizer_name", "Test Organizer");
        eventData.put("lotteryRun", false);
        eventData.put("status", "active");

        db.collection("events").document(testEventId)
                .set(eventData)
                .addOnCompleteListener(task -> latch.countDown());

        assertTrue("Setup timed out", latch.await(5, TimeUnit.SECONDS));
    }

    // Helper method to create event with waitlist limit
    private void createTestEventWithLimit(int limit) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event_id", testEventId);
        eventData.put("event_name", "Test Event");
        eventData.put("max", 100);
        eventData.put("waitlistLimit", limit);
        eventData.put("lotteryRun", false);

        db.collection("events").document(testEventId)
                .set(eventData)
                .addOnCompleteListener(task -> latch.countDown());

        assertTrue("Setup timed out", latch.await(5, TimeUnit.SECONDS));
    }

    // Cleanup method
    private void cleanupEvent() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            db.collection("events").document(testEventId)
                    .delete()
                    .addOnCompleteListener(task -> latch.countDown());
            latch.await(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e("EventLotteryTest", "Cleanup failed", e);
        }
    }
}