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
 * Adapter for displaying waiting list entries
 * Similar to EventsAdapter pattern
 */
public class MyWaitingListAdapter extends RecyclerView.Adapter<MyWaitingListAdapter.ViewHolder> {
    
    private Context context;
    private List<WaitingListEntry> entries = new ArrayList<>();
    private OnLeaveClickListener leaveClickListener;
    
    public MyWaitingListAdapter(Context context) {
        this.context = context;
    }
    
    public void setWaitingListEntries(List<WaitingListEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }
    
    public void setOnLeaveClickListener(OnLeaveClickListener listener) {
        this.leaveClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_waiting_list, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaitingListEntry entry = entries.get(position);
        
        // Set default image (launcher icon)
        holder.ivEventImage.setImageResource(R.mipmap.ic_launcher);
        
        // Display event ID (can be replaced with event name later)
        holder.tvEventId.setText("Event: " + entry.getEvent_id());
        
        // Display status with color coding
        String status = entry.getStatus();
        holder.tvStatus.setText("Status: " + capitalizeFirst(status));
        
        // Color code by status
        switch (status) {
            case "waiting":
                holder.tvStatus.setTextColor(Color.GRAY);
                break;
            case "selected":
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                break;
            case "accepted":
                holder.tvStatus.setTextColor(Color.parseColor("#2196F3"));
                break;
            case "declined":
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
                break;
            case "cancelled":
                holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
                break;
            case "enrolled":
                holder.tvStatus.setTextColor(Color.parseColor("#1B5E20"));
                break;
        }
        
        // Display date joined
        holder.tvJoinedDate.setText("Joined: " + formatDate(entry.getJoined_at()));
        
        // Show leave button only for "waiting" status
        if ("waiting".equals(status)) {
            holder.btnLeave.setVisibility(View.VISIBLE);
            holder.btnLeave.setOnClickListener(v -> {
                if (leaveClickListener != null) {
                    leaveClickListener.onLeaveClick(entry);
                }
            });
        } else {
            holder.btnLeave.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return entries.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventImage;
        TextView tvEventId;
        TextView tvStatus;
        TextView tvJoinedDate;
        Button btnLeave;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.ivEventImage);
            tvEventId = itemView.findViewById(R.id.tvEventId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvJoinedDate = itemView.findViewById(R.id.tvJoinedDate);
            btnLeave = itemView.findViewById(R.id.btnLeave);
        }
    }
    
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    public interface OnLeaveClickListener {
        void onLeaveClick(WaitingListEntry entry);
    }
}
