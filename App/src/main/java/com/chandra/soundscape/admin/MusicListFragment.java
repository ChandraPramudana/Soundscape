package com.chandra.soundscape.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.chandra.soundscape.R;
import com.chandra.soundscape.SupabaseAuthManager;
import com.chandra.soundscape.admin.adapters.MusicAdminAdapter;
import com.chandra.soundscape.api.MusicApiClient;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class MusicListFragment extends Fragment implements MusicAdminAdapter.OnMusicActionListener {
    private static final String TAG = "MusicListFragment";

    // UI Components
    private RecyclerView recyclerMusic;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View emptyView;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private MaterialButton btnAddFirst;

    // Adapter
    private MusicAdminAdapter musicAdapter;
    private List<MusicTrack> musicList = new ArrayList<>();
    private List<MusicTrack> filteredList = new ArrayList<>();

    // API & Auth
    private MusicApiClient musicApiClient;
    private SupabaseAuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_list, container, false);

        // Initialize API & Auth
        musicApiClient = MusicApiClient.getInstance();
        authManager = SupabaseAuthManager.getInstance(requireContext());

        // IMPORTANT: Set access token for API client
        String accessToken = authManager.getAccessToken();
        if (accessToken != null) {
            musicApiClient.setAccessToken(accessToken);
            Log.d(TAG, "Access token set for API operations");
        } else {
            Log.w(TAG, "No access token available - delete operations may fail");
        }

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupSwipeRefresh();
        loadMusic();

        return view;
    }

    private void initViews(View view) {
        recyclerMusic = view.findViewById(R.id.recycler_music);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        btnAddFirst = view.findViewById(R.id.btn_add_first);

        // Add first button click
        btnAddFirst.setOnClickListener(v -> navigateToUpload());
    }

    private void setupRecyclerView() {
        recyclerMusic.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter = new MusicAdminAdapter(getContext(), filteredList, this);
        recyclerMusic.setAdapter(musicAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMusic(s.toString());
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            btnClearSearch.setVisibility(View.GONE);
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadMusic);
        swipeRefresh.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark
        );
    }

    private void loadMusic() {
        showLoading(true);

        musicApiClient.getAllMusic(new MusicApiClient.ApiCallback<List<MusicTrack>>() {
            @Override
            public void onSuccess(List<MusicTrack> result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        musicList.clear();
                        if (result != null) {
                            musicList.addAll(result);
                        }
                        filterMusic(etSearch.getText().toString());
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        updateEmptyView();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                        updateEmptyView();
                    });
                }
            }
        });
    }

    private void filterMusic(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(musicList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (MusicTrack music : musicList) {
                if (music.getTitle().toLowerCase().contains(lowerQuery) ||
                        (music.getArtist() != null && music.getArtist().toLowerCase().contains(lowerQuery)) ||
                        (music.getCategory() != null && music.getCategory().toLowerCase().contains(lowerQuery))) {
                    filteredList.add(music);
                }
            }
        }

        musicAdapter.updateData(filteredList);
        updateEmptyView();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerMusic.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyView() {
        boolean isEmpty = filteredList.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerMusic.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // =================== IMPLEMENTASI INTERFACE OnMusicActionListener ===================

    @Override
    public void onItemClick(MusicTrack music) {
        // Navigasi ke detail musik dengan validasi
        if (music != null) {
            Log.d(TAG, "Music item clicked: " + music.getTitle());

            try {
                // Pastikan music memiliki ID yang valid
                if (music.getId() == null || music.getId().isEmpty()) {
                    Toast.makeText(requireContext(), "Data musik tidak valid", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Navigate to MusicDetailFragment
                MusicDetailFragment detailFragment = MusicDetailFragment.newInstance(music);

                // Validasi fragment manager
                if (getParentFragmentManager() != null && getActivity() != null) {
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, detailFragment)
                            .addToBackStack("detail")
                            .commit();
                } else {
                    Log.e(TAG, "Fragment manager is null");
                    Toast.makeText(requireContext(), "Tidak dapat membuka detail", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error navigating to detail", e);
                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Music item is null");
            Toast.makeText(requireContext(), "Data musik tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEditClick(MusicTrack music) {
        // Navigate to edit fragment
        EditMusicFragment editFragment = EditMusicFragment.newInstance(music);
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDeleteClick(MusicTrack music) {
        // Check authentication first
        if (!authManager.isUserLoggedIn()) {
            Toast.makeText(requireContext(), "Sesi login telah berakhir. Silakan login kembali.", Toast.LENGTH_LONG).show();
            return;
        }

        // Update access token before delete operation
        String currentToken = authManager.getAccessToken();
        if (currentToken == null || currentToken.isEmpty()) {
            Toast.makeText(requireContext(), "Token autentikasi tidak valid. Silakan login ulang.", Toast.LENGTH_LONG).show();
            return;
        }

        // Set the current token to API client
        musicApiClient.setAccessToken(currentToken);

        new AlertDialog.Builder(requireContext())
                .setTitle("Hapus Musik Permanen")
                .setMessage("⚠️ PERINGATAN!\n\n" +
                        "Data \"" + music.getTitle() + "\" akan dihapus PERMANEN dari database!\n\n" +
                        "Tindakan ini TIDAK DAPAT DIBATALKAN!\n\n" +
                        "Apakah Anda yakin?")
                .setPositiveButton("Ya, Hapus Permanen", (dialog, which) -> hardDeleteMusic(music))
                .setNegativeButton("Batal", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void hardDeleteMusic(MusicTrack music) {
        Log.d(TAG, "=== STARTING HARD DELETE ===");
        Log.d(TAG, "Music ID: " + music.getId());
        Log.d(TAG, "Music Title: " + music.getTitle());

        progressBar.setVisibility(View.VISIBLE);

        // Ensure we have the latest access token
        String accessToken = authManager.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "❌ Gagal: Token autentikasi tidak valid", Toast.LENGTH_LONG).show();
            return;
        }

        // Set access token again just before the delete call
        musicApiClient.setAccessToken(accessToken);

        musicApiClient.deleteMusic(music.getId(), new MusicApiClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "✅ Delete API call successful");

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        // Remove from both lists
                        musicList.remove(music);
                        filteredList.remove(music);

                        // Update adapter
                        musicAdapter.updateData(filteredList);

                        // Update empty view
                        updateEmptyView();

                        Toast.makeText(getContext(), "✅ Musik berhasil dihapus permanen", Toast.LENGTH_SHORT).show();

                        // Force refresh to ensure sync with database
                        loadMusic();
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Delete failed: " + error);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        // Show detailed error message
                        new AlertDialog.Builder(requireContext())
                                .setTitle("❌ Gagal Menghapus")
                                .setMessage(error + "\n\nSolusi:\n" +
                                        "1. Pastikan Anda login sebagai Admin\n" +
                                        "2. Coba login ulang\n" +
                                        "3. Periksa RLS Policy di Supabase")
                                .setPositiveButton("OK", null)
                                .show();

                        // Refresh the list to show actual state
                        loadMusic();
                    });
                }
            }
        });
    }

    private void navigateToUpload() {
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
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update access token when fragment resumes
        String accessToken = authManager.getAccessToken();
        if (accessToken != null) {
            musicApiClient.setAccessToken(accessToken);
        }

        // Refresh the list
        loadMusic();
    }
}