package com.chandra.soundscape;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.chandra.soundscape.admin.adapters.MusicTrackAdapter;
import com.chandra.soundscape.api.MusicApiClient;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExploreFragment extends Fragment implements MusicTrackAdapter.OnMusicClickListener {
    private static final String TAG = "ExploreFragment";

    // UI Components
    private EditText searchEditText;
    private ImageView searchIcon, clearSearchIcon;
    private ChipGroup chipGroupCategories;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View emptyView;
    private TextView tvEmptyMessage, tvResultCount;

    // Data
    private List<MusicTrack> allMusicList = new ArrayList<>();
    private List<MusicTrack> filteredMusicList = new ArrayList<>();
    private MusicTrackAdapter adapter;
    private MusicApiClient musicApiClient;

    // Filter state
    private String currentSearchQuery = "";
    private String selectedCategory = "All";
    private Set<String> availableCategories = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        musicApiClient = MusicApiClient.getInstance();

        initViews(view);
        setupRecyclerView();
        setupSearchBar();
        setupSwipeRefresh();
        setupCategoryFilter();

        loadAllMusic();

        return view;
    }

    private void initViews(View view) {
        searchEditText = view.findViewById(R.id.et_search);
        searchIcon = view.findViewById(R.id.iv_search_icon);
        clearSearchIcon = view.findViewById(R.id.iv_clear_search);
        chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        recyclerView = view.findViewById(R.id.recycler_explore);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);
        tvResultCount = view.findViewById(R.id.tv_result_count);
    }

    private void setupRecyclerView() {
        // Use GridLayoutManager for 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        // Initialize adapter
        adapter = new MusicTrackAdapter(getContext(), filteredMusicList, true);
        adapter.setOnMusicClickListener(this);
        recyclerView.setAdapter(adapter);

        // Add item decoration for spacing
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing, true));
    }

    private void setupSearchBar() {
        // Clear search icon click
        clearSearchIcon.setOnClickListener(v -> {
            searchEditText.setText("");
            currentSearchQuery = "";
            filterMusic();
        });

        // Search text change listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                clearSearchIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterMusic();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadAllMusic);
        swipeRefresh.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark
        );
    }

    private void setupCategoryFilter() {
        // Add "All" chip as default
        addCategoryChip("All", true);
    }

    private void addCategoryChip(String category, boolean isSelected) {
        Chip chip = new Chip(getContext());
        chip.setText(category);
        chip.setCheckable(true);
        chip.setChecked(isSelected);
        chip.setChipBackgroundColorResource(R.color.chip_background_selector);
        chip.setTextColor(getResources().getColorStateList(R.color.chip_text_color_selector));

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

    private void loadAllMusic() {
        showLoading(true);

        musicApiClient.getAllMusic(new MusicApiClient.ApiCallback<List<MusicTrack>>() {
            @Override
            public void onSuccess(List<MusicTrack> musicTracks) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        allMusicList.clear();
                        availableCategories.clear();
                        availableCategories.add("All");

                        if (musicTracks != null) {
                            allMusicList.addAll(musicTracks);

                            // Extract unique categories
                            for (MusicTrack track : musicTracks) {
                                if (track.getCategory() != null && !track.getCategory().isEmpty()) {
                                    availableCategories.add(track.getCategory());
                                }
                            }

                            // Update category chips
                            updateCategoryChips();
                        }

                        filterMusic();
                        showLoading(false);

                        if (swipeRefresh.isRefreshing()) {
                            swipeRefresh.setRefreshing(false);
                        }

                        Log.d(TAG, "Music loaded: " + allMusicList.size());
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Error loading music: " + error);
                        showLoading(false);

                        if (swipeRefresh.isRefreshing()) {
                            swipeRefresh.setRefreshing(false);
                        }

                        Toast.makeText(getContext(), "Error loading music", Toast.LENGTH_SHORT).show();
                        showEmptyView(true, "Gagal memuat musik. Tarik untuk refresh.");
                    });
                }
            }
        });
    }

    private void updateCategoryChips() {
        // Clear existing chips except "All"
        for (int i = chipGroupCategories.getChildCount() - 1; i >= 1; i--) {
            chipGroupCategories.removeViewAt(i);
        }

        // Add category chips
        for (String category : availableCategories) {
            if (!category.equals("All")) {
                addCategoryChip(category, false);
            }
        }
    }

    private void filterMusic() {
        filteredMusicList.clear();

        for (MusicTrack track : allMusicList) {
            boolean matchesCategory = selectedCategory.equals("All") ||
                    (track.getCategory() != null && track.getCategory().equals(selectedCategory));

            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (track.getTitle() != null && track.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                    (track.getArtist() != null && track.getArtist().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                    (track.getDoctorName() != null && track.getDoctorName().toLowerCase().contains(currentSearchQuery.toLowerCase()));

            if (matchesCategory && matchesSearch) {
                filteredMusicList.add(track);
            }
        }

        // Update UI
        adapter.updateData(filteredMusicList);
        updateResultCount();

        // Show empty view if no results
        if (filteredMusicList.isEmpty()) {
            String message = currentSearchQuery.isEmpty() ?
                    "Tidak ada musik dalam kategori " + selectedCategory :
                    "Tidak ada hasil untuk \"" + currentSearchQuery + "\"";
            showEmptyView(true, message);
        } else {
            showEmptyView(false, "");
        }
    }

    private void updateResultCount() {
        if (tvResultCount != null) {
            String resultText = filteredMusicList.size() + " soundscape ditemukan";
            tvResultCount.setText(resultText);
            tvResultCount.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyView(boolean show, String message) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);

        if (show && tvEmptyMessage != null) {
            tvEmptyMessage.setText(message);
        }
    }

    @Override
    public void onMusicClick(MusicTrack music) {
        // Open music player with details
        Intent intent = new Intent(getContext(), MusicPlayerActivity.class);
        intent.putExtra("music_track", music);
        startActivity(intent);
    }

    @Override
    public void onPlayClick(MusicTrack music) {
        // Play music
        Intent intent = new Intent(getContext(), MusicPlayerActivity.class);
        intent.putExtra("music_track", music);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(MusicTrack music, boolean isFavorite) {
        // Favorite already handled in adapter
        // Update UI if needed
        if (adapter != null) {
            adapter.updateFavoriteState(music.getId());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh favorite states when returning to this fragment
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // GridSpacingItemDecoration class
    public static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}