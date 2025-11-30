package com.ijaskz.lotteryeventapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Defines main activity that holds fragments and navigation bar
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private UserManager userManager;
    private String userType;

    /**
     * creates Activity when user enters app
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userManager = new UserManager(this);
        userType = userManager.getUserType();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        configureMenuForUserType(navigationView.getMenu());

        if (savedInstanceState == null) {
            loadFragment(new EventsHomeFragment());
            navigationView.setCheckedItem(R.id.nav_events_home);
        }

        // Handle deep link if app launched from a QR scan
        handleDeepLink(getIntent());
    }

    /**
     * Opens app up is user previously logged in
     * @param intent The new intent that was used to start the activity
     *
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent); // Handle when app is already running
    }

    /**
     * Linking Events to intent
     * @param intent The intent that will be used when trying to allow previously logged in user to enter app
     */
    private void handleDeepLink(Intent intent) {
        Uri u = intent != null ? intent.getData() : null;
        if (u == null) return;

        // Expecting: lotteryevent://event/<docId>
        if (!"lotteryevent".equals(u.getScheme())) return;
        if (!"event".equals(u.getHost())) return;

        String eventId = u.getLastPathSegment();
        if (eventId == null || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::openEventFromDoc);
    }

    /**
     * Opens event view fragment on opening activity
     * @param d document for event displaying
     */
    private void openEventFromDoc(DocumentSnapshot d) {
        if (d == null || !d.exists()) return;

        String description = d.getString("event_description") != null
                ? d.getString("event_description") : d.getString("description");
        String location = d.getString("location");
        String name = d.getString("event_name") != null
                ? d.getString("event_name") : d.getString("name");
        String time = d.getString("event_time") != null
                ? d.getString("event_time") : d.getString("time");
        String org_name = d.getString("organizer_name") != null
                ? d.getString("organizer_name") : d.getString("org_name");
        String image = d.getString("image") != null
                ? d.getString("image") : d.getString("imageUrl");
        Long maxL = d.getLong("max");
        int max = maxL != null ? maxL.intValue() : 0;


        Event e = new Event(description, org_name, location, name, max, time, image);
        e.setEvent_id(d.getId());
        e.setRegistrationStart(d.getTimestamp("registrationStart"));
        e.setRegistrationEnd(d.getTimestamp("registrationEnd"));
        try { e.setQrUrl(d.getString("qrUrl")); } catch (Exception ignored) {}
        try { e.setDeeplink(d.getString("deeplink")); } catch (Exception ignored) {}

        loadFragment(EventViewFragment.newInstance(e));
    }

    /**
     * Configuring visibility on nav bar based on user type
     * @param menu The menu the user will see
     */
    private void configureMenuForUserType(android.view.Menu menu) {
        menu.findItem(R.id.nav_events_home).setVisible(true);
        menu.findItem(R.id.nav_create_event).setVisible("organizer".equals(userType));
        menu.findItem(R.id.nav_all_events).setVisible(true);
        menu.findItem(R.id.nav_profile).setVisible("organizer".equals(userType) || "entrant".equals(userType));
        menu.findItem(R.id.nav_my_waiting_lists).setVisible("entrant".equals(userType));
        menu.findItem(R.id.nav_notifications).setVisible("organizer".equals(userType) || "entrant".equals(userType));
        menu.findItem(R.id.nav_manage_profiles).setVisible("admin".equals(userType));
        menu.findItem(R.id.nav_admin_notification_logs).setVisible("admin".equals(userType)); //Admin-only logs
        menu.findItem(R.id.nav_lottery_description).setVisible("entrant".equals(userType));
        menu.findItem(R.id.nav_logout).setVisible(true);

        if ("organizer".equals(userType)) {
            menu.findItem(R.id.nav_all_events).setTitle("My Events");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cam, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_camera) {
            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    /**
     * Nav bar navigation based on button clicked
     * @param item The feature the user wishes to access
     * @return True The Fragment that will be displayed goes through
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;

        if (id == R.id.nav_events_home) fragment = new EventsHomeFragment();
        else if (id == R.id.nav_create_event) fragment = new CreateEventFragment();
        else if (id == R.id.nav_all_events) fragment = new AllEventsFragment();
        else if (id == R.id.nav_profile) fragment = new ProfileFragment();
        else if (id == R.id.nav_my_waiting_lists) fragment = new MyWaitingListFragment();
        else if (id == R.id.nav_notifications) fragment = new NotificationsFragment();
        else if (id == R.id.nav_manage_profiles) fragment = new UserManagerFragment();
        else if (id == R.id.nav_admin_notification_logs) fragment = new AdminNotificationLogsFragment();
        else if (id == R.id.nav_lottery_description) fragment = new LotteryDescription();
        else if (id == R.id.nav_logout) {
            userManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        if (fragment != null) loadFragment(fragment);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * loads new fragment into holder
     * @param fragment The fragment to be displayed
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }
}
