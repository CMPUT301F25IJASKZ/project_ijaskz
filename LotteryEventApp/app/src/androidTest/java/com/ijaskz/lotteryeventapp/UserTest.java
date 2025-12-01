// Tests the User data model

package com.ijaskz.lotteryeventapp;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the User model.
 */
public class UserTest {

    private User user;
    private static final String TEST_USER_ID = "user123";
    private static final String TEST_NAME = "Jane Smith";
    private static final String TEST_EMAIL = "jane@example.com";
    private static final String TEST_TYPE = "entrant";
    private static final String TEST_PHONE = "123-456-7890";

    @Before
    public void setUp() {
        user = new User(TEST_USER_ID, TEST_NAME, TEST_EMAIL, TEST_TYPE, TEST_PHONE);
    }


    /**
     * Verifies that a User object is created with all fields populated.
     */
    @Test
    public void testUserCreationWithAllFields() {
        assertNotNull("User should not be null", user);
        assertEquals("User ID should match", TEST_USER_ID, user.getUser_id());
        assertEquals("User name should match", TEST_NAME, user.getUser_name());
        assertEquals("User email should match", TEST_EMAIL, user.getUser_email());
        assertEquals("User type should match", TEST_TYPE, user.getUser_type());
        assertEquals("User phone should match", TEST_PHONE, user.getUser_phone());
    }

    /**
     * Verifies that an empty User object has null fields.
     */
    @Test
    public void testEmptyUserCreation() {
        User emptyUser = new User();

        assertNotNull("Empty user should not be null", emptyUser);
        assertNull("Empty user should have null ID", emptyUser.getUser_id());
        assertNull("Empty user should have null name", emptyUser.getUser_name());
    }

    /**
     * Ensures user_id can be updated.
     */
    @Test
    public void testUserIdCanBeSet() {
        String newId = "user456";
        user.setUser_id(newId);

        assertEquals("User ID should be updated", newId, user.getUser_id());
    }
    /**
     * Ensures user_name can be updated.
     */

    @Test
    public void testUserNameCanBeSet() {
        String newName = "John Doe";
        user.setUser_name(newName);

        assertEquals("User name should be updated", newName, user.getUser_name());
    }

    /**
     * Ensures user_email can be updated.
     */
    @Test
    public void testUserEmailCanBeSet() {
        String newEmail = "newemail@example.com";
        user.setUser_email(newEmail);

        assertEquals("User email should be updated", newEmail, user.getUser_email());
    }

    /**
     * Ensures user_type can be changed.
     */
    @Test
    public void testUserTypeCanBeChanged() {
        assertEquals("Initial type should be entrant", "entrant", user.getUser_type());

        user.setUser_type("organizer");

        assertEquals("User type should be updated to organizer",
                "organizer", user.getUser_type());
    }

    /**
     * Verifies that a user can be promoted.
     */
    @Test
    public void testUserTypePromotion() {
        User entrant = new User("u1", "Test", "test@test.com", "entrant", "123");
        assertEquals("Should start as entrant", "entrant", entrant.getUser_type());

        // Simulate promotion
        entrant.setUser_type("organizer");
        assertEquals("Should be promoted to organizer", "organizer", entrant.getUser_type());
    }


    /**
     * Verifies that a user can be demoted.
     */
    @Test
    public void testUserTypeDemotion() {
        User organizer = new User("u2", "Test", "test@test.com", "organizer", "123");
        assertEquals("Should start as organizer", "organizer", organizer.getUser_type());

        // Simulate demotion
        organizer.setUser_type("entrant");
        assertEquals("Should be demoted to entrant", "entrant", organizer.getUser_type());
    }
}
