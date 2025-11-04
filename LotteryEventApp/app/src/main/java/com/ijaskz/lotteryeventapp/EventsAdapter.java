package com.ijaskz.lotteryeventapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

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

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvDesc = itemView.findViewById(R.id.tvEventDesc);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}