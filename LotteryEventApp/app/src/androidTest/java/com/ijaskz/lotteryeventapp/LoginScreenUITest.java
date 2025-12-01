package com.ijaskz.lotteryeventapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple UI test for Login Screen visibility and layout.
 * Tests that essential UI elements exist on the login screen.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginScreenUITest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    /**
     * Test 1: Verify Login Screen Core Elements Are Visible
     * Tests that the main login elements are displayed:
     * - Email input field
     * - Password input field
     * - Login button
     */
    @Test
    public void testLoginScreenCoreElementsVisible() {
        // Verify email input is displayed
        onView(withId(R.id.emailEditText))
                .check(matches(isDisplayed()));

        // Verify password input is displayed
        onView(withId(R.id.passwordEditText))
                .check(matches(isDisplayed()));

        // Verify login button is displayed
        onView(withId(R.id.loginButton))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 2: Verify Login Button Has Correct Text
     * Tests that the login button displays "Login" text.
     */
    @Test
    public void testLoginButtonHasCorrectText() {
        onView(withId(R.id.loginButton))
                .check(matches(withText("Login")));
    }
}
