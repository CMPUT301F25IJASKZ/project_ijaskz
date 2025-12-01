package com.ijaskz.lotteryeventapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;

import org.junit.Test;

public class UserPrefTest {
    /** tests: US 01.07.01 As an entrant, I want to be identified by my device, so that I don't have to use a username and password */
    @Test
    public void testUserPref() throws InterruptedException {
        ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.emailEditText)).perform(typeText("ben@gmail.com"));
        onView(withId(R.id.passwordEditText)).perform(typeText("benben"));
        onView(withId(R.id.loginButton)).perform(click());
        Thread.sleep(2000);
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        onView(withText("Ben")).check(matches(isDisplayed()));
        onView(withText("ben@gmail.com")).check(matches(isDisplayed()));
        scenario.close();
        ActivityScenario<MainActivity> scenario2 = ActivityScenario.launch(MainActivity.class);
        onView(withContentDescription("Open navigation drawer")).perform(click());
        onView(withId(R.id.nav_profile)).perform(click());
        onView(withText("Ben")).check(matches(isDisplayed()));
        onView(withText("ben@gmail.com")).check(matches(isDisplayed()));
    }
}
