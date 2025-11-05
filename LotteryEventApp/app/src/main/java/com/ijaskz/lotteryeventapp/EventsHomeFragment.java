package com.ijaskz.lotteryeventapp;

// EventsHomeFragment.java
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

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
        LinearLayout pic1 = v.findViewById(R.id.pic1);
        LinearLayout pic2 = v.findViewById(R.id.pic2);
        rvEvents = v.findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        UserManager userManager = new UserManager(getContext());
        String userType = userManager.getUserType();
        EventsAdapter adapter = new EventsAdapter(userType, pic1, pic2);
        rvEvents.setAdapter(adapter);
        dbHelper.displayEvents(adapter);

        adapter.setOnEventClickListener(new EventsAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                // Open the VIEW / Join Waitlist page
                Bundle b = new Bundle();
                b.putSerializable("event", event);
                EventViewFragment fragment = new EventViewFragment();
                fragment.setArguments(b);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onEditClick(Event event) {
                Bundle b = new Bundle();
                b.putSerializable("event", event);
                EditEventFragment fragment = new EditEventFragment();
                fragment.setArguments(b);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });



        return v;
    }


}