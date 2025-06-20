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
import android.widget.TextView;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private ChipGroup chipGroupCategories;
    private TextView tvResultCount, tvEmptyTitle, tvEmptyMessage;

    // Adapter & Data
    private MusicAdminAdapter musicAdapter;
    private List<MusicTrack> musicList = new ArrayList<>();
    private List<MusicTrack> filteredList = new ArrayList<>();
    private Set<String> availableCategories = new HashSet<>();

    // Filter State
    private String currentSearchQuery = "";
    private String selectedCategory = "Semua";

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

        // Set access token
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
        setupCategoryFilter();
        loadMusic();

        return view;
    }

    private void initViews(View view) {
        // RecyclerView & Progress
        recyclerMusic = view.findViewById(R.id.recycler_music);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);

        // Empty View
        emptyView = view.findViewById(R.id.empty_view);
        tvEmptyTitle = view.findViewById(R.id.tv_empty_title);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);
        btnAddFirst = view.findViewById(R.id.btn_add_first);

        // Search
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);

        // Category & Stats
        chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        tvResultCount = view.findViewById(R.id.tv_result_count);

        // Click listener for add button
        btnAddFirst.setOnClickListener(v -> navigateToUpload());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerMusic.setLayoutManager(layoutManager);

        musicAdapter = new MusicAdminAdapter(getContext(), filteredList, this);
        recyclerMusic.setAdapter(musicAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterMusic();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            currentSearchQuery = "";
            filterMusic();
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

    private void setupCategoryFilter() {
        // Add default "Semua" chip
        addCategoryChip("Semua", true);
    }

    private void addCategoryChip(String category, boolean isSelected) {
        Chip chip = new Chip(getContext());
        chip.setText(category);
        chip.setCheckable(true);
        chip.setChecked(isSelected);

        // Style chip
        if (category.equals("Semua")) {
            chip.setChipIconResource(R.drawable.ic_all_inclusive);
        }

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Uncheck other chips
                for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
                    View child = chipGroupCategories.getChildAt(i);
                    if (child instanceof Chip && child != chip) {
                        ((Chip) child).setChecked(false);
                    }
                }
                selectedCategory = category;
                filterMusic();
            }
        });

        chipGroupCategories.addView(chip);
    }

    private void loadMusic() {
        showLoading(true);

        musicApiClient.getAllMusic(new MusicApiClient.ApiCallback<List<MusicTrack>>() {
            @Override
            public void onSuccess(List<MusicTrack> result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        musicList.clear();
                        availableCategories.clear();
                        availableCategories.add("Semua");

                        if (result != null) {
                            musicList.addAll(result);

                            // Extract unique categories
                            for (MusicTrack track : result) {
                                if (track.getCategory() != null && !track.getCategory().isEmpty()) {
                                    availableCategories.add(track.getCategory());
                                }
                            }

                            // Update category chips
                            updateCategoryChips();
                        }

                        filterMusic();
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);

                        Log.d(TAG, "Music loaded: " + musicList.size() + " tracks");
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

    private void updateCategoryChips() {
        // Clear existing chips except "Semua"
        for (int i = chipGroupCategories.getChildCount() - 1; i >= 1; i--) {
            chipGroupCategories.removeViewAt(i);
        }

        // Add category chips sorted alphabetically
        List<String> sortedCategories = new ArrayList<>(availableCategories);
        sortedCategories.remove("Semua");
        sortedCategories.sort(String::compareTo);

        for (String category : sortedCategories) {
            addCategoryChip(category, false);
        }
    }

    private void filterMusic() {
        filteredList.clear();

        for (MusicTrack track : musicList) {
            boolean matchesCategory = selectedCategory.equals("Semua") ||
                    (track.getCategory() != null && track.getCategory().equals(selectedCategory));

            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (track.getTitle() != null && track.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                    (track.getArtist() != null && track.getArtist().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                    (track.getCategory() != null && track.getCategory().toLowerCase().contains(currentSearchQuery.toLowerCase()));

            if (matchesCategory && matchesSearch) {
                filteredList.add(track);
            }
        }

        // Update adapter
        musicAdapter.updateData(filteredList);

        // Update result count
        updateResultCount();

        // Update empty view
        updateEmptyView();
    }

    private void updateResultCount() {
        String resultText = filteredList.size() + " musik ditemukan";
        tvResultCount.setText(resultText);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerMusic.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyView() {
        boolean isEmpty = filteredList.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerMusic.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            if (!currentSearchQuery.isEmpty()) {
                tvEmptyTitle.setText("Tidak ada hasil");
                tvEmptyMessage.setText("Tidak ada musik untuk \"" + currentSearchQuery + "\"");
                btnAddFirst.setVisibility(View.GONE);
            } else if (!selectedCategory.equals("Semua")) {
                tvEmptyTitle.setText("Kategori kosong");
                tvEmptyMessage.setText("Belum ada musik dalam kategori " + selectedCategory);
                btnAddFirst.setText("Tambah Musik " + selectedCategory);
                btnAddFirst.setVisibility(View.VISIBLE);
            } else {
                tvEmptyTitle.setText("Belum ada musik");
                tvEmptyMessage.setText("Mulai dengan menambahkan musik pertama");
                btnAddFirst.setText("Upload Musik Pertama");
                btnAddFirst.setVisibility(View.VISIBLE);
            }
        }
    }

    // =================== INTERFACE IMPLEMENTATIONS ===================

    @Override
    public void onItemClick(MusicTrack music) {
        if (music != null) {
            Log.d(TAG, "Music item clicked: " + music.getTitle());

            try {
                if (music.getId() == null || music.getId().isEmpty()) {
                    Toast.makeText(requireContext(), "Data musik tidak valid", Toast.LENGTH_SHORT).show();
                    return;
                }

                MusicDetailFragment detailFragment = MusicDetailFragment.newInstance(music);

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
        EditMusicFragment editFragment = EditMusicFragment.newInstance(music);
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDeleteClick(MusicTrack music) {
        if (!authManager.isUserLoggedIn()) {
            Toast.makeText(requireContext(), "Sesi login telah berakhir. Silakan login kembali.", Toast.LENGTH_LONG).show();
            return;
        }

        String currentToken = authManager.getAccessToken();
        if (currentToken == null || currentToken.isEmpty()) {
            Toast.makeText(requireContext(), "Token autentikasi tidak valid. Silakan login ulang.", Toast.LENGTH_LONG).show();
            return;
        }

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

        String accessToken = authManager.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "❌ Gagal: Token autentikasi tidak valid", Toast.LENGTH_LONG).show();
            return;
        }

        musicApiClient.setAccessToken(accessToken);

        musicApiClient.deleteMusic(music.getId(), new MusicApiClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "✅ Delete API call successful");

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        musicList.remove(music);
                        filteredList.remove(music);

                        musicAdapter.updateData(filteredList);
                        updateResultCount();
                        updateEmptyView();

                        Toast.makeText(getContext(), "✅ Musik berhasil dihapus permanen", Toast.LENGTH_SHORT).show();

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

                        new AlertDialog.Builder(requireContext())
                                .setTitle("❌ Gagal Menghapus")
                                .setMessage(error + "\n\nSolusi:\n" +
                                        "1. Pastikan Anda login sebagai Admin\n" +
                                        "2. Coba login ulang\n" +
                                        "3. Periksa RLS Policy di Supabase")
                                .setPositiveButton("OK", null)
                                .show();

                        loadMusic();
                    });
                }
            }
        });
    }

    private void navigateToUpload() {
        // Pre-fill category if one is selected
        Bundle args = new Bundle();
        if (!selectedCategory.equals("Semua")) {
            args.putString("preset_category", selectedCategory);
        }

        UploadMusicFragment uploadFragment = new UploadMusicFragment();
        uploadFragment.setArguments(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, uploadFragment)
                .addToBackStack(null)
                .commit();

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

        String accessToken = authManager.getAccessToken();
        if (accessToken != null) {
            musicApiClient.setAccessToken(accessToken);
        }

        loadMusic();
    }
}