package com.chandra.soundscape;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chandra.soundscape.api.MusicApiClient;
import com.chandra.soundscape.models.*;
import com.chandra.soundscape.admin.adapters.CategoryAdapter;
import com.chandra.soundscape.admin.adapters.MusicTrackAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListenerHomeFragment extends Fragment {
    private static final String TAG = "ListenerHomeFragment";

    // UI Components
    private TextView welcomeText;
    private RecyclerView recyclerCategories, recyclerRecentMusic, recyclerRecommended;
    private SwipeRefreshLayout swipeRefresh;
    private View loadingView, emptyView;

    // Adapters
    private CategoryAdapter categoryAdapter;
    private MusicTrackAdapter recentMusicAdapter, recommendedMusicAdapter;

    // Data
    private List<MusicCategory> categoriesList = new ArrayList<>();
    private List<MusicTrack> recentMusicList = new ArrayList<>();
    private List<MusicTrack> recommendedMusicList = new ArrayList<>();

    // API & Auth
    private MusicApiClient musicApiClient;
    private SupabaseAuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listener_home, container, false);

        // Initialize APIs
        musicApiClient = MusicApiClient.getInstance();
        authManager = SupabaseAuthManager.getInstance(requireContext());

        initViews(view);
        setupRecyclerViews();
        setupSwipeRefresh();
        loadData();

        return view;
    }

    private void initViews(View view) {
        welcomeText = view.findViewById(R.id.tv_welcome);
        recyclerCategories = view.findViewById(R.id.recycler_categories);
        recyclerRecentMusic = view.findViewById(R.id.recycler_recent_soundscapes);
        recyclerRecommended = view.findViewById(R.id.recycler_recommended);

        // Initialize optional views with null checks
        swipeRefresh = view.findViewById(R.id.swipe_refresh_home);
        loadingView = view.findViewById(R.id.loading_view);
        emptyView = view.findViewById(R.id.empty_view);

        // Set welcome message
        setWelcomeMessage();
    }

    private void setWelcomeMessage() {
        String userName = "User"; // Default fallback

        try {
            // Try to get user data from SupabaseAuthManager first
            if (authManager != null && authManager.isUserLoggedIn()) {
                Log.d(TAG, "Getting user data from SupabaseAuthManager...");

                String authUserName = authManager.getCurrentUserName();
                String authUserEmail = authManager.getCurrentUserEmail();

                if (authUserName != null && !authUserName.isEmpty()) {
                    userName = authUserName;
                    Log.d(TAG, "✅ User name from auth: " + userName);
                } else {
                    Log.w(TAG, "Auth user name is empty, trying UserData...");

                    UserData userData = authManager.getCurrentUserData();
                    if (userData != null && userData.getName() != null && !userData.getName().isEmpty()) {
                        userName = userData.getName();
                        Log.d(TAG, "✅ User name from UserData: " + userName);
                    }
                }

                // Fallback to email extraction
                if (userName.equals("User") && authUserEmail != null && !authUserEmail.isEmpty()) {
                    String[] emailParts = authUserEmail.split("@");
                    if (emailParts.length > 0 && !emailParts[0].isEmpty()) {
                        userName = capitalizeFirstLetter(emailParts[0]);
                        Log.d(TAG, "✅ User name extracted from email: " + userName);
                    }
                }
            } else {
                Log.w(TAG, "AuthManager is null or user not logged in, trying SharedPreferences...");
            }

            // Fallback to SharedPreferences if auth manager doesn't have the data
            if (userName.equals("User")) {
                SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs",
                        requireContext().MODE_PRIVATE);
                String prefUserName = prefs.getString("userName", "");
                String prefUserEmail = prefs.getString("userEmail", "");

                if (!prefUserName.isEmpty()) {
                    userName = prefUserName;
                    Log.d(TAG, "✅ User name from SharedPreferences: " + userName);
                } else if (!prefUserEmail.isEmpty()) {
                    String[] emailParts = prefUserEmail.split("@");
                    if (emailParts.length > 0 && !emailParts[0].isEmpty()) {
                        userName = capitalizeFirstLetter(emailParts[0]);
                        Log.d(TAG, "✅ User name extracted from pref email: " + userName);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting user data", e);
        }

        String welcomeMessage = "Selamat datang, " + userName + "!";
        if (welcomeText != null) {
            welcomeText.setText(welcomeMessage);
        }
        Log.d(TAG, "Final welcome message: " + welcomeMessage);
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private void setupRecyclerViews() {
        // Categories RecyclerView (Horizontal)
        if (recyclerCategories != null) {
            recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext(),
                    LinearLayoutManager.HORIZONTAL, false));
            categoryAdapter = new CategoryAdapter(getContext(), categoriesList);
            categoryAdapter.setOnCategoryClickListener(category -> {
                Log.d(TAG, "Category clicked: " + category.getName());
                openCategoryMusic(category);
            });
            recyclerCategories.setAdapter(categoryAdapter);
        }

        // Recent Music RecyclerView (Horizontal)
        if (recyclerRecentMusic != null) {
            recyclerRecentMusic.setLayoutManager(new LinearLayoutManager(getContext(),
                    LinearLayoutManager.HORIZONTAL, false));
            recentMusicAdapter = new MusicTrackAdapter(getContext(), recentMusicList, true);
            recentMusicAdapter.setOnMusicClickListener(new MusicTrackAdapter.OnMusicClickListener() {
                @Override
                public void onMusicClick(MusicTrack music) {
                    Log.d(TAG, "Music clicked: " + music.getTitle());
                    openMusicDetail(music);
                }

                @Override
                public void onPlayClick(MusicTrack music) {
                    Log.d(TAG, "Play clicked: " + music.getTitle());
                    playMusic(music);
                }

                @Override
                public void onFavoriteClick(MusicTrack music, boolean isFavorite) {
                    Log.d(TAG, "Favorite clicked: " + music.getTitle() + ", isFavorite: " + isFavorite);
                    toggleFavorite(music, isFavorite);
                }
            });
            recyclerRecentMusic.setAdapter(recentMusicAdapter);
        }

        // Recommended Music RecyclerView (Vertical)
        if (recyclerRecommended != null) {
            recyclerRecommended.setLayoutManager(new LinearLayoutManager(getContext()));
            recommendedMusicAdapter = new MusicTrackAdapter(getContext(), recommendedMusicList, false);
            recommendedMusicAdapter.setOnMusicClickListener(new MusicTrackAdapter.OnMusicClickListener() {
                @Override
                public void onMusicClick(MusicTrack music) {
                    Log.d(TAG, "Recommended music clicked: " + music.getTitle());
                    openMusicDetail(music);
                }

                @Override
                public void onPlayClick(MusicTrack music) {
                    Log.d(TAG, "Recommended play clicked: " + music.getTitle());
                    playMusic(music);
                }

                @Override
                public void onFavoriteClick(MusicTrack music, boolean isFavorite) {
                    Log.d(TAG, "Recommended favorite clicked: " + music.getTitle() + ", isFavorite: " + isFavorite);
                    toggleFavorite(music, isFavorite);
                }
            });
            recyclerRecommended.setAdapter(recommendedMusicAdapter);
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::refreshData);
            swipeRefresh.setColorSchemeResources(
                    R.color.colorPrimary,
                    R.color.colorAccent,
                    R.color.colorPrimaryDark
            );
        }
    }

    private void loadData() {
        showLoading(true);

        // Load categories first
        loadCategories();

        // Load recent music
        loadRecentMusic();

        // Load recommended music
        loadRecommendedMusic();
    }

    private void refreshData() {
        Log.d(TAG, "Refreshing data...");
        loadData();
    }

    private void loadCategories() {
        // For now, use static categories until we implement category API
        categoriesList.clear();
        categoriesList.add(new MusicCategory("1", "Deep Sleep", R.drawable.ic_category_sleep));
        categoriesList.add(new MusicCategory("2", "Mindfulness", R.drawable.ic_category_focus));
        categoriesList.add(new MusicCategory("3", "Stress Relief", R.drawable.ic_category_sleep));
        categoriesList.add(new MusicCategory("4", "Therapeutic", R.drawable.ic_category_focus));

        if (categoryAdapter != null) {
            categoryAdapter.updateData(categoriesList);
        }
        Log.d(TAG, "Categories loaded: " + categoriesList.size());
    }

    private void loadRecentMusic() {
        musicApiClient.getAllMusic(new MusicApiClient.ApiCallback<List<MusicTrack>>() {
            @Override
            public void onSuccess(List<MusicTrack> musicTracks) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            recentMusicList.clear();
                            if (musicTracks != null) {
                                // Get only recent 10 tracks
                                int maxItems = Math.min(musicTracks.size(), 10);
                                for (int i = 0; i < maxItems; i++) {
                                    recentMusicList.add(musicTracks.get(i));
                                }
                            }
                            if (recentMusicAdapter != null) {
                                recentMusicAdapter.updateData(recentMusicList);
                            }
                            Log.d(TAG, "Recent music loaded: " + recentMusicList.size());

                            showLoading(false);
                            if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
                                swipeRefresh.setRefreshing(false);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating recent music UI", e);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Error loading recent music: " + error);
                        showLoading(false);
                        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
                            swipeRefresh.setRefreshing(false);
                        }
                        // Don't show toast on every error to avoid spam
                        if (!error.contains("Network")) {
                            Toast.makeText(getContext(), "Error loading music", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void loadRecommendedMusic() {
        // For now, just load all music as recommendations
        // In the future, this could be filtered based on user preferences
        musicApiClient.getAllMusic(new MusicApiClient.ApiCallback<List<MusicTrack>>() {
            @Override
            public void onSuccess(List<MusicTrack> musicTracks) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            recommendedMusicList.clear();
                            if (musicTracks != null) {
                                // Get different tracks than recent (skip first 10)
                                int startIndex = Math.min(10, musicTracks.size());
                                for (int i = startIndex; i < musicTracks.size() && recommendedMusicList.size() < 8; i++) {
                                    recommendedMusicList.add(musicTracks.get(i));
                                }

                                // If not enough different tracks, just show some from the beginning
                                if (recommendedMusicList.size() < 3 && musicTracks.size() > 0) {
                                    for (int i = 0; i < Math.min(3, musicTracks.size()); i++) {
                                        if (!recommendedMusicList.contains(musicTracks.get(i))) {
                                            recommendedMusicList.add(musicTracks.get(i));
                                        }
                                    }
                                }
                            }
                            if (recommendedMusicAdapter != null) {
                                recommendedMusicAdapter.updateData(recommendedMusicList);
                            }
                            Log.d(TAG, "Recommended music loaded: " + recommendedMusicList.size());
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating recommended music UI", e);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Error loading recommended music: " + error);
                    });
                }
            }
        });
    }

    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // Show/hide content based on data availability
        if (!show) {
            boolean hasData = !categoriesList.isEmpty() || !recentMusicList.isEmpty() || !recommendedMusicList.isEmpty();
            if (emptyView != null) {
                emptyView.setVisibility(hasData ? View.GONE : View.VISIBLE);
            }
        }
    }

    // =====================================
    // USER INTERACTION METHODS
    // =====================================

    private void playMusic(MusicTrack music) {
        // Validasi music track
        if (music == null) {
            Toast.makeText(getContext(), "Error: Invalid music data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cek apakah audio URL tersedia
        if (!music.hasAudio()) {
            Toast.makeText(getContext(), "Error: Audio tidak tersedia", Toast.LENGTH_SHORT).show();
            return;
        }

        // Launch MusicPlayerActivity
        Intent intent = new Intent(getContext(), MusicPlayerActivity.class);
        intent.putExtra("music_track", music);
        startActivity(intent);

        // Optional: Add transition animation
        if (getActivity() != null) {
            getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out);
        }
    }

    private void openCategoryMusic(MusicCategory category) {
        // Navigate to CategoryMusicFragment
        CategoryMusicFragment fragment = CategoryMusicFragment.newInstance(
                category.getId(),
                category.getName()
        );

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        Log.d(TAG, "Navigating to category: " + category.getName());
    }

    private void openMusicDetail(MusicTrack music) {
        // Navigate to MusicPlayerActivity with detail view
        Intent intent = new Intent(getContext(), MusicPlayerActivity.class);
        intent.putExtra("music_track", music);
        startActivity(intent);
    }

    private void toggleFavorite(MusicTrack music, boolean isFavorite) {
        // The favorite state has already been handled in the adapter
        // This is just for additional logic if needed

        if (isFavorite) {
            Log.d(TAG, "Music added to favorites: " + music.getTitle());
            // You can add additional logic here, like updating a favorites counter
            // or syncing with the server
        } else {
            Log.d(TAG, "Music removed from favorites: " + music.getTitle());
            // Additional logic for removing from favorites
        }

        // Optionally refresh the adapters to update favorite states
        if (recentMusicAdapter != null) {
            recentMusicAdapter.updateFavoriteState(music.getId());
        }
        if (recommendedMusicAdapter != null) {
            recommendedMusicAdapter.updateFavoriteState(music.getId());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh welcome message when fragment resumes
        if (welcomeText != null) {
            setWelcomeMessage();
        }

        // Refresh data if needed
        if ((recentMusicList == null || recentMusicList.isEmpty()) &&
                (categoriesList == null || categoriesList.isEmpty())) {
            loadData();
        }

        // Refresh favorite states
        if (recentMusicAdapter != null) {
            recentMusicAdapter.notifyDataSetChanged();
        }
        if (recommendedMusicAdapter != null) {
            recommendedMusicAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }
}