package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
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
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserManager userManager;

    private WaitingListManager waitingListManager;

    private TextView tvRegStatusDetail, tvRegWindowDetail;

    private ImageView imgEventQr;

    public static EventViewFragment newInstance(Event event) {
        EventViewFragment fragment = new EventViewFragment();
        Bundle args = new Bundle();
        args.putSerializable("event", event);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * creates Fragment to be passed to Holder
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return v The view to be passed to holder
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        tvRegStatusDetail = view.findViewById(R.id.tv_reg_status_detail);
        tvRegWindowDetail = view.findViewById(R.id.tv_reg_window_detail);

        // QR view
        imgEventQr = view.findViewById(R.id.imgEventQr);

        if (event != null) {
            populateEventDetails();
            applyRegistrationGating();
            checkWaitlistStatus();
        }

        btnJoinWaitlist.setOnClickListener(v -> joinWaitlist());

        return view;
    }

    /**
     * Sets information for event being displayed
     */
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

    /**
     * show status/window + gate the Join button
     */
    private void applyRegistrationGating() {
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
    }

    /**
     * check if already in waitlist and the staus of it
     */
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
                    }
                });
            }
        });
    }

    /**
     * If user wants to join waitlist
     */
    private void joinWaitlist() {
        String userId = userManager.getUserId();
        String userName = userManager.getUserName();
        String userEmail = userManager.getUserEmail();
        String eventId = event.getEvent_id();

        if (userId == null || eventId == null) {
            Toast.makeText(getContext(), "Error: Unable to join waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        btnJoinWaitlist.setEnabled(false);

        waitingListManager.joinWaitingList(eventId, userId, userName, userEmail,
                new WaitingListManager.OnCompleteListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "Successfully joined waitlist!", Toast.LENGTH_SHORT).show();
                        btnJoinWaitlist.setText("Joined Waitlist");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        btnJoinWaitlist.setEnabled(true);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Capitalizes first letter of staus to be displayed
     * @param str The status string to capitalize
     * @return Status The string with first letter capitalized
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * formates time and date to be displayed
     * @param ts unformatted time and date
     * @return time The formatted time and date
     */
    private String fmt(Timestamp ts) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(ts.toDate());
    }

    /**
     * Generate a QR bitmap locally
     * @param text The event information
     * @param size The size qr code that will be displayed
     * @return bitmap the mapping of the qr code
     */
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
