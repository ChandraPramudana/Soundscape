package com.chandra.soundscape.admin;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.chandra.soundscape.R;
import com.chandra.soundscape.SupabaseAuthManager;
import com.chandra.soundscape.api.MusicApiClient;
import com.chandra.soundscape.doctor.DoctorRecommendationDetailDialog;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminHomeFragment extends Fragment {

    // UI Components
    private TextView tvWelcome, tvTotalUsers, tvTotalMusic, tvLastUpdate;
    private MaterialCardView cardUploadMusic, cardManageMusic, cardTotalUsers, cardTotalMusic;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressUsers, progressMusic;

    // API & Auth
    private MusicApiClient musicApiClient;
    private SupabaseAuthManager authManager;

    // Auto-refresh handler
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL = 30000; // 30 seconds

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        // Initialize API
        musicApiClient = MusicApiClient.getInstance();
        authManager = SupabaseAuthManager.getInstance(requireContext());

        initViews(view);
        setupClickListeners();
        setupSwipeRefresh();
        setupAutoRefresh();

        // Load statistics on create
        loadStatistics(false);
        loadDoctorRecommendations();

        return view;
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvTotalUsers = view.findViewById(R.id.tv_total_users);
        tvTotalMusic = view.findViewById(R.id.tv_total_music);
        tvLastUpdate = view.findViewById(R.id.tv_last_update);
        cardUploadMusic = view.findViewById(R.id.card_upload_music);
        cardManageMusic = view.findViewById(R.id.card_manage_music);
        cardTotalUsers = view.findViewById(R.id.card_total_users);
        cardTotalMusic = view.findViewById(R.id.card_total_music);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressUsers = view.findViewById(R.id.progress_users);
        progressMusic = view.findViewById(R.id.progress_music);

        // Set welcome message
        setWelcomeMessage();
    }

    private void setWelcomeMessage() {
        String adminName = authManager.getCurrentUserName();
        if (adminName == null || adminName.isEmpty()) {
            adminName = "Admin";
        }
        tvWelcome.setText("Selamat datang, " + adminName + "!");
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.colorPrimary,
                    R.color.colorAccent
            );

            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadStatistics(true);
            });
        }
    }

    private void setupAutoRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded() && getActivity() != null) {
                    loadStatistics(false);
                    // Schedule next refresh
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };
    }

    private void setupClickListeners() {
        // Total Users card - Navigate to Listener List
        if (cardTotalUsers != null) {
            cardTotalUsers.setOnClickListener(v -> {
                // Navigate to listener list fragment
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ListenerListFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Total Music card - Navigate to Music List
        if (cardTotalMusic != null) {
            cardTotalMusic.setOnClickListener(v -> {
                // Navigate to music list fragment
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new MusicListFragment())
                        .addToBackStack(null)
                        .commit();

                // Update bottom navigation
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                            getActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_music_list);
                    }
                }
            });
        }

        // Upload music card
        cardUploadMusic.setOnClickListener(v -> {
            // Navigate to upload fragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new UploadMusicFragment())
                    .addToBackStack(null)
                    .commit();

            // Update bottom navigation
            if (getActivity() != null) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_upload);
                }
            }
        });

        // Manage music card
        cardManageMusic.setOnClickListener(v -> {
            // Navigate to music list fragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MusicListFragment())
                    .addToBackStack(null)
                    .commit();

            // Update bottom navigation
            if (getActivity() != null) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_music_list);
                }
            }
        });
    }

    private void loadStatistics(boolean isManualRefresh) {
        if (!isAdded()) return;

        // Show loading indicators
        showLoading(true);

        // Get statistics from API using the improved method
        musicApiClient.getStatistics(new MusicApiClient.ApiCallback<MusicApiClient.Statistics>() {
            @Override
            public void onSuccess(MusicApiClient.Statistics statistics) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // Update UI with animation
                        updateStatisticsUI(statistics);

                        // Update last refresh time
                        updateLastRefreshTime();

                        // Hide loading
                        showLoading(false);

                        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }

                        if (isManualRefresh) {
                            Toast.makeText(getContext(), "Statistik berhasil diperbarui", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // Show error but keep last known values
                        showLoading(false);

                        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }

                        // Only show error toast for manual refresh
                        if (isManualRefresh) {
                            Toast.makeText(getContext(),
                                    "Gagal memperbarui statistik: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }

                        // Log error for debugging
                        android.util.Log.e("AdminHomeFragment", "Error loading statistics: " + error);
                    });
                }
            }
        });
    }

    private void loadDoctorRecommendations() {
        Log.d(TAG, "=== LOADING DOCTOR RECOMMENDATIONS ===");

        // Get pending doctor recommendations
        musicApiClient.getPendingDoctorRecommendations(new MusicApiClient.ApiCallback<List<MusicTrack>>() {
            @Override
            public void onSuccess(List<MusicTrack> recommendations) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // Update UI dengan rekomendasi dokter
                        updateDoctorRecommendationsUI(recommendations);
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading doctor recommendations: " + error);
            }
        });
    }

    private void updateDoctorRecommendationsUI(List<MusicTrack> recommendations) {
        // Hapus card rekomendasi yang sudah ada
        LinearLayout container = getView().findViewById(R.id.container_recommendations);
        if (container != null) {
            container.removeAllViews();
        }

        // Tambahkan card untuk setiap rekomendasi
        for (MusicTrack music : recommendations) {
            View recommendationCard = createRecommendationCard(music);
            if (recommendationCard != null && container != null) {
                container.addView(recommendationCard);
            }
        }
    }

    private View createRecommendationCard(MusicTrack music) {
        // Inflate card layout
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_doctor_recommendation_admin, null, false);

        // Set data
        TextView tvTitle = cardView.findViewById(R.id.tv_title);
        TextView tvDoctor = cardView.findViewById(R.id.tv_doctor_name);
        TextView tvCategory = cardView.findViewById(R.id.tv_category);
        TextView tvJournal = cardView.findViewById(R.id.tv_journal);
        MaterialButton btnAccept = cardView.findViewById(R.id.btn_accept);
        MaterialButton btnReject = cardView.findViewById(R.id.btn_reject);
        MaterialButton btnDetail = cardView.findViewById(R.id.btn_detail);

        tvTitle.setText(music.getTitle());
        tvDoctor.setText("Dr. " + music.getDoctorName());
        tvCategory.setText(music.getCategory());
        tvJournal.setText(music.getJournalReference());

        // Set click listeners
        btnAccept.setOnClickListener(v -> acceptRecommendation(music));
        btnReject.setOnClickListener(v -> showRejectDialog(music));
        btnDetail.setOnClickListener(v -> showDetailDialog(music));

        // Set margin
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(params);

        return cardView;
    }

    private void acceptRecommendation(MusicTrack music) {
        String adminId = authManager.getCurrentUserId();

        musicApiClient.approveRecommendation(music.getId(), adminId, new MusicApiClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Rekomendasi diterima", Toast.LENGTH_SHORT).show();
                        // Reload recommendations
                        loadDoctorRecommendations();
                        loadStatistics(false);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "❌ Gagal menerima rekomendasi: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void showRejectDialog(MusicTrack music) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reject_recommendation, null);

        EditText etReason = dialogView.findViewById(R.id.et_rejection_reason);

        builder.setView(dialogView)
                .setTitle("Tolak Rekomendasi")
                .setPositiveButton("Tolak", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (TextUtils.isEmpty(reason)) {
                        Toast.makeText(getContext(), "Alasan penolakan harus diisi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    rejectRecommendation(music, reason);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void rejectRecommendation(MusicTrack music, String reason) {
        String adminId = authManager.getCurrentUserId();

        musicApiClient.rejectRecommendation(music.getId(), adminId, reason, new MusicApiClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "❌ Rekomendasi ditolak", Toast.LENGTH_SHORT).show();
                        // Reload recommendations
                        loadDoctorRecommendations();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "❌ Gagal menolak rekomendasi: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void showDetailDialog(MusicTrack music) {
        DoctorRecommendationDetailDialog dialog = new DoctorRecommendationDetailDialog(music);
        dialog.show(getChildFragmentManager(), "recommendation_detail");
    }

    private void updateStatisticsUI(MusicApiClient.Statistics statistics) {
        // Animate number changes
        animateNumberChange(tvTotalUsers, statistics.getTotalUsers());
        animateNumberChange(tvTotalMusic, statistics.getTotalMusic());
    }

    private void animateNumberChange(TextView textView, int newValue) {
        // Get current value
        String currentText = textView.getText().toString();
        int currentValue = 0;
        try {
            currentValue = Integer.parseInt(currentText);
        } catch (NumberFormatException e) {
            // If parse fails, just set the new value
            textView.setText(String.valueOf(newValue));
            return;
        }

        // If values are same, no need to animate
        if (currentValue == newValue) {
            return;
        }

        // Simple animation effect
        textView.animate()
                .alpha(0.3f)
                .setDuration(150)
                .withEndAction(() -> {
                    textView.setText(String.valueOf(newValue));
                    textView.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private void showLoading(boolean show) {
        if (progressUsers != null) {
            progressUsers.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (progressMusic != null) {
            progressMusic.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // Dim the text while loading
        if (tvTotalUsers != null) {
            tvTotalUsers.setAlpha(show ? 0.5f : 1f);
        }
        if (tvTotalMusic != null) {
            tvTotalMusic.setAlpha(show ? 0.5f : 1f);
        }
    }

    private void updateLastRefreshTime() {
        if (tvLastUpdate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String time = sdf.format(new Date());
            tvLastUpdate.setText("Terakhir diperbarui: " + time);
            tvLastUpdate.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh statistics when fragment resumes
        loadStatistics(false);

        // Start auto-refresh
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop auto-refresh
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }
}