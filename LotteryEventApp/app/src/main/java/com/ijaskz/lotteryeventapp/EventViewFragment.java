package com.ijaskz.lotteryeventapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.text.InputType;
import androidx.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ijaskz.lotteryeventapp.service.LotteryService;
import com.ijaskz.lotteryeventapp.util.LotteryDeadlineUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;


// ZXing for local QR generation
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import android.graphics.Bitmap;

// OSMDroid imports
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;

public class EventViewFragment extends Fragment {

    private Event event;
    private ImageView imgEvent;
    private TextView tvEventName, tvEventDescription, tvEventTime, tvEventLocation, tvEventMax;
    private Button btnJoinWaitlist;
    private Button btnRunLottery;
    private Button btnViewWaitingList;
    private Button btnNotifySelectedEntrants;
    private Button btnNotifyNotSelectedEntrants;
    private Button btnNotifyWaitingListEntrants;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserManager userManager;
    private WaitingListManager waitingListManager;

    private TextView tvRegStatusDetail, tvRegWindowDetail;
    private TextView tvLotteryStatusLine, tvLotteryDeadlineLine;
    private ImageView imgEventQr;

    private LotteryService lotteryService;

    private TextView tvWaitingCount;

    private TextView tvEntrantsHeader;
    private ScrollView scrollViewEntrants;
    private TextView tvEntrantsList;
    private Button btnExportCsv;

    private int waitlistCapacity = -1;
    private int currentWaitingCount = 0;

    // OSMDroid Map
    private MapView mapView;


