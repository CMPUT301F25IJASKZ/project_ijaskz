package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class EventViewFragment extends Fragment {

    private Event event;
    private ImageView imgEvent;
    private TextView tvEventName, tvEventDescription, tvEventTime, tvEventLocation, tvEventMax;
    private Button btnJoinWaitlist;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserManager userManager;

    private WaitingListManager waitingListManager;

    public static EventViewFragment newInstance(Event event) {
        EventViewFragment fragment = new EventViewFragment();
        Bundle args = new Bundle();
        args.putSerializable("event", event);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_view, container, false);

        userManager = new UserManager(getContext());

        // Get event from arguments
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable("event");
        }

        // Initialize views
        imgEvent = view.findViewById(R.id.imgEvent);
        tvEventName = view.findViewById(R.id.tvEventName);
        tvEventDescription = view.findViewById(R.id.tvEventDescription);
        tvEventTime = view.findViewById(R.id.tvEventTime);
        tvEventLocation = view.findViewById(R.id.tvEventLocation);
        tvEventMax = view.findViewById(R.id.tvEventMax);
        btnJoinWaitlist = view.findViewById(R.id.btnJoinWaitlist);

        // Populate data
        if (event != null) {
            populateEventDetails();
        }

        // Set up button click
        btnJoinWaitlist.setOnClickListener(v -> joinWaitlist());

        return view;
    }

    private void populateEventDetails() {
        tvEventName.setText(event.getEvent_name());
        tvEventDescription.setText(event.getEvent_description());
        tvEventTime.setText("Time: " + event.getEvent_time());
        tvEventLocation.setText("Location: " + event.getLocation());
        tvEventMax.setText("Max Participants: " + event.getMax());

        // Load image
        String imageUrl = event.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()
                && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imgEvent);
        } else {
            imgEvent.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
    private void checkWaitlistStatus() {
        String userId = userManager.getUserId();
        String eventId = event.getEvent_id();

        if (userId == null || eventId == null) return;

        // Check if user is already on waitlist
        waitingListManager.isOnWaitingList(eventId, userId, isOnList -> {
            if (isOnList) {
                btnJoinWaitlist.setEnabled(false);
                btnJoinWaitlist.setText("Already on Waitlist");

                // Get and display status
                waitingListManager.getWaitingListStatus(eventId, userId, status -> {
                    if (status != null && !status.equals("waiting")) {
                        btnJoinWaitlist.setText("Status: " + capitalizeFirst(status));
                    }
                });
            }
        });
    }
    private void joinWaitlist() {
        String userId = userManager.getUserId();
        String userName = userManager.getUserName();
        String userEmail = userManager.getUserEmail();
        String eventId = event.getEvent_id();

        if (userId == null || eventId == null) {
            Toast.makeText(getContext(), "Error: Unable to join waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        btnJoinWaitlist.setEnabled(false);

        // Use WaitingListManager to join waitlist
        waitingListManager.joinWaitingList(eventId, userId, userName, userEmail,
                new WaitingListManager.OnCompleteListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "Successfully joined waitlist!", Toast.LENGTH_SHORT).show();
                        btnJoinWaitlist.setText("Joined Waitlist");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        btnJoinWaitlist.setEnabled(true);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}