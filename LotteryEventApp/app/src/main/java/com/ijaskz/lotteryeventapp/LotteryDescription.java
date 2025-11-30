package com.ijaskz.lotteryeventapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class LotteryDescription extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_lottery_description, container, false);
        TextView tv = v.findViewById(R.id.lottery_Info);
        String description =
                "How the Event Lottery Works\n\n" +
                        "1. **Registration Window**\n" +
                        "   You can sign up for the event during the open registration period. Everyone who registers during this time has an equal chance.\n\n" +
                        "2. **Lottery Selection**\n" +
                        "   When registration ends, the organizer runs the lottery. A limited number of participants are randomly selected based on the event's available spots.\n\n" +
                        "3. **Accept or Decline Your Spot**\n" +
                        "   If you are selected, you’ll be notified and given the option to accept or decline your spot in the event.\n\n" +
                        "4. **Filling Unclaimed Spots**\n" +
                        "   If some participants decline or do not respond in time, the lottery automatically runs again to fill the remaining spots from the waiting list.\n\n" +
                        "This system ensures fairness by giving everyone a chance without requiring constant refreshing or fighting for a spot.";
        tv.setText(description);
        return v;
    }
}