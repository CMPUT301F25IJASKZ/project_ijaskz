package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.text.InputType;

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

/**
 * Defines EventViewFragment to display event information to user
 */
public class EventViewFragment extends Fragment {

    private Event event;
    private ImageView imgEvent;
    private TextView tvEventName, tvEventDescription, tvEventTime, tvEventLocation, tvEventMax;
    private Button btnJoinWaitlist;
    private Button btnRunLottery;
    /** Button for organizers to view list of entrants on the waiting list */
    private Button btnViewWaitingList;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserManager userManager;
    private WaitingListManager waitingListManager;

    private TextView tvRegStatusDetail, tvRegWindowDetail;
    private TextView tvLotteryStatusLine, tvLotteryDeadlineLine;
    private ImageView imgEventQr;

    private LotteryService lotteryService;

    /** Label that displays "Waiting List: <n>" on the event details page. */
    private TextView tvWaitingCount;

    /** Capacity of the waiting list. Here we use Max Participants as the cap. */
    private int waitlistCapacity = -1;
    /** Cached current count so we can disable the button when full. */
    private int currentWaitingCount = 0;

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

        lotteryService = new LotteryService(waitingListManager);

        if (event != null) {
            // Use Max Participants as the waiting list capacity
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
        }

        return view;
    }

    /** Sets information for event being displayed */
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

        // Show QR
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

    /** Show status/window and gate the Join button based on registration window */
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

        // Now also factor in capacity
        updateJoinButtonForCapacity();
    }

    /** Check if current user is already on waiting list and update button + lottery UI */
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

    /** Load this entrant's waiting list entry and update lottery UI lines */
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

    /** Update simple lottery status / deadline lines */
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

    /** Run lottery dialog for organizers/admins */
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

    /** Simple dialog listing everyone on the waiting list for this event */
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

    /** Join waitlist, enforcing capacity based on Max Participants */
    private void joinWaitlist() {
        String userId = userManager.getUserId();
        String userName = userManager.getUserName();
        String userEmail = userManager.getUserEmail();
        String eventId = event.getEvent_id();

        if (userId == null || eventId == null) {
            Toast.makeText(getContext(), "Error: Unable to join waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // If capacity is set and positive, recheck count from server before joining
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
                    // If count cannot be loaded, still try to join
                    actuallyJoinWaitlist(userId, userName, userEmail, eventId);
                }
            });
        } else {
            // No capacity configured, treat as unlimited
            actuallyJoinWaitlist(userId, userName, userEmail, eventId);
        }
    }

    /** Actual join implementation separated so capacity can call it */
    private void actuallyJoinWaitlist(String userId, String userName, String userEmail, String eventId) {
        btnJoinWaitlist.setEnabled(false);

        waitingListManager.joinWaitingList(eventId, userId, userName, userEmail,
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
    }

    /** Fetch and display number of entrants on waiting list */
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

    /** Disable join button if waiting list is full */
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

    /** Capitalizes first letter of status */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /** Format timestamp for display */
    private String fmt(Timestamp ts) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(ts.toDate());
    }

    /** Generate a QR bitmap locally */
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
}
