// Tests the core data model for waiting list entries

package com.ijaskz.lotteryeventapp;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaitingListEntryTest {

    private WaitingListEntry entry;
    private static final String TEST_EVENT_ID = "event123";
    private static final String TEST_USER_ID = "user456";
    private static final String TEST_USER_NAME = "John Doe";
    private static final String TEST_USER_EMAIL = "john@example.com";

    @Before
    public void setUp() {
        entry = new WaitingListEntry(TEST_EVENT_ID, TEST_USER_ID,
                TEST_USER_NAME, TEST_USER_EMAIL);
    }

    @Test
    public void testInitialStatusIsWaiting() {
        assertEquals("Initial status should be 'waiting'", "waiting", entry.getStatus());
    }

    @Test
    public void testStatusUpdateChangesTimestamp() {
        long initialTime = entry.getUpdated_at();

        // Wait a bit to ensure timestamp changes
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        entry.setStatus("selected");

        assertEquals("Status should be updated to 'selected'", "selected", entry.getStatus());
        assertTrue("Updated timestamp should be greater than initial",
                entry.getUpdated_at() > initialTime);
    }

    @Test
    public void testEntrantDetailsAreStored() {
        assertEquals("Event ID should match", TEST_EVENT_ID, entry.getEvent_id());
        assertEquals("Entrant ID should match", TEST_USER_ID, entry.getEntrant_id());
        assertEquals("Entrant name should match", TEST_USER_NAME, entry.getEntrant_name());
        assertEquals("Entrant email should match", TEST_USER_EMAIL, entry.getEntrant_email());
    }

    @Test
    public void testSelectedAtTimestampIsNull() {
        assertNull("Selected_at should be null initially", entry.getSelected_at());
    }

    @Test
    public void testSetSelectedAtUpdatesTimestamp() {
        long selectedTime = System.currentTimeMillis();
        entry.setSelected_at(selectedTime);

        assertNotNull("Selected_at should not be null", entry.getSelected_at());
        assertEquals("Selected_at should match set value",
                Long.valueOf(selectedTime), entry.getSelected_at());
    }

    @Test
    public void testResponseWindowHoursCanBeSet() {
        assertNull("Response window should be null initially",
                entry.getResponse_window_hours());

        entry.setResponse_window_hours(48);

        assertNotNull("Response window should not be null after setting",
                entry.getResponse_window_hours());
        assertEquals("Response window should be 48 hours",
                Integer.valueOf(48), entry.getResponse_window_hours());
    }

    @Test
    public void testDeclineReasonCanBeSet() {
        String reason = "Schedule conflict";
        entry.setDecline_reason(reason);

        assertEquals("Decline reason should match", reason, entry.getDecline_reason());
    }
}