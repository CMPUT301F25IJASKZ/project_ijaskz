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

import java.util.List;

/**
 * Notification Fragment for displaying a user's notifications
 * in a RecyclerView list.
 */
public class NotificationsFragment extends Fragment {

    private NotificationManager notificationManager;
    private UserManager userManager;
    private NotificationsAdapter adapter;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Use our new layout
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        TextView title = view.findViewById(R.id.fragment_title);
        title.setText("Notifications");

        emptyText = view.findViewById(R.id.notifications_empty);

        RecyclerView recyclerView = view.findViewById(R.id.notifications_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationsAdapter();
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context ctx = requireContext();
        notificationManager = new NotificationManager();
        userManager = new UserManager(ctx);

        String userId = userManager.getUserId();

        if (userId == null) {
            emptyText.setText("Please log in to see your notifications.");
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        notificationManager.getNotificationsForUser(userId, new NotificationManager.OnNotificationsLoadedListener() {
            @Override
            public void onLoaded(List<AppNotification> notifications) {
                if (notifications == null || notifications.isEmpty()) {
                    emptyText.setText("You have no notifications yet.");
                    emptyText.setVisibility(View.VISIBLE);
                } else {
                    emptyText.setVisibility(View.GONE);
                }
                adapter.setNotifications(notifications);
            }

            @Override
            public void onError(Exception e) {
                Log.e("NotificationsFragment", "Error loading notifications", e);
                emptyText.setText("Failed to load notifications.");
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }
}
