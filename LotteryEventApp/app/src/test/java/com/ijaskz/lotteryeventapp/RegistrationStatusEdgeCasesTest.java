package com.ijaskz.lotteryeventapp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pure-Java tests for mapping a registration window to the label used in Step 1.
 * We avoid Firebase Timestamp – use epoch millis instead.
 *
 * Rules we implemented visually in the list:
 * - not set: start==null or end==null
 * - upcoming: now < start
 * - open:     start <= now <= end
 * - closed:   now > end
 */
public class RegistrationStatusEdgeCasesTest {

    private static String label(Long startMs, Long endMs, long nowMs) {
        if (startMs == null || endMs == null) return "not set";
        if (nowMs < startMs) return "upcoming";
        if (nowMs > endMs) return "closed";
        return "open";
    }

    @Test
    public void nullWindow_isNotSet() {
        long now = 1_700_000_000_000L;
        assertEquals("not set", label(null,  now + 10_000, now));
        assertEquals("not set", label(now,  null,          now));
        assertEquals("not set", label(null,  null,         now));
    }

    @Test
    public void beforeStart_isUpcoming() {
        long now   = 1_700_000_000_000L; // t0
        long start = now + 60_000;       // t0 + 1min
        long end   = start + 3_600_000;  // + 1h
        assertEquals("upcoming", label(start, end, now));
    }

    @Test
    public void atStart_isOpen() {
        long start = 1_700_000_000_000L;
        long end   = start + 3_600_000;
        long now   = start;
        assertEquals("open", label(start, end, now));
    }

    @Test
    public void middleOfWindow_isOpen() {
        long start = 1_700_000_000_000L;
        long end   = start + 3_600_000;
        long now   = start + 1_800_000;
        assertEquals("open", label(start, end, now));
    }

    @Test
    public void atEnd_isOpen_inclusive() {
        long start = 1_700_000_000_000L;
        long end   = start + 3_600_000;
        long now   = end; // inclusive
        assertEquals("open", label(start, end, now));
    }

    @Test
    public void afterEnd_isClosed() {
        long start = 1_700_000_000_000L;
        long end   = start + 3_600_000;
        long now   = end + 1;
        assertEquals("closed", label(start, end, now));
    }
}
