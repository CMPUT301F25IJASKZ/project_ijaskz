package com.ijaskz.lotteryeventapp;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditEventFragment extends Fragment {

    private EditText etName, etLocation, etTime, etDescription, etMax;
    private ImageView ivImagePreview;
    private Button btnPickImage, btnRemoveImage, btnUpdate;

    private Event event;                    // original event
    private Uri selectedImageUri = null;    // new image (if chosen)
    private String currentImageUrl = "";    // keeps existing image if not changed

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
        View v = inflater.inflate(R.layout.fragment_edit_event, container, false);

        etName         = v.findViewById(R.id.et_event_name);
        etLocation     = v.findViewById(R.id.et_location);
        etTime         = v.findViewById(R.id.et_event_time);
        etDescription  = v.findViewById(R.id.et_event_description);
        etMax          = v.findViewById(R.id.et_max);
        ivImagePreview = v.findViewById(R.id.ivImagePreview);
        btnPickImage   = v.findViewById(R.id.btnPickImage);
        btnRemoveImage = v.findViewById(R.id.btnRemoveImage);
        btnUpdate      = v.findViewById(R.id.btn_update_event);

        // 1) Get event from args
        event = (Event) getArguments().getSerializable("event");
        if (event == null) {
            Toast.makeText(requireContext(), "No event supplied", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return v;
        }

        // 2) Prefill
        etName.setText(event.getEvent_name());
        etLocation.setText(event.getLocation());
        etTime.setText(event.getEvent_time());
        etDescription.setText(event.getEvent_description());
        etMax.setText(String.valueOf(event.getMax()));
        currentImageUrl = event.getImage();
        if (currentImageUrl != null && currentImageUrl.startsWith("http")) {
            Glide.with(requireContext()).load(currentImageUrl).centerCrop().into(ivImagePreview);
        } else {
            ivImagePreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // 3) Pick/Remove image
        btnPickImage.setOnClickListener(v1 -> pickImage.launch("image/*"));
        btnRemoveImage.setOnClickListener(v12 -> {
            selectedImageUri = null;
            currentImageUrl = ""; // mark as removed
            ivImagePreview.setImageResource(android.R.drawable.ic_menu_gallery);
        });

        // 4) Update
        btnUpdate.setOnClickListener(v13 -> {
            String name = etName.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            int max = safeInt(etMax.getText().toString().trim());

            if (selectedImageUri != null) {
                uploadImageThenUpdate(selectedImageUri, name, location, time, description, max);
            } else {
                updateEvent(name, location, time, description, max, currentImageUrl);
            }
        });

        return v;
    }

    private int safeInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }

    private void uploadImageThenUpdate(Uri uri, String name, String location, String time,
                                       String description, int max) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("event_images/" + UUID.randomUUID() + ".jpg");

        ref.putFile(uri)
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri ->
                        updateEvent(name, location, time, description, max, downloadUri.toString()))
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Image upload failed, keeping old image.", Toast.LENGTH_SHORT).show();
                    updateEvent(name, location, time, description, max, currentImageUrl);
                });
    }

    private void updateEvent(String name, String location, String time,
                             String description, int max, String imageUrl) {
        String id = event.getEvent_id();

        Map<String, Object> updates = new HashMap<>();
        updates.put("event_name", name);
        updates.put("location", location);
        updates.put("event_time", time);
        updates.put("event_description", description);
        updates.put("max", max);
        updates.put("image", imageUrl);

        // keep lists/applied/picked/notPicked as-is
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(id)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Event updated", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