    public static EventViewFragment newInstance(Event event) {
        EventViewFragment fragment = new EventViewFragment();
        Bundle args = new Bundle();
        args.putSerializable("event", event);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_view, container, false);

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext())
        );

        userManager = new UserManager(getContext());
        waitingListManager = new WaitingListManager();

        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable("event");
        }

        imgEvent = view.findViewById(R.id.imgEvent);
        tvEventName = view.findViewById(R.id.tvEventName);
        tvEventDescription = view.findViewById(R.id.tvEventDescription);
        tvEventTime = view.findViewById(R.id.tvEventTime);
        tvEventLocation = view.findViewById(R.id.tvEventLocation);
        tvEventMax = view.findViewById(R.id.tvEventMax);
        btnJoinWaitlist = view.findViewById(R.id.btnJoinWaitlist);
        btnRunLottery = view.findViewById(R.id.btnRunLottery);
        btnViewWaitingList = view.findViewById(R.id.btnViewWaitingList);
        btnNotifySelectedEntrants = view.findViewById(R.id.btnNotifySelectedEntrants);
        btnNotifyNotSelectedEntrants = view.findViewById(R.id.btnNotifyNotSelectedEntrants);
        btnNotifyWaitingListEntrants = view.findViewById(R.id.btnNotifyWaitingListEntrants);

        tvRegStatusDetail = view.findViewById(R.id.tv_reg_status_detail);
        tvRegWindowDetail = view.findViewById(R.id.tv_reg_window_detail);

        tvLotteryStatusLine = view.findViewById(R.id.tvLotteryStatusLine);
        tvLotteryDeadlineLine = view.findViewById(R.id.tvLotteryDeadlineLine);

        imgEventQr = view.findViewById(R.id.imgEventQr);
        tvWaitingCount = view.findViewById(R.id.tv_waiting_count);

        // Initialize map
        mapView = view.findViewById(R.id.mapView);

        lotteryService = new LotteryService(waitingListManager);

        tvEntrantsHeader = view.findViewById(R.id.tvEntrantsHeader);
        scrollViewEntrants = view.findViewById(R.id.scrollViewEntrants);
        tvEntrantsList = view.findViewById(R.id.tvEntrantsList);
        btnExportCsv = view.findViewById(R.id.btnExportCsv);

        if (event != null) {
            waitlistCapacity = event.getMax();

            populateEventDetails();
            applyRegistrationGating();
            checkWaitlistStatus();
            loadWaitingCount();
        }

        btnJoinWaitlist.setOnClickListener(v -> joinWaitlist());

        // Organizers and admins can run lottery and view waiting list and send notifications
        String role = userManager.getUserType();
        if (role != null && (role.equalsIgnoreCase("organizer") || role.equalsIgnoreCase("admin")) ) {
            if (btnRunLottery != null && !event.isLotteryRun() && (Timestamp.now().compareTo(event.getRegistrationEnd())>0) ) {
                btnRunLottery.setVisibility(View.VISIBLE);
                btnRunLottery.setOnClickListener(v -> showRunLotteryPrompt());
            }
            if (btnViewWaitingList != null) {
                btnViewWaitingList.setVisibility(View.VISIBLE);
                btnViewWaitingList.setOnClickListener(v -> showWaitingListDialog());
            }

            // Notify Selected Entrants button
            if (btnNotifySelectedEntrants != null) {
                btnNotifySelectedEntrants.setVisibility(View.VISIBLE);
                btnNotifySelectedEntrants.setOnClickListener(v -> showNotifySelectedDialog());
            }

            // Notify Not-Selected Entrants button
            if (btnNotifyNotSelectedEntrants != null) {
                btnNotifyNotSelectedEntrants.setVisibility(View.VISIBLE);
                btnNotifyNotSelectedEntrants.setOnClickListener(v -> showNotifyNotSelectedDialog());
            }

            // Notify ALL waiting-list entrants
            if (btnNotifyWaitingListEntrants != null) {
                btnNotifyWaitingListEntrants.setVisibility(View.VISIBLE);
                btnNotifyWaitingListEntrants.setOnClickListener(v -> showNotifyWaitingListDialog());
            }

            loadAndDisplayEntrantsList();

            // Set up CSV export button
            if (btnExportCsv != null) {
                btnExportCsv.setOnClickListener(v -> exportEntrantsAsCsv());
            }


            // Show map for organizers/admins
            setupMap();
            loadEntrantLocations();
        }else {
            // Hide organizer-only controls for entrants
            if (btnRunLottery != null) btnRunLottery.setVisibility(View.GONE);
            if (btnViewWaitingList != null) btnViewWaitingList.setVisibility(View.GONE);
            if (btnNotifySelectedEntrants != null) btnNotifySelectedEntrants.setVisibility(View.GONE);
            if (btnNotifyNotSelectedEntrants != null) btnNotifyNotSelectedEntrants.setVisibility(View.GONE);
            if (btnNotifyWaitingListEntrants != null) btnNotifyWaitingListEntrants.setVisibility(View.GONE);
            if (tvEntrantsHeader != null) tvEntrantsHeader.setVisibility(View.GONE);
            if (scrollViewEntrants != null) scrollViewEntrants.setVisibility(View.GONE);
            if (btnExportCsv != null) btnExportCsv.setVisibility(View.GONE);
        }

        requestLocationPermission();
        return view;
    }

    /**
     * Sets up the OSMDroid map with basic configuration
     */
    private void setupMap() {
        if (mapView == null) return;

        mapView.setVisibility(View.VISIBLE);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Set default zoom and center (will be adjusted when markers are added)
        mapView.getController().setZoom(10.0);

        // Default center (Edmonton, Alberta as fallback)
        GeoPoint startPoint = new GeoPoint(53.5461, -113.4938);
        mapView.getController().setCenter(startPoint);
    }

    /**
     * Loads all entrant locations from Firestore and displays them as markers
     */
    private void loadEntrantLocations() {
        if (mapView == null || event == null) return;

        String eventId = event.getEvent_id();
        if (eventId == null) return;

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    mapView.getOverlays().clear(); // Clear existing markers

                    int markerCount = 0;
                    double sumLat = 0;
                    double sumLon = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Double lat = doc.getDouble("latitude");
                        Double lon = doc.getDouble("longitude");
                        String name = doc.getString("entrant_name");

                        if (lat != null && lon != null) {
                            // Create marker
                            Marker marker = new Marker(mapView);
                            GeoPoint point = new GeoPoint(lat, lon);
                            marker.setPosition(point);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                            // Set title (entrant name)
                            if (name != null && !name.isEmpty()) {
                                marker.setTitle(name);
                            } else {
                                marker.setTitle("Entrant");
                            }

                            mapView.getOverlays().add(marker);

                            // Sum for center calculation
                            sumLat += lat;
                            sumLon += lon;
                            markerCount++;
                        }
                        Log.d("EventViewMap", "Marker: " + name + " @ " + lat + ", " + lon);

                    }

                    // Center map on average location of all markers
                    if (markerCount > 0) {
                        double avgLat = sumLat / markerCount;
                        double avgLon = sumLon / markerCount;
                        GeoPoint center = new GeoPoint(avgLat, avgLon);
                        mapView.getController().setCenter(center);

                        // Adjust zoom based on marker count
                        if (markerCount == 1) {
                            mapView.getController().setZoom(15.0);
                        } else if (markerCount < 5) {
                            mapView.getController().setZoom(12.0);
                        } else {
                            mapView.getController().setZoom(10.0);
                        }
                    }

                    mapView.invalidate(); // Refresh the map

                    Log.d("EventViewMap", "Loaded " + markerCount + " entrant locations");
                })
                .addOnFailureListener(e -> {
                    Log.e("EventViewMap", "Failed to load locations: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(getContext(),
                                "Could not load entrant locations",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateEventDetails() {
        tvEventName.setText(event.getEvent_name());
        tvEventDescription.setText(event.getEvent_description());
        tvEventTime.setText("Time: " + event.getEvent_time());
        tvEventLocation.setText("Location: " + event.getLocation());
        tvEventMax.setText("Max Participants: " + event.getMax());

        String imageUrl = event.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()
                && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imgEvent);
        } else {
            imgEvent.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        if (imgEventQr != null) {
            String qrUrl = null;
            try { qrUrl = event.getQrUrl(); } catch (Exception ignored) {}
            if (qrUrl != null && !qrUrl.isEmpty()
                    && (qrUrl.startsWith("http://") || qrUrl.startsWith("https://"))) {
                imgEventQr.setVisibility(View.VISIBLE);
                Glide.with(this).load(qrUrl).into(imgEventQr);
            } else {
                String content = null;
                try { content = event.getDeeplink(); } catch (Exception ignored) {}
                if (content == null || content.isEmpty()) {
                    content = "lotteryevent://event/" + event.getEvent_id();
                }
                Bitmap bmp = makeQr(content, 700);
                if (bmp != null) {
                    imgEventQr.setVisibility(View.VISIBLE);
                    imgEventQr.setImageBitmap(bmp);
                } else {
                    imgEventQr.setVisibility(View.GONE);
                }
            }
        }
    }

    private void applyRegistrationGating() {
        if (event == null || btnJoinWaitlist == null) return;

        Timestamp rs = event.getRegistrationStart();
        Timestamp re = event.getRegistrationEnd();
        Timestamp now = Timestamp.now();

        boolean hasWindow = (rs != null && re != null);
        boolean isOpen = hasWindow && now.compareTo(rs) >= 0 && now.compareTo(re) <= 0;

        if (tvRegStatusDetail != null) {
            if (!hasWindow) {
                tvRegStatusDetail.setText("Registration: not set");
            } else if (now.compareTo(rs) < 0) {
                tvRegStatusDetail.setText("Registration: upcoming");
            } else if (now.compareTo(re) > 0) {
                tvRegStatusDetail.setText("Registration: closed");
            } else {
                tvRegStatusDetail.setText("Registration: open");
            }
        }

        if (tvRegWindowDetail != null) {
            if (hasWindow) {
                if (now.compareTo(rs) < 0) {
                    tvRegWindowDetail.setText("Opens: " + fmt(rs) + "  •  Closes: " + fmt(re));
                } else if (now.compareTo(re) > 0) {
                    tvRegWindowDetail.setText("Closed: " + fmt(re));
                } else {
                    tvRegWindowDetail.setText("Closes: " + fmt(re));
                }
            } else {
                tvRegWindowDetail.setText("");
            }
        }

        btnJoinWaitlist.setEnabled(isOpen);
        btnJoinWaitlist.setAlpha(isOpen ? 1f : 0.5f);

        updateJoinButtonForCapacity();
    }

    private void checkWaitlistStatus() {
        String userId = userManager.getUserId();
        String eventId = event.getEvent_id();

        if (userId == null || eventId == null) return;

        waitingListManager.isOnWaitingList(eventId, userId, isOnList -> {
            if (isOnList) {
                btnJoinWaitlist.setEnabled(false);
                btnJoinWaitlist.setText("Already on Waitlist");

                waitingListManager.getWaitingListStatus(eventId, userId, status -> {
                    if (status != null && !status.equals("waiting")) {
                        btnJoinWaitlist.setText("Status: " + capitalizeFirst(status));
                        loadAndShowLotteryUI(status);
                    }
                });
            } else {
                updateJoinButtonForCapacity();
            }
        });
    }

    private void loadAndShowLotteryUI(String status) {
        String userId = userManager.getUserId();
        String eventId = event.getEvent_id();
        if (userId == null || eventId == null) return;

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("entrant_id", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    Long selectedAt = null;
                    Integer hours = null;
                    if (!snap.isEmpty()) {
                        Object sel = snap.getDocuments().get(0).get("selected_at");
                        if (sel instanceof Number) selectedAt = ((Number) sel).longValue();
                        Object h = snap.getDocuments().get(0).get("response_window_hours");
                        if (h instanceof Number) hours = ((Number) h).intValue();
                    }
                    updateEntrantLotteryUI(status, selectedAt, hours);
                })
                .addOnFailureListener(e -> updateEntrantLotteryUI(status, null, null));
    }

    private void updateEntrantLotteryUI(String status, Long selectedAtMs, Integer hoursOverride) {
        if (tvLotteryStatusLine == null || tvLotteryDeadlineLine == null) return;

        if (status == null) {
            tvLotteryStatusLine.setVisibility(View.GONE);
            tvLotteryDeadlineLine.setVisibility(View.GONE);
            return;
        }

        tvLotteryStatusLine.setVisibility(View.VISIBLE);
        tvLotteryStatusLine.setText("Lottery Status: " + capitalizeFirst(status));

        if ("selected".equalsIgnoreCase(status) && selectedAtMs != null) {
            int hours = (hoursOverride != null) ? hoursOverride
                    : (event.getResponseWindowHours() != null ? event.getResponseWindowHours() : 48);
            String remaining = LotteryDeadlineUtil.getRemainingTimeString(new Date(selectedAtMs), hours);
            tvLotteryDeadlineLine.setVisibility(View.VISIBLE);
            tvLotteryDeadlineLine.setText("Respond within: " + remaining);
        } else {
            tvLotteryDeadlineLine.setVisibility(View.GONE);
        }
    }

    private void showRunLotteryPrompt() {
        if (getContext() == null) return;
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText etSlots = new EditText(getContext());
        etSlots.setHint("Number of slots");
        etSlots.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etSlots);

        final EditText etHours = new EditText(getContext());
        etHours.setHint("Response hours (optional)");
        etHours.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etHours);

        new AlertDialog.Builder(requireContext())
                .setTitle("Run Lottery")
                .setView(layout)
                .setPositiveButton("Run", (d, w) -> {
                    String slotsStr = etSlots.getText().toString().trim();
                    String hoursStr = etHours.getText().toString().trim();
                    if (slotsStr.isEmpty()) {
                        Toast.makeText(getContext(), "Enter number of slots", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int slots;
                    try { slots = Integer.parseInt(slotsStr); } catch (Exception ex) { slots = -1; }
                    if (slots <= 0) {
                        Toast.makeText(getContext(), "Slots must be > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Integer hours = null;
                    if (!hoursStr.isEmpty()) {
                        try { hours = Integer.parseInt(hoursStr); } catch (Exception ignore) { hours = null; }
                    }
                    String eventId = event != null ? event.getEvent_id() : null;
                    if (eventId == null) return;

                    lotteryService.runLottery(eventId, slots, hours, new LotteryService.OnLotteryComplete() {
                        @Override
                        public void onSuccess(java.util.List<WaitingListEntry> winners) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Selected " + winners.size() + " entrants", Toast.LENGTH_LONG).show();
                            }
                            loadWaitingCount();
                            loadEntrantLocations(); // Refresh map after lottery
                            String eventId = event.getEvent_id();
                            FirebaseFirestore.getInstance()
                                    .collection("events")
                                    .document(eventId)
                                    .update("lotteryRun", true)
                                    .addOnSuccessListener(aVoid -> {
                                        event.setLotteryRun(true);
                                        if (btnRunLottery != null){
                                            btnRunLottery.setVisibility(View.GONE);
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    /**
     * Shows dialog for organizer to send a message to all selected entrants.
     */
    private void showNotifySelectedDialog() {
        if (getContext() == null || event == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        final EditText etTitle = new EditText(getContext());
        etTitle.setHint("Notification title (e.g., Event Update)");
        etTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        etTitle.setText("Event update");
        layout.addView(etTitle);

        final EditText etMessage = new EditText(getContext());
        etMessage.setHint("Message to all selected entrants");
        etMessage.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etMessage.setMinLines(3);
        layout.addView(etMessage);

        new AlertDialog.Builder(requireContext())
                .setTitle("Notify Selected Entrants")
                .setView(layout)
                .setPositiveButton("Send", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String message = etMessage.getText().toString().trim();

                    if (title.isEmpty()) {
                        title = "Event update";
                    }
                    if (message.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Message cannot be empty",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String eventId = event.getEvent_id();
                    if (eventId == null) return;

                    btnNotifySelectedEntrants.setEnabled(false);

                    waitingListManager.notifySelectedEntrants(
                            eventId,
                            title,
                            message,
                            new WaitingListManager.OnCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    if (!isAdded()) return;
                                    btnNotifySelectedEntrants.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            "Notification sent to selected entrants.",
                                            Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    if (!isAdded()) return;
                                    btnNotifySelectedEntrants.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            "Failed to send notifications: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows dialog for organizer to send a message to all NOT SELECTED entrants.
     */
    private void showNotifyNotSelectedDialog() {
        if (getContext() == null || event == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        final EditText etTitle = new EditText(getContext());
        etTitle.setHint("Notification title (e.g., Lottery Result)");
        etTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        etTitle.setText("Lottery result");
        layout.addView(etTitle);

        final EditText etMessage = new EditText(getContext());
        etMessage.setHint("Message to all not-selected entrants");
        etMessage.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etMessage.setMinLines(3);
        layout.addView(etMessage);

        new AlertDialog.Builder(requireContext())
                .setTitle("Notify Not-Selected Entrants")
                .setView(layout)
                .setPositiveButton("Send", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String message = etMessage.getText().toString().trim();

                    if (title.isEmpty()) {
                        title = "Lottery result";
                    }
                    if (message.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Message cannot be empty",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String eventId = event.getEvent_id();
                    if (eventId == null) return;

                    btnNotifyNotSelectedEntrants.setEnabled(false);

                    waitingListManager.notifyNotSelectedEntrants(
                            eventId,
                            title,
                            message,
                            new WaitingListManager.OnCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    if (!isAdded()) return;
                                    btnNotifyNotSelectedEntrants.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            "Notification sent to not-selected entrants.",
                                            Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    if (!isAdded()) return;
                                    btnNotifyNotSelectedEntrants.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            "Failed to send notifications: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows dialog for organizer to send a message to ALL waiting-list entrants.
     */
    private void showNotifyWaitingListDialog() {
        if (getContext() == null || event == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        final EditText etTitle = new EditText(getContext());
        etTitle.setHint("Notification title (e.g., Event Update)");
        etTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        etTitle.setText("Event update");
        layout.addView(etTitle);

        final EditText etMessage = new EditText(getContext());
        etMessage.setHint("Message to all waiting-list entrants");
        etMessage.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etMessage.setMinLines(3);
        layout.addView(etMessage);

        new AlertDialog.Builder(requireContext())
                .setTitle("Notify Waiting List Entrants")
                .setView(layout)
                .setPositiveButton("Send", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String message = etMessage.getText().toString().trim();

                    if (title.isEmpty()) {
                        title = "Event update";
                    }
                    if (message.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Message cannot be empty",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String eventId = event.getEvent_id();
                    if (eventId == null) return;

                    btnNotifyWaitingListEntrants.setEnabled(false);

                    waitingListManager.notifyAllWaitingListEntrants(
                            eventId,
                            title,
                            message,
                            new WaitingListManager.OnCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    if (!isAdded()) return;
                                    btnNotifyWaitingListEntrants.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            "Notification sent to all waiting-list entrants.",
                                            Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    if (!isAdded()) return;
                                    btnNotifyWaitingListEntrants.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            "Failed to send notifications: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showWaitingListDialog() {
        if (event == null) return;
        String eventId = event.getEvent_id();
        if (eventId == null) return;

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;

                    if (snap.isEmpty()) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Waiting List")
                                .setMessage("No entrants on the waiting list yet.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    StringBuilder sb = new StringBuilder();

                    // 1. US 02.06.01 - chosen entrants invited to apply
                    //    In your model this is status = "selected"
                    sb.append("Chosen entrants invited to apply:\n");
                    boolean anySelected = false;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String status = doc.getString("status");
                        if (!"selected".equals(status)) {
                            continue;
                        }
                        anySelected = true;

                        String name = doc.getString("entrant_name");
                        String email = doc.getString("entrant_email");

                        if (name == null || name.trim().isEmpty()) {
                            name = "(no name)";
                        }
                        sb.append("• ").append(name);
                        if (email != null && !email.trim().isEmpty()) {
                            sb.append("  <").append(email).append(">");
                        }
                        sb.append("\n");
                    }
                    if (!anySelected) {
                        sb.append("  (none)\n");
                    }

                    sb.append("\n");

                    // 2. US 02.06.02 - cancelled entrants
                    //    For your code, treat "declined" as cancelled, and also include "cancelled" if you add it later
                    sb.append("Cancelled entrants:\n");
                    boolean anyCancelled = false;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String status = doc.getString("status");
                        if (!"declined".equals(status) && !"cancelled".equals(status)) {
                            continue;
                        }
                        anyCancelled = true;

                        String name = doc.getString("entrant_name");
                        String email = doc.getString("entrant_email");

                        if (name == null || name.trim().isEmpty()) {
                            name = "(no name)";
                        }
                        sb.append("• ").append(name);
                        if (email != null && !email.trim().isEmpty()) {
                            sb.append("  <").append(email).append(">");
                        }
                        // Optional, if you want to show the exact status
                        sb.append("  [").append(status).append("]");
                        sb.append("\n");
                    }
                    if (!anyCancelled) {
                        sb.append("  (none)\n");
                    }

                    sb.append("\n");

                    // 3. Everything else on the waiting list, to keep your old behavior
                    sb.append("Other waiting list entrants:\n");
                    boolean anyOther = false;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String status = doc.getString("status");

                        // Skip the ones already shown above
                        if ("selected".equals(status)
                                || "declined".equals(status)
                                || "cancelled".equals(status)) {
                            continue;
                        }

                        anyOther = true;
                        String name = doc.getString("entrant_name");
                        String email = doc.getString("entrant_email");

                        if (name == null || name.trim().isEmpty()) {
                            name = "(no name)";
                        }
                        if (email == null || email.trim().isEmpty()) {
                            email = "(no email)";
                        }
                        if (status == null || status.trim().isEmpty()) {
                            status = "unknown";
                        }

                        sb.append("• ")
                                .append(name)
                                .append("  <").append(email).append(">")
                                .append("  [").append(status).append("]")
                                .append("\n");
                    }
                    if (!anyOther) {
                        sb.append("  (none)\n");
                    }

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Waiting List")
                            .setMessage(sb.toString())
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Waiting List")
                            .setMessage("Failed to load waiting list: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                });
    }



    private void joinWaitlist() {
        String userId = userManager.getUserId();
        String userName = userManager.getUserName();
        String userEmail = userManager.getUserEmail();
        String eventId = event.getEvent_id();

        if (userId == null || eventId == null) {
            Toast.makeText(getContext(), "Error: Unable to join waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (waitlistCapacity > 0) {
            waitingListManager.getWaitingListCount(eventId, new WaitingListManager.OnCountListener() {
                @Override
                public void onCount(int count) {
                    currentWaitingCount = count;
                    if (count >= waitlistCapacity) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Waitlist is full for this event.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        updateJoinButtonForCapacity();
                        return;
                    }
                    actuallyJoinWaitlist(userId, userName, userEmail, eventId);
                }

                @Override
                public void onError(Exception e) {
                    actuallyJoinWaitlist(userId, userName, userEmail, eventId);
                }
            });
        } else {
            actuallyJoinWaitlist(userId, userName, userEmail, eventId);
        }
    }

    private void actuallyJoinWaitlist(String userId, String userName, String userEmail, String eventId) {
        btnJoinWaitlist.setEnabled(false);

        getLocation((lat, lon) -> {
            waitingListManager.joinWaitingList(eventId, userId, userName, userEmail, lat, lon,
                    new WaitingListManager.OnCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Successfully joined waitlist!", Toast.LENGTH_SHORT).show();
                            btnJoinWaitlist.setText("Joined Waitlist");
                            loadWaitingCount();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            btnJoinWaitlist.setEnabled(true);
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    interface LocationCallback {
        void onLocation(Double lat, Double lon);
    }

    private static final int LOCATION_PERMISSION_CODE = 100;

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d("EventViewFragment", "Requesting location permission...");

            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );
        } else {
            Log.d("EventViewFragment", "Permission already granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("EventViewFragment", "Location permission granted!");
                Toast.makeText(getContext(), "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Log.w("EventViewFragment", "Location permission denied");
                Toast.makeText(getContext(), "Location permission denied - joining without location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocation(LocationCallback callback) {
        Log.d("EventViewFragment", "getLocation() called");

        if (getContext() == null) {
            Log.e("EventViewFragment", "Context is null!");
            callback.onLocation(null, null);
            return;
        }

        FusedLocationProviderClient fusedClient =
                LocationServices.getFusedLocationProviderClient(requireActivity());

        if (ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("EventViewFragment", "Location permission not granted");
            callback.onLocation(null, null);
            return;
        }

        Log.d("EventViewFragment", "Permission granted, requesting location...");

        fusedClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d("EventViewFragment", "Location obtained: " +
                                location.getLatitude() + ", " + location.getLongitude());
                        callback.onLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.w("EventViewFragment", "Location is null");
                        callback.onLocation(null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EventViewFragment", "Failed to get location: " + e.getMessage());
                    callback.onLocation(null, null);
                });
    }

    private void loadWaitingCount() {
        if (tvWaitingCount == null || event == null) return;
        waitingListManager.getWaitingListCount(event.getEvent_id(), new WaitingListManager.OnCountListener() {
            @Override
            public void onCount(int count) {
                currentWaitingCount = count;

                if (waitlistCapacity > 0) {
                    tvWaitingCount.setText("Waiting List: " + count + " / " + waitlistCapacity);
                } else {
                    tvWaitingCount.setText("Waiting List: " + count);
                }

                updateJoinButtonForCapacity();
            }

            @Override
            public void onError(Exception e) {
                tvWaitingCount.setText("Waiting List: -");
                updateJoinButtonForCapacity();
            }
        });
    }

    private void updateJoinButtonForCapacity() {
        if (btnJoinWaitlist == null || event == null) return;

        boolean capacityAvailable = true;
        if (waitlistCapacity > 0) {
            capacityAvailable = currentWaitingCount < waitlistCapacity;
        }

        if (!capacityAvailable) {
            btnJoinWaitlist.setEnabled(false);
            btnJoinWaitlist.setAlpha(0.5f);

            CharSequence txt = btnJoinWaitlist.getText();
            String text = txt != null ? txt.toString().toLowerCase(Locale.getDefault()) : "";
            if (!text.contains("already") && !text.contains("joined") && !text.contains("status")) {
                btnJoinWaitlist.setText("Waitlist Full");
            }
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String fmt(Timestamp ts) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(ts.toDate());
    }

    private Bitmap makeQr(String text, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bmp;
        } catch (WriterException e) {
            return null;
        }
    }
    /**
     * Adds a section title inside the entrants list
     */
    private void addSectionTitle(LinearLayout parent, String title) {
        TextView sectionTitle = new TextView(getContext());
        sectionTitle.setText(title);
        sectionTitle.setTextSize(15);
        sectionTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        sectionTitle.setPadding(0, 16, 0, 4);
        parent.addView(sectionTitle);
    }

    /**
     * Adds the "Name   Email" header row for a section
     */
    private void addHeaderRow(LinearLayout parent) {
        LinearLayout headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView nameHeader = new TextView(getContext());
        nameHeader.setText("Name");
        nameHeader.setTextSize(14);
        nameHeader.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        nameHeader.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));
        headerLayout.addView(nameHeader);

        TextView emailHeader = new TextView(getContext());
        emailHeader.setText("Email");
        emailHeader.setTextSize(14);
        emailHeader.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        emailHeader.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));
        headerLayout.addView(emailHeader);

        TextView empty = new TextView(getContext());
        empty.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        headerLayout.addView(empty);

        parent.addView(headerLayout);
    }


    /**
     * Adds one entrant row with name, email and red "remove" action
     */
    private void addEntrantRow(LinearLayout parent, String name, String email, String docId) {
        if (name == null || name.trim().isEmpty()) name = "(no name)";
        if (email == null || email.trim().isEmpty()) email = "(no email)";

        LinearLayout rowLayout = new LinearLayout(getContext());
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        rowLayout.setPadding(0, 6, 0, 6);

        // NAME COLUMN
        TextView nameText = new TextView(getContext());
        nameText.setText(name);
        nameText.setTextSize(14);
        nameText.setTypeface(android.graphics.Typeface.MONOSPACE);
        nameText.setSingleLine(true);
        nameText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));
        rowLayout.addView(nameText);

        // EMAIL COLUMN
        TextView emailText = new TextView(getContext());
        emailText.setText(email);
        emailText.setTextSize(14);
        emailText.setTypeface(android.graphics.Typeface.MONOSPACE);
        emailText.setSingleLine(true);
        emailText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        emailText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));
        rowLayout.addView(emailText);

        // REMOVE BUTTON
        TextView removeButton = new TextView(getContext());
        removeButton.setText("remove");
        removeButton.setTextSize(12);
        removeButton.setTextColor(Color.RED);
        removeButton.setPadding(16, 0, 0, 0);
        removeButton.setClickable(true);

        final String finalName = name;
        removeButton.setOnClickListener(v -> showRemoveConfirmation(docId, finalName));

        rowLayout.addView(removeButton);
        parent.addView(rowLayout);
    }



    /**
     * Loads and displays the list of entrants for organizers
     */

    private void loadAndDisplayEntrantsList() {
        if (event == null) return;
        String eventId = event.getEvent_id();
        if (eventId == null) return;

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    if (querySnapshot.isEmpty()) {
                        tvEntrantsHeader.setVisibility(View.VISIBLE);
                        scrollViewEntrants.setVisibility(View.VISIBLE);
                        btnExportCsv.setVisibility(View.VISIBLE);
                        tvEntrantsList.setText("No entrants yet.");
                        return;
                    }

                    // Clear the ScrollView content
                    scrollViewEntrants.removeAllViews();

                    LinearLayout container = new LinearLayout(getContext());
                    container.setOrientation(LinearLayout.VERTICAL);
                    container.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    // Separate docs by status
                    List<DocumentSnapshot> waitingDocs = new ArrayList<>();
                    List<DocumentSnapshot> selectedDocs = new ArrayList<>();
                    List<DocumentSnapshot> acceptedDocs = new ArrayList<>();
                    List<DocumentSnapshot> cancelledDocs = new ArrayList<>();
                    List<DocumentSnapshot> otherDocs = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String status = doc.getString("status");

                        if ("selected".equals(status)) {
                            selectedDocs.add(doc);
                        } else if ("accepted".equals(status) || "enrolled".equals(status)) {
                            acceptedDocs.add(doc);
                        } else if ("declined".equals(status) || "cancelled".equals(status)) {
                            cancelledDocs.add(doc);
                        } else if (status == null || "waiting".equals(status)) {
                            waitingDocs.add(doc);
                        } else {
                            otherDocs.add(doc);
                        }
                    }

                    boolean hasLotteryActivity =
                            !selectedDocs.isEmpty() || !acceptedDocs.isEmpty() || !cancelledDocs.isEmpty();

                    // Case 1: before lottery, show a single simple waiting list
                    if (!hasLotteryActivity) {
                        addSectionTitle(container, "Waiting list entrants");
                        addHeaderRow(container);
                        for (DocumentSnapshot doc : waitingDocs) {
                            String name = doc.getString("entrant_name");
                            String email = doc.getString("entrant_email");
                            String docId = doc.getId();
                            addEntrantRow(container, name, email, docId);
                        }
                    } else {
                        // Case 2: after lottery, show grouped sections

                        // 2.0 All chosen entrants, regardless of response
                        List<DocumentSnapshot> allChosen = new ArrayList<>();
                        allChosen.addAll(selectedDocs);
                        allChosen.addAll(acceptedDocs);
                        allChosen.addAll(cancelledDocs);

                        if (!allChosen.isEmpty()) {
                            addSectionTitle(container, "All entrants chosen in lottery");
                            addHeaderRow(container);
                            for (DocumentSnapshot doc : allChosen) {
                                String name = doc.getString("entrant_name");
                                String email = doc.getString("entrant_email");
                                String docId = doc.getId();
                                addEntrantRow(container, name, email, docId);
                            }
                        }

                        // 2.1 Still selected, invited to apply
                        if (!selectedDocs.isEmpty()) {
                            addSectionTitle(container, "Chosen entrants invited to apply");
                            addHeaderRow(container);
                            for (DocumentSnapshot doc : selectedDocs) {
                                String name = doc.getString("entrant_name");
                                String email = doc.getString("entrant_email");
                                String docId = doc.getId();
                                addEntrantRow(container, name, email, docId);
                            }
                        }

                        // 2.2 Accepted / enrolled
                        if (!acceptedDocs.isEmpty()) {
                            addSectionTitle(container, "Accepted entrants");
                            addHeaderRow(container);
                            for (DocumentSnapshot doc : acceptedDocs) {
                                String name = doc.getString("entrant_name");
                                String email = doc.getString("entrant_email");
                                String docId = doc.getId();
                                addEntrantRow(container, name, email, docId);
                            }
                        }

                        // 2.3 Cancelled / declined
                        if (!cancelledDocs.isEmpty()) {
                            addSectionTitle(container, "Cancelled entrants");
                            addHeaderRow(container);
                            for (DocumentSnapshot doc : cancelledDocs) {
                                String name = doc.getString("entrant_name");
                                String email = doc.getString("entrant_email");
                                String docId = doc.getId();
                                addEntrantRow(container, name, email, docId);
                            }
                        }

                        // 2.4 Never chosen, still just on waiting list
                        if (!waitingDocs.isEmpty() || !otherDocs.isEmpty()) {
                            addSectionTitle(container, "Other waiting list entrants");
                            addHeaderRow(container);

                            for (DocumentSnapshot doc : waitingDocs) {
                                String name = doc.getString("entrant_name");
                                String email = doc.getString("entrant_email");
                                String docId = doc.getId();
                                addEntrantRow(container, name, email, docId);
                            }

                            for (DocumentSnapshot doc : otherDocs) {
                                String name = doc.getString("entrant_name");
                                String email = doc.getString("entrant_email");
                                String docId = doc.getId();
                                addEntrantRow(container, name, email, docId);
                            }
                        }
                    }

                    scrollViewEntrants.addView(container);
                    tvEntrantsHeader.setVisibility(View.VISIBLE);
                    scrollViewEntrants.setVisibility(View.VISIBLE);
                    btnExportCsv.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e("EventViewFragment", "Failed to load entrants: " + e.getMessage());
                });
    }



    /**
     * Shows confirmation dialog before removing an entrant
     */
    private void showRemoveConfirmation(String waitingListDocId, String entrantName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Entrant")
                .setMessage("Are you sure you want to remove " + entrantName + " from the waiting list?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    removeEntrantFromWaitingList(waitingListDocId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Removes an entrant from the waiting list in Firebase
     */
    private void removeEntrantFromWaitingList(String waitingListDocId) {
        db.collection("waiting_list")
                .document(waitingListDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Entrant removed from waiting list",
                            Toast.LENGTH_SHORT).show();
                    // Reload the list to reflect changes
                    loadAndDisplayEntrantsList();
                    loadWaitingCount(); // Update the count
                    loadEntrantLocations(); // Update the map
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Failed to remove entrant: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Helper method to truncate long strings
     */
    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Exports the entrants list as a CSV file
     */
    private void exportEntrantsAsCsv() {
        if (event == null) return;
        String eventId = event.getEvent_id();
        if (eventId == null) return;

        db.collection("waiting_list")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    // Build CSV content
                    StringBuilder csv = new StringBuilder();
                    csv.append("Name,Email,Status,Joined Date\n");

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("entrant_name");
                        String email = doc.getString("entrant_email");
                        String status = doc.getString("status");
                        Long joinedAt = doc.getLong("joined_at");

                        // Handle null values
                        if (name == null) name = "";
                        if (email == null) email = "";
                        if (status == null) status = "";

                        String joinedDate = "";
                        if (joinedAt != null) {
                            joinedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm",
                                    Locale.getDefault()).format(new Date(joinedAt));
                        }

                        // Escape commas and quotes in CSV
                        name = escapeCsv(name);
                        email = escapeCsv(email);

                        csv.append(String.format("%s,%s,%s,%s\n",
                                name, email, status, joinedDate));
                    }

                    // Save to file
                    saveCsvFile(csv.toString());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Failed to export: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Escapes special characters for CSV format
     */
    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Saves CSV content to Downloads folder
     */
    private void saveCsvFile(String csvContent) {
        try {
            String eventName = event.getEvent_name() != null ?
                    event.getEvent_name().replaceAll("[^a-zA-Z0-9]", "_") : "event";
            String fileName = eventName + "_entrants_" +
                    System.currentTimeMillis() + ".csv";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ using MediaStore
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS);

                android.net.Uri uri = requireContext().getContentResolver().insert(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    java.io.OutputStream outputStream =
                            requireContext().getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        outputStream.write(csvContent.getBytes());
                        outputStream.close();
                        Toast.makeText(getContext(),
                                "CSV exported to Downloads/" + fileName,
                                Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // For older Android versions
                java.io.File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(downloadDir, fileName);

                java.io.FileWriter writer = new java.io.FileWriter(file);
                writer.write(csvContent);
                writer.close();

                Toast.makeText(getContext(),
                        "CSV exported to Downloads/" + fileName,
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Failed to save file: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            Log.e("EventViewFragment", "CSV save error", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDetach();
        }
    }


}