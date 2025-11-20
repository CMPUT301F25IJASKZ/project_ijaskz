package com.ijaskz.lotteryeventapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * Black-box instrumented tests for Profile Management (Create, Update, Delete).
 * Tests user-visible behavior without knowledge of implementation details.
 * 
 * Preconditions:
 * - App must be installed on device/emulator
 * - Firebase must be configured
 * - Test user accounts should be cleaned up between runs
 * 
 * Test Coverage:
 * - Create profile with name, email, password
 * - Update profile fields with validation
 * - Delete profile as entrant with confirmation
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProfileManagementTest {

    // Test credentials - using entrant account
    private static final String TEST_EMAIL = "ben@gmail.com";
    private static final String TEST_PASSWORD = "benben";

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    /**
     * Login before each test to ensure clean state
     */
    @Before
    public void setUp() throws InterruptedException {
        // Login with test credentials
        onView(withId(R.id.emailEditText))
                .perform(replaceText(TEST_EMAIL), closeSoftKeyboard());
        onView(withId(R.id.passwordEditText))
                .perform(replaceText(TEST_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());
        
        // Wait for MainActivity to load
        Thread.sleep(2000);
    }

    // ========== CREATE PROFILE TESTS ==========

    /**
     * TC-CREATE-01: Valid Profile Creation
     * Tests successful registration with valid name, email, and password.
     * NOTE: Skipped - requires logout and navigation to registration screen
     */
    @Ignore("Requires logout and registration flow - test manually")
    @Test
    public void test_A_CreateProfile_ValidData_Success() {
        // Navigate to register screen (implementation depends on your app flow)
        // This is a template - adjust IDs based on your actual layout
        
        // Enter valid data
        onView(withId(R.id.name_input))
                .perform(replaceText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.email_input))
                .perform(replaceText("john.doe.test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.password_input))
                .perform(replaceText("SecurePass123!"), closeSoftKeyboard());
        onView(withId(R.id.confirm_password_input))
                .perform(replaceText("SecurePass123!"), closeSoftKeyboard());
        
        // Submit registration
        onView(withId(R.id.register_button)).perform(click());
        
        // Verify success - should navigate to main screen or show profile
        // Adjust assertion based on your app's post-registration behavior
        onView(withId(R.id.fragment_container)).check(matches(isDisplayed()));
    }

    /**
     * TC-CREATE-02: Empty Name Field
     * Tests that registration fails when name is empty.
     */
    @Ignore("Requires logout and registration flow - test manually")
    @Test
    public void test_A_CreateProfile_EmptyName_ShowsError() {
        onView(withId(R.id.name_input))
                .perform(replaceText(""), closeSoftKeyboard());
        onView(withId(R.id.email_input))
                .perform(replaceText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.password_input))
                .perform(replaceText("SecurePass123!"), closeSoftKeyboard());
        
        onView(withId(R.id.register_button)).perform(click());
        
        // Verify error message appears (adjust based on your error display method)
        onView(withText("Name cannot be empty")).check(matches(isDisplayed()));
    }

    /**
     * TC-CREATE-03: Invalid Email Format
     * Tests that registration fails with invalid email format.
     */
    @Ignore("Requires logout and registration flow - test manually")
    @Test
    public void test_A_CreateProfile_InvalidEmail_ShowsError() {
        onView(withId(R.id.name_input))
                .perform(replaceText("Jane Smith"), closeSoftKeyboard());
        onView(withId(R.id.email_input))
                .perform(replaceText("invalidemail"), closeSoftKeyboard());
        onView(withId(R.id.password_input))
                .perform(replaceText("SecurePass123!"), closeSoftKeyboard());
        
        onView(withId(R.id.register_button)).perform(click());
        
        // Verify email validation error
        onView(withText("Valid email required")).check(matches(isDisplayed()));
    }

    // ========== UPDATE PROFILE TESTS ==========

    /**
     * TC-UPDATE-01: Update Name Successfully
     * Tests successful name update in profile.
     * Precondition: User must be logged in.
     */
    @Test
    public void test_B_UpdateProfile_ValidName_Success() {
        // Navigate to profile screen
        // Open navigation drawer
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        
        // Tap edit profile
        onView(withId(R.id.btnEditProfile)).perform(click());
        
        // Update name
        onView(withId(R.id.etEditName))
                .perform(replaceText("Jonathan Doe"), closeSoftKeyboard());
        
        // Save changes
        onView(withText("Save")).perform(click());
        
        // Verify success toast
        onView(withText("Profile updated")).check(matches(isDisplayed()));
        
        // Verify updated name is displayed
        onView(withId(R.id.tvProfileName))
                .check(matches(withText("Jonathan Doe")));
    }

    /**
     * TC-UPDATE-03: Update Name to Empty
     * Tests that updating name to empty value shows error.
     */
    @Test
    public void test_B_UpdateProfile_EmptyName_ShowsError() {
        // Open navigation drawer
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        onView(withId(R.id.btnEditProfile)).perform(click());
        
        // Clear name field
        onView(withId(R.id.etEditName))
                .perform(replaceText(""), closeSoftKeyboard());
        
        onView(withText("Save")).perform(click());
        
        // Verify error message
        onView(withText("Name cannot be empty")).check(matches(isDisplayed()));
    }

    /**
     * TC-UPDATE-04: Update Email to Invalid Format
     * Tests that invalid email format is rejected.
     */
    @Test
    public void test_B_UpdateProfile_InvalidEmail_ShowsError() {
        // Open navigation drawer
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        onView(withId(R.id.btnEditProfile)).perform(click());
        
        // Enter invalid email
        onView(withId(R.id.etEditEmail))
                .perform(replaceText("notanemail"), closeSoftKeyboard());
        
        onView(withText("Save")).perform(click());
        
        // Verify validation error
        onView(withText("Valid email required")).check(matches(isDisplayed()));
    }

    /**
     * TC-UPDATE-07: Cancel Edit
     * Tests that canceling edit dialog does not save changes.
     */
    @Test
    public void test_B_UpdateProfile_Cancel_NoChanges() {
        // Open navigation drawer
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        
        // Get original name (would need to store this in test setup)
        // For this example, assume original is "John Doe"
        
        onView(withId(R.id.btnEditProfile)).perform(click());
        
        // Change name
        onView(withId(R.id.etEditName))
                .perform(replaceText("Different Name"), closeSoftKeyboard());
        
        // Cancel instead of save
        onView(withText("Cancel")).perform(click());
        
        // Verify original name is still displayed
        onView(withId(R.id.tvProfileName))
                .check(matches(withText("John Doe")));
    }

    // ========== DELETE PROFILE TESTS ==========

    /**
     * TC-DELETE-01: Successful Profile Deletion
     * Tests complete profile deletion flow with confirmation.
     * Precondition: User must be logged in as entrant.
     */
    @Test
    public void test_Z_DeleteProfile_Confirm_Success() {
        // Open navigation drawer
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        
        // Tap delete button
        onView(withId(R.id.btnDeleteProfile)).perform(click());
        
        // Verify confirmation dialog appears
        onView(withText("Delete Profile")).check(matches(isDisplayed()));
        onView(withText("Are you sure? This will permanently delete your profile and all waiting list entries."))
                .check(matches(isDisplayed()));
        
        // Confirm deletion
        onView(withText("Delete")).perform(click());
        
        // Verify success toast
        onView(withText("Profile deleted")).check(matches(isDisplayed()));
        
        // Verify redirected to login screen
        onView(withId(R.id.emailEditText)).check(matches(isDisplayed()));
    }

    /**
     * TC-DELETE-02: Cancel Profile Deletion
     * Tests that canceling deletion keeps profile intact.
     */
    @Test
    public void test_Z_DeleteProfile_Cancel_NoChanges() {
        // Open navigation drawer
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        
        // Tap delete button
        onView(withId(R.id.btnDeleteProfile)).perform(click());
        
        // Verify confirmation dialog
        onView(withText("Delete Profile")).check(matches(isDisplayed()));
        
        // Cancel deletion
        onView(withText("Cancel")).perform(click());
        
        // Verify still on profile screen
        onView(withId(R.id.tvProfileName)).check(matches(isDisplayed()));
        onView(withId(R.id.btnDeleteProfile)).check(matches(isDisplayed()));
    }

    /**
     * TC-DELETE-04: Delete Button Disabled During Deletion
     * Tests that delete button is disabled during deletion process.
     */
    @Test
    public void test_Z_DeleteProfile_ButtonDisabledDuringDeletion() {
        // Open navigation drawer
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        
        onView(withId(R.id.btnDeleteProfile)).perform(click());
        onView(withText("Delete")).perform(click());
        
        // Immediately check button state (timing-dependent, may need IdlingResource)
        onView(withId(R.id.btnDeleteProfile)).check(matches(not(isEnabled())));
    }

    // ========== BOUNDARY TESTS ==========

    /**
     * TC-BOUND-02: Special Characters in Name
     * Tests that unicode and special characters are handled correctly.
     */
    @Test
    public void test_B_UpdateProfile_SpecialCharactersInName_Success() {
        // Open navigation drawer
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        onView(withId(R.id.btnEditProfile)).perform(click());
        
        // Enter name with special characters
        onView(withId(R.id.etEditName))
                .perform(replaceText("José O'Brien-Smith 李明"), closeSoftKeyboard());
        
        onView(withText("Save")).perform(click());
        
        // Verify success
        onView(withText("Profile updated")).check(matches(isDisplayed()));
        
        // Verify name displays correctly
        onView(withId(R.id.tvProfileName))
                .check(matches(withText("José O'Brien-Smith 李明")));
    }

    /**
     * TC-BOUND-04: Whitespace in Fields
     * Tests that leading/trailing whitespace is trimmed.
     */
    @Test
    public void test_B_UpdateProfile_WhitespaceInFields_Trimmed() {
        // Open navigation drawer
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        onView(withId(R.id.btnEditProfile)).perform(click());
        
        // Enter name with spaces
        onView(withId(R.id.etEditName))
                .perform(replaceText("  John Doe  "), closeSoftKeyboard());
        
        onView(withText("Save")).perform(click());
        
        // Verify trimmed value is displayed
        onView(withId(R.id.tvProfileName))
                .check(matches(withText("John Doe")));
    }
}
