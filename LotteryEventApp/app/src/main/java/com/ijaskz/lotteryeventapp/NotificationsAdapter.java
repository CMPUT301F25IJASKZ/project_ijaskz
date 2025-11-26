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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying AppNotification items in a RecyclerView,
 * styled similarly to the Waiting List / Event History cards.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private final List<AppNotification> notifications = new ArrayList<>();
    private final Map<String, Event> eventCache = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

        // Message and time
        holder.message.setText(n.getMessage());

        Date date = new Date(n.getCreatedAt());
        String formatted = DateFormat.getMediumDateFormat(holder.itemView.getContext())
                .format(date) + " " +
                DateFormat.getTimeFormat(holder.itemView.getContext()).format(date);
        holder.time.setText(formatted);

        // Since these notifications are for "you were selected", we show that as status
        holder.status.setText("Status: Selected");

        // Default text based on event id (until we load the Event)
        String eventId = n.getEventId();
        if (eventId != null && !eventId.isEmpty()) {
            holder.eventTitle.setText("Event: " + eventId);
        } else {
            holder.eventTitle.setText("Event");
        }

        // Default image placeholder
        holder.eventImage.setImageResource(R.mipmap.ic_launcher_round);

        // If we have an event id, try to load the event details (name + image)
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
                                    // Make sure we’re still binding the same item
                                    if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                                        bindEventToHolder(event, holder);
                                    }
                                }
                            }
                        });
            }
        }
    }

    private void bindEventToHolder(Event event, ViewHolder holder) {
        // Show event name if available, otherwise id
        String title;
        if (event.getEvent_name() != null && !event.getEvent_name().isEmpty()) {
            title = "Event: " + event.getEvent_name();
        } else if (event.getEvent_id() != null && !event.getEvent_id().isEmpty()) {
            title = "Event: " + event.getEvent_id();
        } else {
            title = "Event";
        }
        holder.eventTitle.setText(title);

        // Load image from event.getImage() if present
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
