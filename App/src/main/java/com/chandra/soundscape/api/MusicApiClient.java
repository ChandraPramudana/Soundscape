package com.chandra.soundscape.api;

import android.util.Log;

import com.chandra.soundscape.admin.DoctorListFragment;
import com.chandra.soundscape.admin.ListenerListFragment;
import com.chandra.soundscape.models.MusicTrack;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.Interceptor;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

public class MusicApiClient {
    private static final String TAG = "MusicApiClient";
    private static final String SUPABASE_URL = "https://ibldlqyhcwdvgfkcotih.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlibGRscXloY3dkdmdma2NvdGloIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDkyMjA0MzIsImV4cCI6MjA2NDc5NjQzMn0.Ri5AoGKBJjpvQiSbRfI2on2dJ_Ff4RefX-XNYl7IJRk";

    private static MusicApiClient instance;
    private MusicApiService apiService;
    private Gson gson;
    private String currentAccessToken; // TAMBAHAN: Store current access token

    // API Interface
    public interface MusicApiService {
        // Get all music tracks
        @GET("rest/v1/music_tracks")
        Call<List<MusicTrack>> getAllMusic(@Query("select") String select,
                                           @Query("is_active") String isActive,
                                           @Query("order") String order);

        // Get music by ID
        @GET("rest/v1/music_tracks")
        Call<List<MusicTrack>> getMusicById(@Query("id") String id,
                                            @Query("select") String select);

        // Create new music track - FIXED: Remove accessToken parameter
        @POST("rest/v1/music_tracks")
        Call<ResponseBody> createMusic(@Body MusicTrack music);

        // Update music track
        @PATCH("rest/v1/music_tracks")
        Call<ResponseBody> updateMusic(@Query("id") String id,
                                       @Body MusicTrack music);

        // Get total users count
        @GET("rest/v1/users")
        @Headers("Prefer: count=exact")
        Call<List<JsonObject>> getTotalUsers(@Query("select") String select);

        @GET("rest/v1/users")
        Call<List<JsonObject>> getListeners(@Query("select") String select,
                                            @Query("order") String order);

        // Get total music count
        @GET("rest/v1/music_tracks")
        @Headers("Prefer: count=exact")
        Call<List<JsonObject>> getTotalMusic(@Query("select") String select,
                                             @Query("is_active") String isActive);

        @GET("rest/v1/user_profiles")
        Call<List<JsonObject>> getUserProfiles(@Query("select") String select,
                                               @Query("order") String order);

        // Search music
        @GET("rest/v1/music_tracks")
        Call<List<MusicTrack>> searchMusic(@Query("or") String searchQuery,
                                           @Query("is_active") String isActive);

        // RPC calls for statistics
        @POST("rest/v1/rpc/get_dashboard_stats")
        Call<JsonObject> getDashboardStats();

        @POST("rest/v1/rpc/get_total_users")
        Call<JsonObject> getTotalUsersRPC();

        @POST("rest/v1/rpc/get_total_music")
        Call<JsonObject> getTotalMusicRPC();

        // TAMBAHAN: Hard delete music track
        @DELETE("rest/v1/music_tracks")
        Call<ResponseBody> hardDeleteMusic(@Query("id") String id);

        // Soft delete music track (yang sudah ada)
        @PATCH("rest/v1/music_tracks")
        Call<ResponseBody> deleteMusic(@Query("id") String id,
                                       @Body JsonObject updateBody);

        @GET("rest/v1/users")
        Call<List<JsonObject>> getDoctors(@Query("select") String select,
                                          @Query("role") String role,
                                          @Query("order") String order);
    }

    // Constructor
    private MusicApiClient() {
        gson = new Gson();
        initializeRetrofit();
    }

    public static MusicApiClient getInstance() {
        if (instance == null) {
            instance = new MusicApiClient();
        }
        return instance;
    }

    // FIXED: Method untuk set access token
    public void setAccessToken(String accessToken) {
        this.currentAccessToken = accessToken;
        Log.d(TAG, "Access token updated: " + (accessToken != null ? accessToken.substring(0, 20) + "..." : "null"));
    }

    private void initializeRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // FIXED: Interceptor yang menggunakan access token untuk authenticated requests
        Interceptor authInterceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Log.d(TAG, "🔗 Making request to: " + original.url());

                Request.Builder requestBuilder = original.newBuilder()
                        .header("apikey", SUPABASE_ANON_KEY)
                        .header("Content-Type", "application/json");

