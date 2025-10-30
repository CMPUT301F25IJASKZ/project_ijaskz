package com.ijaskz.lotteryeventapp;

// EventsHomeFragment.java
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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
        adapter = new EventsAdapter();
        rvEvents.setAdapter(adapter);
        // How adding an event will work
        //Event event = new Event("Basketball in gym", "gym","Basketball 5v5 training", 25, "5pm");
        //dbHelper.addEvent(event);
        //displaying the events
        dbHelper.getEvents(adapter);


        return v;
    }


}