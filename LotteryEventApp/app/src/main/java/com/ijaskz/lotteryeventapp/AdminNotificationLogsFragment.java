package com.ijaskz.lotteryeventapp;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Admin-only fragment that shows a log of all notifications in the system.
 * Uses the same "notifications" collection as user notifications.
 */
public class AdminNotificationLogsFragment extends Fragment {

    private AdminNotificationLogsAdapter adapter;
    private TextView emptyText;
    private FirebaseFirestore db;
    private UserManager userManager;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_admin_notification_logs, container, false);

        TextView title = view.findViewById(R.id.fragment_title_admin);
        title.setText("Notification Logs");

        emptyText = view.findViewById(R.id.logs_empty);

        RecyclerView recyclerView = view.findViewById(R.id.logs_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminNotificationLogsAdapter();
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userManager = new UserManager(requireContext());

        return view;
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        String userType = userManager.getUserType();
        if (userType == null || !"admin".equalsIgnoreCase(userType)) {
            emptyText.setText("You do not have permission to view notification logs.");
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        loadNotificationLogs();
    }
    /**
     * Loads all notification documents from Firestore,
     * converts them into AppNotification objects,
     * sorts them, and updates the adapter.
     */
    private void loadNotificationLogs() {
        db.collection("notifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AppNotification> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        AppNotification n = doc.toObject(AppNotification.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            list.add(n);
                        }
                    }

                    // Sort by createdAt descending (newest first)
                    Collections.sort(list, new Comparator<AppNotification>() {
                        @Override
                        public int compare(AppNotification a, AppNotification b) {
                            return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                        }
                    });

                    if (list.isEmpty()) {
                        emptyText.setText("No notifications found.");
                        emptyText.setVisibility(View.VISIBLE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                    }

                    adapter.setNotifications(list);
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminNotificationLogs", "Failed to load logs", e);
                    emptyText.setText("Failed to load notification logs.");
                    emptyText.setVisibility(View.VISIBLE);
                });
    }
}
