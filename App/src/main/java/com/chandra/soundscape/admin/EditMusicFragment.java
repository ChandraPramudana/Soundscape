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

public class EditMusicFragment extends Fragment {

    private static final String TAG = "EditMusicFragment";
    private static final String ARG_MUSIC = "music";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_AUDIO_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private TextInputEditText etTitle, etArtist, etDoctorName, etJournalReference;
    private TextInputEditText etDuration, etDescription, etImageUrl, etAudioUrl;
    private Spinner spinnerCategory;
    private ImageView ivPreview;
    private TextView tvAudioFilename;
    private MaterialButton btnUpdate, btnCancel, btnChooseImage, btnChooseAudio;

    // Data
    private MusicTrack currentMusic;
    private MusicApiClient musicApiClient;
    private SupabaseAuthManager authManager;

    // File URIs
    private Uri selectedImageUri;
    private Uri selectedAudioUri;

    // URLs after upload
    private String uploadedImageUrl;
    private String uploadedAudioUrl;

    // Categories
    private String[] categories = {"DeepSleep", "Mindfulness", "Stress Relief", "Therapeutic"};

    public static EditMusicFragment newInstance(MusicTrack music) {
        EditMusicFragment fragment = new EditMusicFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MUSIC, music);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentMusic = (MusicTrack) getArguments().getSerializable(ARG_MUSIC);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_music, container, false);

        // Initialize API
        musicApiClient = MusicApiClient.getInstance();
        authManager = SupabaseAuthManager.getInstance(requireContext());

        initViews(view);
        modifyUIForEdit();
        setupSpinner();
        loadMusicData();
        setupClickListeners();
        checkPermissions();

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
        btnUpdate = view.findViewById(R.id.btn_upload);
        btnChooseImage = view.findViewById(R.id.btn_choose_image);
        btnChooseAudio = view.findViewById(R.id.btn_choose_audio);

        // Show file chooser buttons for edit mode (they were hidden in the old version)
        btnChooseImage.setVisibility(View.VISIBLE);
        btnChooseAudio.setVisibility(View.VISIBLE);
        tvAudioFilename.setVisibility(View.VISIBLE);
    }

    private void modifyUIForEdit() {
        // Change button text
        btnUpdate.setText("Update Musik");
        btnUpdate.setIconResource(R.drawable.ic_save);

        // Change title
        if (getActivity() != null) {
            getActivity().setTitle("Edit Musik");
        }

        // Update button text
        btnChooseImage.setText("Ganti Gambar");
        btnChooseAudio.setText("Ganti Audio");
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

    private void loadMusicData() {
        if (currentMusic == null) return;

        // Load text fields
        etTitle.setText(currentMusic.getTitle());
        etArtist.setText(currentMusic.getArtist());
        etDoctorName.setText(currentMusic.getDoctorName());
        etJournalReference.setText(currentMusic.getJournalReference());
        etDuration.setText(currentMusic.getDuration());
        etDescription.setText(currentMusic.getDescription());
        etImageUrl.setText(currentMusic.getImageUrl());
        etAudioUrl.setText(currentMusic.getAudioUrl());

        // Show current audio file info
        if (currentMusic.hasAudio()) {
            tvAudioFilename.setText("Audio saat ini: Tersimpan");
        }

        // Set category spinner
        String category = currentMusic.getCategory();
        if (category != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(category)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        // Load image preview
        if (currentMusic.hasImage()) {
            Glide.with(this)
                    .load(currentMusic.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.image_placeholder)
                    .into(ivPreview);
        }
    }

    private void setupClickListeners() {
        btnUpdate.setOnClickListener(v -> updateMusic());
        btnChooseImage.setOnClickListener(v -> chooseImage());
        btnChooseAudio.setOnClickListener(v -> chooseAudio());

        // Preview image from URL
        etImageUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String url = etImageUrl.getText().toString().trim();
                if (!url.isEmpty()) {
                    Glide.with(this)
                            .load(url)
                            .centerCrop()
                            .placeholder(R.drawable.image_placeholder)
                            .error(R.drawable.image_placeholder)
                            .into(ivPreview);
                    // Clear selected image if URL is provided
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
                Glide.with(this)
                        .load(selectedImageUri)
                        .centerCrop()
                        .placeholder(R.drawable.image_placeholder)
                        .into(ivPreview);
                // Clear URL field when image is selected
                etImageUrl.setText("");
            } else if (requestCode == PICK_AUDIO_REQUEST) {
                selectedAudioUri = data.getData();
                String filename = getFileName(selectedAudioUri);
                tvAudioFilename.setText("Audio baru: " + filename);
                // Clear URL field when audio is selected
                etAudioUrl.setText("");
            }
        }
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

    private void updateMusic() {
        // Validate required fields
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Judul harus diisi");
            etTitle.requestFocus();
            return;
        }

        // Show loading
        btnUpdate.setEnabled(false);
        btnUpdate.setText("Menyimpan...");

        // Check if we need to upload new files
        boolean needImageUpload = selectedImageUri != null;
        boolean needAudioUpload = selectedAudioUri != null;

        if (needImageUpload || needAudioUpload) {
            // Upload files first, then update music entry
            uploadFilesAndUpdateMusic(needImageUpload, needAudioUpload);
        } else {
            // No file uploads needed, update music entry directly
            updateMusicEntry();
        }
    }

    private void uploadFilesAndUpdateMusic(boolean uploadImage, boolean uploadAudio) {
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
                            getActivity().runOnUiThread(() -> updateMusicEntry());
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Image upload failed: " + error);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnUpdate.setEnabled(true);
                            btnUpdate.setText("Update Musik");
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
                            getActivity().runOnUiThread(() -> updateMusicEntry());
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Audio upload failed: " + error);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnUpdate.setEnabled(true);
                            btnUpdate.setText("Update Musik");
                            Toast.makeText(getContext(), "❌ Gagal upload audio: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        }
    }

    private void updateMusicEntry() {
        // Update music object
        currentMusic.setTitle(etTitle.getText().toString().trim());
        currentMusic.setArtist(etArtist.getText().toString().trim());
        currentMusic.setCategory(spinnerCategory.getSelectedItem().toString());
        currentMusic.setDoctorName(etDoctorName.getText().toString().trim());
        currentMusic.setJournalReference(etJournalReference.getText().toString().trim());
        currentMusic.setDuration(etDuration.getText().toString().trim());
        currentMusic.setDescription(etDescription.getText().toString().trim());

        // Handle image URL - prioritize uploaded URL, then manual URL
        if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
            currentMusic.setImageUrl(uploadedImageUrl);
        } else {
            String imageUrl = etImageUrl.getText().toString().trim();
            if (!TextUtils.isEmpty(imageUrl)) {
                currentMusic.setImageUrl(imageUrl);
            }
        }

        // Handle audio URL - prioritize uploaded URL, then manual URL
        if (uploadedAudioUrl != null && !uploadedAudioUrl.isEmpty()) {
            currentMusic.setAudioUrl(uploadedAudioUrl);
        } else {
            String audioUrl = etAudioUrl.getText().toString().trim();
            if (!TextUtils.isEmpty(audioUrl)) {
                currentMusic.setAudioUrl(audioUrl);
            }
        }

        // Get access token
        String accessToken = authManager.getAccessToken();
        musicApiClient.setAccessToken(accessToken);

        // Update in database
        musicApiClient.updateMusic(currentMusic.getId(), currentMusic, new MusicApiClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Musik berhasil diupdate!", Toast.LENGTH_SHORT).show();
                        // Go back to list
                        getParentFragmentManager().popBackStack();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnUpdate.setEnabled(true);
                        btnUpdate.setText("Update Musik");
                        Toast.makeText(getContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
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