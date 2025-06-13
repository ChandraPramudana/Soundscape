package com.chandra.soundscape;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class ListenerDashboardActivity extends AppCompatActivity{

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigation;
    private NavigationView navigationView;
    private SharedPreferences sharedPreferences;
    private UserSessionManager sessionManager;
    private SupabaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listener_dashboard);

        // Initialize managers
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        sessionManager = new UserSessionManager(this);
        authManager = SupabaseAuthManager.getInstance(this);

        // Verify user is logged in and has correct role
        if (!verifyUserAccess()) {
            return;
        }

        initViews();
        setupToolbar();
        setupNavigationDrawer();
        setupBottomNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new ListenerHomeFragment());
        }
    }

    private boolean verifyUserAccess() {
        // Check if user is logged in
        if (!authManager.isUserLoggedIn() || !sessionManager.isLoggedIn()) {
            navigateToLogin();
            return false;
        }

        // Check if user has correct role (Listener)
        String userRole = sharedPreferences.getString("userRole", "");
        if (!userRole.equals("Listener")) {
            Toast.makeText(this, "Akses ditolak. Halaman ini khusus untuk Listener.",
                    Toast.LENGTH_LONG).show();
            logout();
            return false;
        }

        return true;
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("SoundScape - Listener");
        }
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, findViewById(R.id.toolbar),
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }


    private String extractNameFromEmail(String email) {
        String name = email.substring(0, email.indexOf("@"));
        name = name.replace(".", " ").replace("_", " ");
        String[] parts = name.split(" ");
        StringBuilder formattedName = new StringBuilder();

        for (String part : parts) {
            if (part.length() > 0) {
                formattedName.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return formattedName.toString().trim();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new ListenerHomeFragment();
            } else if (itemId == R.id.nav_explore) {
                fragment = new ExploreFragment();
            } else if (itemId == R.id.nav_favorites) {
                fragment = new FavoritesFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            return fragment != null && loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }


    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Tentang SoundScape")
                .setMessage("SoundScape v1.0\n\nAplikasi streaming musik terbaik untuk Anda.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin logout?")
                .setPositiveButton("Ya", (dialog, which) -> performLogout())
                .setNegativeButton("Tidak", null)
                .show();
    }

    private void performLogout() {
        // Show loading or disable UI during logout
        authManager.logoutUser(new SupabaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                // Clear local session
                clearLocalSession();
                Toast.makeText(ListenerDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }

            @Override
            public void onError(String error) {
                // Even if server logout fails, clear local session
                clearLocalSession();
                navigateToLogin();
            }
        });
    }

    private void clearLocalSession() {
        // Clear SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Clear session manager
        sessionManager.logout();
    }

    private void logout() {
        clearLocalSession();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Show exit confirmation
            new AlertDialog.Builder(this)
                    .setTitle("Keluar Aplikasi")
                    .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                    .setPositiveButton("Ya", (dialog, which) -> finishAffinity())
                    .setNegativeButton("Tidak", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check authentication status
        if (!authManager.isUserLoggedIn()) {
            navigateToLogin();
        }
    }
}