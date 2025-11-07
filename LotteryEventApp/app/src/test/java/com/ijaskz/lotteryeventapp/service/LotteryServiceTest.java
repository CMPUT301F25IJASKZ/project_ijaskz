package com.ijaskz.lotteryeventapp.service;

import com.ijaskz.lotteryeventapp.WaitingListEntry;
import com.ijaskz.lotteryeventapp.WaitingListManager;
import com.ijaskz.lotteryeventapp.testdata.TestDataFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for LotteryService using a fake WaitingListManager to avoid Firebase.
 */
@RunWith(JUnit4.class)
public class LotteryServiceTest {

    /**
     * A simple fake of WaitingListManager that serves preloaded entries and captures batch updates.
     */
    static class FakeWaitingListManager extends WaitingListManager {
        FakeWaitingListManager() { super(true); }
        List<WaitingListEntry> pool = new ArrayList<>();
        List<String> lastUpdatedIds = new ArrayList<>();
        String lastStatus = null;
        Integer lastHours = null;

        @Override
        public void getEntriesByStatus(String eventId, String status, OnEntriesLoadedListener listener) {
            listener.onEntriesLoaded(new ArrayList<>(pool));
        }

        @Override
        public void updateEntriesStatus(List<String> entryIds, String newStatus, Integer hours, OnCompleteListener listener) {
            lastUpdatedIds = new ArrayList<>(entryIds);
            lastStatus = newStatus;
            lastHours = hours;
            listener.onSuccess();
        }
    }

    /**
     * Ensures that runLottery selects up to slots unique winners and updates status to selected.
     */
    @Test
    public void runLottery_selectsUpToSlots_andSetsSelected() {
        FakeWaitingListManager fake = new FakeWaitingListManager();
        fake.pool = TestDataFactory.waitingEntries("ev1", 10);

        LotteryService svc = new LotteryService(fake);
        final List<WaitingListEntry> winnersBox = new ArrayList<>();

        svc.runLottery("ev1", 3, null, new LotteryService.OnLotteryComplete() {
            @Override public void onSuccess(List<WaitingListEntry> winners) { winnersBox.addAll(winners); }
            @Override public void onFailure(Exception e) { fail("should not fail: " + e); }
        });

        assertEquals(3, winnersBox.size());
        // Unique ids
        Set<String> ids = new HashSet<>();
        for (WaitingListEntry e : winnersBox) ids.add(e.getId());
        assertEquals(3, ids.size());
        // Status and timestamps set in-memory
        for (WaitingListEntry e : winnersBox) {
            assertEquals("selected", e.getStatus());
            assertNotNull(e.getSelected_at());
        }
        // Batch update captured
        assertEquals(3, fake.lastUpdatedIds.size());
        assertEquals("selected", fake.lastStatus);
        assertEquals(Integer.valueOf(48), fake.lastHours); // default 48h applied
    }

    /**
     * Ensures that override response window hours are applied to selected entries.
     */
    @Test
    public void runLottery_appliesOverrideHours() {
        FakeWaitingListManager fake = new FakeWaitingListManager();
        fake.pool = TestDataFactory.waitingEntries("ev2", 5);

        LotteryService svc = new LotteryService(fake);
        final List<WaitingListEntry> winnersBox = new ArrayList<>();

        svc.runLottery("ev2", 2, 12, new LotteryService.OnLotteryComplete() {
            @Override public void onSuccess(List<WaitingListEntry> winners) { winnersBox.addAll(winners); }
            @Override public void onFailure(Exception e) { fail("should not fail: " + e); }
        });

        assertEquals(2, winnersBox.size());
        for (WaitingListEntry e : winnersBox) {
            assertEquals(Integer.valueOf(12), e.getResponse_window_hours());
        }
        assertEquals(Integer.valueOf(12), fake.lastHours);
    }

    /**
     * Ensures error is surfaced when slots is invalid.
     */
    @Test
    public void runLottery_invalidSlots_fails() {
        FakeWaitingListManager fake = new FakeWaitingListManager();
        LotteryService svc = new LotteryService(fake);
        final boolean[] failed = { false };

        svc.runLottery("ev", 0, null, new LotteryService.OnLotteryComplete() {
            @Override public void onSuccess(List<WaitingListEntry> winners) { fail("should fail"); }
            @Override public void onFailure(Exception e) { failed[0] = true; }
        });

        assertTrue(failed[0]);
    }
}
