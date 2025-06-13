package com.chandra.soundscape.admin;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.chandra.soundscape.R;
import com.chandra.soundscape.SupabaseAuthManager;
import com.chandra.soundscape.api.MusicApiClient;
import com.chandra.soundscape.api.SupabaseStorageHelper;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class UploadMusicFragment extends Fragment {

    private static final String TAG = "UploadMusicFragment";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_AUDIO_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private TextInputEditText etTitle, etArtist, etDoctorName, etJournalReference;
    private TextInputEditText etDuration, etDescription, etImageUrl, etAudioUrl;
    private Spinner spinnerCategory;
    private ImageView ivPreview;
    private TextView tvAudioFilename;
    private MaterialButton btnChooseImage, btnChooseAudio, btnUpload;

    // File URIs
    private Uri selectedImageUri;
    private Uri selectedAudioUri;

    // URLs after upload
    private String uploadedImageUrl;
    private String uploadedAudioUrl;

    // API & Auth
    private MusicApiClient musicApiClient;
    private SupabaseAuthManager authManager;

    // Categories
    private String[] categories = {"DeepSleep", "Mindfulness", "Stress Relief", "Therapeutic"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_music, container, false);

        // Initialize API
        musicApiClient = MusicApiClient.getInstance();
        authManager = SupabaseAuthManager.getInstance(requireContext());

        initViews(view);
        setupSpinner();
        setupClickListeners();
        checkPermissions();

        // Create storage buckets if not exist
        SupabaseStorageHelper.createBucketsIfNotExist();

        // Debug authentication status
        debugAuthenticationStatus();

        return view;
    }

    private void initViews(View view) {
        etTitle = view.findViewById(R.id.et_title);
        etArtist = view.findViewById(R.id.et_artist);
        etDoctorName = view.findViewById(R.id.et_doctor_name);
        etJournalReference = view.findViewById(R.id.et_journal_reference);
        etDuration = view.findViewById(R.id.et_duration);
        etDescription = view.findViewById(R.id.et_description);
        etImageUrl = view.findViewById(R.id.et_image_url);
        etAudioUrl = view.findViewById(R.id.et_audio_url);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        ivPreview = view.findViewById(R.id.iv_preview);
        tvAudioFilename = view.findViewById(R.id.tv_audio_filename);
        btnChooseImage = view.findViewById(R.id.btn_choose_image);
        btnChooseAudio = view.findViewById(R.id.btn_choose_audio);
        btnUpload = view.findViewById(R.id.btn_upload);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnChooseImage.setOnClickListener(v -> chooseImage());
        btnChooseAudio.setOnClickListener(v -> chooseAudio());
        btnUpload.setOnClickListener(v -> uploadMusic());

        // Preview image from URL
        etImageUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String url = etImageUrl.getText().toString().trim();
                if (!url.isEmpty()) {
                    loadImagePreview(url);
                    // Clear selected image if URL is provided
                    selectedImageUri = null;
                }
            }
        });
    }

    private void debugAuthenticationStatus() {
        Log.d(TAG, "=== DEBUGGING AUTHENTICATION ===");
        Log.d(TAG, "Is user logged in: " + authManager.isUserLoggedIn());
        Log.d(TAG, "User email: " + authManager.getCurrentUserEmail());
        Log.d(TAG, "User role: " + authManager.getCurrentUserRole());

        String accessToken = authManager.getAccessToken();
        Log.d(TAG, "Access token available: " + (accessToken != null && !accessToken.isEmpty()));
        if (accessToken != null) {
            Log.d(TAG, "Access token (first 30 chars): " + accessToken.substring(0, Math.min(30, accessToken.length())) + "...");
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void chooseAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Pilih File Audio"), PICK_AUDIO_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                selectedImageUri = data.getData();
                loadImagePreview(selectedImageUri);
                // Clear URL field when image is selected
                etImageUrl.setText("");
            } else if (requestCode == PICK_AUDIO_REQUEST) {
                selectedAudioUri = data.getData();
                String filename = getFileName(selectedAudioUri);
                tvAudioFilename.setText(filename);
                // Clear URL field when audio is selected
                etAudioUrl.setText("");
            }
        }
    }

    private void loadImagePreview(Uri uri) {
        Glide.with(this)
                .load(uri)
                .centerCrop()
                .placeholder(R.drawable.image_placeholder)
                .into(ivPreview);
    }

    private void loadImagePreview(String url) {
        Glide.with(this)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(ivPreview);
    }

    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1) {
                return path.substring(lastSlash + 1);
            }
        }
        return "audio_file";
    }

    private void uploadMusic() {
        Log.d(TAG, "=== STARTING MUSIC UPLOAD ===");

        // Validasi form
        if (!validateForm()) {
            return;
        }

        // Authentication check
        if (!validateAuthentication()) {
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Check if we need to upload files or use URLs
        boolean needImageUpload = selectedImageUri != null && etImageUrl.getText().toString().trim().isEmpty();
        boolean needAudioUpload = selectedAudioUri != null && etAudioUrl.getText().toString().trim().isEmpty();

        if (needImageUpload || needAudioUpload) {
            // Upload files first, then create music entry
            uploadFilesAndCreateMusic(needImageUpload, needAudioUpload);
        } else {
            // No file uploads needed, create music entry directly
            createMusicEntry();
        }
    }

    private void uploadFilesAndCreateMusic(boolean uploadImage, boolean uploadAudio) {
        Log.d(TAG, "📤 Starting file uploads - Image: " + uploadImage + ", Audio: " + uploadAudio);

        // Track upload completion
        final boolean[] imageUploaded = {!uploadImage}; // Mark as done if not needed
        final boolean[] audioUploaded = {!uploadAudio}; // Mark as done if not needed

        // Upload image if needed
        if (uploadImage && selectedImageUri != null) {
            SupabaseStorageHelper.uploadImage(requireContext(), selectedImageUri, new SupabaseStorageHelper.UploadCallback() {
                @Override
                public void onSuccess(String fileUrl) {
                    Log.d(TAG, "✅ Image uploaded successfully: " + fileUrl);
                    uploadedImageUrl = fileUrl;
                    imageUploaded[0] = true;

                    // Check if all uploads are complete
                    if (imageUploaded[0] && audioUploaded[0]) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> createMusicEntry());
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Image upload failed: " + error);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            setLoadingState(false);
                            Toast.makeText(getContext(), "❌ Gagal upload gambar: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        }

        // Upload audio if needed
        if (uploadAudio && selectedAudioUri != null) {
            SupabaseStorageHelper.uploadAudioFile(requireContext(), selectedAudioUri, new SupabaseStorageHelper.UploadCallback() {
                @Override
                public void onSuccess(String fileUrl) {
                    Log.d(TAG, "✅ Audio uploaded successfully: " + fileUrl);
                    uploadedAudioUrl = fileUrl;
                    audioUploaded[0] = true;

                    // Check if all uploads are complete
                    if (imageUploaded[0] && audioUploaded[0]) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> createMusicEntry());
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Audio upload failed: " + error);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            setLoadingState(false);
                            Toast.makeText(getContext(), "❌ Gagal upload audio: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        }
    }

    private void createMusicEntry() {
        Log.d(TAG, "📝 Creating music entry in database");

        // Get access token
        String accessToken = authManager.getAccessToken();
        Log.d(TAG, "🔐 Using access token: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");

        // Create music object dengan URL yang sudah diupload atau dari input manual
        MusicTrack music = createMusicTrackFromForm();
        if (music == null) {
            setLoadingState(false);
            return;
        }

        // Log music data
        Log.d(TAG, "📤 Music data to upload:");
        Log.d(TAG, "Title: " + music.getTitle());
        Log.d(TAG, "Artist: " + music.getArtist());
        Log.d(TAG, "Category: " + music.getCategory());
        Log.d(TAG, "Duration: " + music.getDuration());
        Log.d(TAG, "Image URL: " + music.getImageUrl());
        Log.d(TAG, "Audio URL: " + music.getAudioUrl());

        // Upload ke database
        musicApiClient.createMusic(music, accessToken, new MusicApiClient.ApiCallback<MusicTrack>() {
            @Override
            public void onSuccess(MusicTrack result) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);

                        Log.d(TAG, "✅ UPLOAD SUCCESS!");
                        Toast.makeText(getContext(), "✅ Musik berhasil diupload!", Toast.LENGTH_LONG).show();

                        // Clear form setelah berhasil
                        clearForm();

                        // Reset uploaded URLs
                        uploadedImageUrl = null;
                        uploadedAudioUrl = null;
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);

                        Log.e(TAG, "❌ UPLOAD ERROR: " + error);

                        // Show user-friendly error message
                        showErrorToUser(error);

                        // Check if need to re-authenticate
                        if (error.contains("401") || error.contains("AUTHORIZATION")) {
                            handleAuthenticationError();
                        }
                    });
                }
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Title validation
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("❌ Judul harus diisi");
            etTitle.requestFocus();
            isValid = false;
        } else if (title.length() < 2) {
            etTitle.setError("❌ Judul minimal 2 karakter");
            etTitle.requestFocus();
            isValid = false;
        }

        // Audio validation
        String audioUrl = etAudioUrl.getText().toString().trim();
        if (selectedAudioUri == null && TextUtils.isEmpty(audioUrl)) {
            Toast.makeText(getContext(), "❌ Pilih file audio atau masukkan URL audio", Toast.LENGTH_LONG).show();
            isValid = false;
        }

        // Duration validation (optional but recommended)
        String duration = etDuration.getText().toString().trim();
        if (!TextUtils.isEmpty(duration)) {
            if (!duration.matches("\\d{2}:\\d{2}")) {
                etDuration.setError("❌ Format durasi: MM:SS (contoh: 04:30)");
                isValid = false;
            }
        }

        return isValid;
    }

    private boolean validateAuthentication() {
        Log.d(TAG, "🔍 Validating authentication...");

        if (!authManager.isUserLoggedIn()) {
            Log.e(TAG, "❌ User not logged in");
            showAuthenticationError("❌ Anda harus login terlebih dahulu");
            return false;
        }

        String accessToken = authManager.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            Log.e(TAG, "❌ Access token is empty");
            showAuthenticationError("❌ Token akses tidak valid. Silakan login ulang");
            return false;
        }

        String userRole = authManager.getCurrentUserRole();
        if (!"Admin".equalsIgnoreCase(userRole)) {
            Log.e(TAG, "❌ User role is not Admin: " + userRole);
            showAuthenticationError("❌ Hanya Admin yang dapat mengupload musik");
            return false;
        }

        Log.d(TAG, "✅ Authentication validation passed");
        return true;
    }

    private MusicTrack createMusicTrackFromForm() {
        try {
            MusicTrack music = new MusicTrack();

            music.setTitle(etTitle.getText().toString().trim());
            music.setArtist(etArtist.getText().toString().trim());
            music.setCategory(spinnerCategory.getSelectedItem().toString());
            music.setDoctorName(etDoctorName.getText().toString().trim());
            music.setJournalReference(etJournalReference.getText().toString().trim());
            music.setDuration(etDuration.getText().toString().trim());
            music.setDescription(etDescription.getText().toString().trim());

            // Handle image URL - prioritize uploaded URL, then manual URL
            if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
                music.setImageUrl(uploadedImageUrl);
            } else {
                String imageUrl = etImageUrl.getText().toString().trim();
                if (!TextUtils.isEmpty(imageUrl)) {
                    music.setImageUrl(imageUrl);
                }
            }

            // Handle audio URL - prioritize uploaded URL, then manual URL
            if (uploadedAudioUrl != null && !uploadedAudioUrl.isEmpty()) {
                music.setAudioUrl(uploadedAudioUrl);
            } else {
                String audioUrl = etAudioUrl.getText().toString().trim();
                if (!TextUtils.isEmpty(audioUrl)) {
                    music.setAudioUrl(audioUrl);
                }
            }

            return music;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating MusicTrack object", e);
            Toast.makeText(getContext(), "❌ Error memproses data musik", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void setLoadingState(boolean isLoading) {
        btnUpload.setEnabled(!isLoading);
        btnUpload.setText(isLoading ? "⏳ Mengupload..." : "🎵 Upload Musik");

        // Disable other buttons during upload
        btnChooseImage.setEnabled(!isLoading);
        btnChooseAudio.setEnabled(!isLoading);

        // Disable input fields during upload
        etTitle.setEnabled(!isLoading);
        etArtist.setEnabled(!isLoading);
        etDoctorName.setEnabled(!isLoading);
        etJournalReference.setEnabled(!isLoading);
        etDuration.setEnabled(!isLoading);
        etDescription.setEnabled(!isLoading);
        etImageUrl.setEnabled(!isLoading);
        etAudioUrl.setEnabled(!isLoading);
        spinnerCategory.setEnabled(!isLoading);
    }

    private void showErrorToUser(String error) {
        String userMessage;

        if (error.contains("401") || error.contains("AUTHORIZATION")) {
            userMessage = "🔐 Masalah autentikasi. Silakan login ulang.";
        } else if (error.contains("403") || error.contains("PERMISSION")) {
            userMessage = "⛔ Anda tidak memiliki izin untuk mengupload musik.";
        } else if (error.contains("422") || error.contains("duplicate")) {
            userMessage = "📝 Musik dengan judul ini sudah ada.";
        } else if (error.contains("network") || error.contains("timeout")) {
            userMessage = "🌐 Masalah koneksi internet. Periksa koneksi Anda.";
        } else if (error.contains("500")) {
            userMessage = "⚠️ Terjadi kesalahan server. Coba lagi nanti.";
        } else {
            userMessage = "❌ Upload gagal: " + error;
        }

        Toast.makeText(getContext(), userMessage, Toast.LENGTH_LONG).show();
    }

    private void showAuthenticationError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void handleAuthenticationError() {
        Log.d(TAG, "🔄 Handling authentication error - clearing session");

        // Clear current session
        authManager.clearSession();

        // Show message to user
        Toast.makeText(getContext(),
                "🔐 Sesi login telah berakhir. Silakan login ulang.",
                Toast.LENGTH_LONG).show();
    }

    private void clearForm() {
        etTitle.setText("");
        etArtist.setText("");
        etDoctorName.setText("");
        etJournalReference.setText("");
        etDuration.setText("");
        etDescription.setText("");
        etImageUrl.setText("");
        etAudioUrl.setText("");
        spinnerCategory.setSelection(0);
        ivPreview.setImageResource(R.drawable.image_placeholder);
        tvAudioFilename.setText("Belum ada file dipilih");
        selectedImageUri = null;
        selectedAudioUri = null;

        // Clear any errors
        etTitle.setError(null);
        etDuration.setError(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "✅ Izin akses storage diberikan", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "⚠️ Izin akses storage diperlukan untuk memilih file", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Reset button state jika fragment di-detach saat upload berlangsung
        if (btnUpload != null) {
            setLoadingState(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-check authentication status when fragment resumes
        debugAuthenticationStatus();
    }
}