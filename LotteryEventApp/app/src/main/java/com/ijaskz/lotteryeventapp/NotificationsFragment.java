package com.ijaskz.lotteryeventapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Notification Fragment for displaying a user's notifications
 * in a RecyclerView list.
 *
 * When the user taps a notification card, the related event is opened
 * in EventViewFragment (using the stored eventId).
 */
public class NotificationsFragment extends Fragment {

    private NotificationManager notificationManager;
    private UserManager userManager;
    private NotificationsAdapter adapter;
    private TextView emptyText;

    /**
     * Creates and inflates the view containing the RecyclerView for notifications.
     * Also initializes the adapter with a click listener to open events.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        TextView title = view.findViewById(R.id.fragment_title);
        title.setText("Notifications");

        emptyText = view.findViewById(R.id.notifications_empty);

        RecyclerView recyclerView = view.findViewById(R.id.notifications_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationsAdapter(notification -> {
            // Handle click on a notification card
            String eventId = notification.getEventId();
            if (eventId == null || eventId.isEmpty()) {
                return; // nothing to open
            }
            openEventFromId(eventId);
        });
        recyclerView.setAdapter(adapter);

        return view;
    }

    /**
     * Once the view is created, fetches the user's notifications
     * and handles empty states or disabled notification preferences.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context ctx = requireContext();
        notificationManager = new NotificationManager();
        userManager = new UserManager(ctx);

        if (!userManager.isLoggedIn()) {
            emptyText.setText("Please log in to see your notifications.");
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        final boolean notificationsEnabled = userManager.isNotificationsEnabled();
        String userId = userManager.getUserId();

        // Always load existing notifications, even if the user has opted out.
        notificationManager.getNotificationsForUser(userId, new NotificationManager.OnNotificationsLoadedListener() {
            @Override
            public void onLoaded(List<AppNotification> notifications) {
                if (notifications == null || notifications.isEmpty()) {
                    // No notifications stored yet
                    if (notificationsEnabled) {
                        emptyText.setText("You have no notifications yet.");
                    } else {
                        emptyText.setText("You have turned off notifications.");
                    }
                    emptyText.setVisibility(View.VISIBLE);
                } else {
                    // We have notifications to show
                    adapter.setNotifications(notifications);

                    if (notificationsEnabled) {
                        emptyText.setVisibility(View.GONE);
                    } else {
                        emptyText.setText("You have turned off notifications.\n" +
                                "You will not receive new notifications.");
                        emptyText.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("NotificationsFragment", "Error loading notifications", e);
                emptyText.setText("Failed to load notifications.");
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Fetches an event by ID and opens it inside EventViewFragment.
     * @param eventId Firestore document ID of the event
     */
    private void openEventFromId(String eventId) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::openEventFromDoc)
                .addOnFailureListener(e ->
                        Log.e("NotificationsFragment", "Failed to load event: " + e.getMessage()));
    }

    /**
     * Converts a Firestore event document into an Event object
     * and navigates to the detailed event view.
     * Mirrors the logic used in MainActivity.
     */
    private void openEventFromDoc(DocumentSnapshot d) {
        if (d == null || !d.exists() || !isAdded()) return;

        String description = d.getString("event_description") != null
                ? d.getString("event_description") : d.getString("description");
        String location = d.getString("location");
        String name = d.getString("event_name") != null
                ? d.getString("event_name") : d.getString("name");
        String time = d.getString("event_time") != null
                ? d.getString("event_time") : d.getString("time");
        String org_name = d.getString("organizer_name") != null
                ? d.getString("organizer_name") : d.getString("org_name");
        String image = d.getString("image") != null
                ? d.getString("image") : d.getString("imageUrl");
        Long maxL = d.getLong("max");
        int max = maxL != null ? maxL.intValue() : 0;

        Event e = new Event(description, org_name, location, name, max, time, image);
        e.setEvent_id(d.getId());
        e.setRegistrationStart(d.getTimestamp("registrationStart"));
        e.setRegistrationEnd(d.getTimestamp("registrationEnd"));
        try { e.setQrUrl(d.getString("qrUrl")); } catch (Exception ignored) {}
        try { e.setDeeplink(d.getString("deeplink")); } catch (Exception ignored) {}

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, EventViewFragment.newInstance(e))
                .addToBackStack(null)
                .commit();
    }
}
