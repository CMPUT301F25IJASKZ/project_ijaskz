package com.ijaskz.lotteryeventapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Defines the EventsAdapter to display them with a RecyclerView
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private final String userType;
    private final LinearLayout pic1;
    private final LinearLayout pic2;

    private OnEventClickListener listener;

    /**
     * interface to defines the listeners
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);   // row tap -> view/join
        void onEditClick(Event event);    // pencil tap -> edit
    }

    public EventsAdapter(String userType) {
        this.userType = userType;
        this.pic1 = null;
        this.pic2 = null;
    }

    public EventsAdapter(String userType, LinearLayout pic1, LinearLayout pic2) {
        this.userType = userType;
        this.pic1 = pic1;
        this.pic2 = pic2;
    }

    /**
     * Sets the listener in adapter
     * @param listener The click listener
     */
    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    /**
     * Defines all events to be displayed
     * @param events the list of current events in database
     */
    public void setEvents(List<Event> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * creating the holder of event info to be displayed
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return v The View holder for events
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(v);
    }

    /**
     * Bind The Information to the holder and where it will be set
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event e = events.get(position);
        holder.tvName.setText(e.getEvent_name());
        holder.tvDesc.setText(e.getEvent_description());

        // Registration window gating
        Timestamp rs = e.getRegistrationStart();
        Timestamp re = e.getRegistrationEnd();
        Timestamp now = Timestamp.now();

        boolean hasWindow = (rs != null && re != null);
        boolean isOpen = hasWindow && now.compareTo(rs) >= 0 && now.compareTo(re) <= 0;
        if (holder.tvRegStatus != null) {
            if (!hasWindow) {
                holder.tvRegStatus.setText("Registration: not set");
            } else if (now.compareTo(rs) < 0) {
                holder.tvRegStatus.setText("Registration: upcoming");
            } else if (now.compareTo(re) > 0) {
                holder.tvRegStatus.setText("Registration: closed");
            } else {
                holder.tvRegStatus.setText("Registration: open");
            }
        }

        if (holder.tvRegWindow != null) {
            if (hasWindow) {
                holder.tvRegWindow.setText("Opens: " + fmt(rs) + "  •  Closes: " + fmt(re));
                holder.tvRegWindow.setVisibility(View.VISIBLE);
            } else {
                holder.tvRegWindow.setText("");
                holder.tvRegWindow.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(e);
        });

        // Who can edit?
        boolean canEdit = "organizer".equals(userType) || "admin".equals(userType);

        // Button icon
        if (canEdit) {
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.btnMore.setImageResource(android.R.drawable.ic_menu_edit);
            holder.btnMore.setEnabled(true);
            holder.btnMore.setAlpha(1f);
            holder.btnMore.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(e);
            });
        } else {
            // For entrants
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.btnMore.setImageResource(android.R.drawable.ic_menu_add);
            holder.btnMore.setEnabled(isOpen);
            holder.btnMore.setAlpha(isOpen ? 1f : 0.5f);
            holder.btnMore.setOnClickListener(v -> {
                if (listener != null && isOpen) listener.onEventClick(e);
            });
        }

        String imageUrl = e.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()
                && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgView);

            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == 0 && pic1 != null) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .into(new com.bumptech.glide.request.target.ViewTarget<LinearLayout, android.graphics.drawable.Drawable>(pic1) {
                            @Override
                            public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource,
                                                        @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                                pic1.setBackground(resource);
                            }
                        });
            } else if (adapterPos == 1 && pic2 != null) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .into(new com.bumptech.glide.request.target.ViewTarget<LinearLayout, android.graphics.drawable.Drawable>(pic2) {
                            @Override
                            public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource,
                                                        @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                                pic2.setBackground(resource);
                            }
                        });
            }
        } else {
            holder.imgView.setImageResource(android.R.drawable.zoom_plate);
        }
    }

    /**
     * get amount of events being displayed
     * @return events size # of events
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Formatting date info
     * @param ts The date unformatted
     * @return date formatted
     */
    private String fmt(Timestamp ts) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(ts.toDate());
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        ImageButton btnMore;
        ImageView imgView;
        TextView tvRegStatus;
        TextView tvRegWindow;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvDesc = itemView.findViewById(R.id.tvEventDesc);
            btnMore = itemView.findViewById(R.id.btnMore);
            imgView = itemView.findViewById(R.id.imgView);
            tvRegStatus = itemView.findViewById(R.id.tv_reg_status);
            tvRegWindow = itemView.findViewById(R.id.tv_reg_window);
        }
    }
}
