package com.ijaskz.lotteryeventapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private String userType;

    // Constructor to accept userType
    public EventsAdapter(String userType) {
        this.userType = userType;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event e = events.get(position);
        holder.tvName.setText(e.getEvent_name());
        holder.tvDesc.setText(e.getEvent_description());

        // Load image
        String imageUrl = e.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()
                && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgView);
        } else {
            holder.imgView.setImageResource(android.R.drawable.zoom_plate);
        }

        // Change icon based on user type
        if ("organizer".equals(userType) || "admin".equals(userType)) {
            holder.btnMore.setImageResource(android.R.drawable.ic_menu_edit);
        } else {
            holder.btnMore.setImageResource(android.R.drawable.ic_menu_add);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        ImageButton btnMore;
        ImageView imgView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvDesc = itemView.findViewById(R.id.tvEventDesc);
            btnMore = itemView.findViewById(R.id.btnMore);
            imgView = itemView.findViewById(R.id.imgView);
        }
    }
}