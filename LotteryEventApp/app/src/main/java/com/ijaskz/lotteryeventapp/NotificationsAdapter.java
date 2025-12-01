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
 * Adapter for showing notifications to the user in a RecyclerView.
 * Each row displays message, event info, status, and time.
 * Also supports clicking a notification to open the related event.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    /**
     * Listener for when a notification card is tapped.
     */
    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    private final List<AppNotification> notifications = new ArrayList<>();
    private final Map<String, Event> eventCache = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final OnNotificationClickListener clickListener;

    /**
     * Creates adapter and stores a click listener for notification taps.
     * @param listener callback to open an event when a notification is clicked
     */
    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Updates adapter with a new list of notifications.
     * @param list list of AppNotification items
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

    /**
     * Binds each notification's UI content into its card.
     */
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

        // Status text
        String type = n.getType();
        if ("selected".equals(type) || "selection".equals(type)) {
            holder.status.setText("Status: Selected");
        } else if ("not_selected".equals(type)) {
            holder.status.setText("Status: Not selected");
        } else {
            holder.status.setText("Status: " + n.getTitle());
        }

        // Show event id until event details load
        String eventId = n.getEventId();
        if (eventId != null && !eventId.isEmpty()) {
            holder.eventTitle.setText("Event: " + eventId);
        } else {
            holder.eventTitle.setText("Event");
        }

        // Default placeholder image
        holder.eventImage.setImageResource(R.mipmap.ic_launcher_round);

        // Load event info if available (cached or fetched)
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

        // Click listener to open event page
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNotificationClick(n);
            }
        });
    }

    /**
     * Fills event name and event image once the event is loaded.
     * @param event event object fetched from Firestore
     * @param holder UI row to update
     */
    private void bindEventToHolder(Event event, ViewHolder holder) {
        // Event title
        String title;
        if (event.getEvent_name() != null && !event.getEvent_name().isEmpty()) {
            title = "Event: " + event.getEvent_name();
        } else if (event.getEvent_id() != null && !event.getEvent_id().isEmpty()) {
            title = "Event: " + event.getEvent_id();
        } else {
            title = "Event";
        }
        holder.eventTitle.setText(title);

        // Event image
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
     * ViewHolder for a single notification card.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImage;
        TextView eventTitle;
        TextView status;
        TextView message;
        TextView time;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.image_event);
            eventTitle = itemView.findViewById(R.id.text_event_title);
            status = itemView.findViewById(R.id.text_status);
            message = itemView.findViewById(R.id.text_message);
            time = itemView.findViewById(R.id.text_time);
        }
    }
}
