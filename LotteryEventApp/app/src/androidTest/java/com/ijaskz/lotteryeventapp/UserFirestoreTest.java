package com.ijaskz.lotteryeventapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * Firebase UI test for registering and deleting a user.
 */
public class UserFirestoreTest {
    @Rule
    public ActivityScenarioRule<RegisterActivity> registerRule =
            new ActivityScenarioRule<>(RegisterActivity.class);
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FireStoreHelper helper = new FireStoreHelper();

    /**
     * Verifies that a user can be registered and then deleted from Firestore.
     */
    @Test
    public void testAdd_DeleteUsers() throws ExecutionException, InterruptedException {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        String name = "Test User " + uniqueSuffix;
        String email = "testuser" + uniqueSuffix + "@example.com";
        String phone = "5551234567";
        String password = "abcdef";
        onView(withId(R.id.name_input))
                .perform(typeText(name), closeSoftKeyboard());
        onView(withId(R.id.email_input))
                .perform(typeText(email), closeSoftKeyboard());
        onView(withId(R.id.phone_input))
                .perform(typeText(phone), closeSoftKeyboard());
        onView(withId(R.id.password_input))
                .perform(typeText(password), closeSoftKeyboard());
        onView(withId(R.id.confirm_password_input))
                .perform(typeText(password), closeSoftKeyboard());
        onView(withId(R.id.register_button)).perform(click());
        Thread.sleep(500);
        QuerySnapshot snap = Tasks.await(
                db.collection("users")
                        .whereEqualTo("user_email", email)
                        .limit(1)
                        .get()
        );
        Assert.assertFalse("User document should exist after registration", snap.isEmpty());
        DocumentSnapshot userDoc = snap.getDocuments().get(0);
        String userDocId = userDoc.getId();
        String storedEmail = userDoc.getString("user_email");
        Assert.assertEquals(email, storedEmail);
        Tasks.await(
                db.collection("users")
                        .document(userDocId)
                        .delete()
        );
        Tasks.await(
                db.collection("users")
                        .document(userDocId)
                        .delete()
        );
        DocumentSnapshot afterDelete = Tasks.await(
                db.collection("users")
                        .document(userDocId)
                        .get()
        );
        Assert.assertFalse("User document should NOT exist after delete", afterDelete.exists());
    }
}
