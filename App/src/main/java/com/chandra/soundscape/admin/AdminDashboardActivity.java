package com.chandra.soundscape.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.chandra.soundscape.LoginActivity;
import com.chandra.soundscape.ProfileFragment;
import com.chandra.soundscape.R;
import com.chandra.soundscape.SupabaseAuthManager;
import com.chandra.soundscape.UserSessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    private BottomNavigationView bottomNavigation;
    private SharedPreferences sharedPreferences;
    private UserSessionManager sessionManager;
    private SupabaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize managers
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        sessionManager = new UserSessionManager(this);
        authManager = SupabaseAuthManager.getInstance(this);

        // Debug logging
        logUserSessionInfo();

        // Verify admin access
        if (!verifyAdminAccess()) {
            return;
        }

        initViews();
        setupToolbar();
        setupBottomNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new AdminHomeFragment());
        }
    }

    private void logUserSessionInfo() {
        Log.d(TAG, "=== USER SESSION DEBUG INFO ===");
        Log.d(TAG, "AuthManager isLoggedIn: " + authManager.isUserLoggedIn());
        Log.d(TAG, "SessionManager isLoggedIn: " + sessionManager.isLoggedIn());
        Log.d(TAG, "SessionManager getUserRole: " + sessionManager.getUserRole());
        Log.d(TAG, "SessionManager getUserEmail: " + sessionManager.getUserEmail());
        Log.d(TAG, "UserPrefs userRole: " + sharedPreferences.getString("userRole", "null"));
        Log.d(TAG, "UserPrefs isLoggedIn: " + sharedPreferences.getBoolean("isLoggedIn", false));
        Log.d(TAG, "================================");
    }

    private boolean verifyAdminAccess() {
        // Check if user is logged in using session manager first
        boolean isLoggedIn = sessionManager.isLoggedIn();

        Log.d(TAG, "Login check - SessionManager: " + isLoggedIn);

        if (!isLoggedIn) {
            Log.w(TAG, "User not logged in, redirecting to login");
            navigateToLogin();
            return false;
        }

        // Get user role from session manager
        String userRole = sessionManager.getUserRole();
        Log.d(TAG, "User role from SessionManager: " + userRole);

        // If role is null or empty from session manager, check UserPrefs as fallback
        if (userRole == null || userRole.isEmpty() || userRole.equals("Listener")) {
            userRole = sharedPreferences.getString("userRole", "Listener");
            Log.d(TAG, "Fallback role from UserPrefs: " + userRole);
        }

        // Check if user has admin role (case-insensitive)
        if (userRole == null || !userRole.equalsIgnoreCase("Admin")) {
            Log.w(TAG, "Access denied - User role: " + userRole + " (expected: Admin)");

            // Show more specific error message
            String message = "Akses ditolak. Anda login sebagai '" +
                    (userRole != null ? userRole : "Unknown") +
                    "'. Halaman ini khusus untuk Admin.";

            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            // Don't logout, just redirect to appropriate dashboard
            redirectToUserDashboard(userRole);
            return false;
        }

        Log.i(TAG, "Admin access verified successfully");
        return true;
    }

    private void redirectToUserDashboard(String userRole) {
        if (userRole != null && userRole.equalsIgnoreCase("Listener")) {
            // Redirect to listener dashboard if exists
            // Intent intent = new Intent(this, ListenerDashboardActivity.class);
            // startActivity(intent);
            // For now, just go back to login
            Toast.makeText(this, "Silakan login ulang atau hubungi administrator", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        } else {
            navigateToLogin();
        }
        finish();
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                fragment = new AdminHomeFragment();
            } else if (itemId == R.id.nav_upload) {
                fragment = new UploadMusicFragment();
            } else if (itemId == R.id.nav_music_list) {
                fragment = new MusicListFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            return fragment != null && loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void logout() {
        Log.d(TAG, "Logging out user");

        // Clear session
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        sessionManager.logout();

        // Navigate to login
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
        // Show exit confirmation
        new AlertDialog.Builder(this)
                .setTitle("Keluar Aplikasi")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("Ya", (dialog, which) -> finishAffinity())
                .setNegativeButton("Tidak", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check authentication
        if (!sessionManager.isLoggedIn()) {
            Log.w(TAG, "Session expired, redirecting to login");
            navigateToLogin();
        }
    }
}