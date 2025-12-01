// Tests the WaitingListManager business logic (without Firebase)

package com.ijaskz.lotteryeventapp;

import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Unit tests for WaitingListManager logic.
 * Covers business rules for waiting list flow.
 * Firebase operations are tested separately.
 */
public class WaitingListManagerTest {

    private WaitingListManager manager;

    @Before
    public void setUp() {
        // Use the testing constructor to avoid Firebase initialization
        manager = new TestableWaitingListManager(true);
    }

    /**
     * Ensures the manager initializes correctly.
     */
    @Test
    public void testManagerCreation() {
        assertNotNull("Manager should not be null", manager);
    }

    /**
     * Tests valid status progression for a waiting list entry.
     *
     * User Stories:
     * - US 01.05.02: As an entrant I want to be able to accept the invitation to register/sign up when chosen to participate in an event.
     * - US 01.05.01 : As an entrant I want another chance to be chosen from the waiting list if a selected user declines an invitation to sign up.
     * - US 02.06.03 : As an organizer I want to see a final list of entrants who enrolled for the event.
     */
    @Test
    public void testEntryStatusProgression() {
        WaitingListEntry entry = new WaitingListEntry(
                "event1", "user1", "Test User", "test@example.com"
        );

        assertEquals("Should start as waiting", "waiting", entry.getStatus());

        entry.setStatus("selected");
        assertEquals("Should progress to selected", "selected", entry.getStatus());

        entry.setStatus("accepted");
        assertEquals("Should progress to accepted", "accepted", entry.getStatus());

        entry.setStatus("enrolled");
        assertEquals("Should progress to enrolled", "enrolled", entry.getStatus());
    }

    /**
     * Tests decline flow for an entrant.
     *
     * User Story:
     * - US 01.05.03: As an entrant I want to be able to decline an invitation when chosen to participate in an event.
     */
    @Test
    public void testDeclineStatusProgression() {
        WaitingListEntry entry = new WaitingListEntry(
                "event1", "user1", "Test User", "test@example.com"
        );

        entry.setStatus("selected");
        entry.setStatus("declined");

        assertEquals("Should be declined", "declined", entry.getStatus());
        assertNotNull("Updated_at should be set", entry.getUpdated_at());
    }

    /**
     * Tests cancellation status.
     *
     * User Story:
     * - US 02.06.04: As an organizer I want to cancel entrants that did not sign up for the event
     */
    @Test
    public void testCancelledStatus() {
        WaitingListEntry entry = new WaitingListEntry(
                "event1", "user1", "Test User", "test@example.com"
        );

        entry.setStatus("cancelled");

        assertEquals("Should be cancelled", "cancelled", entry.getStatus());
    }

    /**
     * Tests that multiple entrants can join the same waiting list.
     *
     * User Story:
     * - US 01.01.01: As an entrant, I want to join the waiting list for a specific event
     */
    @Test
    public void testMultipleEntriesForSameEvent() {
        WaitingListEntry entry1 = new WaitingListEntry(
                "event1", "user1", "User One", "user1@example.com"
        );
        WaitingListEntry entry2 = new WaitingListEntry(
                "event1", "user2", "User Two", "user2@example.com"
        );

        assertEquals("Both entries should be for same event",
                entry1.getEvent_id(), entry2.getEvent_id());
        assertNotEquals("Entries should have different users",
                entry1.getEntrant_id(), entry2.getEntrant_id());
    }

    /**
     * Tests that one user can join multiple event waiting lists.
     *
     * User Story:
     * - US 01.01.01: As an entrant, I want to join the waiting list for a specific event
     */
    @Test
    public void testSameUserMultipleEvents() {
        WaitingListEntry entry1 = new WaitingListEntry(
                "event1", "user1", "User One", "user1@example.com"
        );
        WaitingListEntry entry2 = new WaitingListEntry(
                "event2", "user1", "User One", "user1@example.com"
        );

        assertEquals("Both entries should be for same user",
                entry1.getEntrant_id(), entry2.getEntrant_id());
        assertNotEquals("Entries should be for different events",
                entry1.getEvent_id(), entry2.getEvent_id());
    }

    /**
     * Tests the response window logic for entrants responding to invitations.
     *
     * User Stories:
     * - US 01.05.02: As an entrant I want to be able to accept the invitation to register/sign up when chosen to participate in an event.
     * - US 01.05.03: As an entrant I want to be able to decline an invitation when chosen to participate in an event.
     */
    @Test
    public void testResponseWindowCalculation() {
        WaitingListEntry entry = new WaitingListEntry(
                "event1", "user1", "Test User", "test@example.com"
        );

        entry.setResponse_window_hours(24);

        assertEquals("Response window should be 24 hours",
                Integer.valueOf(24), entry.getResponse_window_hours());

        entry.setResponse_window_hours(48);

        assertEquals("Response window should be updated to 48 hours",
                Integer.valueOf(48), entry.getResponse_window_hours());
    }

    /**
     * Testable version of WaitingListManager that skips Firebase initialization.
     */
    private static class TestableWaitingListManager extends WaitingListManager {
        public TestableWaitingListManager(boolean skipInit) {
            super(skipInit);
        }
    }
}
