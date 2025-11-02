package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private MyWaitingListAdapter adapter;
    private WaitingListManager waitingListManager;
    private UserManager userManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_waiting_list, container, false);
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rvMyWaitingList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize adapter
        adapter = new MyWaitingListAdapter(getContext());
        recyclerView.setAdapter(adapter);
        
        // Set up leave button listener
        adapter.setOnLeaveClickListener(entry -> leaveWaitingList(entry));
        
        // Initialize managers
        waitingListManager = new WaitingListManager();
        userManager = new UserManager(getContext());
        
        // Load waiting lists
        loadMyWaitingLists();
        
        // TEMPORARY: Uncomment this to add test data
         addTestWaitingListEntry();
        
        return view;
    }
    
    /**
     * TEMPORARY METHOD FOR TESTING
     * Adds a test waiting list entry to Firestore
     * 
     * TO REMOVE THIS FEATURE:
     * Comment out or delete the line: addTestWaitingListEntry();
     * in the onCreateView() method above (around line 52)
     */
    private void addTestWaitingListEntry() {
        String userId = userManager.getUserId();
        String userName = userManager.getUserName();
        String userEmail = userManager.getUserEmail();
        
        if (userId == null) {
            Toast.makeText(getContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Add a test entry
        waitingListManager.joinWaitingList(
            "test_event_123",  // Test event ID
            userId,
            userName != null ? userName : "Test User",
            userEmail != null ? userEmail : "test@example.com",
            new WaitingListManager.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), 
                        "Test entry added! Refresh to see it. Remove line 52 or addTestWaitingListEntry() in MyWaitingListFragment.java to remove this event.", 
                        Toast.LENGTH_LONG).show();
                    loadMyWaitingLists();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), 
                        "Error adding test entry: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
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
                        Toast.makeText(getContext(), 
                            "You're not on any waiting lists yet", 
                            Toast.LENGTH_SHORT).show();
                    }
                    adapter.setWaitingListEntries(entries);
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
    
    @Override
    public void onResume() {
        super.onResume();
        loadMyWaitingLists();
    }
}
