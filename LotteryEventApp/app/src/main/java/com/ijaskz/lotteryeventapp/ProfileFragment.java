package com.ijaskz.lotteryeventapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

/**
 * Defines the ProfileFragment that will display a users information
 */
public class ProfileFragment extends Fragment {

    private ImageView ivProfileAvatar;
    private TextView tvName, tvEmail, tvPhone;
    private Button btnEditProfile, btnDeleteProfile, btnBack;
    private SwitchCompat switchNotifications;
    private UserManager userManager;
    private FirebaseFirestore db;
    private String userDocId;
    private String currentAvatarUrl;
    private Uri selectedImageUri;
    private User user;
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(requireContext()).load(uri).centerCrop().into(ivProfileAvatar);
                    uploadAvatar(uri);
                }
            });


    /**
     * creates the Fragment to be passed to holder
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return v The view to be displayed
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        userManager = new UserManager(getContext());
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        tvName = view.findViewById(R.id.tvProfileName);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvPhone = view.findViewById(R.id.tvProfilePhone);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnEditProfile.setVisibility(("organizer".equals(userManager.getUserType()) || "entrant".equals(userManager.getUserType())) ? View.VISIBLE : View.GONE);
        btnDeleteProfile = view.findViewById(R.id.btnDeleteProfile);
        btnDeleteProfile.setVisibility(("organizer".equals(userManager.getUserType()) || "entrant".equals(userManager.getUserType())) ? View.VISIBLE : View.GONE);
        btnBack= view.findViewById(R.id.btnBack);
        btnBack.setVisibility("admin".equals(userManager.getUserType())  ? View.VISIBLE : View.GONE);

        // Notifications opt-out switch
        switchNotifications = view.findViewById(R.id.switch_notifications);
        if ("entrant".equals(userManager.getUserType())) {
            switchNotifications.setVisibility(View.VISIBLE);
            switchNotifications.setChecked(userManager.isNotificationsEnabled());
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                userManager.setNotificationsEnabled(isChecked);
                Toast.makeText(
                        getContext(),
                        isChecked ? "Notifications enabled" : "Notifications turned off",
                        Toast.LENGTH_SHORT
                ).show();
            });
        } else {
            // Only entrants need this setting
            switchNotifications.setVisibility(View.GONE);
        }

        db = FirebaseFirestore.getInstance();
        if(getArguments() == null){
            user = userManager.createUserClass();
        }else {
            user = (User) getArguments().getSerializable("user");
        }
        loadProfileData();

        ivProfileAvatar.setOnClickListener(v -> pickImage.launch("image/*"));
        btnEditProfile.setOnClickListener(v -> showEditDialog());
        btnDeleteProfile.setOnClickListener(v -> confirmDeleteProfile());
        btnBack.setOnClickListener(v -> {
            UserManagerFragment fragment = new UserManagerFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
                });
        return view;
    }

    /**
     * Loads the users profile from database
     */
    private void loadProfileData() {

        String userId = user.getUser_id();
        tvName.setText(user.getUser_name());
        tvEmail.setText(user.getUser_email());

        if (userId == null) { return;};
            db.collection("users").document(user.getUser_id()).get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded()) { return;}
                            userDocId = doc.getId();
                            String phone = doc.getString("user_phone");
                            currentAvatarUrl = doc.getString("avatar_url");

                            tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Not provided");

                            if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                                Glide.with(requireContext())
                                        .load(currentAvatarUrl)
                                        .centerCrop()
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .into(ivProfileAvatar);
                            }

                    });
        }

    /**
     * Takes picture user wishes to use for profile photo
     * @param uri The URL for picture user wishes to use
     */
    private void uploadAvatar(Uri uri) {
        if (userDocId == null) return;

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("profile_avatars/" + user.getUser_id() + ".jpg");

        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String avatarUrl = downloadUri.toString();
                    db.collection("users")
                            .document(userDocId)
                            .update("avatar_url", avatarUrl)
                            .addOnSuccessListener(aVoid -> {
                                currentAvatarUrl = avatarUrl;
                                Toast.makeText(getContext(), "Avatar updated", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows the edit box and get the information for user, makes sure user filled out required info
     */
    private void showEditDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);

        EditText etName = dialogView.findViewById(R.id.etEditName);
        EditText etEmail = dialogView.findViewById(R.id.etEditEmail);
        EditText etPhone = dialogView.findViewById(R.id.etEditPhone);

        etName.setText(user.getUser_id());
        etEmail.setText(user.getUser_email());
        String currentPhone = tvPhone.getText().toString();
        if (!currentPhone.equals("Not provided")) {
            etPhone.setText(currentPhone);
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newEmail = etEmail.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newEmail.isEmpty() || !newEmail.contains("@")) {
                        Toast.makeText(getContext(), "Valid email required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateProfile(newName, newEmail, newPhone);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * updates profile if user tries
     * @param name The new name user enters
     * @param email The new email user enters
     * @param phone The new phone # the user enters
     */
    private void updateProfile(String name, String email, String phone) {
        if (userDocId == null) {
            Toast.makeText(getContext(), "Error updating profile", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(userDocId)
                .update("user_name", name, "user_email", email, "user_phone", phone)
                .addOnSuccessListener(aVoid -> {
                    userManager.saveUser(userManager.getUserId(), userManager.getUserType(), email, name);
                    loadProfileData();
                    Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Confirmation that user wants to delete profile
     */
    private void confirmDeleteProfile() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure? This will permanently delete your profile and all waiting list entries.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * deletion of profile from database and redirection to login screen
     */
    private void deleteProfile() {
        String userId = user.getUser_id();
        if (userId == null) return;

        btnDeleteProfile.setEnabled(false);

        db.collection("waiting_list")
                .whereEqualTo("entrant_id", userId)
                .get()
                .addOnSuccessListener(waitingListSnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : waitingListSnapshot) {
                        doc.getReference().delete();
                    }

                    db.collection("users")
                            .whereEqualTo("user_id", userId)
                            .get()
                            .addOnSuccessListener(userSnapshot -> {
                                if (!userSnapshot.isEmpty()) {
                                    userSnapshot.getDocuments().get(0).getReference().delete();
                                }

                                Toast.makeText(getContext(), "Profile deleted", Toast.LENGTH_SHORT).show();
                                userManager.logout();

                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                if (getActivity() != null) getActivity().finish();
                            })
                            .addOnFailureListener(e -> {
                                btnDeleteProfile.setEnabled(true);
                                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }
}