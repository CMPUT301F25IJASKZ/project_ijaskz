package com.ijaskz.lotteryeventapp;

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

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserManager userManager;
    private WaitingListManager waitingListManager;

    private TextView tvRegStatusDetail, tvRegWindowDetail;
    private TextView tvLotteryStatusLine, tvLotteryDeadlineLine;
    private ImageView imgEventQr;

    private LotteryService lotteryService;

    private TextView tvWaitingCount;

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

        tvRegStatusDetail = view.findViewById(R.id.tv_reg_status_detail);
        tvRegWindowDetail = view.findViewById(R.id.tv_reg_window_detail);

        tvLotteryStatusLine = view.findViewById(R.id.tvLotteryStatusLine);
        tvLotteryDeadlineLine = view.findViewById(R.id.tvLotteryDeadlineLine);

        imgEventQr = view.findViewById(R.id.imgEventQr);
        tvWaitingCount = view.findViewById(R.id.tv_waiting_count);

        // Initialize map
        mapView = view.findViewById(R.id.mapView);

        lotteryService = new LotteryService(waitingListManager);

        if (event != null) {
            waitlistCapacity = event.getMax();

            populateEventDetails();
            applyRegistrationGating();
            checkWaitlistStatus();
            loadWaitingCount();
        }

        btnJoinWaitlist.setOnClickListener(v -> joinWaitlist());

        // Organizers and admins can run lottery and view waiting list
        String role = userManager.getUserType();
        if (role != null && (role.equalsIgnoreCase("organizer") || role.equalsIgnoreCase("admin"))) {
            if (btnRunLottery != null) {
                btnRunLottery.setVisibility(View.VISIBLE);
                btnRunLottery.setOnClickListener(v -> showRunLotteryPrompt());
            }
            if (btnViewWaitingList != null) {
                btnViewWaitingList.setVisibility(View.VISIBLE);
                btnViewWaitingList.setOnClickListener(v -> showWaitingListDialog());
            }

            // Show map for organizers/admins
            setupMap();
            loadEntrantLocations();
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
                    for (DocumentSnapshot doc : snap.getDocuments()) {
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

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Waiting List")
                            .setMessage(sb.toString())
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Failed to load waiting list: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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