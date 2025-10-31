package com.ijaskz.lotteryeventapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.firebase.ui.auth.AuthUI;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private UserManager userManager;
    private String userType;

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
    }

    private void configureMenuForUserType(android.view.Menu menu) {
        menu.findItem(R.id.nav_events_home).setVisible(true);
        menu.findItem(R.id.nav_create_event).setVisible("organizer".equals(userType));
        // All events - all users
        menu.findItem(R.id.nav_all_events).setVisible(true);
        //profile - organizer + entrant
        menu.findItem(R.id.nav_profile).setVisible("organizer".equals(userType) || "entrant".equals(userType));
        // edit event - organizer + entrant
        menu.findItem(R.id.nav_edit_event).setVisible("organizer".equals(userType) || "admin".equals(userType));
        // notification - organizer + entrant
        menu.findItem(R.id.nav_notifications).setVisible("organizer".equals(userType) || "entrant".equals(userType));
        menu.findItem(R.id.nav_logout).setVisible(true);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;

        if (id == R.id.nav_events_home) fragment = new EventsHomeFragment();
        else if (id == R.id.nav_create_event) fragment = new CreateEventFragment();
        else if (id == R.id.nav_all_events) fragment = new AllEventsFragment();
        else if (id == R.id.nav_profile) fragment = new ProfileFragment();
        else if (id == R.id.nav_edit_event) fragment = new EditEventFragment();
        else if (id == R.id.nav_notifications) fragment = new NotificationsFragment();
        else if (id == R.id.nav_logout) {
            // Proper logout
            userManager.logout();
            AuthUI.getInstance().signOut(this).addOnCompleteListener(task -> {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
            return true;
        }

        if (fragment != null) loadFragment(fragment);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

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
