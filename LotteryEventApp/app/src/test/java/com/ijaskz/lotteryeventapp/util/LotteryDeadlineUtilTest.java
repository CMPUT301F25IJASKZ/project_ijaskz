package com.ijaskz.lotteryeventapp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests for LotteryDeadlineUtil covering calculation, pass checks,
 * and human-friendly remaining time strings.
 */
@RunWith(JUnit4.class)
public class LotteryDeadlineUtilTest {

    /**
     * Verifies that calculateDeadline adds the requested number of hours to the base timestamp.
     */
    @Test
    public void calculateDeadline_addsHoursCorrectly() {
        Date now = new Date();
        int hours = 24;
        Date deadline = LotteryDeadlineUtil.calculateDeadline(now, hours);
        long expected = now.getTime() + TimeUnit.HOURS.toMillis(hours);
        assertEquals(expected, deadline.getTime());
    }

    /**
     * Future deadline should not be considered passed.
     */
    @Test
    public void isDeadlinePassed_future_false() {
        Date now = new Date();
        Date selectedAt = new Date(now.getTime());
        boolean passed = LotteryDeadlineUtil.isDeadlinePassed(selectedAt, 48);
        assertFalse(passed);
    }

    /**
     * Past deadline should be considered passed.
     */
    @Test
    public void isDeadlinePassed_past_true() {
        // selected 25 hours ago, window 24 hours
        Date selectedAt = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25));
        boolean passed = LotteryDeadlineUtil.isDeadlinePassed(selectedAt, 24);
        assertTrue(passed);
    }

    /**
     * Remaining time string should show hours and minutes when more than an hour remains.
     */
    @Test
    public void remainingTime_moreThanHour_showsHoursMinutes() {
        Date selectedAt = new Date();
        int windowHours = 3; // small window to avoid large ranges
        String remaining = LotteryDeadlineUtil.getRemainingTimeString(selectedAt, windowHours);
        assertTrue(remaining.contains("h") || remaining.contains("minutes"));
    }

    /**
     * When the deadline is in the past, string should indicate passed.
     */
    @Test
    public void remainingTime_deadlinePassed_message() {
        Date selectedAt = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(48));
        String remaining = LotteryDeadlineUtil.getRemainingTimeString(selectedAt, 1);
        assertEquals("Deadline passed", remaining);
    }
}
