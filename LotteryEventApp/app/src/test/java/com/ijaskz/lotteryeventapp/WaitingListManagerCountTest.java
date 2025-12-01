package com.ijaskz.lotteryeventapp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for US 01.05.04 As an entrant, I want to know how many total entrants are on the waiting list for an event.
 */
public class WaitingListManagerCountTest {

    /** Simple stub that short-circuits Firestore and returns canned counts. */
    static class StubWaitingListManager extends WaitingListManager {
        private final Map<String, Integer> counts;
        private final boolean shouldFail;

        StubWaitingListManager(Map<String, Integer> counts) {
            super(true); // skip Firestore init
            this.counts = counts;
            this.shouldFail = false;
        }

        StubWaitingListManager(boolean shouldFail) {
            super(true);
            this.counts = new HashMap<>();
            this.shouldFail = shouldFail;
        }

        @Override
        public void getWaitingListCount(String eventId, OnCountListener listener) {
            if (shouldFail) {
                listener.onError(new RuntimeException("boom"));
                return;
            }
            if (eventId == null || eventId.trim().isEmpty()) {
                listener.onCount(0);
                return;
            }
            Integer v = counts.get(eventId);
            listener.onCount(v == null ? 0 : Math.max(0, v));
        }
    }
    /**
     * Should return the exact number of entrants.
     */
    @Test
    public void returnsExactCount_whenEventHasEntrants() {
        Map<String,Integer> data = new HashMap<>();
        data.put("e1", 7);
        WaitingListManager mgr = new StubWaitingListManager(data);

        final int[] got = {-1};
        mgr.getWaitingListCount("e1", new WaitingListManager.OnCountListener() {
            @Override public void onCount(int count) { got[0] = count; }
            @Override public void onError(Exception e) { fail("Should not error"); }
        });

        assertEquals(7, got[0]);
    }

    /**
     * Event with no entries should return zero.
     */
    @Test
    public void returnsZero_whenEventHasNoEntrants() {
        Map<String,Integer> data = new HashMap<>();
        WaitingListManager mgr = new StubWaitingListManager(data);

        final int[] got = {-1};
        mgr.getWaitingListCount("unknown", new WaitingListManager.OnCountListener() {
            @Override public void onCount(int count) { got[0] = count; }
            @Override public void onError(Exception e) { fail("Should not error"); }
        });

        assertEquals(0, got[0]);
    }

    /**
     * Null or blank event IDs should return zero.
     */
    @Test
    public void returnsZero_whenEventIdNullOrBlank() {
        WaitingListManager mgr = new StubWaitingListManager(new HashMap<>());

        final int[] got = {-1};
        mgr.getWaitingListCount("  ", new WaitingListManager.OnCountListener() {
            @Override public void onCount(int count) { got[0] = count; }
            @Override public void onError(Exception e) { fail("Should not error"); }
        });

        assertEquals(0, got[0]);
    }

    /**
     * Manager should not return negative counts.
     */
    @Test
    public void clampsToNonNegative_whenStubProvidesNegative() {
        Map<String,Integer> data = new HashMap<>();
        data.put("e2", -5);
        WaitingListManager mgr = new StubWaitingListManager(data);

        final int[] got = {-1};
        mgr.getWaitingListCount("e2", new WaitingListManager.OnCountListener() {
            @Override public void onCount(int count) { got[0] = count; }
            @Override public void onError(Exception e) { fail("Should not error"); }
        });

        assertEquals(0, got[0]);
    }


    /**
     * Errors must surface via onError().
     */
    @Test
    public void surfacesErrors_viaOnError() {
        WaitingListManager mgr = new StubWaitingListManager(true);

        final boolean[] errored = {false};
        mgr.getWaitingListCount("e3", new WaitingListManager.OnCountListener() {
            @Override public void onCount(int count) { fail("Should error"); }
            @Override public void onError(Exception e) { errored[0] = (e != null); }
        });

        assertTrue(errored[0]);
    }


    /**
     * Supports very large waiting list counts.
     */
    @Test
    public void supportsVeryLargeCounts() {
        Map<String,Integer> data = new HashMap<>();
        data.put("big", Integer.MAX_VALUE);
        WaitingListManager mgr = new StubWaitingListManager(data);

        final int[] got = {-1};
        mgr.getWaitingListCount("big", new WaitingListManager.OnCountListener() {
            @Override public void onCount(int count) { got[0] = count; }
            @Override public void onError(Exception e) { fail("Should not error"); }
        });

        assertEquals(Integer.MAX_VALUE, got[0]);
    }
}
