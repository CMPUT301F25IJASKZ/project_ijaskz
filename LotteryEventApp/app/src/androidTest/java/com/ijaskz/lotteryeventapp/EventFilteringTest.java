/** US 01.01.04: As an entrant, I want to filter events based on my interests and availability.
* // Tests event filtering functionality
*/
package com.ijaskz.lotteryeventapp;

import com.google.firebase.Timestamp;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static org.junit.Assert.*;

public class EventFilteringTest {

    private List<Event> allEvents;
    private Timestamp now;

    @Before
    public void setUp() {
        now = Timestamp.now();
        allEvents = createTestEvents();
    }

    /**
     * Creates sample events with different registration statuses
     */
    private List<Event> createTestEvents() {
        List<Event> events = new ArrayList<>();

        // Event 1: Registration open (available now)
        Event openEvent = new Event("Music Concert", "ken", "Downtown Arena",
                "Summer Music Fest", 100, "2025-12-15 19:00", "");
        openEvent.setEvent_id("event1");
        openEvent.setRegistrationStart(new Timestamp(new Date(now.toDate().getTime() - 86400000))); // Started yesterday
        openEvent.setRegistrationEnd(new Timestamp(new Date(now.toDate().getTime() + 86400000))); // Ends tomorrow
        events.add(openEvent);

        // Event 2: Registration upcoming (not available yet)
        Event upcomingEvent = new Event("Tech Conference", "ken", "Convention Center",
                "DevCon 2025", 50, "2026-01-20 09:00", "");
        upcomingEvent.setEvent_id("event2");
        upcomingEvent.setRegistrationStart(new Timestamp(new Date(now.toDate().getTime() + 172800000))); // Starts in 2 days
        upcomingEvent.setRegistrationEnd(new Timestamp(new Date(now.toDate().getTime() + 604800000))); // Ends in 7 days
        events.add(upcomingEvent);

        // Event 3: Registration closed (no longer available)
        Event closedEvent = new Event("Art Exhibition", "ken", "City Gallery",
                "Modern Art Show", 30, "2025-11-01 10:00", "");
        closedEvent.setEvent_id("event3");
        closedEvent.setRegistrationStart(new Timestamp(new Date(now.toDate().getTime() - 604800000))); // Started 7 days ago
        closedEvent.setRegistrationEnd(new Timestamp(new Date(now.toDate().getTime() - 86400000))); // Ended yesterday
        events.add(closedEvent);

        // Event 4: No registration window set
        Event noWindowEvent = new Event("Food Festival", "ken", "Park Square",
                "Taste of the City", 200, "2025-12-10 12:00", "");
        noWindowEvent.setEvent_id("event4");
        events.add(noWindowEvent);

        return events;
    }

    /**
     * Verifies that only open events are returned when filtering by "Open".
     */
    @Test
    public void testFilterOpenEventsOnly() {
        // Filter for only open events (available now)
        List<Event> openEvents = filterEventsByStatus(allEvents, "Open");

        assertEquals("Should find exactly 1 open event", 1, openEvents.size());
        assertEquals("Open event should be the music concert", "event1", openEvents.get(0).getEvent_id());
    }

    /**
     * Verifies that only upcoming events are returned when filtering by "Upcoming".
     */
    @Test
    public void testFilterUpcomingEventsOnly() {
        // Filter for upcoming events (registration hasn't started)
        List<Event> upcomingEvents = filterEventsByStatus(allEvents, "Upcoming");

        assertEquals("Should find exactly 1 upcoming event", 1, upcomingEvents.size());
        assertEquals("Upcoming event should be the tech conference", "event2", upcomingEvents.get(0).getEvent_id());
    }


    /**
     * Verifies that only closed events are returned when filtering by "Closed".
     */
    @Test
    public void testFilterClosedEventsOnly() {
        // Filter for closed events (registration ended)
        List<Event> closedEvents = filterEventsByStatus(allEvents, "Closed");

        assertEquals("Should find exactly 1 closed event", 1, closedEvents.size());
        assertEquals("Closed event should be the art exhibition", "event3", closedEvents.get(0).getEvent_id());
    }

    /**
     * Verifies that events with no registration window are returned when filtering by "Not set".
     */

    @Test
    public void testFilterNotSetEvents() {
        // Filter for events with no registration window
        List<Event> notSetEvents = filterEventsByStatus(allEvents, "Not set");

        assertEquals("Should find exactly 1 event without registration window", 1, notSetEvents.size());
        assertEquals("No-window event should be the food festival", "event4", notSetEvents.get(0).getEvent_id());
    }


    /**
     * Verifies that filtering by text finds events by name.
     */
    @Test
    public void testFilterByTextSearch() {
        // Search by event name
        List<Event> musicEvents = filterEventsByText(allEvents, "music");

        assertEquals("Should find 1 event with 'music' in name", 1, musicEvents.size());
        assertTrue("Should find Summer Music Fest",
                musicEvents.get(0).getEvent_name().toLowerCase().contains("music"));
    }

    /**
     * Verifies that filtering by text finds events by description or name.
     */
    @Test
    public void testFilterByDescription() {
        // Search by description
        List<Event> artEvents = filterEventsByText(allEvents, "art");

        assertEquals("Should find 1 event with 'art' in description or name", 1, artEvents.size());
        assertTrue("Should find Art Exhibition",
                artEvents.get(0).getEvent_name().toLowerCase().contains("art"));
    }


    /**
     * Helper method to filter events by registration status
     * Mimics the logic from AllEventsFragment.matchesStatus()
     */
    private List<Event> filterEventsByStatus(List<Event> events, String status) {
        List<Event> filtered = new ArrayList<>();
        Timestamp currentTime = Timestamp.now();

        for (Event e : events) {
            if (matchesStatus(e, currentTime, status)) {
                filtered.add(e);
            }
        }

        return filtered;
    }

    /**
     * Helper method to filter events by text search
     * Mimics the logic from AllEventsFragment.applyFilters()
     */
    private List<Event> filterEventsByText(List<Event> events, String query) {
        List<Event> filtered = new ArrayList<>();
        String q = query.toLowerCase();

        for (Event e : events) {
            String name = e.getEvent_name() != null ? e.getEvent_name().toLowerCase() : "";
            String desc = e.getEvent_description() != null ? e.getEvent_description().toLowerCase() : "";

            if (q.isEmpty() || name.contains(q) || desc.contains(q)) {
                filtered.add(e);
            }
        }

        return filtered;
    }

    /**
     * Helper method to check if event matches status
     * Mimics the logic from AllEventsFragment.matchesStatus()
     */
    private boolean matchesStatus(Event e, Timestamp now, String status) {
        if ("Any".equals(status)) return true;

        Timestamp rs = e.getRegistrationStart();
        Timestamp re = e.getRegistrationEnd();
        boolean hasWindow = (rs != null && re != null);

        if ("Not set".equals(status)) return !hasWindow;
        if (!hasWindow) return false;

        if ("Upcoming".equals(status)) return now.compareTo(rs) < 0;
        if ("Closed".equals(status)) return now.compareTo(re) > 0;
        if ("Open".equals(status)) return now.compareTo(rs) >= 0 && now.compareTo(re) <= 0;

        return true;
    }
}