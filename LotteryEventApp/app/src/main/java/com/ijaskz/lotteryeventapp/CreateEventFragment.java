package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CreateEventFragment extends Fragment {

    private EditText etName, etLocation, etTime, etDescription, etMax;
    private Button btnSubmit;
    private final FireStoreHelper fireStoreHelper = new FireStoreHelper();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        etName = view.findViewById(R.id.et_event_name);
        etLocation = view.findViewById(R.id.et_location);
        etTime = view.findViewById(R.id.et_event_time);
        etDescription = view.findViewById(R.id.et_event_description);
        etMax = view.findViewById(R.id.et_max);
        btnSubmit = view.findViewById(R.id.btn_submit_event);

        btnSubmit.setOnClickListener(v -> submitEvent());

        return view;
    }

    private void submitEvent() {
        String name = etName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String maxStr = etMax.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(location) ||
                TextUtils.isEmpty(time) || TextUtils.isEmpty(description) || TextUtils.isEmpty(maxStr)) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int max;
        try {
            max = Integer.parseInt(maxStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Max attendees must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = new Event(description, location, name, max, time);


        fireStoreHelper.addEvent(event);
        Toast.makeText(getContext(), "Event submitted", Toast.LENGTH_SHORT).show();


        etName.setText("");
        etLocation.setText("");
        etTime.setText("");
        etDescription.setText("");
        etMax.setText("");
    }
}
