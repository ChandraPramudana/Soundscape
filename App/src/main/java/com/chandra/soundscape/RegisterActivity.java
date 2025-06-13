package com.chandra.soundscape;

import android.content.Intent;
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

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    private EditText nameInput, emailInput, passwordInput, confirmPasswordInput, phoneInput;
    private Spinner roleSpinner;
    private Button registerButton;
    private TextView loginLink;
    private ProgressBar progressBar;
    private SupabaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = SupabaseAuthManager.getInstance(this);

        // Clear any previous session data to avoid conflicts
        authManager.forceRefresh();

        initViews();
        setupSpinner();
        setupListeners();

        // Debug Supabase connection
        authManager.debugSupabaseConnection();
    }

    private void initViews() {
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
//        phoneInput = findViewById(R.id.phone_input); // PERBAIKAN: Tambahkan phoneInput
        roleSpinner = findViewById(R.id.role_spinner);
        registerButton = findViewById(R.id.register_button);
        loginLink = findViewById(R.id.login_link);
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
        registerButton.setOnClickListener(v -> performRegister());
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void performRegister() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String phone = phoneInput != null ? phoneInput.getText().toString().trim() : "";
        String role = roleSpinner.getSelectedItem().toString();

        // Validate input
        if (!validateInput(name, email, password, confirmPassword, role)) {
            return;
        }

        // Show loading
        showLoading(true);

        Log.d(TAG, "=== STARTING REGISTRATION ===");
        Log.d(TAG, "Email: " + email + ", Role: " + role);

        // Register with Supabase
        authManager.registerUser(email, password, name, role, phone.isEmpty() ? null : phone,
                new SupabaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Log.d(TAG, "✅ REGISTRATION SUCCESSFUL: " + message);

                            // Show clear success message
                            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();

                            // Wait for user to see success message, then navigate
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                Log.d(TAG, "Navigating to login with email: " + email);

                                // Navigate to login with clean state
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.putExtra("registered_email", email); // Pre-fill email in login
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }, 2000); // Wait 2 seconds for user to see success message
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Log.e(TAG, "❌ REGISTRATION FAILED: " + error);

                            // Show error message directly (already user-friendly from SupabaseAuthManager)
                            Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();

                            // If it's a network error, suggest checking internet
                            if (error.contains("koneksi") || error.contains("network") ||
                                    error.contains("resolve host")) {
                                Toast.makeText(RegisterActivity.this,
                                        "💡 Tips: Periksa koneksi internet Anda",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private boolean validateInput(String name, String email, String password, String confirmPassword, String role) {
        // Reset errors
        nameInput.setError(null);
        emailInput.setError(null);
        passwordInput.setError(null);
        confirmPasswordInput.setError(null);

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Nama harus diisi");
            nameInput.requestFocus();
            return false;
        }

        if (name.length() < 2) {
            nameInput.setError("Nama minimal 2 karakter");
            nameInput.requestFocus();
            return false;
        }

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

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Password tidak sama");
            confirmPasswordInput.requestFocus();
            return false;
        }

        if (role.equals("Pilih Role")) {
            Toast.makeText(this, "Pilih role terlebih dahulu", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        registerButton.setEnabled(!show);
        nameInput.setEnabled(!show);
        emailInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
        confirmPasswordInput.setEnabled(!show);
        if (phoneInput != null) phoneInput.setEnabled(!show);
        roleSpinner.setEnabled(!show);

        if (show) {
            registerButton.setText("Mendaftar...");
        } else {
            registerButton.setText("Daftar");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any ongoing requests if needed
    }
}