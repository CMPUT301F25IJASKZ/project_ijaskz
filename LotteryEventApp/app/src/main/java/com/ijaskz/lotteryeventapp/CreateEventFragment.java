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

import java.util.UUID;

public class CreateEventFragment extends Fragment {

    private EditText etName, etLocation, etTime, etDescription, etMax;
    private ImageView ivImagePreview;
    private Button btnPickImage, btnSubmit;

    private Uri selectedImageUri = null;   // holds the picked image
    private String uploadedImageUrl = "";  // set after upload

    // 1) Gallery picker (no permission prompt needed)
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

        etName = v.findViewById(R.id.etEventName);
        etLocation = v.findViewById(R.id.etLocation);
        etTime = v.findViewById(R.id.etEventTime);
        etDescription = v.findViewById(R.id.etDescription);
        etMax = v.findViewById(R.id.etMax);
        ivImagePreview = v.findViewById(R.id.ivImagePreview);
        btnPickImage = v.findViewById(R.id.btnPickImage);
        btnSubmit = v.findViewById(R.id.btnSubmitEvent);

        btnPickImage.setOnClickListener(view -> pickImage.launch("image/*"));

        btnSubmit.setOnClickListener(view -> {
            String name = etName.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            int max = parseIntSafe(etMax.getText().toString().trim());

            // If image chosen, upload first; else create event with empty image
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

    // 2) Upload to Firebase Storage, then create the Event with the download URL
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
                .addOnFailureListener(e -> {
                    // fallback: still save event without image
                    saveEvent(description, location, name, max, time, "");
                });
    }

    // 3) Create and write Event (adjust to your Firestore helper)
    private void saveEvent(String description, String location, String name, int max, String time, String imageUrl) {
        Event event = new Event(description, location, name, max, time, imageUrl);
        // TODO: use your existing helper to add the event
        // e.g., new FireStoreHelper().addEvent(event, ...);
    }
}
