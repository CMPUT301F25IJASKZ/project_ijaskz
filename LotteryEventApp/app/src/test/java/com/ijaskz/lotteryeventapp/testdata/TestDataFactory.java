package com.ijaskz.lotteryeventapp.testdata;

import com.ijaskz.lotteryeventapp.WaitingListEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * TestDataFactory centralizes creation of test objects for unit tests.
 */
public final class TestDataFactory {

    private TestDataFactory() { }

    /**
     * Creates a list of synthetic WaitingListEntry objects with ids "e0".."e{count-1}".
     * @param eventId event id to assign
     * @param count how many entries to create
     * @return list of entries
     */
    public static List<WaitingListEntry> waitingEntries(String eventId, int count) {
        List<WaitingListEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            WaitingListEntry e = new WaitingListEntry(eventId, "u" + i, "User" + i, "u" + i + "@t.com");
            e.setId("e" + i);
            list.add(e);
        }
        return list;
    }
}
