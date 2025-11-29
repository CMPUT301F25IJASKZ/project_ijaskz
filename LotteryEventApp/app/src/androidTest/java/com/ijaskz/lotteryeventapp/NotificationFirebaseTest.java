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
 * Firebase integration tests for Notification operations
 * Tests: Create, Read, Update, Delete notifications
 */
@RunWith(AndroidJUnit4.class)
public class NotificationFirebaseTest {

    private FirebaseFirestore db;
    private String testNotificationId;
    private String testUserId;
    private String testEventId;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        testNotificationId = "test_notif_" + System.currentTimeMillis();
        testUserId = "test_user_" + System.currentTimeMillis();
        testEventId = "test_event_" + System.currentTimeMillis();
    }

    @After
    public void tearDown() throws Exception {
        cleanupNotification();
    }

    // Test creating a "selected" notification
    @Test
    public void testCreateSelectionNotification() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("userId", testUserId);
        notifData.put("title", "You were selected!");
        notifData.put("message", "You have been selected for the event.");
        notifData.put("eventId", testEventId);
        notifData.put("type", "selected");
        notifData.put("createdAt", System.currentTimeMillis());
        notifData.put("read", false);

        db.collection("notifications").document(testNotificationId)
                .set(notifData)
                .addOnSuccessListener(aVoid -> {
                    success[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e("NotificationTest", "Failed to create notification", e);
                    latch.countDown();
                });

        assertTrue("Create notification timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Notification should be created", success[0]);
    }

    // Test creating a "not_selected" notification
    @Test
    public void testCreateNotSelectedNotification() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("userId", testUserId);
        notifData.put("title", "Lottery Result");
        notifData.put("message", "You were not selected this time.");
        notifData.put("eventId", testEventId);
        notifData.put("type", "not_selected");
        notifData.put("createdAt", System.currentTimeMillis());
        notifData.put("read", false);

        db.collection("notifications").document(testNotificationId)
                .set(notifData)
                .addOnSuccessListener(aVoid -> {
                    success[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Create notification timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Not-selected notification should be created", success[0]);
    }

    // Test creating an organizer message notification
    @Test
    public void testCreateOrganizerMessageNotification() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("userId", testUserId);
        notifData.put("title", "Event Update");
        notifData.put("message", "The event location has changed.");
        notifData.put("eventId", testEventId);
        notifData.put("type", "organizer_message");
        notifData.put("createdAt", System.currentTimeMillis());
        notifData.put("read", false);

        db.collection("notifications").document(testNotificationId)
                .set(notifData)
                .addOnSuccessListener(aVoid -> {
                    success[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Create notification timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Organizer message should be created", success[0]);
    }

    // Test marking notification as read
    @Test
    public void testMarkNotificationAsRead() throws Exception {
        createTestNotification();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] updated = {false};

        db.collection("notifications").document(testNotificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    updated[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Update timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Notification should be marked as read", updated[0]);

        // Verify the update
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final Boolean[] isRead = {false};

        db.collection("notifications").document(testNotificationId)
                .get()
                .addOnSuccessListener(doc -> {
                    isRead[0] = doc.getBoolean("read");
                    verifyLatch.countDown();
                });

        assertTrue("Verify timed out", verifyLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Notification should be marked as read", isRead[0]);
    }

    // Test deleting notification
    @Test
    public void testDeleteNotification() throws Exception {
        createTestNotification();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] deleted = {false};

        db.collection("notifications").document(testNotificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    deleted[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Delete timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Notification should be deleted", deleted[0]);

        // Verify deletion
        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] exists = {true};

        db.collection("notifications").document(testNotificationId)
                .get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    verifyLatch.countDown();
                });

        assertTrue("Verify timed out", verifyLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Notification should not exist", exists[0]);
    }

    // Test querying notifications by user
    @Test
    public void testQueryNotificationsByUser() throws Exception {
        createTestNotification();

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("notifications")
                .whereEqualTo("userId", testUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    count[0] = querySnapshot.size();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Query timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should find at least one notification", count[0] >= 1);
    }

    // Test querying unread notifications
    @Test
    public void testQueryUnreadNotifications() throws Exception {
        createTestNotification();

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("notifications")
                .whereEqualTo("userId", testUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    count[0] = querySnapshot.size();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Query timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should find unread notifications", count[0] >= 1);
    }

    // Test querying notifications by type
    @Test
    public void testQueryNotificationsByType() throws Exception {
        createTestNotification();

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("notifications")
                .whereEqualTo("type", "selected")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    count[0] = querySnapshot.size();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        assertTrue("Query timed out", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should find selection notifications", count[0] >= 1);
    }

    // Helper method to create test notification
    private void createTestNotification() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("userId", testUserId);
        notifData.put("title", "Test Notification");
        notifData.put("message", "Test message");
        notifData.put("eventId", testEventId);
        notifData.put("type", "selected");
        notifData.put("createdAt", System.currentTimeMillis());
        notifData.put("read", false);

        db.collection("notifications").document(testNotificationId)
                .set(notifData)
                .addOnCompleteListener(task -> latch.countDown());

        assertTrue("Setup timed out", latch.await(5, TimeUnit.SECONDS));
    }

    // Cleanup method
    private void cleanupNotification() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            db.collection("notifications").document(testNotificationId)
                    .delete()
                    .addOnCompleteListener(task -> latch.countDown());
            latch.await(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e("NotificationTest", "Cleanup failed", e);
        }
    }
}