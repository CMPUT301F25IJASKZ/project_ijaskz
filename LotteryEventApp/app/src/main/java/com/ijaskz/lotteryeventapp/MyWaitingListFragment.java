package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Fragment to display user's waiting lists
 */
public class MyWaitingListFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private MyWaitingListAdapter adapter;
    private WaitingListManager waitingListManager;
    private UserManager userManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_waiting_list, container, false);
        
        // Initialize RecyclerView and empty state
        recyclerView = view.findViewById(R.id.rvMyWaitingList);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize adapter
        adapter = new MyWaitingListAdapter(getContext());
        recyclerView.setAdapter(adapter);
        
        // Set up leave button listener
        adapter.setOnLeaveClickListener(entry -> leaveWaitingList(entry));
        
        // Set up accept button listener
        adapter.setOnAcceptClickListener(entry -> acceptInvitation(entry));
        
        // Set up decline button listener
        adapter.setOnDeclineClickListener(entry -> declineInvitation(entry));
        
        // Initialize managers
        waitingListManager = new WaitingListManager();
        userManager = new UserManager(getContext());
        
        // Load waiting lists
        loadMyWaitingLists();
        
        return view;
    }
    
    private void loadMyWaitingLists() {
        String userId = userManager.getUserId();
        
        if (userId == null) {
            Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        waitingListManager.getMyWaitingLists(userId, 
            new WaitingListManager.OnWaitingListLoadedListener() {
                @Override
                public void onLoaded(List<WaitingListEntry> entries) {
                    if (entries.isEmpty()) {
                        // Show empty state
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        // Show list
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.setWaitingListEntries(entries);
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), 
                        "Error: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void leaveWaitingList(WaitingListEntry entry) {
        String userId = userManager.getUserId();
        
        waitingListManager.leaveWaitingList(entry.getEvent_id(), userId, 
            new WaitingListManager.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), 
                        "Left waiting list", 
                        Toast.LENGTH_SHORT).show();
                    loadMyWaitingLists();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), 
                        "Error: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void acceptInvitation(WaitingListEntry entry) {
        String userId = userManager.getUserId();
        
        waitingListManager.acceptInvitation(entry.getEvent_id(), userId, 
            new WaitingListManager.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), 
                        "Invitation accepted! You're registered for this event.", 
                        Toast.LENGTH_SHORT).show();
                    loadMyWaitingLists();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), 
                        "Error: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void declineInvitation(WaitingListEntry entry) {
        String userId = userManager.getUserId();
        
        waitingListManager.declineInvitation(entry.getEvent_id(), userId, 
            new WaitingListManager.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), 
                        "Invitation declined", 
                        Toast.LENGTH_SHORT).show();
                    loadMyWaitingLists();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), 
                        "Error: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadMyWaitingLists();
    }
}
