package com.ijaskz.lotteryeventapp.util;

import java.util.Calendar;
import java.util.Date;

/**
 * Utility methods for calculating and formatting lottery invitation response deadlines.
 * All methods are pure Java for easy unit testing.
 */
public final class LotteryDeadlineUtil {

    private LotteryDeadlineUtil() { }

    /**
     * Calculates the deadline time by adding a number of hours to a start timestamp.
     *
     * @param selectedAt the timestamp when the user was selected
     * @param responseWindowHours the number of hours the user has to respond
     * @return a new Date representing the deadline time
     * @throws IllegalArgumentException if selectedAt is null or responseWindowHours is negative
     */
    public static Date calculateDeadline(Date selectedAt, int responseWindowHours) {
        if (selectedAt == null) {
            throw new IllegalArgumentException("selectedAt cannot be null");
        }
        if (responseWindowHours < 0) {
            throw new IllegalArgumentException("responseWindowHours cannot be negative");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedAt);
        cal.add(Calendar.HOUR_OF_DAY, responseWindowHours);
        return cal.getTime();
    }

    /**
     * Determines if the deadline has already passed relative to the current time.
     *
     * @param selectedAt the timestamp when the user was selected
     * @param responseWindowHours the allowed response window in hours
     * @return true if now is after the deadline; false otherwise (or if selectedAt is null)
     */
    public static boolean isDeadlinePassed(Date selectedAt, int responseWindowHours) {
        if (selectedAt == null) {
            return false;
        }
        Date deadline = calculateDeadline(selectedAt, responseWindowHours);
        return new Date().after(deadline);
        
    }

    /**
     * Returns a human-friendly string for time remaining until the deadline.
     *
     * @param selectedAt the timestamp when the user was selected
     * @param responseWindowHours the allowed response window in hours
     * @return a string like "2h 15m remaining", "45 minutes remaining", or "Deadline passed"
     */
    public static String getRemainingTimeString(Date selectedAt, int responseWindowHours) {
        if (selectedAt == null) {
            return "No deadline";
        }
        Date deadline = calculateDeadline(selectedAt, responseWindowHours);
        long remainingMs = deadline.getTime() - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return "Deadline passed";
        }
        long hours = remainingMs / (60 * 60 * 1000);
        long minutes = (remainingMs % (60 * 60 * 1000)) / (60 * 1000);
        if (hours > 0) {
            return String.format("%dh %02dm remaining", hours, minutes);
        }
        return String.format("%d minutes remaining", minutes);
    }
}
