package com.chandra.soundscape.doctor;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.chandra.soundscape.R;
import com.chandra.soundscape.SupabaseAuthManager;
import com.chandra.soundscape.api.MusicApiClient;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class DoctorHomeFragment extends Fragment {

    private static final String TAG = "DoctorHomeFragment";

    // UI Components
    private TextView tvWelcome, tvPendingCount, tvApprovedCount, tvRejectedCount;
    private RecyclerView rvRecommendations;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private MaterialCardView cardUploadMusic;

    // Adapters
    private DoctorRecommendationAdapter recommendationAdapter;

    // API & Auth
    private MusicApiClient musicApiClient;
    private SupabaseAuthManager authManager;

    // Data
    private List<MusicTrack> recommendations = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_home, container, false);

        // Initialize API
        musicApiClient = MusicApiClient.getInstance();
        authManager = SupabaseAuthManager.getInstance(requireContext());

        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupClickListeners();

        // Load data
        loadDoctorRecommendations();

        return view;
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvPendingCount = view.findViewById(R.id.tv_pending_count);
        tvApprovedCount = view.findViewById(R.id.tv_approved_count);
        tvRejectedCount = view.findViewById(R.id.tv_rejected_count);
        rvRecommendations = view.findViewById(R.id.rv_recommendations);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        cardUploadMusic = view.findViewById(R.id.card_upload_music);

        // Set welcome message
        String doctorName = authManager.getCurrentUserName();
        if (doctorName == null || doctorName.isEmpty()) {
            doctorName = "Dokter";
        }
        tvWelcome.setText("Selamat datang, Dr. " + doctorName + "!");
    }

    private void setupRecyclerView() {
        recommendationAdapter = new DoctorRecommendationAdapter(recommendations, new DoctorRecommendationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MusicTrack music) {
                // Navigate to detail
                showRecommendationDetail(music);
            }
        });

        rvRecommendations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecommendations.setAdapter(recommendationAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadDoctorRecommendations();
        });
    }

    private void setupClickListeners() {
        cardUploadMusic.setOnClickListener(v -> {
            // Navigate to upload fragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new DoctorUploadMusicFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadDoctorRecommendations() {
        progressBar.setVisibility(View.VISIBLE);

        String doctorId = authManager.getCurrentUserId();

        // Get doctor's music recommendations
        musicApiClient.getDoctorRecommendations(doctorId, new MusicApiClient.ApiCallback<List<MusicTrack>>() {
            @Override
            public void onSuccess(List<MusicTrack> result) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);

                        recommendations.clear();
                        recommendations.addAll(result);
                        recommendationAdapter.notifyDataSetChanged();

                        // Update counters
                        updateStatusCounters(result);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateStatusCounters(List<MusicTrack> tracks) {
        int pending = 0, approved = 0, rejected = 0;

        for (MusicTrack track : tracks) {
            String status = track.getApprovalStatus();
            if ("pending".equals(status)) {
                pending++;
            } else if ("approved".equals(status)) {
                approved++;
            } else if ("rejected".equals(status)) {
                rejected++;
            }
        }

        tvPendingCount.setText(String.valueOf(pending));
        tvApprovedCount.setText(String.valueOf(approved));
        tvRejectedCount.setText(String.valueOf(rejected));
    }

    private void showRecommendationDetail(MusicTrack music) {
        // Show detail dialog or navigate to detail fragment
        DoctorRecommendationDetailDialog dialog = new DoctorRecommendationDetailDialog(music);
        dialog.show(getChildFragmentManager(), "recommendation_detail");
    }
}