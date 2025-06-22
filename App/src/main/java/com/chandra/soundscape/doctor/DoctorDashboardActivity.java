package com.chandra.soundscape.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.chandra.soundscape.LoginActivity;
import com.chandra.soundscape.R;
import com.chandra.soundscape.SupabaseAuthManager;
import com.chandra.soundscape.admin.MusicListFragment;
import com.chandra.soundscape.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DoctorDashboardActivity extends AppCompatActivity {

    private static final String TAG = "DoctorDashboard";
    private SupabaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        authManager = SupabaseAuthManager.getInstance(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Dashboard Dokter");

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DoctorHomeFragment())
                    .commit();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    selectedFragment = new DoctorHomeFragment();
                } else if (itemId == R.id.nav_upload) {
                    selectedFragment = new DoctorUploadMusicFragment();
                } else if (itemId == R.id.nav_music_list) {
                    selectedFragment = new MusicListFragment(); // Reuse admin fragment
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment(); // Reuse admin fragment
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            };

    @Override
    public void onBackPressed() {
        // Logout confirmation or handle back
        super.onBackPressed();
    }
}