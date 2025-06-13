package com.chandra.soundscape;

import android.content.Context;
import android.content.SharedPreferences;

public class UserStatsManager {

    private static final String STATS_PREF_PREFIX = "user_stats_";
    private static final String KEY_TOTAL_LISTENING_MINUTES = "total_listening_minutes";
    private static final String KEY_FAVORITES_COUNT = "favorites_count";
    private static final String KEY_PLAYLISTS_COUNT = "playlists_count";
    private static final String KEY_SESSIONS_COUNT = "sessions_count";
    private static final String KEY_LAST_ACTIVITY = "last_activity";

    private SharedPreferences statsPrefs;
    private SharedPreferences.Editor editor;
    private Context context;
    private String userId;

    public UserStatsManager(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.statsPrefs = context.getSharedPreferences(STATS_PREF_PREFIX + userId, Context.MODE_PRIVATE);
        this.editor = statsPrefs.edit();
    }

    /**
     * Add listening time in minutes
     */
    public void addListeningTime(int minutes) {
        int currentTotal = statsPrefs.getInt(KEY_TOTAL_LISTENING_MINUTES, 0);
        editor.putInt(KEY_TOTAL_LISTENING_MINUTES, currentTotal + minutes);
        updateLastActivity();
        editor.apply();
    }

    /**
     * Get total listening time in minutes
     */
    public int getTotalListeningMinutes() {
        return statsPrefs.getInt(KEY_TOTAL_LISTENING_MINUTES, 0);
    }

    /**
     * Add to favorites count
     */
    public void addFavorite() {
        int currentCount = statsPrefs.getInt(KEY_FAVORITES_COUNT, 0);
        editor.putInt(KEY_FAVORITES_COUNT, currentCount + 1);
        updateLastActivity();
        editor.apply();
    }

    /**
     * Remove from favorites count
     */
    public void removeFavorite() {
        int currentCount = statsPrefs.getInt(KEY_FAVORITES_COUNT, 0);
        if (currentCount > 0) {
            editor.putInt(KEY_FAVORITES_COUNT, currentCount - 1);
            updateLastActivity();
            editor.apply();
        }
    }

    /**
     * Set favorites count
     */
    public void setFavoritesCount(int count) {
        editor.putInt(KEY_FAVORITES_COUNT, count);
        updateLastActivity();
        editor.apply();
    }

    /**
     * Get favorites count
     */
    public int getFavoritesCount() {
        return statsPrefs.getInt(KEY_FAVORITES_COUNT, 0);
    }

    /**
     * Add to playlists count
     */
    public void addPlaylist() {
        int currentCount = statsPrefs.getInt(KEY_PLAYLISTS_COUNT, 0);
        editor.putInt(KEY_PLAYLISTS_COUNT, currentCount + 1);
        updateLastActivity();
        editor.apply();
    }

    /**
     * Remove from playlists count
     */
    public void removePlaylist() {
        int currentCount = statsPrefs.getInt(KEY_PLAYLISTS_COUNT, 0);
        if (currentCount > 0) {
            editor.putInt(KEY_PLAYLISTS_COUNT, currentCount - 1);
            updateLastActivity();
            editor.apply();
        }
    }

    /**
     * Set playlists count
     */
    public void setPlaylistsCount(int count) {
        editor.putInt(KEY_PLAYLISTS_COUNT, count);
        updateLastActivity();
        editor.apply();
    }

    /**
     * Get playlists count
     */
    public int getPlaylistsCount() {
        return statsPrefs.getInt(KEY_PLAYLISTS_COUNT, 0);
    }

    /**
     * Increment session count
     */
    public void incrementSessionCount() {
        int currentCount = statsPrefs.getInt(KEY_SESSIONS_COUNT, 0);
        editor.putInt(KEY_SESSIONS_COUNT, currentCount + 1);
        updateLastActivity();
        editor.apply();
    }

    /**
     * Get sessions count
     */
    public int getSessionsCount() {
        return statsPrefs.getInt(KEY_SESSIONS_COUNT, 0);
    }

    /**
     * Update last activity timestamp
     */
    private void updateLastActivity() {
        editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
    }

    /**
     * Get last activity timestamp
     */
    public long getLastActivity() {
        return statsPrefs.getLong(KEY_LAST_ACTIVITY, 0);
    }

    /**
     * Reset all stats
     */
    public void resetStats() {
        editor.clear();
        editor.apply();
    }

    /**
     * Get formatted listening time string
     */
    public String getFormattedListeningTime() {
        int totalMinutes = getTotalListeningMinutes();
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (hours > 0) {
            return hours + " jam " + minutes + " menit";
        } else {
            return minutes + " menit";
        }
    }

    /**
     * Static method to get UserStatsManager instance
     */
    public static UserStatsManager getInstance(Context context, String userId) {
        return new UserStatsManager(context, userId);
    }
}