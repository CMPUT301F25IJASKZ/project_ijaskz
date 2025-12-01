package com.ijaskz.lotteryeventapp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pure-Java tests for the Step 1 search behavior.
 * We mimic the adapter’s "name OR description contains query" matching.
 *  - US 01.01.04: Entrants filter events based on interests (keyword search)
 */
public class SearchFilterLogicTest {

    // same rule we applied in Step 1
    private static boolean matchesQuery(String name, String desc, String rawQuery) {
        if (rawQuery == null) return true;                // empty query = show all
        String q = rawQuery.trim().toLowerCase();
        if (q.isEmpty()) return true;
        String n = name == null ? "" : name.toLowerCase();
        String d = desc == null ? "" : desc.toLowerCase();
        return n.contains(q) || d.contains(q);
    }

    /** Empty or null queries should return all events. */
    @Test
    public void emptyQuery_showsEverything() {
        assertTrue(matchesQuery("Concert", "music", ""));
        assertTrue(matchesQuery("Hackathon", "coding", "   "));
        assertTrue(matchesQuery(null, null, null));
    }

    /** Case-insensitive name matching. */
    @Test
    public void nameMatch_basicCaseInsensitive() {
        assertTrue(matchesQuery("Concert", "music", "con"));
        assertTrue(matchesQuery("DOG SHOW", "cute", "dog"));
        assertFalse(matchesQuery("Elk Island Tour", "nature", "hackathon"));
    }

    /** Case-insensitive description matching.*/
    @Test
    public void descriptionMatch_basicCaseInsensitive() {
        assertTrue(matchesQuery("Anything", "Live MUSIC and food", "music"));
        assertTrue(matchesQuery("Anything", "  lots of Coding challenges ", "coding"));
        assertFalse(matchesQuery("Anything", "nature trip", "concert"));
    }

    /** Null-safe search behavior.*/
    @Test
    public void nullFields_safeToSearch() {
        assertTrue(matchesQuery(null, "great vibes", "great"));
        assertTrue(matchesQuery("Picnic", null, "pic"));
        assertFalse(matchesQuery(null, null, "anything"));
    }

    /** Partial words and whitespace trimming match correctly.*/
    @Test
    public void partialWord_and_whitespace() {
        assertTrue(matchesQuery("Hot dog eating competition", "food", "  eat  "));
        assertTrue(matchesQuery("Imagine Dragons Concert", "live show", "drag"));
        assertFalse(matchesQuery("Yoga", "wellness", "concert  "));
    }
}
