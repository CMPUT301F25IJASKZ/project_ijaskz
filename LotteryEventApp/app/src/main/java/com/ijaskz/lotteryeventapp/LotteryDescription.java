package com.ijaskz.lotteryeventapp;

import android.app.Activity;
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
        String description = "How The Lottery Works:\n\n After an event opens people are able to join the waitlist for the event. When the deadline passes the organizer can choose to run a lottery to pick the people participating in said event from the waitlist. You can then choose to accept or deny this invite.\n\nAfter receiving the invite in the 'My Wait lists' tab and accepting you will be put on the participating list and you're all signed up. To see all events you have accepted look in the 'My Waiting Lists' tab\n\nIf the invite it rejected the lottery is then run again for the spot to be filled. Keep an eye on the 'My Waiting Lists' tab cause even if your not chosen at first you may be chosen if someone drops out.\n\n Our Lottery system enables a nice fair way to find out and participate in events. There is no special treatment in the lottery and no way to improve chances to being picked. So get to signing up and join an event near you.";
        tv.setText(description);
        return v;
    }
}