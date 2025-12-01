package com.ijaskz.lotteryeventapp;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// ZXing (QR)
import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Defines the CreateEventFragment where organizers can create events.
 */
public class CreateEventFragment extends Fragment {

    private EditText etName, etLocation, etTime, etDescription, etMax, etRegStart, etRegEnd;
    /** Optional waiting list limit field */
    private EditText etWaitlistLimit;
    private ImageView ivImagePreview;
    private Button btnPickImage, btnSubmit;

    // Added for QR preview
    private Button btnGenerateQR;
    private ImageView ivQrPreview;

    private Cloudinary cloudinary;
    private String pendingEventId = null;

    private Uri selectedImageUri = null;
    private String uploadedImageUrl = "";
    private Date regStartDate = null, regEndDate = null;

    /**
     * Image picker launcher for selecting an event image.
     */
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(requireContext()).load(uri).centerCrop().into(ivImagePreview);
                    ivImagePreview.setTag("image_loaded");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_event, container, false);

        // init cloudinary for image uses
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dmhywl2qk");
        config.put("api_key", "817482869461352");
        config.put("api_secret", "v_Zs360--cUHKgAgZvdqk0iLFvo");
        cloudinary = new Cloudinary(config);

        etName = v.findViewById(R.id.et_event_name);
        etLocation = v.findViewById(R.id.et_location);
        etTime = v.findViewById(R.id.et_event_time);
        etDescription = v.findViewById(R.id.et_event_description);
        etMax = v.findViewById(R.id.et_max);
        etRegStart = v.findViewById(R.id.et_reg_start);
        etRegEnd = v.findViewById(R.id.et_reg_end);
        etWaitlistLimit = v.findViewById(R.id.et_waitlist_limit);

        ivImagePreview = v.findViewById(R.id.ivImagePreview);
        btnPickImage = v.findViewById(R.id.btnPickImage);
        btnSubmit = v.findViewById(R.id.btn_submit_event);

        // New QR preview UI
        btnGenerateQR = v.findViewById(R.id.btn_generate_qr);
        ivQrPreview = v.findViewById(R.id.iv_qr_preview);
        if (ivQrPreview != null) ivQrPreview.setVisibility(View.GONE);

        // Date-time pickers
        if (etRegStart != null) {
            etRegStart.setOnClickListener(view -> pickDateTime(d -> {
                regStartDate = d;
                etRegStart.setText(fmt(d));
            }));
        }
        if (etRegEnd != null) {
            etRegEnd.setOnClickListener(view -> pickDateTime(d -> {
                regEndDate = d;
                etRegEnd.setText(fmt(d));
            }));
        }

        btnPickImage.setOnClickListener(view -> pickImage.launch("image/*"));

        // Generate QR preview
        if (btnGenerateQR != null) {
            btnGenerateQR.setOnClickListener(view -> {
                if (pendingEventId == null) {
                    pendingEventId = FirebaseFirestore.getInstance()
                            .collection("events").document().getId();
                }
                String deeplink = "lotteryevent://event/" + pendingEventId;
                Bitmap bmp = generateQrBitmap(deeplink, 700);
                if (bmp != null && ivQrPreview != null) {
                    ivQrPreview.setImageBitmap(bmp);
                    ivQrPreview.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "QR preview generated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Could not generate QR", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnSubmit.setOnClickListener(view -> {
            String name = etName.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            int max = parseIntSafe(etMax.getText().toString().trim());

            // Optional waitlist limit
            Integer waitlistLimit = null;
            if (etWaitlistLimit != null) {
                String limitStr = etWaitlistLimit.getText().toString().trim();
                if (!limitStr.isEmpty()) {
                    try {
                        int parsed = Integer.parseInt(limitStr);
                        if (parsed > 0) waitlistLimit = parsed;
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (regStartDate == null || regEndDate == null) {
                Toast.makeText(requireContext(), "Set registration start and end", Toast.LENGTH_SHORT).show();
                return;
            }
            if (regStartDate.after(regEndDate)) {
                Toast.makeText(requireContext(), "Start must be before end", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedImageUri != null) {
                uploadImageThenSaveEvent(selectedImageUri, name, location, time, description, max, waitlistLimit);
            } else {
                UserManager userManager = new UserManager(requireContext());
                String nizer_name = userManager.getUserName();
                saveEvent(description, location, name, max, time, "", nizer_name, waitlistLimit);
            }
        });

        return v;
    }

    /**
     * Ensures max entrants is a valid number.
     */
    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    /**
     * Uploads an image first, then proceeds to event saving.
     */
    private void uploadImageThenSaveEvent(Uri uri, String name, String location, String time,
                                          String description, int max, Integer waitlistLimit) {
        Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
        btnSubmit.setEnabled(false);

        new Thread(() -> {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);

                Map uploadResult = cloudinary.uploader().upload(inputStream, ObjectUtils.asMap(
                        "folder", "event_images",
                        "resource_type", "image"
                ));

                String imageUrl = (String) uploadResult.get("secure_url");

                requireActivity().runOnUiThread(() -> {
                    uploadedImageUrl = imageUrl;
                    UserManager userManager = new UserManager(requireContext());
                    String nizer_name = userManager.getUserName();
                    saveEvent(description, location, name, max, time, uploadedImageUrl, nizer_name, waitlistLimit);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    btnSubmit.setEnabled(true);
                });
            }
        }).start();
    }

    /**
     * Saves event data to Firestore.
     */
    private void saveEvent(String description, String location, String name, int max,
                           String time, String imageUrl, String org_name, Integer waitlistLimit) {

        Map<String, Object> data = new HashMap<>();
        data.put("event_name", name);
        data.put("event_description", description);
        data.put("event_time", time);
        data.put("image", imageUrl);
        data.put("organizer_name", org_name);
        data.put("location", location);
        data.put("max", max);
        data.put("createdAt", com.google.firebase.Timestamp.now());
        data.put("lotteryRun", false);
        data.put("waitlistLimit", waitlistLimit);

        com.google.firebase.Timestamp regStartTs = new com.google.firebase.Timestamp(regStartDate);
        com.google.firebase.Timestamp regEndTs = new com.google.firebase.Timestamp(regEndDate);
        data.put("registrationStart", regStartTs);
        data.put("registrationEnd", regEndTs);
        data.put("status", "active");

        btnSubmit.setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // If QR was generated earlier
        if (pendingEventId != null) {
            final String docId = pendingEventId;
            data.put("event_id", docId);
            data.put("deeplink", "lotteryevent://event/" + docId);

            db.collection("events").document(docId)
                    .set(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(requireContext(), "Event submitted!", Toast.LENGTH_SHORT).show();
                        if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(requireContext(), "Could not submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {

            // Normal creation
            db.collection("events")
                    .add(data)
                    .addOnSuccessListener(doc -> {
                        String docId = doc.getId();
                        Map<String,Object> up = new HashMap<>();
                        up.put("event_id", docId);
                        up.put("deeplink", "lotteryevent://event/" + docId);
                        db.collection("events").document(docId).update(up);

                        Toast.makeText(requireContext(), "Event submitted!", Toast.LENGTH_SHORT).show();
                        if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(requireContext(), "Could not submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    /**
     * Callback for when a date and time are selected.
     */
    private interface DatePicked { void onPicked(Date d); }

    /**
     * Opens date and time pickers and returns a final Date.
     */
    private void pickDateTime(DatePicked cb) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    final Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, y);
                    cal.set(Calendar.MONTH, m);
                    cal.set(Calendar.DAY_OF_MONTH, d);
                    new TimePickerDialog(requireContext(),
                            (tp, hh, mm) -> {
                                cal.set(Calendar.HOUR_OF_DAY, hh);
                                cal.set(Calendar.MINUTE, mm);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                cb.onPicked(cal.getTime());
                            },
                            c.get(Calendar.HOUR_OF_DAY),
                            c.get(Calendar.MINUTE),
                            false
                    ).show();
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /**
     * Formats dates for UI display.
     */
    private String fmt(Date d) {
        return new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(d);
    }

    /**
     * Creates a QR code bitmap from a string.
     */
    private Bitmap generateQrBitmap(String content, int sizePx) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            return null;
        }
    }
}
