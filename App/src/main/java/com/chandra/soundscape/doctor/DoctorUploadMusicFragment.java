package com.chandra.soundscape.doctor;

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

public class DoctorUploadMusicFragment extends Fragment {

    private static final String TAG = "DoctorUploadMusic";
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

        // Pre-fill doctor name
        String doctorName = authManager.getCurrentUserName();
        if (doctorName != null && !doctorName.isEmpty()) {
            etDoctorName.setText("Dr. " + doctorName);
        }

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

        // Update button text
        btnUpload.setText("🎵 Kirim Rekomendasi");
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

        etImageUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String url = etImageUrl.getText().toString().trim();
                if (!url.isEmpty()) {
                    loadImagePreview(url);
                    selectedImageUri = null;
                }
            }
        });
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
                etImageUrl.setText("");
            } else if (requestCode == PICK_AUDIO_REQUEST) {
                selectedAudioUri = data.getData();
                String filename = getFileName(selectedAudioUri);
                tvAudioFilename.setText(filename);
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
        Log.d(TAG, "=== STARTING DOCTOR MUSIC RECOMMENDATION ===");

        if (!validateForm()) {
            return;
        }

        if (!validateAuthentication()) {
            return;
        }

        setLoadingState(true);

        boolean needImageUpload = selectedImageUri != null && etImageUrl.getText().toString().trim().isEmpty();
        boolean needAudioUpload = selectedAudioUri != null && etAudioUrl.getText().toString().trim().isEmpty();

        if (needImageUpload || needAudioUpload) {
            uploadFilesAndCreateMusic(needImageUpload, needAudioUpload);
        } else {
            createMusicEntry();
        }
    }

    private void uploadFilesAndCreateMusic(boolean uploadImage, boolean uploadAudio) {
        final boolean[] imageUploaded = {!uploadImage};
        final boolean[] audioUploaded = {!uploadAudio};

        if (uploadImage && selectedImageUri != null) {
            SupabaseStorageHelper.uploadImage(requireContext(), selectedImageUri, new SupabaseStorageHelper.UploadCallback() {
                @Override
                public void onSuccess(String fileUrl) {
                    uploadedImageUrl = fileUrl;
                    imageUploaded[0] = true;

                    if (imageUploaded[0] && audioUploaded[0]) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> createMusicEntry());
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            setLoadingState(false);
                            Toast.makeText(getContext(), "❌ Gagal upload gambar: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        }

        if (uploadAudio && selectedAudioUri != null) {
            SupabaseStorageHelper.uploadAudioFile(requireContext(), selectedAudioUri, new SupabaseStorageHelper.UploadCallback() {
                @Override
                public void onSuccess(String fileUrl) {
                    uploadedAudioUrl = fileUrl;
                    audioUploaded[0] = true;

                    if (imageUploaded[0] && audioUploaded[0]) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> createMusicEntry());
                        }
                    }
                }

                @Override
                public void onError(String error) {
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
        Log.d(TAG, "📝 Creating doctor music recommendation");

        String accessToken = authManager.getAccessToken();
        MusicTrack music = createMusicTrackFromForm();

        if (music == null) {
            setLoadingState(false);
            return;
        }

        // Set doctor-specific fields
        music.setDoctorId(authManager.getCurrentUserId());
        music.setUploadedByDoctor(true);
        music.setApprovalStatus("pending");
        music.setIsDoctorRecommendation(true);

        musicApiClient.createMusic(music, accessToken, new MusicApiClient.ApiCallback<MusicTrack>() {
            @Override
            public void onSuccess(MusicTrack result) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(getContext(),
                                "✅ Rekomendasi berhasil dikirim!\nMenunggu persetujuan admin.",
                                Toast.LENGTH_LONG).show();
                        clearForm();
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
                        showErrorToUser(error);
                    });
                }
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

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

        String audioUrl = etAudioUrl.getText().toString().trim();
        if (selectedAudioUri == null && TextUtils.isEmpty(audioUrl)) {
            Toast.makeText(getContext(), "❌ Pilih file audio atau masukkan URL audio", Toast.LENGTH_LONG).show();
            isValid = false;
        }

        String duration = etDuration.getText().toString().trim();
        if (!TextUtils.isEmpty(duration)) {
            if (!duration.matches("\\d{2}:\\d{2}")) {
                etDuration.setError("❌ Format durasi: MM:SS (contoh: 04:30)");
                isValid = false;
            }
        }

        // Journal reference required for doctor
        String journalRef = etJournalReference.getText().toString().trim();
        if (TextUtils.isEmpty(journalRef)) {
            etJournalReference.setError("❌ Referensi jurnal wajib diisi");
            etJournalReference.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    private boolean validateAuthentication() {
        if (!authManager.isUserLoggedIn()) {
            showAuthenticationError("❌ Anda harus login terlebih dahulu");
            return false;
        }

        String accessToken = authManager.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            showAuthenticationError("❌ Token akses tidak valid. Silakan login ulang");
            return false;
        }

        String userRole = authManager.getCurrentUserRole();
        if (!"Dokter".equalsIgnoreCase(userRole)) {
            showAuthenticationError("❌ Hanya Dokter yang dapat mengirim rekomendasi");
            return false;
        }

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

            if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
                music.setImageUrl(uploadedImageUrl);
            } else {
                String imageUrl = etImageUrl.getText().toString().trim();
                if (!TextUtils.isEmpty(imageUrl)) {
                    music.setImageUrl(imageUrl);
                }
            }

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
        btnUpload.setText(isLoading ? "⏳ Mengirim..." : "🎵 Kirim Rekomendasi");

        btnChooseImage.setEnabled(!isLoading);
        btnChooseAudio.setEnabled(!isLoading);

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
            userMessage = "⛔ Anda tidak memiliki izin untuk mengirim rekomendasi.";
        } else if (error.contains("422") || error.contains("duplicate")) {
            userMessage = "📝 Musik dengan judul ini sudah ada.";
        } else if (error.contains("network") || error.contains("timeout")) {
            userMessage = "🌐 Masalah koneksi internet. Periksa koneksi Anda.";
        } else if (error.contains("500")) {
            userMessage = "⚠️ Terjadi kesalahan server. Coba lagi nanti.";
        } else {
            userMessage = "❌ Gagal mengirim: " + error;
        }

        Toast.makeText(getContext(), userMessage, Toast.LENGTH_LONG).show();
    }

    private void showAuthenticationError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void clearForm() {
        etTitle.setText("");
        etArtist.setText("");
        // Keep doctor name
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

        etTitle.setError(null);
        etDuration.setError(null);
        etJournalReference.setError(null);
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
}