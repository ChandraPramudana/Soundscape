package com.chandra.soundscape;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.chandra.soundscape.models.MusicTrack;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment implements FavoritesAdapter.OnFavoriteClickListener {

    private static final String PREFS_NAME = "soundscape_prefs";
    private static final String KEY_FAVORITES = "favorite_music_tracks";

    private RecyclerView recyclerViewFavorites;
    private FavoritesAdapter favoritesAdapter;
    private List<MusicTrack> favoritesList;
    private TextView tvTotalFavorites;
    private View emptyStateView;
    private MaterialButton btnExplore;

    private SharedPreferences sharedPreferences;
    private Gson gson;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        initViews(view);
        setupRecyclerView();
        loadFavorites();

        return view;
    }

    private void initViews(View view) {
        recyclerViewFavorites = view.findViewById(R.id.recycler_favorites);
        tvTotalFavorites = view.findViewById(R.id.tv_total_favorites);
        emptyStateView = view.findViewById(R.id.empty_state_view);
        btnExplore = view.findViewById(R.id.btn_explore);

        // Initialize SharedPreferences and Gson
        sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();

        // Set up explore button
        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> {
                // Navigate back to home/dashboard
                if (getActivity() != null) {
                    // If using Navigation Component
                    // Navigation.findNavController(v).navigate(R.id.homeFragment);

                    // Or if using Fragment transactions
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new ListenerHomeFragment())
                            .commit();
                }
            });
        }
    }

    private void setupRecyclerView() {
        favoritesList = new ArrayList<>();
        favoritesAdapter = new FavoritesAdapter(getContext(), favoritesList);
        favoritesAdapter.setOnFavoriteClickListener(this);

        // Use GridLayoutManager for a nice grid view
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerViewFavorites.setLayoutManager(layoutManager);
        recyclerViewFavorites.setAdapter(favoritesAdapter);

        // Add item decoration for spacing
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        recyclerViewFavorites.addItemDecoration(new GridSpacingItemDecoration(2, spacing, true));
    }

    private void loadFavorites() {
        // Load favorites from SharedPreferences
        String favoritesJson = sharedPreferences.getString(KEY_FAVORITES, "");

        if (!favoritesJson.isEmpty()) {
            Type type = new TypeToken<List<MusicTrack>>(){}.getType();
            favoritesList = gson.fromJson(favoritesJson, type);
            if (favoritesList == null) {
                favoritesList = new ArrayList<>();
            }
        } else {
            favoritesList = new ArrayList<>();
        }

        updateUI();
    }

    private void saveFavorites() {
        // Save favorites to SharedPreferences
        String favoritesJson = gson.toJson(favoritesList);
        sharedPreferences.edit().putString(KEY_FAVORITES, favoritesJson).apply();
    }

    private void updateUI() {
        if (favoritesList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            recyclerViewFavorites.setVisibility(View.GONE);
            tvTotalFavorites.setText("0 favorit");
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyclerViewFavorites.setVisibility(View.VISIBLE);
            tvTotalFavorites.setText(favoritesList.size() + " favorit");
            favoritesAdapter.updateData(favoritesList);
        }
    }

    @Override
    public void onPlayClick(MusicTrack musicTrack) {
        // Navigate to MusicPlayerActivity
        Intent intent = new Intent(getContext(), MusicPlayerActivity.class);
        intent.putExtra("music_track", musicTrack);
        startActivity(intent);
    }

    @Override
    public void onFavoriteToggle(MusicTrack musicTrack, int position) {
        // Remove from favorites
        favoritesList.remove(position);
        favoritesAdapter.notifyItemRemoved(position);
        favoritesAdapter.notifyItemRangeChanged(position, favoritesList.size());

        // Save updated favorites
        saveFavorites();

        // Update UI
        updateUI();

        // Show toast
        Toast.makeText(getContext(), musicTrack.getTitle() + " dihapus dari favorit",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(MusicTrack musicTrack) {
        // Navigate to MusicPlayerActivity with details
        Intent intent = new Intent(getContext(), MusicPlayerActivity.class);
        intent.putExtra("music_track", musicTrack);
        startActivity(intent);
    }

    // Method to add music to favorites (called from other fragments/activities)
    public static void addToFavorites(Context context, MusicTrack musicTrack) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();

        // Get current favorites
        String favoritesJson = prefs.getString(KEY_FAVORITES, "");
        List<MusicTrack> favorites;

        if (!favoritesJson.isEmpty()) {
            Type type = new TypeToken<List<MusicTrack>>(){}.getType();
            favorites = gson.fromJson(favoritesJson, type);
            if (favorites == null) {
                favorites = new ArrayList<>();
            }
        } else {
            favorites = new ArrayList<>();
        }

        // Check if already in favorites
        boolean alreadyExists = false;
        for (MusicTrack track : favorites) {
            if (track.getId() != null && track.getId().equals(musicTrack.getId())) {
                alreadyExists = true;
                break;
            }
        }

        if (!alreadyExists) {
            favorites.add(musicTrack);
            String updatedJson = gson.toJson(favorites);
            prefs.edit().putString(KEY_FAVORITES, updatedJson).apply();
            Toast.makeText(context, musicTrack.getTitle() + " ditambahkan ke favorit",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, musicTrack.getTitle() + " sudah ada di favorit",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Method to remove music from favorites
    public static void removeFromFavorites(Context context, String musicId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String favoritesJson = prefs.getString(KEY_FAVORITES, "");
        if (!favoritesJson.isEmpty()) {
            Type type = new TypeToken<List<MusicTrack>>(){}.getType();
            List<MusicTrack> favorites = gson.fromJson(favoritesJson, type);

            if (favorites != null) {
                favorites.removeIf(track -> track.getId() != null && track.getId().equals(musicId));
                String updatedJson = gson.toJson(favorites);
                prefs.edit().putString(KEY_FAVORITES, updatedJson).apply();
            }
        }
    }

    // Method to check if music is in favorites
    public static boolean isFavorite(Context context, String musicId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String favoritesJson = prefs.getString(KEY_FAVORITES, "");
        if (!favoritesJson.isEmpty()) {
            Type type = new TypeToken<List<MusicTrack>>(){}.getType();
            List<MusicTrack> favorites = gson.fromJson(favoritesJson, type);

            if (favorites != null) {
                for (MusicTrack track : favorites) {
                    if (track.getId() != null && track.getId().equals(musicId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload favorites when fragment resumes
        loadFavorites();
    }

    // GridSpacingItemDecoration class for grid spacing
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