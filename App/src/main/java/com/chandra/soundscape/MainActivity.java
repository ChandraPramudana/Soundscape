package com.chandra.soundscape;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button logoutButton;
    private TextView userInfoText;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private SupabaseAuthManager authManager;
    private UserSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        authManager = SupabaseAuthManager.getInstance(this);
        sessionManager = new UserSessionManager(this);

        // Check if user is logged in
        if (!authManager.isUserLoggedIn() || !sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Check if user has correct role (Admin)
        String userRole = sharedPreferences.getString("userRole", "");
        if (!userRole.equals("Admin")) {
            Toast.makeText(this, "Akses ditolak. Hanya Admin yang bisa mengakses halaman ini.",
                    Toast.LENGTH_LONG).show();
            logout();
            return;
        }

        initViews();
        setupListeners();
        displayUserInfo();
    }

    private void initViews() {
        logoutButton = findViewById(R.id.logout_button);
        userInfoText = findViewById(R.id.user_info_text); // Add this to your layout
        progressBar = findViewById(R.id.progress_bar); // Add this to your layout
    }

    private void setupListeners() {
        logoutButton.setOnClickListener(v -> performLogout());
    }

    private void displayUserInfo() {
        String email = authManager.getCurrentUserEmail();
        String role = sharedPreferences.getString("userRole", "");

        if (userInfoText != null && email != null) {
            userInfoText.setText("Logged in as: " + email + "\nRole: " + role);
        }
    }

    private void performLogout() {
        showLoading(true);

        authManager.logoutUser(new SupabaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                // Clear local session
                clearLocalSession();
                showLoading(false);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "Logout gagal: " + error, Toast.LENGTH_LONG).show();
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

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        logoutButton.setEnabled(!show);
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