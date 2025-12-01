package com.ijaskz.lotteryeventapp;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for admin notification logs.
 * Shows all notifications across users, with recipient and event info.
 */
public class AdminNotificationLogsAdapter extends RecyclerView.Adapter<AdminNotificationLogsAdapter.ViewHolder> {

    /** List of all notifications to show. */
    private final List<AppNotification> notifications = new ArrayList<>();

    /** Cache for event objects to avoid fetching the same event repeatedly. */
    private final Map<String, Event> eventCache = new HashMap<>();

    /** Firestore instance for loading event details. */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Sets the notifications list and refreshes the adapter.
     *
     * @param list the new list of notifications
     */
    public void setNotifications(List<AppNotification> list) {
        notifications.clear();
        if (list != null) {
            notifications.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification n = notifications.get(position);

        // Message body
        holder.message.setText(n.getMessage());

        // Time formatting
        Date date = new Date(n.getCreatedAt());
        String formatted = DateFormat.getMediumDateFormat(holder.itemView.getContext())
                .format(date) + " " +
                DateFormat.getTimeFormat(holder.itemView.getContext()).format(date);
        holder.time.setText(formatted);

        // Recipient
        String userId = n.getUserId();
        if (userId != null && !userId.isEmpty()) {
            holder.recipient.setText("To: " + userId);
        } else {
            holder.recipient.setText("To: (unknown)");
        }

        // Notification type
        String type = n.getType();
        if ("selected".equals(type)) {
            holder.status.setText("Type: Selected (auto)");
        } else if ("not_selected".equals(type)) {
            holder.status.setText("Type: Not selected (auto)");
        } else if ("organizer_message".equals(type)) {
            holder.status.setText("Type: Organizer message");
        } else {
            holder.status.setText("Type: " + (type != null ? type : "general"));
        }

        // Event placeholder
        String eventId = n.getEventId();
        if (eventId != null && !eventId.isEmpty()) {
            holder.eventTitle.setText("Event: " + eventId);
        } else {
            holder.eventTitle.setText("Event: (none)");
        }

        // Default image
        holder.eventImage.setImageResource(R.mipmap.ic_launcher_round);

        // Load event if needed
        if (eventId != null && !eventId.isEmpty()) {
            Event cached = eventCache.get(eventId);
            if (cached != null) {
                bindEventToHolder(cached, holder);
            } else {
                db.collection("events")
                        .document(eventId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                Event event = doc.toObject(Event.class);
                                if (event != null) {
                                    event.setEvent_id(doc.getId());
                                    eventCache.put(eventId, event);
                                    if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                                        bindEventToHolder(event, holder);
                                    }
                                }
                            }
                        });
            }
        }
    }

    /**
     * Fills in the event name and loads the event image for the row.
     *
     * @param event the event model
     * @param holder the ViewHolder for that row
     */
    private void bindEventToHolder(Event event, ViewHolder holder) {
        String title;
        if (event.getEvent_name() != null && !event.getEvent_name().isEmpty()) {
            title = "Event: " + event.getEvent_name();
        } else if (event.getEvent_id() != null && !event.getEvent_id().isEmpty()) {
            title = "Event: " + event.getEvent_id();
        } else {
            title = "Event: (unknown)";
        }
        holder.eventTitle.setText(title);

        if (event.getImage() != null && !event.getImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getImage())
                    .placeholder(R.mipmap.ic_launcher_round)
                    .into(holder.eventImage);
        } else {
            holder.eventImage.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * ViewHolder class for each notification item.
     * Stores references to the UI components used in the layout.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImage;
        TextView eventTitle;
        TextView recipient;
        TextView status;
        TextView message;
        TextView time;

        /**
         * Creates a ViewHolder for the notification row.
         *
         * @param itemView the inflated layout view
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.image_event);
            eventTitle = itemView.findViewById(R.id.text_event_title);
            recipient = itemView.findViewById(R.id.text_recipient);
            status = itemView.findViewById(R.id.text_status);
            message = itemView.findViewById(R.id.text_message);
            time = itemView.findViewById(R.id.text_time);
        }
    }
}
