package com.ijaskz.lotteryeventapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.google.firebase.firestore.FirebaseFirestore;
import com.ijaskz.lotteryeventapp.MainActivity;
import com.ijaskz.lotteryeventapp.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AdminTests {

    private static final String TEST_ORG_ID = "test-organizer-" + System.currentTimeMillis();
    private static final String TEST_ORG_NAME = "Test Organizer To Delete";

    @Rule
    public ActivityScenarioRule<MainActivity> mainRule =
            new ActivityScenarioRule<>(adminIntent());

    @Before
    public void setUp() throws Exception {
        createTestOrganizer();
        Intents.init();

    }

    @After
    public void tearDown() {
        Intents.release();
    }

    private void goToManageUsers() {
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_manage_profiles)).perform(click());
    }

    private static Intent adminIntent() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("user_type", "admin")
                .putString("userType", "admin")
                .apply();
        return new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }



    private void createTestOrganizer() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("user_name", TEST_ORG_NAME);
        data.put("user_email", "test_org_delete@example.com");
        data.put("user_type", "organizer");
        data.put("user_phone", "999-999-9999");

        CountDownLatch latch = new CountDownLatch(1);

        db.collection("users").document(TEST_ORG_ID)
                .set(data)
                .addOnSuccessListener(v -> {
                    android.util.Log.d("AdminTests", "Test organizer created");
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminTests", "Failed to create test organizer", e);
                    latch.countDown();
                });

        // wait for write to complete so we don't race against UI
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out creating test organizer");
        }
    }
// Tests: US 03.02.01 As an administrator, I want to be able to remove profiles.
    @Test
    public void adminCanDeleteOrganizer() throws Exception {
        goToManageUsers();

        // Wait a moment for Firestore to load users (for real projects, use IdlingResource)
        Thread.sleep(2000);

        onView(withId(R.id.searchView)).perform(ViewActions.typeText("Test"));
        Thread.sleep(2000);
        onView(withId(R.id.btnDelete)).perform(click());

        // If you show a confirm dialog, handle it here:
        // onView(withText("Delete")).perform(click());

        // Check it's no longer visible in the list
        onView(withText(TEST_ORG_NAME)).check(doesNotExist());

        // Optional: verify Firestore doc is deleted
        assertUserDeleted(TEST_ORG_ID);
    }

    private void assertUserDeleted(String userId) throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] exists = {true};

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    exists[0] = true; // treat failure as "not sure"
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);
        assert !exists[0] : "User document was not deleted";
    }
    //tests; US 03.07.01 As an administrator I want to remove organizers that violate app policy
    @Test
    public void canDemote() throws Exception {
        goToManageUsers();
        Thread.sleep(2000);
        onView(withId(R.id.searchView)).perform(ViewActions.typeText("Test"));
        Thread.sleep(2000);
        onView(withId(R.id.btnDemote)).perform(click());
        onView(withId(R.id.searchView)).perform(ViewActions.typeText("_"));
        //onView(withId(R.id.btnDemote)).check(doesNotExist());
        onView(withText("Promote")).check(matches(isDisplayed()));
        onView(withId(R.id.btnDelete)).perform(click());
        assertUserDeleted(TEST_ORG_ID);
    }
}
