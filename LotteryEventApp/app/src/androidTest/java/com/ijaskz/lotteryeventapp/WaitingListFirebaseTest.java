package com.ijaskz.lotteryeventapp;

import static org.junit.Assert.*;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Firebase integration tests for Waiting List operations
 * Tests ONLY Firebase CRUD and query operations
 * Business logic is tested in WaitingListManagerTest
 */
@RunWith(AndroidJUnit4.class)
public class WaitingListFirebaseTest {

    private FirebaseFirestore db;
    private String testEventId;
    private String testEntrantId;
    private String testWaitingListDocId;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        testEventId = "test_event_" + System.currentTimeMillis();
        testEntrantId = "test_entrant_" + System.currentTimeMillis();
        testWaitingListDocId = "test_wl_" + System.currentTimeMillis();
    }

    @After
    public void tearDown() throws Exception {
        cleanupWaitingListEntry();
    }

    // ============ FIREBASE CRUD OPERATIONS ============

    /**
     * Verifies that an entrant can be added to the waiting_list collection.
     */
    @Test
    public void testAddEntrantToWaitingListInFirebase() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        Map<String, Object> entryData = new HashMap<>();
        entryData.put("event_id", testEventId);
        entryData.put("entrant_id", testEntrantId);
        entryData.put("entrant_name", "Test Entrant");
        entryData.put("entrant_email", "entrant@test.com");
        entryData.put("status", "waiting");
        entryData.put("joined_at", System.currentTimeMillis());
        entryData.put("updated_at", System.currentTimeMillis());

        db.collection("waiting_list").document(testWaitingListDocId)
                .set(entryData)
                .addOnSuccessListener(aVoid -> {
                    success[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e("WaitingListTest", "Failed to add entrant", e);
                    latch.countDown();
                });

        assertTrue("Add entrant timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Entrant should be added to Firebase", success[0]);

        // Verify it was actually saved to Firebase
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] exists = {false};

        db.collection("waiting_list").document(testWaitingListDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    verifyLatch.countDown();
                });

        assertTrue("Verify timed out", verifyLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Entry should exist in Firebase", exists[0]);
    }


    /**
     * Verifies that an entrant can be deleted from the waiting_list.
     */
    @Test
    public void testRemoveEntrantFromWaitingListInFirebase() throws Exception {
        createTestWaitingListEntry();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] deleted = {false};

        db.collection("waiting_list").document(testWaitingListDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    deleted[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Remove entrant timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Entrant should be removed from Firebase", deleted[0]);

        // Verify deletion in Firebase
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] exists = {true};

        db.collection("waiting_list").document(testWaitingListDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    verifyLatch.countDown();
                });

        assertTrue("Verify timed out", verifyLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Entry should not exist in Firebase after deletion", exists[0]);
    }

    /**
     * Verifies that status and timestamps can be updated in Firebase.
     */
    @Test
    public void testUpdateStatusInFirebase() throws Exception {
        createTestWaitingListEntry();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "selected");
        updates.put("selected_at", System.currentTimeMillis());
        updates.put("updated_at", System.currentTimeMillis());

        db.collection("waiting_list").document(testWaitingListDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Status should be updated in Firebase", updated[0]);

        // Verify the update persisted in Firebase
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final String[] status = {null};

        db.collection("waiting_list").document(testWaitingListDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    status[0] = doc.getString("status");
                    verifyLatch.countDown();
                });

        assertTrue("Verify timed out", verifyLatch.await(5, TimeUnit.SECONDS));
        assertEquals("Status should be 'selected' in Firebase", "selected", status[0]);
    }

    /**
     * Reads a waiting_list entry and verifies stored fields.
     */
    @Test
    public void testReadWaitingListEntryFromFirebase() throws Exception {
        createTestWaitingListEntry();

        CountDownLatch latch = new CountDownLatch(1);
        final String[] eventId = {null};
        final String[] entrantName = {null};

        db.collection("waiting_list").document(testWaitingListDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        eventId[0] = doc.getString("event_id");
                        entrantName[0] = doc.getString("entrant_name");
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Read timed out", latch.await(5, TimeUnit.SECONDS));
        assertEquals("Event ID should match", testEventId, eventId[0]);
        assertEquals("Entrant name should match", "Test Entrant", entrantName[0]);
    }

    /**
     * Checks that location data (lat, lon) can be updated and retrieved.
     */
    @Test
    public void testUpdateLocationDataInFirebase() throws Exception {
        createTestWaitingListEntry();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        db.collection("waiting_list").document(testWaitingListDocId)
                .update("latitude", 53.5461, "longitude", -113.4938)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Location should be updated in Firebase", updated[0]);

        // Verify location data persisted
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final Double[] lat = {null};
        final Double[] lon = {null};

        db.collection("waiting_list").document(testWaitingListDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    lat[0] = doc.getDouble("latitude");
                    lon[0] = doc.getDouble("longitude");
                    verifyLatch.countDown();
                });

        assertTrue("Verify timed out", verifyLatch.await(5, TimeUnit.SECONDS));
        assertNotNull("Latitude should be saved in Firebase", lat[0]);
        assertNotNull("Longitude should be saved in Firebase", lon[0]);
        assertEquals("Latitude should match", 53.5461, lat[0], 0.0001);
        assertEquals("Longitude should match", -113.4938, lon[0], 0.0001);
    }

    /**
     * Ensures decline_reason and related fields can be updated.
     */
    @Test
    public void testUpdateDeclineReasonInFirebase() throws Exception {
        createTestWaitingListEntry();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "declined");
        updates.put("decline_reason", "Schedule conflict");
        updates.put("responded_at", System.currentTimeMillis());

        db.collection("waiting_list").document(testWaitingListDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Decline reason should be updated in Firebase", updated[0]);

        // Verify it was saved
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final String[] reason = {null};

        db.collection("waiting_list").document(testWaitingListDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    reason[0] = doc.getString("decline_reason");
                    verifyLatch.countDown();
                });

        assertTrue("Verify timed out", verifyLatch.await(5, TimeUnit.SECONDS));
        assertEquals("Decline reason should match", "Schedule conflict", reason[0]);
    }

    // ============ FIREBASE QUERY OPERATIONS ============

    /**
     * Queries waiting_list by event_id.
     */
    @Test
    public void testQueryWaitingListByEventIdInFirebase() throws Exception {
        createTestWaitingListEntry();

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("waiting_list")
                .whereEqualTo("event_id", testEventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    count[0] = querySnapshot.size();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Query timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should find at least one entry for this event in Firebase", count[0] >= 1);
    }


    /**
     * Queries waiting_list by status field.
     */
    @Test
    public void testQueryWaitingListByStatusInFirebase() throws Exception {
        createTestWaitingListEntry();

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("waiting_list")
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    count[0] = querySnapshot.size();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Query timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should find at least one 'waiting' entry in Firebase", count[0] >= 1);
    }

    /**
     * Queries waiting_list by entrant_id.
     */
    @Test
    public void testQueryWaitingListByEntrantIdInFirebase() throws Exception {
        createTestWaitingListEntry();

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("waiting_list")
                .whereEqualTo("entrant_id", testEntrantId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    count[0] = querySnapshot.size();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Query timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should find entries for this entrant in Firebase", count[0] >= 1);
    }

    /**
     * Queries waiting_list using multiple conditions.
     */
    @Test
    public void testQueryMultipleConditionsInFirebase() throws Exception {
        createTestWaitingListEntry();

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("waiting_list")
                .whereEqualTo("event_id", testEventId)
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    count[0] = querySnapshot.size();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Query timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should find waiting entries for this event in Firebase", count[0] >= 1);
    }

    // ============ HELPER METHODS ============

    /**
     * Creates a basic test waiting_list entry.
     */
    private void createTestWaitingListEntry() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Map<String, Object> entryData = new HashMap<>();
        entryData.put("event_id", testEventId);
        entryData.put("entrant_id", testEntrantId);
        entryData.put("entrant_name", "Test Entrant");
        entryData.put("entrant_email", "entrant@test.com");
        entryData.put("status", "waiting");
        entryData.put("joined_at", System.currentTimeMillis());
        entryData.put("updated_at", System.currentTimeMillis());

        db.collection("waiting_list").document(testWaitingListDocId)
                .set(entryData)
                .addOnCompleteListener(task -> latch.countDown());

        assertTrue("Setup timed out", latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Deletes the test waiting_list entry if present.
     */
    private void cleanupWaitingListEntry() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            db.collection("waiting_list").document(testWaitingListDocId)
                    .delete()
                    .addOnCompleteListener(task -> latch.countDown());
            latch.await(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e("WaitingListTest", "Cleanup failed", e);
        }
    }
}