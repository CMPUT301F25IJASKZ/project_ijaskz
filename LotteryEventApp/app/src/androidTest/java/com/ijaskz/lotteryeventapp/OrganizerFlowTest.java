package com.ijaskz.lotteryeventapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.VerificationModes.times;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Happy-path organizer UI tests for the Create Event flow.
 * - Prefills SharedPreferences with organizer role
 * - Navigates via the drawer
 * - Stubs gallery picker and verifies previews
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerFlowTest {

    // --------- Test rule (launch as organizer) ---------
    @Rule
    public ActivityScenarioRule<MainActivity> mainRule =
            new ActivityScenarioRule<>(organizerIntent());

    // --------- Constants / matchers ----------
    private static final Uri FAKE_IMAGE_URI =
            Uri.parse("content://media/external/images/media/1");

    /** Matches ACTION_GET_CONTENT or a chooser wrapping it. */
    private static final Matcher<Intent> PICKER_INTENT = anyOf(
            hasAction(Intent.ACTION_GET_CONTENT),
            allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(is(Intent.EXTRA_INTENT), hasAction(Intent.ACTION_GET_CONTENT))
            )
    );

    // --------- Lifecycle ----------
    @Before public void setUp() { Intents.init(); }
    @After  public void tearDown() { Intents.release(); }

    // --------- Helpers ----------
    /** Launches MainActivity with prefs marking the user as an organizer. */
    private static Intent organizerIntent() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("user_type", "organizer")
                .putString("userType", "organizer")
                .apply();
        return new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** Opens the navigation drawer and navigates to Create Event. */
    private void goToCreateEvent() {
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_create_event)).perform(click());
        onView(withId(R.id.et_event_name)).check(matches(isDisplayed()));
    }

    /** Clicks OK on date picker then OK on time picker. */
    private void confirmDateThenTime() {
        onView(withId(android.R.id.button1)).perform(click()); // Date OK
        onView(withId(android.R.id.button1)).perform(click()); // Time OK
    }

    /** Types text into an EditText with scroll safety. */
    private void typeInto(int viewId, String text) {
        onView(withId(viewId)).perform(scrollTo(), replaceText(text));
    }

    /** Stubs the next gallery pick to return the given URI. */
    private void stubNextPick(Uri uri) {
        Intent data = new Intent().setData(uri);
        Instrumentation.ActivityResult result =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, data);
        intending(PICKER_INTENT).respondWith(result);
    }

    // --------- Tests ----------

    /** US 02.01.01 — Generate QR Code shows preview. */
    @Test
    public void createEvent_andGenerateQr_displaysQrImage() {
        goToCreateEvent();

        typeInto(R.id.et_event_name, "Campus Fest");
        typeInto(R.id.et_location, "Main Hall");
        typeInto(R.id.et_event_time, "2025-11-20 18:00");
        onView(withId(R.id.et_max)).perform(scrollTo(), replaceText("100"), closeSoftKeyboard());

        onView(withId(R.id.btn_generate_qr)).perform(scrollTo(), click());
        onView(withId(R.id.iv_qr_preview)).check(matches(isDisplayed()));
    }

    /** US 02.01.04 — Set registration period (DatePicker + TimePicker). */
    @Test
    public void setRegistrationPeriod_andSave_showsSavedState() {
        goToCreateEvent();

        onView(withId(R.id.et_reg_start)).perform(scrollTo(), click());
        confirmDateThenTime();

        onView(withId(R.id.et_reg_end)).perform(scrollTo(), click());
        confirmDateThenTime();

        onView(withId(R.id.et_reg_start)).check(matches(isDisplayed()));
        onView(withId(R.id.et_reg_end)).check(matches(isDisplayed()));
    }

    /** US 02.04.01 — Upload poster from gallery shows preview. */
    @Test
    public void uploadPoster_fromGallery_displaysPoster() {
        goToCreateEvent();

        stubNextPick(FAKE_IMAGE_URI);
        onView(withId(R.id.btnPickImage)).perform(scrollTo(), click());
        intended(PICKER_INTENT, times(1));

        onView(withId(R.id.ivImagePreview)).check(matches(isDisplayed()));
    }

    /** US 02.04.02 — Updating poster replaces preview (verifies 2 launches total). */
    @Test
    public void updatePoster_replacesExistingPosterPreview() {
        goToCreateEvent();

        // 1st pick
        stubNextPick(FAKE_IMAGE_URI);
        onView(withId(R.id.btnPickImage)).perform(scrollTo(), click());
        intended(PICKER_INTENT, times(1));
        onView(withId(R.id.ivImagePreview)).check(matches(isDisplayed()));

        // 2nd pick
        stubNextPick(Uri.parse("content://media/external/images/media/2"));
        onView(withId(R.id.btnPickImage)).perform(scrollTo(), click());
        intended(PICKER_INTENT, times(2));

        onView(withId(R.id.ivImagePreview)).check(matches(isDisplayed()));
    }
}
