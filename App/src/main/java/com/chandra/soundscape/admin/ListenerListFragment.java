package com.chandra.soundscape.admin;

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
import com.chandra.soundscape.api.MusicApiClient;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListenerListFragment extends Fragment {

    private static final String TAG = "ListenerListFragment";

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTotalListeners, tvLastUpdate;

    // Adapter
    private ListenerAdapter listenerAdapter;

    // API Client
    private MusicApiClient musicApiClient;

    // Data
    private List<Listener> listenerList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listener_list, container, false);

        // Initialize API
        musicApiClient = MusicApiClient.getInstance();

        initViews(view);
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();

        // Load listeners
        loadListeners();

        return view;
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        recyclerView = view.findViewById(R.id.recycler_listeners);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        tvTotalListeners = view.findViewById(R.id.tv_total_listeners);
        tvLastUpdate = view.findViewById(R.id.tv_last_update);
    }

    private void setupToolbar() {
        toolbar.setTitle("Daftar Listener");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> {
            // FIXED: Use FragmentManager to properly navigate back
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void setupRecyclerView() {
        listenerAdapter = new ListenerAdapter(listenerList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(listenerAdapter);
        recyclerView.setHasFixedSize(true);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadListeners();
        });
    }

    private void loadListeners() {
        Log.d(TAG, "=== LOADING LISTENERS ===");
        showLoading(true);

        // Get listeners from API using user_profiles table instead of auth.users
        musicApiClient.getUserProfiles(new MusicApiClient.ApiCallback<List<Listener>>() {
            @Override
            public void onSuccess(List<Listener> listeners) {
                Log.d(TAG, "✅ API Success: Received " + (listeners != null ? listeners.size() : 0) + " listeners");

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        listenerList.clear();
                        if (listeners != null && !listeners.isEmpty()) {
                            listenerList.addAll(listeners);
                            Log.d(TAG, "✅ Added " + listenerList.size() + " listeners to list");
                        } else {
                            Log.w(TAG, "⚠️ No listeners received from API");
                        }

                        // Notify adapter
                        listenerAdapter.notifyDataSetChanged();

                        // Update UI
                        updateUI();
                        showLoading(false);

                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }

                        // Show success message if we have data
                        if (!listenerList.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "✅ Berhasil memuat " + listenerList.size() + " listener",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ API Error: " + error);

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);

                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }

                        // Tampilkan error yang lebih user-friendly
                        String errorMessage = "Gagal memuat data listener";
                        String detailMessage = "";

                        if (error.contains("Table 'user_profiles' not found")) {
                            errorMessage = "Tabel user_profiles belum diatur";
                            detailMessage = "Pastikan tabel user_profiles sudah dibuat di Supabase dengan kolom:\n" +
                                    "• id (uuid)\n" +
                                    "• name (text)\n" +
                                    "• email (text)\n" +
                                    "• created_at (timestamp)\n" +
                                    "• last_active (timestamp)\n" +
                                    "• total_played (integer)";
                        } else if (error.contains("Permission denied")) {
                            errorMessage = "Akses ditolak";
                            detailMessage = "Periksa RLS Policy untuk tabel user_profiles.\n" +
                                    "Pastikan authenticated users dapat membaca data.";
                        } else if (error.contains("Authentication required") || error.contains("401")) {
                            errorMessage = "Sesi login telah berakhir";
                            detailMessage = "Silakan login kembali untuk melanjutkan.";
                        } else if (error.contains("Network")) {
                            errorMessage = "Tidak ada koneksi internet";
                            detailMessage = "Periksa koneksi internet Anda.";
                        }

                        // Show toast with main error
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();

                        // Update empty state with detailed message
                        String fullMessage = errorMessage;
                        if (!detailMessage.isEmpty()) {
                            fullMessage += "\n\n" + detailMessage;
                        }
                        fullMessage += "\n\nTarik ke bawah untuk coba lagi.";

                        tvEmpty.setText(fullMessage);
                        tvEmpty.setVisibility(View.VISIBLE);

                        updateUI();
                    });
                }
            }
        });
    }

    private void updateUI() {
        Log.d(TAG, "=== UPDATE UI - List size: " + listenerList.size() + " ===");

        if (listenerList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);

            // Only set default empty message if no error message is shown
            if (tvEmpty.getText().toString().isEmpty() ||
                    tvEmpty.getText().toString().equals("Belum ada listener terdaftar")) {
                tvEmpty.setText("Belum ada listener terdaftar");
            }

            // Update info text
            if (tvTotalListeners != null) {
                tvTotalListeners.setText("Total: 0 Listener");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);

            // Update total count
            if (tvTotalListeners != null) {
                tvTotalListeners.setText("Total: " + listenerList.size() + " Listener");
            }
        }

        // Update last refresh time
        if (tvLastUpdate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            String time = sdf.format(new Date());
            tvLastUpdate.setText("Terakhir diperbarui: " + time);
        }
    }

    private void showLoading(boolean show) {
        Log.d(TAG, "Show loading: " + show);

        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }

        // Hide empty view when loading
        if (show && tvEmpty != null) {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references
        toolbar = null;
        recyclerView = null;
        swipeRefreshLayout = null;
        progressBar = null;
        tvEmpty = null;
        tvTotalListeners = null;
        tvLastUpdate = null;
    }

    // Listener Model Class
    public static class Listener {
        private String id;
        private String name;
        private String email;
        private String joinDate;
        private int totalPlayed;
        private String lastActive;

        // Constructor
        public Listener() {
            // Default constructor
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getJoinDate() { return joinDate; }
        public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

        public int getTotalPlayed() { return totalPlayed; }
        public void setTotalPlayed(int totalPlayed) { this.totalPlayed = totalPlayed; }

        public String getLastActive() { return lastActive; }
        public void setLastActive(String lastActive) { this.lastActive = lastActive; }
    }
}