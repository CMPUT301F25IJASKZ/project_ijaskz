# Profile Management Black-Box Tests

## Overview
This directory contains instrumented (UI) tests for profile management features using Espresso framework.

## Test File
- `ProfileManagementTest.java` - Black-box tests for Create, Update, and Delete profile operations

## Test Coverage

### Create Profile (7 tests planned, 3 implemented)
- ✅ TC-CREATE-01: Valid profile creation
- ✅ TC-CREATE-02: Empty name field validation
- ✅ TC-CREATE-03: Invalid email format validation
- ⏳ TC-CREATE-04: Empty email field
- ⏳ TC-CREATE-05: Weak password validation
- ⏳ TC-CREATE-06: Duplicate email
- ⏳ TC-CREATE-07: Network failure

### Update Profile (8 tests planned, 5 implemented)
- ✅ TC-UPDATE-01: Update name successfully
- ⏳ TC-UPDATE-02: Update email successfully
- ✅ TC-UPDATE-03: Update name to empty
- ✅ TC-UPDATE-04: Update email to invalid format
- ⏳ TC-UPDATE-05: Update email to existing
- ⏳ TC-UPDATE-06: Update phone number
- ✅ TC-UPDATE-07: Cancel edit
- ⏳ TC-UPDATE-08: Network failure

### Delete Profile (7 tests planned, 3 implemented)
- ✅ TC-DELETE-01: Successful deletion with confirmation
- ✅ TC-DELETE-02: Cancel deletion
- ⏳ TC-DELETE-03: Delete with waiting list entries
- ✅ TC-DELETE-04: Button disabled during deletion
- ⏳ TC-DELETE-05: Network failure during deletion
- ⏳ TC-DELETE-06: Organizer cannot see delete button
- ⏳ TC-DELETE-07: Admin viewing another user's profile

### Boundary Tests (4 tests planned, 2 implemented)
- ⏳ TC-BOUND-01: Very long name
- ✅ TC-BOUND-02: Special characters in name
- ⏳ TC-BOUND-03: Email case sensitivity
- ✅ TC-BOUND-04: Whitespace trimming

### Security Tests (2 tests planned, 0 implemented)
- ⏳ TC-SEC-01: Cannot delete another user's profile
- ⏳ TC-SEC-02: Session cleared after deletion

## Running the Tests

### Prerequisites
1. Android device or emulator must be connected
2. App must be installed on the device
3. Firebase must be configured
4. Test user accounts should be cleaned up between runs

### Run All Profile Tests
```bash
./gradlew connectedAndroidTest
```

### Run Specific Test Class (using adb)
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ijaskz.lotteryeventapp.ProfileManagementTest
```

### Run Specific Test Method
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ijaskz.lotteryeventapp.ProfileManagementTest#testCreateProfile_ValidData_Success
```

### Run from Android Studio
1. Right-click on `ProfileManagementTest.java`
2. Select "Run 'ProfileManagementTest'"
3. View results in Run tool window

## Important Notes

### View IDs Used (✅ Updated to Match Actual Layouts)

**Registration Screen** (`activity_register.xml`):
- ✅ `R.id.name_input` - Name EditText
- ✅ `R.id.email_input` - Email EditText
- ✅ `R.id.phone_input` - Phone EditText (optional)
- ✅ `R.id.password_input` - Password EditText
- ✅ `R.id.confirm_password_input` - Confirm Password EditText
- ✅ `R.id.register_button` - Register Button
- ✅ `R.id.error_text` - Error message TextView

**Profile Screen** (`fragment_profile.xml`):
- ✅ `R.id.tvProfileName` - Name TextView
- ✅ `R.id.tvProfileEmail` - Email TextView
- ✅ `R.id.tvProfilePhone` - Phone TextView
- ✅ `R.id.btnEditProfile` - Edit Profile Button
- ✅ `R.id.btnDeleteProfile` - Delete Profile Button
- ✅ `R.id.ivProfileAvatar` - Avatar ImageView

**Edit Profile Dialog** (`dialog_edit_profile.xml`):
- ✅ `R.id.etEditName` - Name EditText in dialog
- ✅ `R.id.etEditEmail` - Email EditText in dialog
- ✅ `R.id.etEditPhone` - Phone EditText in dialog

**Login Screen** (`activity_login.xml`):
- ✅ `R.id.emailEditText` - Email EditText
- ✅ `R.id.passwordEditText` - Password EditText
- ✅ `R.id.loginButton` - Login Button

**Navigation** (`drawer_menu.xml` and `activity_main.xml`):
- ✅ `R.id.nav_profile` - Profile menu item in navigation drawer
- ✅ `R.id.fragment_container` - Main fragment container
- ✅ `R.id.drawer_layout` - DrawerLayout container
- ✅ `R.id.nav_view` - NavigationView

### Test Data Management
- Tests use email addresses like `john.doe.test@example.com`
- Clean up test accounts between runs to avoid conflicts
- Consider using Firebase Emulator for isolated testing

### Timing Considerations
- Some tests may need `IdlingResource` for async operations
- Network-dependent tests require proper timeout handling
- Delete button state test is timing-sensitive

### Navigation Drawer Note
The app uses a navigation drawer. To navigate to profile, tests need to:
1. Open the drawer (if not already open)
2. Click on `R.id.nav_profile`

If tests fail with "view not found", you may need to add drawer opening:
```java
// Open navigation drawer
onView(withId(R.id.drawer_layout))
    .perform(DrawerActions.open());

// Then click profile
onView(withId(R.id.nav_profile)).perform(click());
```

Add this import if needed:
```java
import androidx.test.espresso.contrib.DrawerActions;
```

### Customization Status
- ✅ View IDs updated to match actual layouts
- ✅ Navigation flows verified (`R.id.nav_profile` from drawer menu)
- ⚠️ Test user credentials need configuration
- ⚠️ Test cleanup between runs needs setup
- ⚠️ IdlingResources may be needed for async operations
- ⚠️ Error message text assertions may need adjustment based on actual validation messages
- ⚠️ Drawer navigation may need to be opened first (see note below)

## Test Execution Strategy

### Manual Test Execution Order
1. Run Create tests first (establishes test accounts)
2. Run Update tests (modifies existing accounts)
3. Run Delete tests last (removes test accounts)
4. Clean up any remaining test data

### Automated CI/CD
- Use Firebase Test Lab for parallel execution
- Configure test sharding for faster results
- Set up automatic cleanup jobs

## Troubleshooting

### Common Issues
1. **View not found**: Update view IDs to match your layouts
2. **Timing issues**: Add explicit waits or IdlingResources
3. **Firebase errors**: Check network and Firebase configuration
4. **Test data conflicts**: Clean up test accounts between runs

### Debug Tips
- Use `adb logcat` to view real-time logs
- Enable Espresso debug logging
- Take screenshots on test failures
- Use Android Studio's Test Recorder for complex interactions

## Future Enhancements
- Add remaining test cases (marked with ⏳)
- Implement network failure simulation
- Add screenshot capture on failures
- Create test data factory for consistent test accounts
- Add performance benchmarks
- Integrate with CI/CD pipeline
