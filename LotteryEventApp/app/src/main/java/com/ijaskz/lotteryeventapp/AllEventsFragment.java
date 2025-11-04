package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AllEventsFragment extends Fragment {

    private RecyclerView rvEvents;
    private EventsAdapter adapter;
    private final FireStoreHelper helper = new FireStoreHelper();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_all_events, container, false);

        rvEvents = v.findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new EventsAdapter();
        rvEvents.setAdapter(adapter);
        helper.displayEvents(adapter);

        return v;
    }
}
