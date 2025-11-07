package com.ijaskz.lotteryeventapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ijaskz.lotteryeventapp.R;
import com.ijaskz.lotteryeventapp.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Class defines the ManageUsersAdapter to be used by UserManagerFragment
 */
public class ManageUsersAdapter extends RecyclerView.Adapter<ManageUsersAdapter.UserVH>  {
    /**
     * defines an interface for the buttons
     */
    public interface UserActionListener {
        void onPromote(User user);
        void onDemote(User user);
        void onDelete(User user);
    }

    /**
     * defines interact for user click
     */
    public interface UserOnClickListener{
        void onUserClick(User user);
    }
    public List<User> users = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private final UserActionListener listener;
    private UserOnClickListener userListener;

    /**
     * Takes in a listener object to use later for buttons
     * @param listener
     */
    public ManageUsersAdapter(UserActionListener listener) {
        this.listener = listener;
    }

    /**
     * Takes in listener object to be used for user clicks
     * @param listener
     */
    public void setOnUserClickListener(ManageUsersAdapter.UserOnClickListener listener) {
        this.userListener = listener;
    }

    /**
     * defines/updates the user lists (filtered/unfiltered)
     * @param newUsers
     */
    public void setUsers(List<User> newUsers) {
        users.clear();
        users.addAll(newUsers);
        filteredUsers.clear();
        filteredUsers.addAll(newUsers);
        notifyDataSetChanged();
    }

    /**
     * Creates the user information holder
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return UserVH(v)
     */
    @NonNull
    @Override
    public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserVH(v);
    }

    /**
     * binds the items/buttons to list and there visability
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull UserVH holder, int position) {
        User u = filteredUsers.get(position);
        holder.tvName.setText(u.getUser_name());
        holder.tvEmail.setText(u.getUser_email());
        holder.tvType.setText(u.getUser_type());

        // show/hide promote/demote
        if ("organizer".equals(u.getUser_type())) {
            holder.btnPromote.setVisibility(View.GONE);
            holder.btnDemote.setVisibility(View.VISIBLE);
        } else { // entrant
            holder.btnPromote.setVisibility(View.VISIBLE);
            holder.btnDemote.setVisibility(View.GONE);
        }

        holder.btnPromote.setOnClickListener(v -> listener.onPromote(u));
        holder.btnDemote.setOnClickListener(v -> listener.onDemote(u));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(u));

        holder.itemView.setOnClickListener(v -> {
            if (userListener != null) userListener.onUserClick(u);
        });
    }

    /**
     * gets item count of filtered list of users
     * @return filteredUsers.size()
     */
    @Override
    public int getItemCount() {
        return filteredUsers.size();
    }

    /**
     * Does the filtering for searches if needed
     * @param query
     */
    public void getFiltered(String query){
        filteredUsers = new ArrayList<>();
        if(query.isEmpty()) {
            filteredUsers.clear();
            filteredUsers.addAll(users);
        }else {
            query = query.toLowerCase();
            filteredUsers.clear();
            for (User user : users) {
                if (user.getUser_name() != null && user.getUser_name().toLowerCase().contains(query) || (user.getUser_email() != null && user.getUser_email().toLowerCase().contains(query))) {
                    filteredUsers.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Class defines UserVH that holds all the user info
     */
    static class UserVH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvType;
        Button btnPromote, btnDemote, btnDelete;

        public UserVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvType = itemView.findViewById(R.id.tvType);
            btnPromote = itemView.findViewById(R.id.btnPromote);
            btnDemote = itemView.findViewById(R.id.btnDemote);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}