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

import com.google.firebase.firestore.ListenerRegistration;

public class AllEventsFragment extends Fragment {

    private RecyclerView rvEvents;
    private EventsAdapter adapter;
    private final FireStoreHelper helper = new FireStoreHelper();
    private ListenerRegistration reg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_all_events, container, false);

        rvEvents = v.findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));

        //  Find your two LinearLayouts from the layout
        LinearLayout pic1 = v.findViewById(R.id.pic1);
        LinearLayout pic2 = v.findViewById(R.id.pic2);

        //  Pass them into the adapter constructor
        adapter = new EventsAdapter("guest", pic1, pic2);
        rvEvents.setAdapter(adapter);

        // 🔹Load events
       reg =  helper.listenToEvents(adapter);

        // Handle both row click (view) and pencil click (edit)
        adapter.setOnEventClickListener(new EventsAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {

                Bundle bundle = new Bundle();
                bundle.putSerializable("event", event);

                EventViewFragment fragment = new EventViewFragment();
                fragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onEditClick(Event event) {
                // Open the EDIT page for this event
                Bundle bundle = new Bundle();
                bundle.putSerializable("event", event);

                EditEventFragment fragment = new EditEventFragment();
                fragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        return v;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) {
            reg.remove();     // stop listening to avoid leaks + duplicate updates
            reg = null;
        }
        rvEvents.setAdapter(null);
    }
}
