package com.ijaskz.lotteryeventapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple UI test for Register Screen.
 * Tests that registration screen elements are displayed correctly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NavigationUITest {

    @Rule
    public ActivityScenarioRule<RegisterActivity> activityRule =
            new ActivityScenarioRule<>(RegisterActivity.class);

    /**
     * Test 1: Verify Register Screen Core Elements Are Visible
     * Tests that registration form fields are displayed:
     * - Name input
     * - Email input
     * - Password input
     * - Register button
     */
    @Test
    public void testRegisterScreenCoreElementsVisible() {
        // Verify name input is displayed
        onView(withId(R.id.name_input))
                .check(matches(isDisplayed()));

        // Verify email input is displayed
        onView(withId(R.id.email_input))
                .check(matches(isDisplayed()));

        // Verify password input is displayed
        onView(withId(R.id.password_input))
                .check(matches(isDisplayed()));

        // Verify confirm password input is displayed
        onView(withId(R.id.confirm_password_input))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 2: Verify Register Button Exists
     * Tests that the register button is present on the screen.
     */
    @Test
    public void testRegisterButtonExists() {
        // Verify register button exists
        onView(withId(R.id.register_button))
                .check(matches(isDisplayed()));
    }
}
