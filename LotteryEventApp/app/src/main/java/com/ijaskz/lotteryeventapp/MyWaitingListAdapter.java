package com.ijaskz.lotteryeventapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying waiting list entries with section headers
 * Organizes entries into active, registered, and past categories
 */
public class MyWaitingListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ENTRY = 1;
    
    private Context context;
    private List<Object> items = new ArrayList<>();  // Can hold String (header) or WaitingListEntry
    private OnLeaveClickListener leaveClickListener;
    private OnAcceptClickListener acceptClickListener;
    private OnDeclineClickListener declineClickListener;


    /**
     * Creates adapter for waiting list items
     * @param context application context
     */
    public MyWaitingListAdapter(Context context) {
        this.context = context;
    }
    /**
     * Updates the list with new waiting list entries
     * Automatically organizes them into sections with headers
     * @param entries list of waiting list entries to display
     */
    public void setWaitingListEntries(List<WaitingListEntry> entries) {
        this.items = organizeEntriesWithHeaders(entries);
        notifyDataSetChanged();
    }
    /**
     * Groups entries into sections based on their status
     * Creates headers for Active, Registered, and Past sections
     * @param entries raw list of waiting list entries
     * @return organized list with headers and entries mixed
     */
    private List<Object> organizeEntriesWithHeaders(List<WaitingListEntry> entries) {
        List<Object> organized = new ArrayList<>();
        
        // Group entries by category
        List<WaitingListEntry> active = new ArrayList<>();
        List<WaitingListEntry> registered = new ArrayList<>();
        List<WaitingListEntry> past = new ArrayList<>();
        
        for (WaitingListEntry entry : entries) {
            String status = entry.getStatus();
            if ("waiting".equals(status) || "selected".equals(status)) {
                active.add(entry);
            } else if ("accepted".equals(status) || "enrolled".equals(status)) {
                registered.add(entry);
            } else {
                past.add(entry);
            }
        }
        
        // Add sections with headers
        if (!active.isEmpty()) {
            organized.add("Active Waiting Lists");
            organized.addAll(active);
        }
        
        if (!registered.isEmpty()) {
            organized.add("Registered Events");
            organized.addAll(registered);
        }
        
        if (!past.isEmpty()) {
            organized.add("Past Events");
            organized.addAll(past);
        }
        
        return organized;
    }
    /**
     * Sets listener for when user clicks leave button
     * @param listener callback for leave actions
     */
    public void setOnLeaveClickListener(OnLeaveClickListener listener) {
        this.leaveClickListener = listener;
    }
    /**
     * Sets listener for when user accepts an invitation
     * @param listener callback for accept actions
     */
    public void setOnAcceptClickListener(OnAcceptClickListener listener) {
        this.acceptClickListener = listener;
    }
    /**
     * Sets listener for when user declines an invitation
     * @param listener callback for decline actions
     */
    public void setOnDeclineClickListener(OnDeclineClickListener listener) {
        this.declineClickListener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? VIEW_TYPE_HEADER : VIEW_TYPE_ENTRY;
    }

    /**
     * Creates appropriate ViewHolder depending on the view type
     * @param parent parent view group
     * @param viewType header or entry type
     * @return a new ViewHolder
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waiting_list, parent, false);
            return new EntryViewHolder(view);
        }
    }
    /**
     * Binds data to header or entry view holders
     * @param holder ViewHolder to bind to
     * @param position index in combined list
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            String header = (String) items.get(position);
            ((HeaderViewHolder) holder).tvSectionHeader.setText(header);
            return;
        }
        
        EntryViewHolder entryHolder = (EntryViewHolder) holder;
        WaitingListEntry entry = (WaitingListEntry) items.get(position);
        
        // Set default image (launcher icon)
        entryHolder.ivEventImage.setImageResource(R.mipmap.ic_launcher);
        
        // Display event ID (can be replaced with event name later)
        entryHolder.tvEventId.setText("Event: " + entry.getEvent_id());
        
        // Display status with color coding
        String status = entry.getStatus();
        entryHolder.tvStatus.setText("Status: " + capitalizeFirst(status));
        
        // Color code by status
        switch (status) {
            case "waiting":
                entryHolder.tvStatus.setTextColor(Color.GRAY);
                break;
            case "selected":
                entryHolder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                break;
            case "accepted":
                entryHolder.tvStatus.setTextColor(Color.parseColor("#2196F3"));
                break;
            case "declined":
                entryHolder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
                break;
            case "cancelled":
                entryHolder.tvStatus.setTextColor(Color.parseColor("#F44336"));
                break;
            case "enrolled":
                entryHolder.tvStatus.setTextColor(Color.parseColor("#1B5E20"));
                break;
        }
        
        // Display date joined
        entryHolder.tvJoinedDate.setText("Joined: " + formatDate(entry.getJoined_at()));
        
        // Show leave button only for "waiting" status
        if ("waiting".equals(status)) {
            entryHolder.btnLeave.setVisibility(View.VISIBLE);
            entryHolder.layoutAcceptDecline.setVisibility(View.GONE);
            entryHolder.btnLeave.setOnClickListener(v -> {
                if (leaveClickListener != null) {
                    leaveClickListener.onLeaveClick(entry);
                }
            });
        } else if ("selected".equals(status)) {
            // Show accept/decline buttons for "selected" status
            entryHolder.btnLeave.setVisibility(View.GONE);
            entryHolder.layoutAcceptDecline.setVisibility(View.VISIBLE);
            entryHolder.btnAccept.setOnClickListener(v -> {
                if (acceptClickListener != null) {
                    acceptClickListener.onAcceptClick(entry);
                }
            });
            entryHolder.btnDecline.setOnClickListener(v -> {
                if (declineClickListener != null) {
                    declineClickListener.onDeclineClick(entry);
                }
            });
        } else {
            // Hide all buttons for other statuses
            entryHolder.btnLeave.setVisibility(View.GONE);
            entryHolder.layoutAcceptDecline.setVisibility(View.GONE);
        }
    }

    /**
     * @return total count of mixed header and entry items
     */
    @Override
    public int getItemCount() {
        return items.size();
    }
    /**
     * ViewHolder for section header rows
     */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectionHeader;
        
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSectionHeader = itemView.findViewById(R.id.tvSectionHeader);
        }
    }
    /**
     * ViewHolder for waiting list entry rows
     * Handles different button states based on entry status
     */
    static class EntryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventImage;
        TextView tvEventId;
        TextView tvStatus;
        TextView tvJoinedDate;
        Button btnLeave;
        ViewGroup layoutAcceptDecline;
        Button btnAccept;
        Button btnDecline;
        
        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.ivEventImage);
            tvEventId = itemView.findViewById(R.id.tvEventId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvJoinedDate = itemView.findViewById(R.id.tvJoinedDate);
            btnLeave = itemView.findViewById(R.id.btnLeave);
            layoutAcceptDecline = itemView.findViewById(R.id.layoutAcceptDecline);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
    /**
     * Formats timestamp into readable date string
     * @param timestamp milliseconds since epoch
     * @return formatted date like "Nov 07, 2024"
     */
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    /**
     * Capitalizes first letter of a string
     * @param str input string
     * @return string with first letter capitalized
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    /**
     * Callback for when user wants to leave a waiting list
     */
    public interface OnLeaveClickListener {
        void onLeaveClick(WaitingListEntry entry);
    }
    /**
     * Callback for when user accepts an event invitation
     */
    public interface OnAcceptClickListener {
        void onAcceptClick(WaitingListEntry entry);
    }
    /**
     * Callback for when user declines an event invitation
     */
    public interface OnDeclineClickListener {
        void onDeclineClick(WaitingListEntry entry);
    }
}
