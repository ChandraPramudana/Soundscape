package com.chandra.soundscape;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePhoto;
    private TextInputLayout tilName, tilEmail, tilPhone;
    private TextInputEditText etName, etEmail, etPhone;
    private MaterialButton btnSave, btnCancel;
    private UserSessionManager sessionManager;
    private UserData currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new UserSessionManager(this);
        currentUser = sessionManager.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Error: User session not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadCurrentUserData();
        setupClickListeners();
    }

    private void initViews() {
        ivProfilePhoto = findViewById(R.id.iv_profile_photo);
        tilName = findViewById(R.id.til_name);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Edit Profil");
        }
    }

    private void loadCurrentUserData() {
        etName.setText(currentUser.getName());
        etEmail.setText(currentUser.getEmail());
        etPhone.setText(currentUser.getPhone());

        // Load profile photo if available
        // In a real app, you would load the image from URL or local storage
        // For now, we'll use the default profile icon
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());

        // Handle profile photo click
        ivProfilePhoto.setOnClickListener(v -> selectProfilePhoto());

        // Handle toolbar back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Reset errors
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);

        // Validate input
        if (TextUtils.isEmpty(name)) {
            tilName.setError("Nama tidak boleh kosong");
            etName.requestFocus();
            return;
        }

        if (name.length() < 2) {
            tilName.setError("Nama minimal 2 karakter");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email tidak boleh kosong");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Format email tidak valid");
            etEmail.requestFocus();
            return;
        }

        // Phone is optional, but if provided, validate format
        if (!TextUtils.isEmpty(phone)) {
            if (!android.util.Patterns.PHONE.matcher(phone).matches() || phone.length() < 10) {
                tilPhone.setError("Format nomor telepon tidak valid");
                etPhone.requestFocus();
                return;
            }
        }

        // Update user profile
        sessionManager.updateUserProfile(name, email, phone, currentUser.getProfilePhoto());

        Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();

        // Return result to indicate successful update
        setResult(RESULT_OK);
        finish();
    }

    private void selectProfilePhoto() {
        // In a real app, you would implement image picker here
        // For now, show a toast
        Toast.makeText(this, "Fitur ganti foto profil akan tersedia di update selanjutnya", Toast.LENGTH_SHORT).show();

        // Example implementation would be:
        // Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog if user has made changes
        if (hasUnsavedChanges()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Perubahan Belum Disimpan")
                    .setMessage("Anda memiliki perubahan yang belum disimpan. Apakah Anda yakin ingin keluar?")
                    .setPositiveButton("Keluar", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("Batal", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        String currentName = etName.getText().toString().trim();
        String currentEmail = etEmail.getText().toString().trim();
        String currentPhone = etPhone.getText().toString().trim();

        return !currentName.equals(currentUser.getName()) ||
                !currentEmail.equals(currentUser.getEmail()) ||
                !currentPhone.equals(currentUser.getPhone() != null ? currentUser.getPhone() : "");
    }
}