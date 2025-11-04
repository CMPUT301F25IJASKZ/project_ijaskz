package com.ijaskz.lotteryeventapp;

// EventsHomeFragment.java
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.time.LocalTime;

public class EventsHomeFragment extends Fragment {
    private RecyclerView rvEvents;
    private EventsAdapter adapter;
    private FireStoreHelper dbHelper = new FireStoreHelper();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.events_home, container, false);
        rvEvents = v.findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        UserManager userManager = new UserManager(getContext());
        String userType = userManager.getUserType();
        EventsAdapter adapter = new EventsAdapter(userType);
        rvEvents.setAdapter(adapter);
        // How adding an event will work
        //Event event = new Event("Basketball in gym", "gym","Basketball 5v5 training", 25, LocalTime);
        //dbHelper.addEvent(event);
        //displaying the events
        dbHelper.displayEvents(adapter);


        return v;
    }


}