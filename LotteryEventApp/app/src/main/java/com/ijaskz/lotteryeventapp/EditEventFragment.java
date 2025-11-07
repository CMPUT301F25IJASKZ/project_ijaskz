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
import com.google.firebase.Timestamp;

import java.util.UUID;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines EditEventFragment where Admins and organizers can change event details including removing images
 */
public class EditEventFragment extends Fragment {

    private EditText etName, etLocation, etTime, etDescription, etMax;
    private ImageView ivImagePreview;
    private Button btnPickImage, btnSubmit;

    private Uri selectedImageUri = null;   // holds the picked image
    private String uploadedImageUrl = "";  // set after upload

    // registration window UI + data
    private EditText etRegStart, etRegEnd;
    private Date regStartDate = null, regEndDate = null;

    private Event event; // existing event to edit

    // Gallery picker
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(requireContext()).load(uri).centerCrop().into(ivImagePreview);
                }
            });

    public static EditEventFragment newInstance(Event event) {
        EditEventFragment f = new EditEventFragment();
        Bundle b = new Bundle();
        b.putSerializable("event", event);
        f.setArguments(b);
        return f;
    }

    /**
     * Creation of Fragment to be sent to holder
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return View the view to be displayed
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_edit_event, container, false);

        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable("event");
        }

        etName         = v.findViewById(R.id.et_event_name);
        etLocation     = v.findViewById(R.id.et_location);
        etTime         = v.findViewById(R.id.et_event_time);
        etDescription  = v.findViewById(R.id.et_event_description);
        etMax          = v.findViewById(R.id.et_max);
        ivImagePreview = v.findViewById(R.id.ivImagePreview);
        btnPickImage   = v.findViewById(R.id.btnPickImage);
        btnSubmit      = v.findViewById(R.id.btn_update_event);
        etRegStart     = v.findViewById(R.id.et_reg_start);
        etRegEnd       = v.findViewById(R.id.et_reg_end);

        if (btnSubmit == null) {
            Toast.makeText(requireContext(), "Edit button not found in layout", Toast.LENGTH_SHORT).show();
            return v;
        }

        if (event != null) {
            etName.setText(event.getEvent_name());
            etLocation.setText(event.getLocation());
            etTime.setText(event.getEvent_time());
            etDescription.setText(event.getEvent_description());
            etMax.setText(String.valueOf(event.getMax()));

            String imageUrl = event.getImage();
            if (imageUrl != null && !imageUrl.isEmpty()
                    && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                Glide.with(this).load(imageUrl).centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivImagePreview);
                uploadedImageUrl = imageUrl;
            } else {
                ivImagePreview.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            if (event.getRegistrationStart() != null) {
                regStartDate = event.getRegistrationStart().toDate();
                if (etRegStart != null) etRegStart.setText(fmt(regStartDate));
            }
            if (event.getRegistrationEnd() != null) {
                regEndDate = event.getRegistrationEnd().toDate();
                if (etRegEnd != null) etRegEnd.setText(fmt(regEndDate));
            }
        }

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
                uploadImageThenUpdateEvent(selectedImageUri, name, location, time, description, max);
            } else {
                updateEvent(description, location, name, max, time, uploadedImageUrl);
            }
        });

        return v;
    }

    /**
     * Makes sure the editor enters a number for max entrants
     * @param s String to be converted
     * @return max number of entrants
     */
    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    /**
     * starts saving the event with the image before moving on to other info
     * @param uri Url for image for event
     * @param name the name of the event
     * @param location the location of event
     * @param time the time of the event
     * @param description the description of the event
     * @param max the max # of entrants for event
     */
    private void uploadImageThenUpdateEvent(Uri uri, String name, String location, String time,
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
                    updateEvent(description, location, name, max, time, uploadedImageUrl);
                })
                .addOnFailureListener(e -> {
                    // fallback: keep old image url
                    updateEvent(description, location, name, max, time, uploadedImageUrl);
                });
    }

    /**
     * Saves event to firebase
     * @param description event description
     * @param location event location
     * @param name event name
     * @param max max # of entrants
     * @param time time of event
     * @param imageUrl image URL for event poster
     */
    private void updateEvent(String description, String location, String name, int max, String time, String imageUrl) {
        if (event == null || event.getEvent_id() == null || event.getEvent_id().isEmpty()) {
            Toast.makeText(requireContext(), "Missing event id", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("event_description", description);
        data.put("location", location);
        data.put("event_name", name);
        data.put("max", max);
        data.put("event_time", time);
        data.put("image", imageUrl);
        data.put("registrationStart", new Timestamp(regStartDate));
        data.put("registrationEnd", new Timestamp(regEndDate));

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(event.getEvent_id())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Event updated", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed(); // or navigate up
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    /**
     * Interface for picking date
     */
    private interface DatePicked { void onPicked(Date d); }
    /**
     * Allows editor of event to pick date and time with calendar
     * @param cb return the Date picked
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
     * Formating the Date and time
     * @param d Date to be formatted
     * @return date The date in the correct format for saving
     */
    private String fmt(Date d) {
        return new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(d);
    }
}
