package com.chandra.soundscape;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.chandra.soundscape.admin.adapters.MusicTrackAdapter;
import com.chandra.soundscape.api.MusicApiClient;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.appbar.AppBarLayout;
import java.util.ArrayList;
import java.util.List;

public class CategoryMusicFragment extends Fragment implements MusicTrackAdapter.OnMusicClickListener {
    private static final String TAG = "CategoryMusicFragment";
    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";

    // UI Components
    private Toolbar toolbar;
    private TextView tvCategoryTitle, tvCategorySubtitle, tvEmptyMessage;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View loadingView, emptyView;

    // Data
    private String categoryId;
    private String categoryName;
    private List<MusicTrack> musicList = new ArrayList<>();
    private MusicTrackAdapter adapter;
    private MusicApiClient musicApiClient;

    public static CategoryMusicFragment newInstance(String categoryId, String categoryName) {
        CategoryMusicFragment fragment = new CategoryMusicFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_ID, categoryId);
        args.putString(ARG_CATEGORY_NAME, categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString(ARG_CATEGORY_ID);
            categoryName = getArguments().getString(ARG_CATEGORY_NAME);
        }
        musicApiClient = MusicApiClient.getInstance();

        // Handle back press
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBack();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_music, container, false);

        initViews(view);
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        loadMusicByCategory();

        return view;
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tvCategoryTitle = view.findViewById(R.id.tv_category_title);
        tvCategorySubtitle = view.findViewById(R.id.tv_category_subtitle);
        recyclerView = view.findViewById(R.id.recycler_category_music);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        loadingView = view.findViewById(R.id.loading_view);
        emptyView = view.findViewById(R.id.empty_view);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);

        // Set category title
        if (tvCategoryTitle != null && categoryName != null) {
            tvCategoryTitle.setText(categoryName);
        }
    }

    private void setupToolbar() {
        if (toolbar != null && getActivity() != null) {
            toolbar.setTitle(categoryName);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationOnClickListener(v -> navigateBack());
        }
    }

    private void navigateBack() {
        // Navigate back to previous fragment
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            // If no back stack, go to home fragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ListenerHomeFragment())
                    .commit();
        }
    }

    private void setupRecyclerView() {
        // Use GridLayoutManager for 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        // Initialize adapter
        adapter = new MusicTrackAdapter(getContext(), musicList, true);
        adapter.setOnMusicClickListener(this);
        recyclerView.setAdapter(adapter);

        // Add item decoration for spacing
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing, true));
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadMusicByCategory);
            swipeRefresh.setColorSchemeResources(
                    R.color.colorPrimary,
                    R.color.colorAccent,
                    R.color.colorPrimaryDark
            );
        }
    }

    private void loadMusicByCategory() {
        showLoading(true);

        // Get all music and filter by category
        musicApiClient.getAllMusic(new MusicApiClient.ApiCallback<List<MusicTrack>>() {
            @Override
            public void onSuccess(List<MusicTrack> allMusic) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            musicList.clear();

                            // Filter music by category
                            if (allMusic != null) {
                                for (MusicTrack track : allMusic) {
                                    if (track.getCategory() != null &&
                                            track.getCategory().equalsIgnoreCase(categoryName)) {
                                        musicList.add(track);
                                    }
                                }
                            }

                            // Update UI
                            adapter.updateData(musicList);
                            updateSubtitle();
                            showLoading(false);

                            // Show empty view if no music found
                            if (musicList.isEmpty()) {
                                showEmptyView(true);
                            } else {
                                showEmptyView(false);
                            }

                            if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
                                swipeRefresh.setRefreshing(false);
                            }

                            Log.d(TAG, "Music loaded for category " + categoryName + ": " + musicList.size());

                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI", e);
                            showLoading(false);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Error loading music: " + error);
                        showLoading(false);

                        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
                            swipeRefresh.setRefreshing(false);
                        }

                        Toast.makeText(getContext(), "Error loading music", Toast.LENGTH_SHORT).show();
                        showEmptyView(true);
                    });
                }
            }
        });
    }

    private void updateSubtitle() {
        if (tvCategorySubtitle != null) {
            String subtitle = musicList.size() + " soundscape" + (musicList.size() != 1 ? "s" : "") + " tersedia";
            tvCategorySubtitle.setText(subtitle);
        }
    }

    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmptyView(boolean show) {
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (show && tvEmptyMessage != null) {
            tvEmptyMessage.setText("Belum ada soundscape untuk kategori " + categoryName);
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