                // FIXED: Use access token for database operations that require authentication
                if (original.url().toString().contains("/rest/v1/") &&
                        (original.method().equals("POST") || original.method().equals("PATCH") || original.method().equals("DELETE"))) {

                    if (currentAccessToken != null && !currentAccessToken.isEmpty()) {
                        Log.d(TAG, "🔐 Adding user access token for authenticated request");
                        requestBuilder.header("Authorization", "Bearer " + currentAccessToken);
                    } else {
                        Log.w(TAG, "⚠️ No access token available for authenticated request");
                        requestBuilder.header("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                    }
                } else {
                    // For read operations, use anon key
                    requestBuilder.header("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                }

                // Add Prefer header for database operations
                if (original.url().toString().contains("/rest/v1/")) {
                    requestBuilder.header("Prefer", "return=representation");
                }

                Request request = requestBuilder.build();

                // Log headers for debugging
                Log.d(TAG, "📤 Request headers:");
                for (String name : request.headers().names()) {
                    if (name.equals("Authorization") || name.equals("apikey")) {
                        String value = request.header(name);
                        Log.d(TAG, name + ": " + (value != null ? value.substring(0, Math.min(30, value.length())) + "..." : "null"));
                    } else {
                        Log.d(TAG, name + ": " + request.header(name));
                    }
                }

                return chain.proceed(request);
            }
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(MusicApiService.class);
    }

    // Get all music tracks
    public void getAllMusic(ApiCallback<List<MusicTrack>> callback) {
        apiService.getAllMusic("*", "eq.true", "created_at.desc").enqueue(new Callback<List<MusicTrack>>() {
            @Override
            public void onResponse(Call<List<MusicTrack>> call, Response<List<MusicTrack>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to get music: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<MusicTrack>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Get music by ID - untuk MusicDetailFragment
    public void getMusicById(int musicId, ApiCallback<MusicTrack> callback) {
        getMusicById(String.valueOf(musicId), callback);
    }

    // Get music by ID (String version)
    public void getMusicById(String id, ApiCallback<MusicTrack> callback) {
        Log.d(TAG, "=== GETTING MUSIC BY ID ===");
        Log.d(TAG, "Music ID: " + id);

        apiService.getMusicById("eq." + id, "*").enqueue(new Callback<List<MusicTrack>>() {
            @Override
            public void onResponse(Call<List<MusicTrack>> call, Response<List<MusicTrack>> response) {
                Log.d(TAG, "=== GET MUSIC BY ID RESPONSE ===");
                Log.d(TAG, "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    List<MusicTrack> musicList = response.body();

                    if (!musicList.isEmpty()) {
                        // Supabase returns array even for single item query
                        MusicTrack music = musicList.get(0);
                        Log.d(TAG, "✅ Music found: " + music.getTitle());
                        callback.onSuccess(music);
                    } else {
                        Log.w(TAG, "⚠️ No music found with ID: " + id);
                        callback.onError("Music tidak ditemukan");
                    }
                } else {
                    String errorMsg = "Failed to get music: HTTP " + response.code();

                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "❌ Error body: " + errorBody);

                            if (response.code() == 404) {
                                errorMsg = "Music tidak ditemukan";
                            } else if (response.code() == 400) {
                                errorMsg = "Invalid request format";
                            } else {
                                errorMsg = "Failed to get music: " + errorBody;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }

                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<MusicTrack>> call, Throwable t) {
                Log.e(TAG, "=== GET MUSIC BY ID FAILURE ===", t);

                String errorMessage = t.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.contains("Unable to resolve host")) {
                        callback.onError("❌ Tidak dapat terhubung ke server. Periksa koneksi internet.");
                    } else if (errorMessage.contains("timeout")) {
                        callback.onError("❌ Koneksi timeout. Coba lagi.");
                    } else {
                        callback.onError("❌ Network error: " + errorMessage);
                    }
                } else {
                    callback.onError("❌ Terjadi kesalahan jaringan");
                }
            }
        });
    }

    // FIXED: Create new music method
    public void createMusic(MusicTrack music, String accessToken, ApiCallback<MusicTrack> callback) {
        Log.d(TAG, "=== CREATING MUSIC ===");
        Log.d(TAG, "Music title: " + music.getTitle());
        Log.d(TAG, "Access token provided: " + (accessToken != null && !accessToken.isEmpty()));

        // Set access token for this request
        setAccessToken(accessToken);

        // Validate music data
        if (music.getTitle() == null || music.getTitle().trim().isEmpty()) {
            callback.onError("❌ Judul musik tidak boleh kosong");
            return;
        }

        // Log music data being sent
        String musicJson = gson.toJson(music);
        Log.d(TAG, "📤 Sending music data: " + musicJson);

        apiService.createMusic(music).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "=== CREATE MUSIC RESPONSE ===");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response message: " + response.message());

                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Music created successfully!");

                    try {
                        String responseStr = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "📥 Response body: " + responseStr);

                        if (!responseStr.isEmpty()) {
                            // Handle array response from Supabase
                            if (responseStr.startsWith("[") && responseStr.endsWith("]")) {
                                responseStr = responseStr.substring(1, responseStr.length() - 1);
                            }

                            if (!responseStr.trim().isEmpty()) {
                                MusicTrack created = gson.fromJson(responseStr, MusicTrack.class);
                                callback.onSuccess(created);
                                return;
                            }
                        }

                        // If response is empty but successful, return original music
                        callback.onSuccess(music);

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error parsing response", e);
                        // Even if parsing fails, the creation was successful
                        callback.onSuccess(music);
                    }
                } else {
                    // Handle specific error codes
                    handleCreateMusicError(response, callback);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "=== CREATE MUSIC FAILURE ===", t);

                String errorMessage = t.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.contains("Unable to resolve host")) {
                        callback.onError("❌ Tidak dapat terhubung ke server. Periksa koneksi internet.");
                    } else if (errorMessage.contains("timeout")) {
                        callback.onError("❌ Koneksi timeout. Coba lagi.");
                    } else {
                        callback.onError("❌ Gagal upload: " + errorMessage);
                    }
                } else {
                    callback.onError("❌ Terjadi kesalahan jaringan");
                }
            }
        });
    }

    // FIXED: Handle create music errors
    private void handleCreateMusicError(Response<ResponseBody> response, ApiCallback<MusicTrack> callback) {
        Log.e(TAG, "❌ Create music failed with code: " + response.code());

        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            Log.e(TAG, "❌ Error body: " + errorBody);

            switch (response.code()) {
                case 401:
                    callback.onError("❌ AUTHORIZATION FAILED!\n\n" +
                            "Kemungkinan masalah:\n" +
                            "1. Token akses sudah kadaluarsa\n" +
                            "2. User belum login dengan benar\n" +
                            "3. Permission denied di Supabase\n\n" +
                            "Solusi: Login ulang atau periksa RLS Policy");
                    break;
                case 403:
                    callback.onError("❌ PERMISSION DENIED!\n\n" +
                            "Setup RLS Policy di Supabase:\n" +
                            "1. Buka Table Editor → music_tracks\n" +
                            "2. RLS → New Policy\n" +
                            "3. Enable INSERT untuk authenticated users");
                    break;
                case 404:
                    callback.onError("❌ TABLE TIDAK DITEMUKAN!\n\n" +
                            "Pastikan table 'music_tracks' sudah dibuat");
                    break;
                case 422:
                    if (errorBody.contains("duplicate") || errorBody.contains("unique")) {
                        callback.onError("❌ Musik dengan judul ini sudah ada");
                    } else {
                        callback.onError("❌ Data tidak valid: " + errorBody);
                    }
                    break;
                case 500:
                    callback.onError("❌ Server error. Coba lagi nanti.");
                    break;
                default:
                    callback.onError("❌ Upload gagal (HTTP " + response.code() + "): " + errorBody);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response", e);
            callback.onError("❌ Upload gagal (HTTP " + response.code() + ")");
        }
    }

    // Update music
    public void updateMusic(String id, MusicTrack music, ApiCallback<Void> callback) {
        apiService.updateMusic("eq." + id, music).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Failed to update music: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Modifikasi method deleteMusic untuk hard delete
    public void deleteMusic(String id, ApiCallback<Void> callback) {
        Log.d(TAG, "=== HARD DELETING MUSIC ===");
        Log.d(TAG, "Music ID: " + id);

        apiService.hardDeleteMusic("eq." + id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "=== DELETE MUSIC RESPONSE ===");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response message: " + response.message());

                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Music deleted successfully!");
                    callback.onSuccess(null);
                } else {
                    handleDeleteMusicError(response, callback);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "=== DELETE MUSIC FAILURE ===", t);

                String errorMessage = t.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.contains("Unable to resolve host")) {
                        callback.onError("❌ Tidak dapat terhubung ke server. Periksa koneksi internet.");
                    } else if (errorMessage.contains("timeout")) {
                        callback.onError("❌ Koneksi timeout. Coba lagi.");
                    } else {
                        callback.onError("❌ Gagal menghapus: " + errorMessage);
                    }
                } else {
                    callback.onError("❌ Terjadi kesalahan jaringan");
                }
            }
        });
    }

    // Tambahkan method untuk soft delete jika masih diperlukan
    public void softDeleteMusic(String id, ApiCallback<Void> callback) {
        Log.d(TAG, "=== SOFT DELETING MUSIC ===");
        JsonObject updateBody = new JsonObject();
        updateBody.addProperty("is_active", false);

        apiService.deleteMusic("eq." + id, updateBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Music soft deleted successfully!");
                    callback.onSuccess(null);
                } else {
                    handleDeleteMusicError(response, callback);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Soft delete failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Method untuk handle error saat delete
    private void handleDeleteMusicError(Response<ResponseBody> response, ApiCallback<Void> callback) {
        Log.e(TAG, "❌ Delete music failed with code: " + response.code());

        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            Log.e(TAG, "❌ Error body: " + errorBody);

            switch (response.code()) {
                case 401:
                    callback.onError("❌ AUTHORIZATION FAILED!\n\n" +
                            "Kemungkinan masalah:\n" +
                            "1. Token akses sudah kadaluarsa\n" +
                            "2. User belum login dengan benar\n" +
                            "3. Permission denied di Supabase\n\n" +
                            "Solusi: Login ulang atau periksa RLS Policy");
                    break;
                case 403:
                    callback.onError("❌ PERMISSION DENIED!\n\n" +
                            "Setup RLS Policy di Supabase:\n" +
                            "1. Buka Table Editor → music_tracks\n" +
                            "2. RLS → New Policy\n" +
                            "3. Enable DELETE untuk authenticated users");
                    break;
                case 404:
                    callback.onError("❌ MUSIC TIDAK DITEMUKAN!\n\n" +
                            "Data mungkin sudah dihapus sebelumnya");
                    break;
                case 409:
                    callback.onError("❌ CONFLICT!\n\n" +
                            "Data sedang digunakan oleh sistem lain");
                    break;
                case 500:
                    callback.onError("❌ Server error. Coba lagi nanti.");
                    break;
                default:
                    callback.onError("❌ Hapus gagal (HTTP " + response.code() + "): " + errorBody);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing delete error response", e);
            callback.onError("❌ Hapus gagal (HTTP " + response.code() + ")");
        }
    }

    // Get statistics
    public void getStatistics(ApiCallback<Statistics> callback) {
        Log.d(TAG, "=== GETTING STATISTICS ===");

        // Get total users from 'users' table
        Call<List<JsonObject>> usersCall = apiService.getTotalUsers("id");
        usersCall.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                int userCount = 0;

                if (response.isSuccessful()) {
                    // Try getting count from Content-Range header
                    String contentRange = response.headers().get("content-range");
                    Log.d(TAG, "Users Content-Range: " + contentRange);

                    if (contentRange != null) {
                        String[] parts = contentRange.split("/");
                        if (parts.length > 1) {
                            try {
                                userCount = Integer.parseInt(parts[1]);
                                Log.d(TAG, "✅ Total users from header: " + userCount);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing user count from header", e);
                            }
                        }
                    }

                    // If no count in header, use response body size
                    if (userCount == 0 && response.body() != null) {
                        userCount = response.body().size();
                        Log.d(TAG, "✅ Total users from body size: " + userCount);
                    }
                } else {
                    Log.e(TAG, "❌ Failed to get users: " + response.code() + " " + response.message());
                }

                final int finalUserCount = userCount;

                // Get total music tracks
                Call<List<JsonObject>> musicCall = apiService.getTotalMusic("id", "eq.true");
                musicCall.enqueue(new Callback<List<JsonObject>>() {
                    @Override
                    public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                        int musicCount = 0;

                        if (response.isSuccessful()) {
                            String contentRange = response.headers().get("content-range");
                            Log.d(TAG, "Music Content-Range: " + contentRange);

                            if (contentRange != null) {
                                String[] parts = contentRange.split("/");
                                if (parts.length > 1) {
                                    try {
                                        musicCount = Integer.parseInt(parts[1]);
                                        Log.d(TAG, "✅ Total music from header: " + musicCount);
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Error parsing music count from header", e);
                                    }
                                }
                            }

                            if (musicCount == 0 && response.body() != null) {
                                musicCount = response.body().size();
                                Log.d(TAG, "✅ Total music from body size: " + musicCount);
                            }
                        } else {
                            Log.e(TAG, "❌ Failed to get music: " + response.code() + " " + response.message());
                        }

                        // Return statistics
                        Statistics stats = new Statistics(finalUserCount, musicCount);
                        Log.d(TAG, "=== STATISTICS RESULT ===");
                        Log.d(TAG, "Total Users: " + stats.getTotalUsers());
                        Log.d(TAG, "Total Music: " + stats.getTotalMusic());

                        callback.onSuccess(stats);
                    }

                    @Override
                    public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                        Log.e(TAG, "❌ Network error getting music count", t);
                        callback.onError("Failed to get music count: " + t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "❌ Network error getting user count", t);
                callback.onError("Failed to get user count: " + t.getMessage());
            }
        });
    }

    public void getRealtimeStatistics(ApiCallback<Statistics> callback) {
        Log.d(TAG, "=== GETTING REALTIME STATISTICS ===");

        // Create OkHttpClient with custom interceptor for count requests
        OkHttpClient countClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("apikey", SUPABASE_ANON_KEY)
                            .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                            .header("Prefer", "count=exact,head=true") // Request only count, no data
                            .header("Content-Type", "application/json")
                            .build();
                    return chain.proceed(request);
                })
                .build();

        // Create separate Retrofit instance for count requests
        Retrofit countRetrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL + "/")
                .client(countClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MusicApiService countService = countRetrofit.create(MusicApiService.class);

        // Get users count
        countService.getTotalUsers("id").enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                int userCount = extractCountFromResponse(response, "users");

                // Get music count
                countService.getTotalMusic("id", "eq.true").enqueue(new Callback<List<JsonObject>>() {
                    @Override
                    public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                        int musicCount = extractCountFromResponse(response, "music");

                        Statistics stats = new Statistics(userCount, musicCount);
                        callback.onSuccess(stats);
                    }

                    @Override
                    public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                        Log.e(TAG, "Failed to get music count", t);
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "Failed to get user count", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Helper method to extract count from response
    private int extractCountFromResponse(Response<List<JsonObject>> response, String type) {
        int count = 0;

        if (response.isSuccessful()) {
            // Check all possible headers for count
            String contentRange = response.headers().get("content-range");
            String xTotalCount = response.headers().get("x-total-count");
            String preferenceApplied = response.headers().get("preference-applied");

            Log.d(TAG, "=== " + type.toUpperCase() + " COUNT HEADERS ===");
            Log.d(TAG, "Content-Range: " + contentRange);
            Log.d(TAG, "X-Total-Count: " + xTotalCount);
            Log.d(TAG, "Preference-Applied: " + preferenceApplied);

            // Try Content-Range header first
            if (contentRange != null) {
                String[] parts = contentRange.split("/");
                if (parts.length > 1 && !parts[1].equals("*")) {
                    try {
                        count = Integer.parseInt(parts[1]);
                        Log.d(TAG, "✅ Got " + type + " count from Content-Range: " + count);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing " + type + " count", e);
                    }
                }
            }

            // Try X-Total-Count header
            if (count == 0 && xTotalCount != null) {
                try {
                    count = Integer.parseInt(xTotalCount);
                    Log.d(TAG, "✅ Got " + type + " count from X-Total-Count: " + count);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing " + type + " count from X-Total-Count", e);
                }
            }

            // Fallback to response body size
            if (count == 0 && response.body() != null) {
                count = response.body().size();
                Log.d(TAG, "✅ Got " + type + " count from body size: " + count);
            }
        } else {
            Log.e(TAG, "❌ Failed to get " + type + " count: " + response.code());
        }

        return count;
    }

    // Search music
    public void searchMusic(String query, ApiCallback<List<MusicTrack>> callback) {
        String searchQuery = "(title.ilike.%" + query + "%,artist.ilike.%" + query + "%,category.ilike.%" + query + "%)";

        apiService.searchMusic(searchQuery, "eq.true").enqueue(new Callback<List<MusicTrack>>() {
            @Override
            public void onResponse(Call<List<MusicTrack>> call, Response<List<MusicTrack>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Search failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<MusicTrack>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Add this method to MusicApiClient class
    public void getStatisticsViaRPC(ApiCallback<Statistics> callback) {
        Log.d(TAG, "=== GETTING STATISTICS VIA RPC ===");

        apiService.getDashboardStats().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject stats = response.body();
                        int totalUsers = stats.get("total_users").getAsInt();
                        int totalMusic = stats.get("total_music").getAsInt();

                        Log.d(TAG, "✅ RPC Stats - Users: " + totalUsers + ", Music: " + totalMusic);

                        Statistics statistics = new Statistics(totalUsers, totalMusic);
                        callback.onSuccess(statistics);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing RPC response", e);
                        // Fallback to regular method
                        getStatistics(callback);
                    }
                } else {
                    Log.e(TAG, "RPC call failed: " + response.code());
                    // Fallback to regular method
                    getStatistics(callback);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "RPC call error", t);
                // Fallback to regular method
                getStatistics(callback);
            }
        });
    }

    public void getUserProfiles(ApiCallback<List<ListenerListFragment.Listener>> callback) {
        Log.d(TAG, "=== GETTING LISTENERS FROM USERS TABLE ===");

        // Query ke tabel 'users' sesuai dengan skema database Anda
        String select = "id,email,name,role,created_at,last_login,total_listening_time";
        String order = "created_at.desc";

        // Gunakan endpoint yang benar untuk tabel users
        apiService.getListeners(select, order).enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<JsonObject> usersData = response.body();
                        List<ListenerListFragment.Listener> listeners = new ArrayList<>();

                        Log.d(TAG, "✅ Received " + usersData.size() + " users from database");

                        for (JsonObject userData : usersData) {
                            // Filter hanya role "Listener" (bukan admin atau doctor)
                            String role = userData.has("role") && !userData.get("role").isJsonNull()
                                    ? userData.get("role").getAsString()
                                    : "Listener";

                            if (!role.equalsIgnoreCase("Listener")) {
                                continue; // Skip non-listener users
                            }

                            ListenerListFragment.Listener listener = new ListenerListFragment.Listener();

                            // Map fields dari tabel users ke Listener model
                            listener.setId(userData.has("id") ? userData.get("id").getAsString() : "");
                            listener.setEmail(userData.has("email") ? userData.get("email").getAsString() : "");
                            listener.setName(userData.has("name") ? userData.get("name").getAsString() : "User");

                            // Format tanggal
                            String createdAt = userData.has("created_at") ? userData.get("created_at").getAsString() : "";
                            listener.setJoinDate(createdAt);

                            // Total listening time sebagai total played (dalam menit)
                            int totalListeningTime = userData.has("total_listening_time") && !userData.get("total_listening_time").isJsonNull()
                                    ? userData.get("total_listening_time").getAsInt()
                                    : 0;
                            listener.setTotalPlayed(totalListeningTime / 60); // Convert seconds to minutes

                            // Last login sebagai last active
                            String lastLogin = userData.has("last_login") && !userData.get("last_login").isJsonNull()
                                    ? userData.get("last_login").getAsString()
                                    : createdAt; // Default ke created_at jika belum pernah login
                            listener.setLastActive(lastLogin);

                            listeners.add(listener);
                            Log.d(TAG, "Added listener: " + listener.getName() + " (" + listener.getEmail() + ")");
                        }

                        Log.d(TAG, "✅ Successfully loaded " + listeners.size() + " listeners");
                        callback.onSuccess(listeners);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing listeners data", e);
                        callback.onError("Error parsing data: " + e.getMessage());
                    }
                } else {
                    String errorMsg = "Failed to get listeners: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);

                            // Handle specific errors
                            if (errorBody.contains("relation") && errorBody.contains("does not exist")) {
                                errorMsg = "Tabel 'users' tidak ditemukan di database";
                            } else if (errorBody.contains("permission denied")) {
                                errorMsg = "Akses ditolak. Periksa RLS Policy untuk tabel users";
                            } else {
                                errorMsg += " - " + errorBody;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "=== GET LISTENERS FAILURE ===", t);

                String errorMessage = t.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.contains("Unable to resolve host")) {
                        callback.onError("❌ Tidak dapat terhubung ke server. Periksa koneksi internet.");
                    } else if (errorMessage.contains("timeout")) {
                        callback.onError("❌ Koneksi timeout. Coba lagi.");
                    } else {
                        callback.onError("❌ Network error: " + errorMessage);
                    }
                } else {
                    callback.onError("❌ Terjadi kesalahan jaringan");
                }
            }
        });
    }

    public void getListeners(ApiCallback<List<ListenerListFragment.Listener>> callback) {
        Log.d(TAG, "=== GETTING LISTENERS ===");

        // Query untuk mendapatkan data users dengan field yang diperlukan
        String select = "id,email,created_at,raw_user_meta_data";
        String order = "created_at.desc";

        apiService.getListeners(select, order).enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<JsonObject> usersData = response.body();
                        List<ListenerListFragment.Listener> listeners = new ArrayList<>();

                        for (JsonObject userData : usersData) {
                            ListenerListFragment.Listener listener = new ListenerListFragment.Listener();

                            // Extract user data
                            listener.setId(userData.has("id") ? userData.get("id").getAsString() : "");
                            listener.setEmail(userData.has("email") ? userData.get("email").getAsString() : "");
                            listener.setJoinDate(userData.has("created_at") ? userData.get("created_at").getAsString() : "");

                            // Extract name from raw_user_meta_data if available
                            if (userData.has("raw_user_meta_data") && !userData.get("raw_user_meta_data").isJsonNull()) {
                                JsonObject metaData = userData.getAsJsonObject("raw_user_meta_data");
                                if (metaData.has("full_name")) {
                                    listener.setName(metaData.get("full_name").getAsString());
                                } else if (metaData.has("name")) {
                                    listener.setName(metaData.get("name").getAsString());
                                } else {
                                    // Use email prefix as name
                                    String email = listener.getEmail();
                                    if (email != null && email.contains("@")) {
                                        listener.setName(email.substring(0, email.indexOf("@")));
                                    } else {
                                        listener.setName("Pengguna");
                                    }
                                }
                            } else {
                                // Use email prefix as name
                                String email = listener.getEmail();
                                if (email != null && email.contains("@")) {
                                    listener.setName(email.substring(0, email.indexOf("@")));
                                } else {
                                    listener.setName("Pengguna");
                                }
                            }

                            // Set default values for now - you'll need to implement these separately
                            listener.setTotalPlayed(0); // Needs separate query to get play count
                            listener.setLastActive(listener.getJoinDate()); // Default to join date

                            listeners.add(listener);
                        }

                        Log.d(TAG, "✅ Successfully loaded " + listeners.size() + " listeners");
                        callback.onSuccess(listeners);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing listeners data", e);
                        callback.onError("Error parsing data: " + e.getMessage());
                    }
                } else {
                    String errorMsg = "Failed to get listeners: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                            errorMsg += " - " + errorBody;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "=== GET LISTENERS FAILURE ===", t);

                String errorMessage = t.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.contains("Unable to resolve host")) {
                        callback.onError("❌ Tidak dapat terhubung ke server. Periksa koneksi internet.");
                    } else if (errorMessage.contains("timeout")) {
                        callback.onError("❌ Koneksi timeout. Coba lagi.");
                    } else {
                        callback.onError("❌ Network error: " + errorMessage);
                    }
                } else {
                    callback.onError("❌ Terjadi kesalahan jaringan");
                }
            }
        });
    }

    // Optimized method that tries RPC first, then falls back to regular queries
    public void getOptimizedStatistics(ApiCallback<Statistics> callback) {
        // Try RPC first (fastest)
        getStatisticsViaRPC(new ApiCallback<Statistics>() {
            @Override
            public void onSuccess(Statistics result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "RPC failed, trying regular method: " + error);
                // Fallback to regular method
                getStatistics(callback);
            }
        });
    }

    public void getSimpleUserProfiles(ApiCallback<String> callback) {
        Log.d(TAG, "=== GETTING USER PROFILES (SIMPLE) ===");

        // Build URL manually
        String fullUrl = SUPABASE_URL + "/rest/v1/user_profiles?select=*&order=created_at.desc";
        Log.d(TAG, "Full URL: " + fullUrl);

        // Create basic HTTP client
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Log.d(TAG, "Interceptor - URL: " + original.url());

                    Request request = original.newBuilder()
                            .header("apikey", SUPABASE_ANON_KEY)
                            .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .build();

                    Log.d(TAG, "Headers sent:");
                    for (String name : request.headers().names()) {
                        if (name.contains("apikey") || name.contains("Authorization")) {
                            Log.d(TAG, "  " + name + ": [HIDDEN]");
                        } else {
                            Log.d(TAG, "  " + name + ": " + request.header(name));
                        }
                    }

                    okhttp3.Response response = chain.proceed(request);

                    Log.d(TAG, "Response received:");
                    Log.d(TAG, "  Code: " + response.code());
                    Log.d(TAG, "  Message: " + response.message());

                    return response;
                })
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .get()
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Simple query failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                try {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    Log.d(TAG, "=== SIMPLE QUERY RESPONSE ===");
                    Log.d(TAG, "Code: " + response.code());
                    Log.d(TAG, "Body length: " + responseBody.length());

                    if (responseBody.length() > 500) {
                        Log.d(TAG, "Body (first 500 chars): " + responseBody.substring(0, 500));
                    } else {
                        Log.d(TAG, "Body: " + responseBody);
                    }

                    if (response.isSuccessful()) {
                        callback.onSuccess(responseBody);
                    } else {
                        callback.onError("HTTP " + response.code() + ": " + responseBody);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    callback.onError("Error: " + e.getMessage());
                }
            }
        });
    }

    // Get pending doctor recommendations
    public void getPendingDoctorRecommendations(ApiCallback<List<MusicTrack>> callback) {
        Log.d(TAG, "=== GETTING PENDING DOCTOR RECOMMENDATIONS ===");

        // Query for pending doctor recommendations
        apiService.getAllMusic("*", "eq.true", "created_at.desc").enqueue(new Callback<List<MusicTrack>>() {
            @Override
            public void onResponse(Call<List<MusicTrack>> call, Response<List<MusicTrack>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MusicTrack> allMusic = response.body();
                    List<MusicTrack> pendingRecommendations = new ArrayList<>();

                    // Filter for pending doctor recommendations
                    for (MusicTrack music : allMusic) {
                        if (music.isUploadedByDoctor() &&
                                "pending".equals(music.getApprovalStatus())) {
                            pendingRecommendations.add(music);
                        }
                    }

                    Log.d(TAG, "Found " + pendingRecommendations.size() + " pending recommendations");
                    callback.onSuccess(pendingRecommendations);
                } else {
                    callback.onError("Failed to get recommendations: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<MusicTrack>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Get doctor's own recommendations
    public void getDoctorRecommendations(String doctorId, ApiCallback<List<MusicTrack>> callback) {
        Log.d(TAG, "=== GETTING DOCTOR RECOMMENDATIONS ===");
        Log.d(TAG, "Doctor ID: " + doctorId);

        apiService.getAllMusic("*", "eq.true", "created_at.desc").enqueue(new Callback<List<MusicTrack>>() {
            @Override
            public void onResponse(Call<List<MusicTrack>> call, Response<List<MusicTrack>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MusicTrack> allMusic = response.body();
                    List<MusicTrack> doctorMusic = new ArrayList<>();

                    // Filter for this doctor's recommendations
                    for (MusicTrack music : allMusic) {
                        if (doctorId.equals(music.getDoctorId()) ||
                                doctorId.equals(music.getCreatedBy())) {
                            doctorMusic.add(music);
                        }
                    }

                    Log.d(TAG, "Found " + doctorMusic.size() + " recommendations for doctor");
                    callback.onSuccess(doctorMusic);
                } else {
                    callback.onError("Failed to get recommendations: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<MusicTrack>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Approve recommendation
    public void approveRecommendation(String musicId, String adminId, ApiCallback<Void> callback) {
        Log.d(TAG, "=== APPROVING RECOMMENDATION ===");
        Log.d(TAG, "Music ID: " + musicId);
        Log.d(TAG, "Admin ID: " + adminId);

        // Create update object
        MusicTrack update = new MusicTrack();
        update.setApprovalStatus("approved");
        update.setApprovedBy(adminId);

        apiService.updateMusic("eq." + musicId, update).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Recommendation approved successfully");
                    callback.onSuccess(null);
                } else {
                    callback.onError("Failed to approve: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Reject recommendation
    public void rejectRecommendation(String musicId, String adminId, String reason, ApiCallback<Void> callback) {
        Log.d(TAG, "=== REJECTING RECOMMENDATION ===");
        Log.d(TAG, "Music ID: " + musicId);
        Log.d(TAG, "Admin ID: " + adminId);
        Log.d(TAG, "Reason: " + reason);

        // Create update object
        MusicTrack update = new MusicTrack();
        update.setApprovalStatus("rejected");
        update.setApprovedBy(adminId);
        update.setRejectionReason(reason);

        apiService.updateMusic("eq." + musicId, update).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Recommendation rejected successfully");
                    callback.onSuccess(null);
                } else {
                    callback.onError("Failed to reject: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getStatisticsWithDoctors(ApiCallback<StatisticsWithDoctors> callback) {
        Log.d(TAG, "=== GETTING STATISTICS WITH DOCTORS ===");

        // Get total users from 'users' table
        Call<List<JsonObject>> usersCall = apiService.getTotalUsers("id");
        usersCall.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                int userCount = 0;

                if (response.isSuccessful()) {
                    // Try getting count from Content-Range header
                    String contentRange = response.headers().get("content-range");
                    Log.d(TAG, "Users Content-Range: " + contentRange);

                    if (contentRange != null) {
                        String[] parts = contentRange.split("/");
                        if (parts.length > 1) {
                            try {
                                userCount = Integer.parseInt(parts[1]);
                                Log.d(TAG, "✅ Total users from header: " + userCount);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing user count from header", e);
                            }
                        }
                    }

                    // If no count in header, use response body size
                    if (userCount == 0 && response.body() != null) {
                        userCount = response.body().size();
                        Log.d(TAG, "✅ Total users from body size: " + userCount);
                    }
                } else {
                    Log.e(TAG, "❌ Failed to get users: " + response.code() + " " + response.message());
                }

                final int finalUserCount = userCount;

                // Get total music tracks
                Call<List<JsonObject>> musicCall = apiService.getTotalMusic("id", "eq.true");
                musicCall.enqueue(new Callback<List<JsonObject>>() {
                    @Override
                    public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                        int musicCount = 0;

                        if (response.isSuccessful()) {
                            String contentRange = response.headers().get("content-range");
                            Log.d(TAG, "Music Content-Range: " + contentRange);

                            if (contentRange != null) {
                                String[] parts = contentRange.split("/");
                                if (parts.length > 1) {
                                    try {
                                        musicCount = Integer.parseInt(parts[1]);
                                        Log.d(TAG, "✅ Total music from header: " + musicCount);
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Error parsing music count from header", e);
                                    }
                                }
                            }

                            if (musicCount == 0 && response.body() != null) {
                                musicCount = response.body().size();
                                Log.d(TAG, "✅ Total music from body size: " + musicCount);
                            }
                        } else {
                            Log.e(TAG, "❌ Failed to get music: " + response.code() + " " + response.message());
                        }

                        final int finalMusicCount = musicCount;

                        // Get all users and count doctors
                        Call<List<JsonObject>> allUsersCall = apiService.getListeners("id,role", "created_at.desc");
                        allUsersCall.enqueue(new Callback<List<JsonObject>>() {
                            @Override
                            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                                int doctorCount = 0;

                                if (response.isSuccessful() && response.body() != null) {
                                    // Count users with role = 'Doctor'
                                    for (JsonObject user : response.body()) {
                                        if (user.has("role") && !user.get("role").isJsonNull()) {
                                            String role = user.get("role").getAsString();
                                            if ("Dokter".equalsIgnoreCase(role)) {
                                                doctorCount++;
                                            }
                                        }
                                    }
                                    Log.d(TAG, "✅ Total doctors counted: " + doctorCount);
                                } else {
                                    Log.e(TAG, "❌ Failed to get users for doctor count: " + response.code());
                                }

                                // Return statistics
                                StatisticsWithDoctors stats = new StatisticsWithDoctors(finalUserCount, finalMusicCount, doctorCount);
                                Log.d(TAG, "=== STATISTICS RESULT ===");
                                Log.d(TAG, "Total Users: " + stats.getTotalUsers());
                                Log.d(TAG, "Total Music: " + stats.getTotalMusic());
                                Log.d(TAG, "Total Doctors: " + stats.getTotalDoctors());

                                callback.onSuccess(stats);
                            }

                            @Override
                            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                                Log.e(TAG, "❌ Network error getting doctor count", t);
                                // Return stats without doctor count
                                StatisticsWithDoctors stats = new StatisticsWithDoctors(finalUserCount, finalMusicCount, 0);
                                callback.onSuccess(stats);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                        Log.e(TAG, "❌ Network error getting music count", t);
                        callback.onError("Failed to get music count: " + t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "❌ Network error getting user count", t);
                callback.onError("Failed to get user count: " + t.getMessage());
            }
        });
    }

    public void getDoctorProfiles(ApiCallback<List<DoctorListFragment.Doctor>> callback) {
        Log.d(TAG, "=== GETTING DOCTORS FROM USERS TABLE ===");

        // Query ke tabel 'users' sesuai dengan skema database Anda
        String select = "id,email,name,role,created_at,last_login,total_listening_time";
        String order = "created_at.desc";

        // Gunakan endpoint yang benar untuk tabel users
        apiService.getListeners(select, order).enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<JsonObject> usersData = response.body();
                        List<DoctorListFragment.Doctor> doctors = new ArrayList<>();

                        Log.d(TAG, "✅ Received " + usersData.size() + " users from database");

                        for (JsonObject userData : usersData) {
                            // Filter hanya role "Doctor"
                            String role = userData.has("role") && !userData.get("role").isJsonNull()
                                    ? userData.get("role").getAsString()
                                    : "";

                            if (!role.equalsIgnoreCase("Dokter")) {
                                continue; // Skip non-doctor users
                            }

                            DoctorListFragment.Doctor doctor = new DoctorListFragment.Doctor();

                            // Map fields dari tabel users ke Doctor model
                            doctor.setId(userData.has("id") ? userData.get("id").getAsString() : "");
                            doctor.setEmail(userData.has("email") ? userData.get("email").getAsString() : "");

                            String name = userData.has("name") ? userData.get("name").getAsString() : "User";
                            // Add Dr. prefix if not exists
                            if (!name.startsWith("Dr.") && !name.startsWith("dr.")) {
                                name = "Dr. " + name;
                            }
                            doctor.setName(name);

                            // Format tanggal
                            String createdAt = userData.has("created_at") ? userData.get("created_at").getAsString() : "";
                            doctor.setJoinDate(createdAt);

                            // Total recommendations (for now set to 0, can be implemented later)
                            doctor.setTotalRecommendations(0);

                            // Last login sebagai last active
                            String lastLogin = userData.has("last_login") && !userData.get("last_login").isJsonNull()
                                    ? userData.get("last_login").getAsString()
                                    : createdAt; // Default ke created_at jika belum pernah login
                            doctor.setLastActive(lastLogin);

                            doctors.add(doctor);
                            Log.d(TAG, "Added doctor: " + doctor.getName() + " (" + doctor.getEmail() + ")");
                        }

                        Log.d(TAG, "✅ Successfully loaded " + doctors.size() + " doctors");
                        callback.onSuccess(doctors);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing doctors data", e);
                        callback.onError("Error parsing data: " + e.getMessage());
                    }
                } else {
                    String errorMsg = "Failed to get doctors: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);

                            // Handle specific errors
                            if (errorBody.contains("relation") && errorBody.contains("does not exist")) {
                                errorMsg = "Tabel 'users' tidak ditemukan di database";
                            } else if (errorBody.contains("permission denied")) {
                                errorMsg = "Akses ditolak. Periksa RLS Policy untuk tabel users";
                            } else {
                                errorMsg += " - " + errorBody;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "=== GET DOCTORS FAILURE ===", t);

                String errorMessage = t.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.contains("Unable to resolve host")) {
                        callback.onError("❌ Tidak dapat terhubung ke server. Periksa koneksi internet.");
                    } else if (errorMessage.contains("timeout")) {
                        callback.onError("❌ Koneksi timeout. Coba lagi.");
                    } else {
                        callback.onError("❌ Network error: " + errorMessage);
                    }
                } else {
                    callback.onError("❌ Terjadi kesalahan jaringan");
                }
            }
        });
    }

    public static class StatisticsWithDoctors extends Statistics {
        private int totalDoctors;

        public StatisticsWithDoctors(int totalUsers, int totalMusic, int totalDoctors) {
            super(totalUsers, totalMusic);
            this.totalDoctors = totalDoctors;
        }

        public int getTotalDoctors() { return totalDoctors; }
        public void setTotalDoctors(int totalDoctors) { this.totalDoctors = totalDoctors; }
    }


    // Add this class if you want more detailed stats
    public static class DetailedStatistics extends Statistics {
        private int playsToday;
        private int downloadsToday;
        private int newUsersToday;
        private String timestamp;

        public DetailedStatistics(int totalUsers, int totalMusic) {
            super(totalUsers, totalMusic);
        }

        // Add getters and setters
        public int getPlaysToday() { return playsToday; }
        public void setPlaysToday(int playsToday) { this.playsToday = playsToday; }

        public int getDownloadsToday() { return downloadsToday; }
        public void setDownloadsToday(int downloadsToday) { this.downloadsToday = downloadsToday; }

        public int getNewUsersToday() { return newUsersToday; }
        public void setNewUsersToday(int newUsersToday) { this.newUsersToday = newUsersToday; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    // Clear access token (for logout)
    public void clearAccessToken() {
        this.currentAccessToken = null;
        Log.d(TAG, "Access token cleared");
    }

    // Callback interface
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // Statistics model
    public static class Statistics {
        private int totalUsers;
        private int totalMusic;

        public Statistics(int totalUsers, int totalMusic) {
            this.totalUsers = totalUsers;
            this.totalMusic = totalMusic;
        }

        public int getTotalUsers() { return totalUsers; }
        public int getTotalMusic() { return totalMusic; }
    }
}