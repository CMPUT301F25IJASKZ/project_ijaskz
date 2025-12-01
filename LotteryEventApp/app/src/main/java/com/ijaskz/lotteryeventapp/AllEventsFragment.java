package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.text.Editable;
import android.text.TextWatcher;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;


/**
 * Class that defines the AllEventsFragment to show all the events.
 *
 * Real-time search and status filtering.
 * Users can search by event name or description
 * and filter by registration status.
 */
public class AllEventsFragment extends Fragment {

    private RecyclerView rvEvents;
    private EventsAdapter adapter;
    private final FireStoreHelper helper = new FireStoreHelper();
    private ListenerRegistration reg;
    private EditText etFilterQuery;
    private Spinner spFilterStatus;

    /**
     * Stores all events before filters are applied.
     */
    private final List<Event> allEvents = new ArrayList<>();
    private String currentQuery = "";
    private String currentStatus = "Any";
    private ListenerRegistration reg2;

    /**
     * Creating the all events fragment view.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_all_events, container, false);

        rvEvents = v.findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Organizer-specific UI controls
        LinearLayout pic1 = v.findViewById(R.id.pic1);
        LinearLayout pic2 = v.findViewById(R.id.pic2);
        UserManager userManager = new UserManager(requireContext());
        String userType = userManager.getUserType();

        adapter = new EventsAdapter(userType, pic1, pic2);
        rvEvents.setAdapter(adapter);

        // Listen for event updates
        reg = helper.listenToEvents(adapter);

        // Search filter input
        etFilterQuery = v.findViewById(R.id.et_filter_query);
        spFilterStatus = v.findViewById(R.id.sp_filter_status);

        // Status filter setup
        if (spFilterStatus != null) {
            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{"Any", "Open", "Upcoming", "Closed", "Not set"}
            );
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spFilterStatus.setAdapter(statusAdapter);

            spFilterStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    currentStatus = (String) parent.getItemAtPosition(position);
                    applyFilters();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        // Text search listener
        if (etFilterQuery != null) {
            etFilterQuery.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s != null ? s.toString().trim() : "";
                    applyFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Real-time listener to keep `allEvents` updated
        reg2 = FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    List<Event> tmp = new ArrayList<>();
                    for (DocumentSnapshot doc : snap) {
                        Event ev = doc.toObject(Event.class);
                        if (ev != null) {
                            ev.setEvent_id(doc.getId());
                            tmp.add(ev);
                        }
                    }
                    allEvents.clear();
                    allEvents.addAll(tmp);
                    applyFilters();
                });

        // Click handlers for viewing or editing events
        adapter.setOnEventClickListener(new EventsAdapter.OnEventClickListener() {

            /**
             * Opens the event details page.
             */
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

            /**
             * Opens the edit event page.
             */
            @Override
            public void onEditClick(Event event) {
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

    /**
     * Applies the current search text and status filter to the event list.
     */
    private void applyFilters() {
        if (adapter == null) return;

        String q = currentQuery.toLowerCase();
        Timestamp now = Timestamp.now();

        UserManager userManager = new UserManager(requireContext());
        String userType = userManager.getUserType();
        String userName = userManager.getUserName();

        List<Event> filtered = new ArrayList<>();
        for (Event e : allEvents) {

            // Filter organizer's own events
            if ("organizer".equals(userType)) {
                String eventOrganizerName = e.getOrganizer_name();
                if (eventOrganizerName == null || !eventOrganizerName.equals(userName)) {
                    continue;
                }
            }

            String name = e.getEvent_name() != null ? e.getEvent_name().toLowerCase() : "";
            String desc = e.getEvent_description() != null ? e.getEvent_description().toLowerCase() : "";
            boolean textOk = q.isEmpty() || name.contains(q) || desc.contains(q);
            boolean statusOk = matchesStatus(e, now, currentStatus);

            if (textOk && statusOk) filtered.add(e);
        }
        adapter.setEvents(filtered);
    }

    /**
     * Checks if an event matches the chosen registration status.
     */
    private boolean matchesStatus(Event e, Timestamp now, String status) {
        if ("Any".equals(status)) return true;

        Timestamp rs = e.getRegistrationStart();
        Timestamp re = e.getRegistrationEnd();
        boolean hasWindow = (rs != null && re != null);

        if ("Not set".equals(status)) return !hasWindow;
        if (!hasWindow) return false;

        if ("Upcoming".equals(status)) return now.compareTo(rs) < 0;
        if ("Closed".equals(status))   return now.compareTo(re) > 0;
        if ("Open".equals(status))     return now.compareTo(rs) >= 0 && now.compareTo(re) <= 0;

        return true;
    }

    /**
     * Cleans up Firestore listeners when leaving the fragment.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) {
            reg.remove();
            reg = null;
        }
        if (reg2 != null) {
            reg2.remove();
            reg2 = null;
        }
        rvEvents.setAdapter(null);
    }
}
