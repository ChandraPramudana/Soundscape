package com.chandra.soundscape;

import android.content.Context;
import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.EOFException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.ResponseBody;
import java.io.IOException;

public class SupabaseAuthManager {
    private static SupabaseAuthManager instance;
    private static final String SUPABASE_URL = "https://ibldlqyhcwdvgfkcotih.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlibGRscXloY3dkdmdma2NvdGloIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDkyMjA0MzIsImV4cCI6MjA2NDc5NjQzMn0.Ri5AoGKBJjpvQiSbRfI2on2dJ_Ff4RefX-XNYl7IJRk";
    private static final String TAG = "SupabaseAuthManager";

    private SupabaseAuthApi authApi;
    private SupabaseDbApi dbApi;
    private Context context;
    private String accessToken;
    private String refreshToken;
    private UserProfile currentUser;

    // Session managers integration
    private UserSessionManager sessionManager;
    private UserStatsManager statsManager;

    // API Interfaces dengan dynamic headers
    public interface SupabaseAuthApi {
        @POST("auth/v1/signup")
        Call<AuthResponse> signUp(@Body SignUpRequest request);

        @POST("auth/v1/token?grant_type=password")
        Call<AuthResponse> signIn(@Body SignInRequest request);

        @POST("auth/v1/logout")
        Call<JsonObject> signOut(@Header("Authorization") String token);
    }

    public interface SupabaseDbApi {
        @POST("rest/v1/users")
        Call<ResponseBody> createUserProfile(@Body UserProfile profile);

        @GET("rest/v1/users")
        Call<UserProfile[]> getUserByEmail(@Query("email") String email,
                                           @Query("select") String select);

        @PATCH("rest/v1/users")
        Call<JsonObject> updateUserProfile(@Query("email") String email,
                                           @Body UserProfileUpdate update);
    }

    // Data Models
    public static class SignUpRequest {
        public String email;
        public String password;

        public SignUpRequest(String email, String password) {
            this.email = email;
            this.password = password;
            Log.d(TAG, "SignUpRequest created: email=" + email + ", password length=" + password.length());
        }
    }

    public static class SignInRequest {
        public String email;
        public String password;

        public SignInRequest(String email, String password) {
            this.email = email;
            this.password = password;
            Log.d(TAG, "SignInRequest created: email=" + email + ", password length=" + password.length());
        }
    }

    public static class AuthResponse {
        @SerializedName("access_token")
        public String accessToken;

        @SerializedName("refresh_token")
        public String refreshToken;

        public User user;

        public static class User {
            public String id;
            public String email;
            @SerializedName("created_at")
            public String createdAt;
        }
    }

    public static class UserProfile {
        public String id;
        public String email;
        public String name;
        public String role;
        public String phone;
        @SerializedName("profile_photo")
        public String profilePhoto;
        @SerializedName("created_at")
        public String createdAt;

        public UserProfile() {}

        public UserProfile(String email, String name, String role) {
            this.email = email;
            this.name = name;
            this.role = role;
            Log.d(TAG, "UserProfile created: email=" + email + ", name=" + name + ", role=" + role);
        }

        public UserProfile(String email, String name, String role, String phone) {
            this.email = email;
            this.name = name;
            this.role = role;
            this.phone = phone;
            Log.d(TAG, "UserProfile created with phone: email=" + email + ", name=" + name + ", role=" + role + ", phone=" + phone);
        }

        // Convert to UserData
        public UserData toUserData() {
            UserData userData = new UserData();
            userData.setUserId(this.id);
            userData.setName(this.name);
            userData.setEmail(this.email);
            userData.setPhone(this.phone);
            userData.setProfilePhoto(this.profilePhoto);
            userData.setCreatedAt(this.createdAt);
            return userData;
        }

        // Convert to UserModel
        public UserModel toUserModel() {
            return new UserModel(this.name != null ? this.name : "",
                    this.email != null ? this.email : "",
                    this.profilePhoto != null ? this.profilePhoto : "");
        }
    }

    // For profile updates
    public static class UserProfileUpdate {
        public String name;
        public String phone;
        @SerializedName("profile_photo")
        public String profilePhoto;

        public UserProfileUpdate(String name, String phone, String profilePhoto) {
            this.name = name;
            this.phone = phone;
            this.profilePhoto = profilePhoto;
        }
    }

