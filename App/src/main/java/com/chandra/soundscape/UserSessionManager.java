package com.chandra.soundscape;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserSessionManager {

    private static final String TAG = "UserSessionManager";
    private static final String PREF_NAME = "user_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_MEMBER_SINCE = "member_since";
    private static final String KEY_PROFILE_PHOTO = "profile_photo";
    private static final String KEY_LAST_LOGIN = "last_login";

    // Integration with existing UserPrefs
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_PREFS_ROLE = "userRole";
    private static final String KEY_USER_PREFS_EMAIL = "userEmail";
    private static final String KEY_USER_PREFS_LOGGED_IN = "isLoggedIn";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private SharedPreferences userPrefs;
    private SharedPreferences.Editor userPrefsEditor;
    private Context context;

    public UserSessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        userPrefs = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        userPrefsEditor = userPrefs.edit();
    }

    /**
     * Create login session with role
     */
    public void createLoginSession(String userId, String name, String email, String phone, String role) {
        Log.d(TAG, "Creating login session for user: " + email + " with role: " + role);

        // Validate and normalize role
        String normalizedRole = normalizeRole(role);

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone != null ? phone : "");
        editor.putString(KEY_USER_ROLE, normalizedRole);
        editor.putLong(KEY_LAST_LOGIN, System.currentTimeMillis());

        // Set member since date if not already set
        if (pref.getString(KEY_MEMBER_SINCE, null) == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID"));
            String memberSince = sdf.format(new Date());
            editor.putString(KEY_MEMBER_SINCE, memberSince);
        }

        editor.commit();

        // Also update UserPrefs for backward compatibility
        userPrefsEditor.putBoolean(KEY_USER_PREFS_LOGGED_IN, true);
        userPrefsEditor.putString(KEY_USER_PREFS_EMAIL, email);
        userPrefsEditor.putString(KEY_USER_PREFS_ROLE, normalizedRole);
        userPrefsEditor.commit(); // Use commit instead of apply for immediate write

        Log.d(TAG, "Session created successfully. Role stored: " + normalizedRole);
        logSessionInfo();
    }

    /**
     * Normalize role to ensure consistency
     */
    private String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "Listener";
        }

        String normalized = role.trim();

        // Handle common variations
        if (normalized.equalsIgnoreCase("admin") ||
                normalized.equalsIgnoreCase("administrator")) {
            return "Admin";
        } else if (normalized.equalsIgnoreCase("listener") ||
                normalized.equalsIgnoreCase("user")) {
            return "Listener";
        }

        // Capitalize first letter
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1).toLowerCase();
    }

    /**
     * Create login session (overload for backward compatibility)
     */
    public void createLoginSession(String userId, String name, String email, String phone) {
        createLoginSession(userId, name, email, phone, "Listener");
    }

    /**
     * Check login method - check both session manager and UserPrefs
     */
    public boolean isLoggedIn() {
        boolean sessionLoggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false);
        boolean prefsLoggedIn = userPrefs.getBoolean(KEY_USER_PREFS_LOGGED_IN, false);

        Log.d(TAG, "Login check - Session: " + sessionLoggedIn + ", UserPrefs: " + prefsLoggedIn);

        return sessionLoggedIn || prefsLoggedIn;
    }

    /**
     * Get stored session data
     */
    public UserData getCurrentUser() {
        if (isLoggedIn()) {
            UserData user = new UserData();
            user.setUserId(pref.getString(KEY_USER_ID, ""));
            user.setName(pref.getString(KEY_USER_NAME, ""));
            user.setEmail(pref.getString(KEY_USER_EMAIL, ""));
            user.setPhone(pref.getString(KEY_USER_PHONE, ""));
            user.setMemberSince(pref.getString(KEY_MEMBER_SINCE, ""));
            user.setProfilePhoto(pref.getString(KEY_PROFILE_PHOTO, ""));
            return user;
        }
        return null;
    }

    /**
     * Get user role from session or UserPrefs with improved fallback logic
     */
    public String getUserRole() {
        // First try to get from session manager
        String role = pref.getString(KEY_USER_ROLE, null);
        Log.d(TAG, "Role from session: " + role);

        // If not found or empty, try UserPrefs
        if (role == null || role.trim().isEmpty()) {
            role = userPrefs.getString(KEY_USER_PREFS_ROLE, null);
            Log.d(TAG, "Role from UserPrefs: " + role);

            // If found in UserPrefs, update session for consistency
            if (role != null && !role.trim().isEmpty()) {
                updateUserRole(role);
            }
        }

        // If still null or empty, default to Listener
        if (role == null || role.trim().isEmpty()) {
            role = "Listener";
            Log.d(TAG, "Using default role: " + role);
        }

        return normalizeRole(role);
    }

    /**
     * Update user profile
     */
    public void updateUserProfile(String name, String email, String phone, String profilePhoto) {
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone != null ? phone : "");
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            editor.putString(KEY_PROFILE_PHOTO, profilePhoto);
        }
        editor.commit();

        // Also update UserPrefs email
        userPrefsEditor.putString(KEY_USER_PREFS_EMAIL, email);
        userPrefsEditor.commit();
    }

    /**
     * Update user role with validation
     */
    public void updateUserRole(String role) {
        String normalizedRole = normalizeRole(role);
        Log.d(TAG, "Updating user role to: " + normalizedRole);

        editor.putString(KEY_USER_ROLE, normalizedRole);
        editor.commit();

        // Also update UserPrefs
        userPrefsEditor.putString(KEY_USER_PREFS_ROLE, normalizedRole);
        userPrefsEditor.commit();

        Log.d(TAG, "Role updated successfully");
    }

    /**
     * Clear session details
     */
    public void logout() {
        Log.d(TAG, "Logging out user");

        // Clear user session data
        editor.clear();
        editor.commit();

        // Clear UserPrefs data
        userPrefsEditor.clear();
        userPrefsEditor.commit();

        Log.d(TAG, "User logged out successfully");
    }

    /**
     * Get user ID
     */
    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    /**
     * Get user name
     */
    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "");
    }

    /**
     * Get user email - check both sources
     */
    public String getUserEmail() {
        String email = pref.getString(KEY_USER_EMAIL, "");
        if (email.isEmpty()) {
            email = userPrefs.getString(KEY_USER_PREFS_EMAIL, "");
        }
        return email;
    }

    /**
     * Get user phone
     */
    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, "");
    }

    /**
     * Get profile photo
     */
    public String getProfilePhoto() {
        return pref.getString(KEY_PROFILE_PHOTO, "");
    }

    /**
     * Get member since
     */
    public String getMemberSince() {
        return pref.getString(KEY_MEMBER_SINCE, "");
    }

    /**
     * Get last login timestamp
     */
    public long getLastLogin() {
        return pref.getLong(KEY_LAST_LOGIN, 0);
    }

    /**
     * Check if user has specific role (case-insensitive)
     */
    public boolean hasRole(String role) {
        if (role == null) return false;
        String currentRole = getUserRole();
        return role.equalsIgnoreCase(currentRole);
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("Admin");
    }

    /**
     * Check if user is listener
     */
    public boolean isListener() {
        return hasRole("Listener");
    }

    /**
     * Convert to UserModel
     */
    public UserModel getUserModel() {
        if (isLoggedIn()) {
            return new UserModel(
                    getUserName(),
                    getUserEmail(),
                    getProfilePhoto()
            );
        }
        return null;
    }

    /**
     * Debug method to log current session info
     */
    public void logSessionInfo() {
        Log.d(TAG, "=== CURRENT SESSION INFO ===");
        Log.d(TAG, "Is Logged In: " + isLoggedIn());
        Log.d(TAG, "User ID: " + getUserId());
        Log.d(TAG, "User Name: " + getUserName());
        Log.d(TAG, "User Email: " + getUserEmail());
        Log.d(TAG, "User Role: " + getUserRole());
        Log.d(TAG, "Is Admin: " + isAdmin());
        Log.d(TAG, "===========================");
    }

    /**
     * Static method to get instance
     */
    public static UserSessionManager getInstance(Context context) {
        return new UserSessionManager(context);
    }
}