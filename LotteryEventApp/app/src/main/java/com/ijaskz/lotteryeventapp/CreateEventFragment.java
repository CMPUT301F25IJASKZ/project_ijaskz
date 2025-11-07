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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// ZXing (QR)
import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class CreateEventFragment extends Fragment {

    private EditText etName, etLocation, etTime, etDescription, etMax, etRegStart, etRegEnd;
    private ImageView ivImagePreview;
    private Button btnPickImage, btnSubmit;

    // Added for QR preview
    private Button btnGenerateQR;
    private ImageView ivQrPreview;
    private String pendingEventId = null;

    private Uri selectedImageUri = null;
    private String uploadedImageUrl = "";
    private Date regStartDate = null, regEndDate = null;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(requireContext()).load(uri).centerCrop().into(ivImagePreview);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_event, container, false);

        etName = v.findViewById(R.id.et_event_name);
        etLocation = v.findViewById(R.id.et_location);
        etTime = v.findViewById(R.id.et_event_time);
        etDescription = v.findViewById(R.id.et_event_description);
        etMax = v.findViewById(R.id.et_max);
        ivImagePreview = v.findViewById(R.id.ivImagePreview);
        btnPickImage = v.findViewById(R.id.btnPickImage);
        btnSubmit = v.findViewById(R.id.btn_submit_event);
        etRegStart = v.findViewById(R.id.et_reg_start);
        etRegEnd = v.findViewById(R.id.et_reg_end);

        // New views (ensure these IDs exist in your XML)
        btnGenerateQR = v.findViewById(R.id.btn_generate_qr);
        ivQrPreview = v.findViewById(R.id.iv_qr_preview);
        if (ivQrPreview != null) ivQrPreview.setVisibility(View.GONE);

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

        // Generate QR preview on demand (creates a future docId so the deeplink is correct)
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

            if (regStartDate == null || regEndDate == null) {
                Toast.makeText(requireContext(), "Set registration start and end", Toast.LENGTH_SHORT).show();
                return;
            }
            if (regStartDate.after(regEndDate)) {
                Toast.makeText(requireContext(), "Start must be before end", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedImageUri != null) {
                uploadImageThenSaveEvent(selectedImageUri, name, location, time, description, max);
            } else {
                saveEvent(description, location, name, max, time, "");
            }
        });

        return v;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private void uploadImageThenSaveEvent(Uri uri, String name, String location, String time,
                                          String description, int max) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("event_images/" + UUID.randomUUID() + ".jpg");

        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    uploadedImageUrl = downloadUri.toString();
                    saveEvent(description, location, name, max, time, uploadedImageUrl);
                })
                .addOnFailureListener(e -> saveEvent(description, location, name, max, time, ""));
    }

    private void saveEvent(String description, String location, String name, int max, String time, String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("event_name", name);
        data.put("event_description", description);
        data.put("event_time", time);
        data.put("image", imageUrl);
        data.put("location", location);
        data.put("max", max);
        data.put("createdAt", com.google.firebase.Timestamp.now());

        com.google.firebase.Timestamp regStartTs = new com.google.firebase.Timestamp(regStartDate);
        com.google.firebase.Timestamp regEndTs = new com.google.firebase.Timestamp(regEndDate);
        data.put("registrationStart", regStartTs);
        data.put("registrationEnd", regEndTs);
        data.put("status", "active");

        btnSubmit.setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    private interface DatePicked { void onPicked(Date d); }

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

    private String fmt(Date d) {
        return new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(d);
    }

    // ===== QR helper =====
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
