package com.chandra.soundscape;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ProfileFragment extends Fragment {

    private ImageView ivProfilePhoto;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvMemberSince;
    private MaterialButton btnEditProfile;

    // Menu items
    private LinearLayout menuAbout;
    private LinearLayout menuLogout;

    // User session management
    private SharedPreferences userPrefs;
    private UserSessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize session manager
        sessionManager = new UserSessionManager(getContext());
        userPrefs = getContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);

        initViews(view);
        loadUserData();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        // Profile header
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvMemberSince = view.findViewById(R.id.tv_member_since);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        // Menu items
        menuAbout = view.findViewById(R.id.menu_about);
        menuLogout = view.findViewById(R.id.menu_logout);
    }

    private void loadUserData() {
        // Get current logged in user data
        if (sessionManager.isLoggedIn()) {
            UserData currentUser = sessionManager.getCurrentUser();
            String userRole = sessionManager.getUserRole();

            // Set profile information
            tvUserName.setText(currentUser.getName());
            tvUserEmail.setText(currentUser.getEmail());
            tvMemberSince.setText("Bergabung sejak " + currentUser.getMemberSince());

            // Show role badge
            showUserRole(userRole);

        } else {
            // If no user logged in, redirect to login
            redirectToLogin();
        }
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            openEditProfile();
        });

        menuAbout.setOnClickListener(v -> {
            showAboutDialog();
        });

        menuLogout.setOnClickListener(v -> {
            showLogoutConfirmation();
        });
    }

    private void openEditProfile() {
        // Create intent to open edit profile activity
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        startActivityForResult(intent, 100); // Request code for profile update
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Tentang SoundScape")
                .setMessage("SoundScape v1.0.0\n\nAplikasi Soundscape terbaik untuk relaksasi, fokus, dan tidur yang lebih baik.\n\nDikembangkan dengan ❤️ oleh Chandra\n\n© 2025 Chandra")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin keluar dari akun Anda?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Logout", (dialog, which) -> {
                    performLogout();
                })
                .show();
    }

    private void performLogout() {
        // Clear user session
        sessionManager.logout();

        // Show logout success message
        Toast.makeText(getContext(), "Logout berhasil", Toast.LENGTH_SHORT).show();

        // Redirect to login activity
        redirectToLogin();
    }

    private void showUserRole(String role) {
        // You can add a TextView in the layout to show user role
        // For now, we'll update the member since text to include role
        String memberText = tvMemberSince.getText().toString();
        tvMemberSince.setText(memberText + " • " + role);
    }

    private void navigateToAppropriateActivity(String role) {
        Intent intent;
        if (role.equals("Listener")) {
            intent = new Intent(getActivity(), ListenerDashboardActivity.class);
        } else {
            intent = new Intent(getActivity(), MainActivity.class);
        }
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle result from edit profile
        if (requestCode == 100 && resultCode == getActivity().RESULT_OK) {
            // Reload user data after profile update
            loadUserData();
            Toast.makeText(getContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            loadUserData();
        }
    }
}