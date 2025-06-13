package com.chandra.soundscape;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.chandra.soundscape.admin.AdminDashboardActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText emailInput, passwordInput;
    private Spinner roleSpinner;
    private Button loginButton;
    private TextView registerLink;
    private ProgressBar progressBar;
    private SupabaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize auth manager
        authManager = SupabaseAuthManager.getInstance(this);

        // CLEAR ANY LEFTOVER SESSION/CACHE
        Log.d(TAG, "Clearing any leftover session data...");
        authManager.forceRefresh();

        // Check if user is already logged in (after clearing)
        if (authManager.isUserLoggedIn()) {
            Log.d(TAG, "User already logged in, navigating to dashboard");
            String role = authManager.getCurrentUserRole();
            navigateToAppropriateActivity(role != null ? role : "Listener");
            return;
        }

        initViews();
        setupSpinner();
        setupListeners();

        // Pre-fill email if coming from registration
        String registeredEmail = getIntent().getStringExtra("registered_email");
        if (registeredEmail != null && !registeredEmail.isEmpty()) {
            emailInput.setText(registeredEmail);
            passwordInput.requestFocus();

            // Clear any previous error messages and show success
            Log.d(TAG, "Coming from registration with email: " + registeredEmail);

            // Use Handler to show message after layout is ready
            new android.os.Handler().postDelayed(() -> {
                Toast.makeText(this, "✅ Registrasi berhasil! Silakan login.", Toast.LENGTH_LONG).show();
            }, 500);
        }
    }

    private void initViews() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        roleSpinner = findViewById(R.id.role_spinner);
        loginButton = findViewById(R.id.login_button);
        registerLink = findViewById(R.id.register_link);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupSpinner() {
        String[] roles = {"Pilih Role", "Listener", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> performLogin());
        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void performLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String selectedRole = roleSpinner.getSelectedItem().toString();

        // Validate input
        if (!validateInput(email, password, selectedRole)) {
            return;
        }

        // Show loading
        showLoading(true);

        Log.d(TAG, "=== STARTING LOGIN PROCESS ===");
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "Selected role: " + selectedRole);

        // Perform login with Supabase
        authManager.loginUser(email, password, new SupabaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                // This will be called when login is successful but before role is received
                Log.d(TAG, "✅ LOGIN SUCCESS: " + message);
                runOnUiThread(() -> {
                    // Don't show toast here, wait for role
                    Log.d(TAG, "Login successful, waiting for role...");
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ LOGIN ERROR: " + error);
                runOnUiThread(() -> {
                    showLoading(false);

                    // Show user-friendly error messages
                    String userMessage = getUserFriendlyError(error);
                    Log.e(TAG, "Displaying error to user: " + userMessage);
                    Toast.makeText(LoginActivity.this, userMessage, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onRoleReceived(String databaseRole) {
                // This will be called after successful login and profile retrieval
                Log.d(TAG, "✅ ROLE RECEIVED: " + databaseRole);
                Log.d(TAG, "Selected role was: " + selectedRole);

                runOnUiThread(() -> {
                    showLoading(false);

                    // Verify role matches or use database role if selected role is "Pilih Role"
                    String finalRole = databaseRole;
                    if (!selectedRole.equals("Pilih Role") && !selectedRole.equals(databaseRole)) {
                        String roleMessage = "Role yang dipilih (" + selectedRole + ") tidak sesuai dengan akun Anda (" + databaseRole + ")";
                        Log.w(TAG, roleMessage);
                        Toast.makeText(LoginActivity.this, roleMessage, Toast.LENGTH_LONG).show();
                        // Still proceed with database role
                    }

                    // Show success message
                    Toast.makeText(LoginActivity.this, "Login berhasil! Selamat datang.", Toast.LENGTH_SHORT).show();

                    // Navigate based on final role
                    Log.d(TAG, "Navigating with role: " + finalRole);
                    navigateToAppropriateActivity(finalRole);
                });
            }
        });
    }

    private boolean validateInput(String email, String password, String selectedRole) {
        // Reset errors
        emailInput.setError(null);
        passwordInput.setError(null);

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email harus diisi");
            emailInput.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Format email tidak valid");
            emailInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password harus diisi");
            passwordInput.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password minimal 6 karakter");
            passwordInput.requestFocus();
            return false;
        }

        if (selectedRole.equals("Pilih Role")) {
            Toast.makeText(this, "Pilih role terlebih dahulu", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String getUserFriendlyError(String error) {
        Log.d(TAG, "Processing LOGIN error message: " + error);

        // Make sure we NEVER show "registrasi" messages in login
        if (error.contains("registrasi") || error.contains("Registrasi")) {
            Log.w(TAG, "⚠️ WARNING: Registration error message appeared in LOGIN! Converting to login error.");
            return "❌ Masalah dengan akun. Silakan coba lagi atau hubungi support.";
        }

        if (error.contains("invalid_credentials") || error.contains("salah") || error.contains("Invalid login credentials")) {
            return "❌ Email atau password salah. Silakan periksa kembali.";
        } else if (error.contains("not_found") || error.contains("tidak ditemukan") || error.contains("User profile tidak ditemukan")) {
            return "❌ Akun tidak ditemukan di database.\n\nMungkin ada masalah saat registrasi. Silakan coba daftar ulang.";
        } else if (error.contains("LOGIN: User profile tidak ditemukan")) {
            return "❌ Profile user tidak lengkap.\n\nSilakan coba daftar ulang atau hubungi support.";
        } else if (error.contains("network") || error.contains("timeout")) {
            return "❌ Masalah koneksi internet. Silakan coba lagi.";
        } else if (error.contains("too_many_requests")) {
            return "❌ Terlalu banyak percobaan login. Silakan tunggu beberapa menit.";
        } else if (error.contains("email_not_confirmed")) {
            return "❌ Email belum dikonfirmasi. Periksa inbox email Anda.";
        } else {
            return "❌ LOGIN gagal: " + error.replace("LOGIN:", "").trim();
        }
    }

    private void navigateToAppropriateActivity(String role) {
        Intent intent;
        Log.d(TAG, "=== NAVIGATION ===");
        Log.d(TAG, "Navigating based on role: " + role);

        switch (role) {
            case "Admin":
                Log.d(TAG, "Navigating to Admin dashboard");
                intent = new Intent(this, AdminDashboardActivity.class); // Admin dashboard
                break;
            case "Listener":
            default:
                Log.d(TAG, "Navigating to Listener dashboard");
                intent = new Intent(this, ListenerDashboardActivity.class); // Listener dashboard
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        loginButton.setEnabled(!show);
        emailInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
        roleSpinner.setEnabled(!show);

        if (show) {
            loginButton.setText("Masuk...");
        } else {
            loginButton.setText("Masuk");
        }
    }

    @Override
    public void onBackPressed() {
        // Disable back press on login screen
        finishAffinity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any ongoing requests if needed
    }
}