    private SupabaseAuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = new UserSessionManager(context);
        Log.d(TAG, "SupabaseAuthManager initialized");
        initializeSupabase();
    }

    public static SupabaseAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseAuthManager(context);
        }
        return instance;
    }

    private void initializeSupabase() {
        Log.d(TAG, "Initializing Supabase with URL: " + SUPABASE_URL);
        Log.d(TAG, "ANON_KEY (first 20 chars): " + SUPABASE_ANON_KEY.substring(0, 20) + "...");

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Custom interceptor untuk menambahkan headers secara dinamis
        Interceptor headerInterceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Log.d(TAG, "Intercepting request to: " + original.url());

                Request.Builder requestBuilder = original.newBuilder()
                        .header("apikey", SUPABASE_ANON_KEY)
                        .header("Content-Type", "application/json");

                // Add Authorization header jika ada access token dan bukan auth endpoint
                if (accessToken != null && !original.url().toString().contains("/auth/")) {
                    Log.d(TAG, "Adding Authorization header with access token");
                    requestBuilder.header("Authorization", "Bearer " + accessToken);
                }

                // Add Prefer header untuk database operations
                if (original.url().toString().contains("/rest/")) {
                    Log.d(TAG, "Adding Prefer header for database operation");
                    requestBuilder.header("Prefer", "return=minimal");
                }

                Request request = requestBuilder.build();

                // Log all headers
                Log.d(TAG, "Request headers:");
                for (String name : request.headers().names()) {
                    if (name.equals("apikey") || name.equals("Authorization")) {
                        Log.d(TAG, name + ": " + request.header(name).substring(0, 20) + "...");
                    } else {
                        Log.d(TAG, name + ": " + request.header(name));
                    }
                }

                return chain.proceed(request);
            }
        };

        // PERBAIKAN: Tambahkan timeout configuration
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(headerInterceptor)
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        authApi = retrofit.create(SupabaseAuthApi.class);
        dbApi = retrofit.create(SupabaseDbApi.class);

        Log.d(TAG, "Retrofit APIs created successfully");
    }

    // Test connection method
    public void testConnection(AuthCallback callback) {
        Log.d(TAG, "Testing connection to Supabase...");

        // Try to call auth endpoint with a test request
        SignUpRequest testRequest = new SignUpRequest("test@test.com", "123456");
        authApi.signUp(testRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                Log.d(TAG, "Test connection response code: " + response.code());
                if (response.code() == 422 || response.code() == 400) {
                    // Expected error for test email, connection is working
                    callback.onSuccess("Connection to Supabase is working");
                } else {
                    try {
                        String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        callback.onError("Connection test failed: " + error);
                    } catch (Exception e) {
                        callback.onError("Connection test failed: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "Test connection failed", t);
                callback.onError("Connection failed: " + t.getMessage());
            }
        });
    }

    // Register dengan Email, Password, Name, Role, dan Phone (optional)
    public void registerUser(String email, String password, String name, String role, String phone, AuthCallback callback) {
        Log.d(TAG, "=== STARTING REGISTRATION ===");
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "Name: " + name);
        Log.d(TAG, "Role: " + role);
        Log.d(TAG, "Phone: " + (phone != null ? phone : "null"));
        Log.d(TAG, "Password length: " + password.length());

        // PERBAIKAN: Check internet connectivity first
        if (!isNetworkAvailable()) {
            callback.onError("❌ Tidak ada koneksi internet. Periksa koneksi Anda.");
            return;
        }

        SignUpRequest request = new SignUpRequest(email, password);

        authApi.signUp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                Log.d(TAG, "=== REGISTRATION AUTH RESPONSE ===");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response message: " + response.message());

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    accessToken = authResponse.accessToken;
                    refreshToken = authResponse.refreshToken;

                    Log.d(TAG, "Auth successful!");
                    Log.d(TAG, "Access token (first 20 chars): " + (accessToken != null ? accessToken.substring(0, 20) + "..." : "null"));
                    Log.d(TAG, "User ID: " + (authResponse.user != null ? authResponse.user.id : "null"));

                    // Create user profile in database
                    createUserProfile(email, name, role, phone, callback);
                } else {
                    Log.e(TAG, "Registration auth failed");
                    handleRegistrationError(response, callback);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "=== REGISTRATION AUTH FAILURE ===", t);
                handleNetworkFailure(t, callback);
            }
        });
    }

    // PERBAIKAN: Better error handling
    private void handleRegistrationError(Response<AuthResponse> response, AuthCallback callback) {
        try {
            String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            Log.e(TAG, "Error body: " + error);

            // Parse Supabase error
            if (error.contains("already_registered") || error.contains("already exists") ||
                    error.contains("email_already_in_use") || error.contains("duplicate")) {
                callback.onError("❌ Email sudah terdaftar. Gunakan email lain atau login.");
            } else if (error.contains("email_provider_disabled")) {
                callback.onError("⚠️ REGISTRASI EMAIL TIDAK AKTIF!\n\n" +
                        "Email provider dimatikan di Supabase.\n" +
                        "Admin perlu mengaktifkan di:\n" +
                        "Authentication → Providers → Email");
            } else if (error.contains("invalid_credentials")) {
                callback.onError("❌ Email atau password tidak valid");
            } else if (error.contains("weak_password")) {
                callback.onError("❌ Password terlalu lemah (minimal 6 karakter)");
            } else if (error.contains("invalid_email")) {
                callback.onError("❌ Format email tidak valid");
            } else if (response.code() == 404) {
                callback.onError("❌ API endpoint tidak ditemukan. Periksa konfigurasi Supabase.");
            } else if (response.code() == 403) {
                callback.onError("❌ Akses ditolak. Periksa API key Supabase.");
            } else {
                callback.onError("❌ Registrasi gagal: " + error);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
            callback.onError("❌ Registrasi gagal: " + response.code());
        }
    }

    // PERBAIKAN: Better network failure handling
    private void handleNetworkFailure(Throwable t, AuthCallback callback) {
        String errorMessage = t.getMessage();
        if (errorMessage == null) errorMessage = "Unknown error";

        Log.e(TAG, "Network failure type: " + t.getClass().getSimpleName());
        Log.e(TAG, "Network failure message: " + errorMessage);

        if (errorMessage.contains("Unable to resolve host") ||
                errorMessage.contains("UnknownHostException")) {
            callback.onError("❌ Tidak bisa terhubung ke server.\n\n" +
                    "Kemungkinan masalah:\n" +
                    "1. Tidak ada koneksi internet\n" +
                    "2. Server Supabase sedang down\n" +
                    "3. URL Supabase salah\n\n" +
                    "Coba lagi nanti.");
        } else if (errorMessage.contains("timeout") ||
                errorMessage.contains("SocketTimeoutException")) {
            callback.onError("❌ Koneksi timeout. Periksa internet Anda.");
        } else if (errorMessage.contains("Network is unreachable")) {
            callback.onError("❌ Jaringan tidak tersedia. Aktifkan WiFi/Data.");
        } else {
            callback.onError("❌ Koneksi gagal: " + errorMessage);
        }
    }

    // PERBAIKAN: Check network availability
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    // Create user profile in users table - FIXED VERSION
    private void createUserProfile(String email, String name, String role, String phone, AuthCallback callback) {
        Log.d(TAG, "=== CREATING USER PROFILE ===");

        UserProfile profile = new UserProfile(email, name, role, phone);
        String profileJson = new Gson().toJson(profile);
        Log.d(TAG, "Profile JSON: " + profileJson);

        dbApi.createUserProfile(profile).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "=== PROFILE CREATION RESPONSE ===");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response message: " + response.message());

                // Success codes: 200, 201, 204 (No Content)
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Profile created successfully! HTTP " + response.code());
                    callback.onSuccess("✅ Registrasi berhasil! Silakan login.");
                    return;
                }

                // Handle error responses
                handleProfileCreationError(response, callback);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "=== PROFILE CREATION FAILURE ===", t);
                handleProfileCreationFailure(t, email, callback);
            }
        });
    }

    // PERBAIKAN: Handle profile creation errors
    private void handleProfileCreationError(Response<ResponseBody> response, AuthCallback callback) {
        Log.e(TAG, "❌ Profile creation failed with HTTP " + response.code());
        try {
            String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            Log.e(TAG, "Error body: " + error);

            if (response.code() == 404) {
                callback.onError("❌ Table 'users' tidak ada!\n\n" +
                        "Buat table di Supabase:\n" +
                        "1. Buka Supabase Dashboard\n" +
                        "2. Table Editor → New Table\n" +
                        "3. Nama: users\n" +
                        "4. Columns: id, email, name, role, phone, profile_photo, created_at");
            } else if (response.code() == 403 || error.contains("permission denied") ||
                    error.contains("policy")) {
                callback.onError("❌ Permission denied!\n\n" +
                        "Setup RLS policy:\n" +
                        "1. Buka Table Editor → users\n" +
                        "2. Policies → New Policy\n" +
                        "3. Enable insert for authenticated users");
            } else if (error.contains("relation \"users\" does not exist")) {
                callback.onError("❌ Table 'users' belum dibuat!");
            } else if (error.contains("duplicate key") || error.contains("already exists")) {
                callback.onError("❌ Email sudah terdaftar");
            } else {
                callback.onError("❌ Gagal membuat profil: " + error);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
            callback.onError("❌ Gagal membuat profil: " + response.code());
        }
    }

    // PERBAIKAN: Handle profile creation failure
    private void handleProfileCreationFailure(Throwable t, String email, AuthCallback callback) {
        Log.e(TAG, "Failure type: " + t.getClass().getSimpleName());
        Log.e(TAG, "Failure message: " + t.getMessage());

        if (t instanceof EOFException) {
            Log.w(TAG, "⚠️ EOFException - Checking if user was created...");

            // Wait and verify
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                verifyUserCreation(email, new ProfileCallback() {
                    @Override
                    public void onProfileReceived(UserProfile profile) {
                        Log.d(TAG, "✅ User was created successfully!");
                        callback.onSuccess("✅ Registrasi berhasil! Silakan login.");
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ User was not created");
                        callback.onError("❌ Database error. Coba lagi.");
                    }
                });
            }, 1000);
        } else if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
            callback.onError("❌ Tidak bisa connect ke database");
        } else {
            callback.onError("❌ Gagal membuat profil: " + t.getMessage());
        }
    }

    // Helper method to verify if user was actually created
    private void verifyUserCreation(String email, ProfileCallback callback) {
        Log.d(TAG, "🔍 Verifying user creation for: " + email);

        dbApi.getUserByEmail("eq." + email, "*").enqueue(new Callback<UserProfile[]>() {
            @Override
            public void onResponse(Call<UserProfile[]> call, Response<UserProfile[]> response) {
                if (response.isSuccessful() && response.body() != null && response.body().length > 0) {
                    UserProfile profile = response.body()[0];
                    Log.d(TAG, "✅ User verification successful: " + profile.email);
                    callback.onProfileReceived(profile);
                } else {
                    Log.d(TAG, "❌ User verification failed: user not found");
                    callback.onError("User not found");
                }
            }

            @Override
            public void onFailure(Call<UserProfile[]> call, Throwable t) {
                Log.d(TAG, "❌ User verification failed: " + t.getMessage());
                callback.onError("Verification failed: " + t.getMessage());
            }
        });
    }

    // Overload method for backward compatibility
    public void registerUser(String email, String password, String name, String role, AuthCallback callback) {
        registerUser(email, password, name, role, null, callback);
    }

    // Login dengan Email & Password
    public void loginUser(String email, String password, AuthCallback callback) {
        Log.d(TAG, "=== STARTING LOGIN ===");
        Log.d(TAG, "Email: " + email);

        // Check internet connectivity first
        if (!isNetworkAvailable()) {
            callback.onError("❌ Tidak ada koneksi internet. Periksa koneksi Anda.");
            return;
        }

        SignInRequest request = new SignInRequest(email, password);

        authApi.signIn(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                Log.d(TAG, "=== LOGIN AUTH RESPONSE ===");
                Log.d(TAG, "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    accessToken = authResponse.accessToken;
                    refreshToken = authResponse.refreshToken;

                    Log.d(TAG, "✅ LOGIN AUTH SUCCESSFUL!");

                    // Get full user profile from database
                    getUserProfile(email, new ProfileCallback() {
                        @Override
                        public void onProfileReceived(UserProfile profile) {
                            Log.d(TAG, "✅ LOGIN: User profile retrieved successfully");
                            currentUser = profile;

                            // Create session with SessionManager
                            sessionManager.createLoginSession(
                                    profile.id != null ? profile.id : email,
                                    profile.name != null ? profile.name : "",
                                    profile.email,
                                    profile.phone != null ? profile.phone : ""
                            );

                            // Initialize stats manager
                            String userId = profile.id != null ? profile.id : email;
                            statsManager = UserStatsManager.getInstance(context, userId);
                            statsManager.incrementSessionCount();

                            // Save to UserPrefs for backward compatibility
                            saveToUserPrefs(profile);

                            Log.d(TAG, "✅ LOGIN COMPLETELY SUCCESSFUL");
                            callback.onSuccess("✅ LOGIN berhasil!");
                            callback.onRoleReceived(profile.role);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ LOGIN: Failed to get user profile: " + error);
                            callback.onError("❌ LOGIN: Gagal mendapatkan profil user");
                        }
                    });
                } else {
                    handleLoginError(response, callback);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "=== LOGIN AUTH FAILURE ===", t);
                handleNetworkFailure(t, callback);
            }
        });
    }

    // PERBAIKAN: Handle login errors
    private void handleLoginError(Response<AuthResponse> response, AuthCallback callback) {
        Log.e(TAG, "❌ LOGIN auth failed with code: " + response.code());
        try {
            String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            Log.e(TAG, "❌ LOGIN error body: " + error);

            if (error.contains("invalid_credentials") || error.contains("Invalid login credentials")) {
                callback.onError("❌ Email atau password salah");
            } else if (error.contains("email_not_confirmed")) {
                callback.onError("📧 Email belum dikonfirmasi!\n\n" +
                        "Periksa inbox email Anda.");
            } else if (error.contains("email_provider_disabled")) {
                callback.onError("⚠️ EMAIL LOGIN DIMATIKAN!\n\n" +
                        "Admin perlu mengaktifkan di Supabase.");
            } else {
                callback.onError("❌ LOGIN gagal: " + error);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing login response", e);
            callback.onError("❌ Email atau password salah");
        }
    }

    // Get full user profile from database
    private void getUserProfile(String email, ProfileCallback callback) {
        Log.d(TAG, "=== GETTING USER PROFILE ===");
        Log.d(TAG, "Email: " + email);

        dbApi.getUserByEmail("eq." + email, "*").enqueue(new Callback<UserProfile[]>() {
            @Override
            public void onResponse(Call<UserProfile[]> call, Response<UserProfile[]> response) {
                Log.d(TAG, "=== GET PROFILE RESPONSE ===");
                Log.d(TAG, "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null && response.body().length > 0) {
                    UserProfile profile = response.body()[0];
                    Log.d(TAG, "✅ Profile received successfully");
                    callback.onProfileReceived(profile);
                } else {
                    Log.e(TAG, "❌ Profile not found");
                    callback.onError("User profile tidak ditemukan");
                }
            }

            @Override
            public void onFailure(Call<UserProfile[]> call, Throwable t) {
                Log.e(TAG, "=== GET PROFILE FAILURE ===", t);
                callback.onError("Gagal mendapatkan profil: " + t.getMessage());
            }
        });
    }

    // Save to UserPrefs for backward compatibility
    private void saveToUserPrefs(UserProfile profile) {
        android.content.SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = userPrefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userEmail", profile.email);
        editor.putString("userRole", profile.role != null ? profile.role : "Listener");
        editor.apply();
        Log.d(TAG, "User preferences saved");
    }

    // Update user profile
    public void updateUserProfile(String name, String phone, String profilePhoto, AuthCallback callback) {
        if (currentUser == null) {
            callback.onError("User tidak ditemukan");
            return;
        }

        UserProfileUpdate update = new UserProfileUpdate(name, phone, profilePhoto);

        dbApi.updateUserProfile("eq." + currentUser.email, update).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Update local user data
                    currentUser.name = name;
                    currentUser.phone = phone;
                    if (profilePhoto != null && !profilePhoto.isEmpty()) {
                        currentUser.profilePhoto = profilePhoto;
                    }

                    // Update session manager
                    sessionManager.updateUserProfile(name, currentUser.email, phone, profilePhoto);

                    callback.onSuccess("Profil berhasil diperbarui");
                } else {
                    callback.onError("Gagal memperbarui profil");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Gagal memperbarui profil: " + t.getMessage());
            }
        });
    }

    // Logout
    public void logoutUser(AuthCallback callback) {
        if (accessToken != null) {
            authApi.signOut("Bearer " + accessToken).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    clearUserData();
                    callback.onSuccess("Logout berhasil");
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    // Even if logout fails on server, clear local data
                    clearUserData();
                    callback.onSuccess("Logout berhasil");
                }
            });
        } else {
            clearUserData();
            callback.onSuccess("Logout berhasil");
        }
    }

    // Clear all user data
    private void clearUserData() {
        accessToken = null;
        refreshToken = null;
        currentUser = null;
        statsManager = null;

        // Clear session manager
        sessionManager.logout();
    }

    // Check login status
    public boolean isUserLoggedIn() {
        return sessionManager.isLoggedIn() && accessToken != null;
    }

    // Get current user as UserData
    public UserData getCurrentUserData() {
        if (currentUser != null) {
            return currentUser.toUserData();
        }
        return sessionManager.getCurrentUser();
    }

    // Get current user as UserModel
    public UserModel getCurrentUserModel() {
        if (currentUser != null) {
            return currentUser.toUserModel();
        }
        UserData userData = sessionManager.getCurrentUser();
        if (userData != null) {
            return new UserModel(userData.getName(), userData.getEmail(), userData.getProfilePhoto());
        }
        return null;
    }

    // Get current user email
    public String getCurrentUserEmail() {
        if (currentUser != null) {
            return currentUser.email;
        }
        return sessionManager.getUserEmail();
    }

    // Get current user name
    public String getCurrentUserName() {
        if (currentUser != null) {
            return currentUser.name;
        }
        return sessionManager.getUserName();
    }

    // Get current user role
    public String getCurrentUserRole() {
        if (currentUser != null) {
            return currentUser.role;
        }
        return sessionManager.getUserRole();
    }

    // Get current user ID
    public String getCurrentUserId() {
        if (currentUser != null) {
            return currentUser.id;
        }
        return sessionManager.getUserId();
    }

    // Get stats manager
    public UserStatsManager getStatsManager() {
        if (statsManager == null && isUserLoggedIn()) {
            String userId = getCurrentUserId();
            if (userId != null && !userId.isEmpty()) {
                statsManager = UserStatsManager.getInstance(context, userId);
            }
        }
        return statsManager;
    }

    // Get access token
    public String getAccessToken() {
        return accessToken;
    }

    // Clear session method for debugging
    public void clearSession() {
        Log.d(TAG, "=== CLEARING SESSION ===");
        clearUserData();

        // Clear SharedPreferences
        android.content.SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userPrefs.edit().clear().apply();

        Log.d(TAG, "All user session data cleared");
    }

    // Force refresh - clear everything and restart
    public void forceRefresh() {
        Log.d(TAG, "=== FORCE REFRESH ===");

        // Clear all tokens and user data
        accessToken = null;
        refreshToken = null;
        currentUser = null;
        statsManager = null;

        // Clear session manager
        if (sessionManager != null) {
            sessionManager.logout();
        }

        // Clear all SharedPreferences
        android.content.SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userPrefs.edit().clear().apply();

        // Re-initialize session manager
        sessionManager = new UserSessionManager(context);

        Log.d(TAG, "Force refresh completed - all data cleared");
    }

    // Interface untuk callback
    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String error);
        default void onRoleReceived(String role) {}
    }

    // Interface untuk profile callback
    public interface ProfileCallback {
        void onProfileReceived(UserProfile profile);
        void onError(String error);
    }



    public void debugSupabaseConnection() {
        Log.d(TAG, "=== DEBUGGING SUPABASE ===");
        Log.d(TAG, "URL: " + SUPABASE_URL);
        Log.d(TAG, "ANON_KEY valid: " + (SUPABASE_ANON_KEY != null && !SUPABASE_ANON_KEY.isEmpty()));
        Log.d(TAG, "Network available: " + isNetworkAvailable());

        // Test simple request
        testConnection(new AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "✅ Connection OK: " + message);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Connection Failed: " + error);
            }
        });
    }
}