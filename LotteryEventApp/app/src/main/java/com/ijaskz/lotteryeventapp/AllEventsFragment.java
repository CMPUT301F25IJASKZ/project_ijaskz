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
 * Class that defines the AllEventsFragment to show all the events
 */
public class AllEventsFragment extends Fragment {

    private RecyclerView rvEvents;
    private EventsAdapter adapter;
    private final FireStoreHelper helper = new FireStoreHelper();
    private ListenerRegistration reg;
    private EditText etFilterQuery;
    private Spinner spFilterStatus;

    // keep the full list from Firestore and filter it locally
    private final List<Event> allEvents = new ArrayList<>();
    private String currentQuery = "";
    private String currentStatus = "Any";
    private ListenerRegistration reg2;

    /**
     * Creating the all events fragment
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return View Sends the view to the fragment holder to be displayed to user
     */
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

       // Wire up filter UI
        etFilterQuery = v.findViewById(R.id.et_filter_query);
        spFilterStatus = v.findViewById(R.id.sp_filter_status);

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
        // Dedicated listener to keep 'allEvents' in sync without changing helper
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
                    applyFilters(); // refresh adapter with filters
                });

        // Handle both row click (view) and pencil click (edit)
        adapter.setOnEventClickListener(new EventsAdapter.OnEventClickListener() {
            /**
             * Pulls up event details when event clicked
             * @param event event to show user
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
             * If edit permissions active, open event edit
             * @param event event to edit
             */
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
    // Filtering helpers
    private void applyFilters() {
        if (adapter == null) return;

        String q = currentQuery.toLowerCase();
        Timestamp now = Timestamp.now();

        List<Event> filtered = new ArrayList<>();
        for (Event e : allEvents) {
            String name = e.getEvent_name() != null ? e.getEvent_name().toLowerCase() : "";
            String desc = e.getEvent_description() != null ? e.getEvent_description().toLowerCase() : "";
            boolean textOk = q.isEmpty() || name.contains(q) || desc.contains(q);
            boolean statusOk = matchesStatus(e, now, currentStatus);
            if (textOk && statusOk) filtered.add(e);
        }
        adapter.setEvents(filtered);
    }

    private boolean matchesStatus(Event e, Timestamp now, String status) {
        if ("Any".equals(status)) return true;

        com.google.firebase.Timestamp rs = e.getRegistrationStart();
        com.google.firebase.Timestamp re = e.getRegistrationEnd();
        boolean hasWindow = (rs != null && re != null);

        if ("Not set".equals(status)) return !hasWindow;
        if (!hasWindow) return false;

        if ("Upcoming".equals(status)) return now.compareTo(rs) < 0;
        if ("Closed".equals(status))   return now.compareTo(re) > 0;
        if ("Open".equals(status))     return now.compareTo(rs) >= 0 && now.compareTo(re) <= 0;

        return true;
    }

    /**
     * destroy fragment when user leaves
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) {
            reg.remove();     // stop listening to avoid leaks + duplicate updates
            reg = null;
        }
        // Also remove filter listener
        if (reg2 != null) {
            reg2.remove();
            reg2 = null;
        }
        rvEvents.setAdapter(null);
    }
}

