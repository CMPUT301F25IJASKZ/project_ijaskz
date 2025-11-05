package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

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

        // 🔹 Find your two LinearLayouts from the layout
        LinearLayout pic1 = v.findViewById(R.id.pic1);
        LinearLayout pic2 = v.findViewById(R.id.pic2);

        // 🔹 Pass them into the adapter constructor
        adapter = new EventsAdapter("guest", pic1, pic2);

        rvEvents.setAdapter(adapter);

        // 🔹 Load events
        helper.displayEvents(adapter);

        return v;
    }
}
