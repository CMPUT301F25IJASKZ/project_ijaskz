package com.ijaskz.lotteryeventapp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pure-Java tests for mapping a registration window to the label used in Step 1.
 * We avoid Firebase Timestamp – use epoch millis instead.
 *
 *  * User Stories (entrant and organizer views):
 *  * - US 01.01.04: As an entrant, I want to filter events based on my interests and availability.
 *  * - US 01.01.03: As an entrant, I want to be able to see a list of events that I can join the waiting list for.
 *  * - US 02.01.04: As an organizer, I want to set a registration period.
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

    /**
     * Tests: registration window missing
     * Supports:
     * - US 01.01.04 As an entrant, I want to filter events based on my interests and availability.
     */

    @Test
    public void nullWindow_isNotSet() {
        long now = 1_700_000_000_000L;
        assertEquals("not set", label(null,  now + 10_000, now));
        assertEquals("not set", label(now,  null,          now));
        assertEquals("not set", label(null,  null,         now));
    }

    /**
     * Tests upcoming events.
     *
     * Supports:
     * - US 01.01.04 As an entrant, I want to filter events based on my interests and availability.
     */
    @Test
    public void beforeStart_isUpcoming() {
        long now   = 1_700_000_000_000L; // t0
        long start = now + 60_000;       // t0 + 1min
        long end   = start + 3_600_000;  // + 1h
        assertEquals("upcoming", label(start, end, now));
    }


    /**
     * Tests start boundary for "open".
     *
     * Supports:
     * - US 01.01.03 As an entrant, I want to be able to see a list of events that I can join the waiting list for.
     */
    @Test
    public void atStart_isOpen() {
        long start = 1_700_000_000_000L;
        long end   = start + 3_600_000;
        long now   = start;
        assertEquals("open", label(start, end, now));
    }

    /**
     * Tests event in the middle of open window.
     *
     * Supports:
     * - US 01.01.04 As an entrant, I want to filter events based on my interests and availability.
     */
    @Test
    public void middleOfWindow_isOpen() {
        long start = 1_700_000_000_000L;
        long end   = start + 3_600_000;
        long now   = start + 1_800_000;
        assertEquals("open", label(start, end, now));
    }


    /**
     * Tests inclusive end boundary for "open".
     */
    @Test
    public void atEnd_isOpen_inclusive() {
        long start = 1_700_000_000_000L;
        long end   = start + 3_600_000;
        long now   = end; // inclusive
        assertEquals("open", label(start, end, now));
    }

    /**
     * Tests post window "closed" state.
     *organizer views closed registration before sampling
     */
    @Test
    public void afterEnd_isClosed() {
        long start = 1_700_000_000_000L;
        long end   = start + 3_600_000;
        long now   = end + 1;
        assertEquals("closed", label(start, end, now));
    }
}
