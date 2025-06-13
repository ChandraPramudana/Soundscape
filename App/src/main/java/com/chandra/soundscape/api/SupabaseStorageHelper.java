package com.chandra.soundscape.api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import okhttp3.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class SupabaseStorageHelper {
    private static final String TAG = "SupabaseStorageHelper";

    // Replace dengan URL dan key Supabase Anda
    private static final String SUPABASE_URL = "https://ibldlqyhcwdvgfkcotih.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlibGRscXloY3dkdmdma2NvdGloIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDkyMjA0MzIsImV4cCI6MjA2NDc5NjQzMn0.Ri5AoGKBJjpvQiSbRfI2on2dJ_Ff4RefX-XNYl7IJRk";

    // Storage buckets - gunakan nama bucket yang baru
    private static final String AUDIO_BUCKET = "soundscape-audio";
    private static final String IMAGE_BUCKET = "soundscape-images";

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor(new LoggingInterceptor())
            .build();

    public interface UploadCallback {
        void onSuccess(String fileUrl);
        void onError(String error);
    }

    public interface ProgressCallback {
        void onProgress(int progress);
        void onSuccess(String fileUrl);
        void onError(String error);
    }

    // =====================================
    // AUDIO FILE UPLOAD
    // =====================================

    public static void uploadAudioFile(Context context, Uri audioUri, UploadCallback callback) {
        uploadAudioFileWithProgress(context, audioUri, new ProgressCallback() {
            @Override
            public void onProgress(int progress) {
                // Progress updates can be ignored for simple callback
            }

            @Override
            public void onSuccess(String fileUrl) {
                callback.onSuccess(fileUrl);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static void uploadAudioFileWithProgress(Context context, Uri audioUri, ProgressCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting audio upload for URI: " + audioUri);

                // Generate unique filename
                String fileName = "audio_" + UUID.randomUUID().toString() + ".mp3";

                // Get input stream from URI
                InputStream inputStream = context.getContentResolver().openInputStream(audioUri);
                if (inputStream == null) {
                    callback.onError("Cannot read audio file");
                    return;
                }

                // Read file data
                byte[] fileData = readInputStream(inputStream);
                inputStream.close();

                Log.d(TAG, "Audio file size: " + fileData.length + " bytes");

                // Create request body with progress tracking
                RequestBody requestBody = new ProgressRequestBody(
                        RequestBody.create(MediaType.parse("audio/mpeg"), fileData),
                        callback::onProgress
                );

                // Build upload request - PERBAIKAN: gunakan path yang benar
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + AUDIO_BUCKET + "/" + fileName;
                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "audio/mpeg")
                        .post(requestBody)
                        .build();

                Log.d(TAG, "Uploading to: " + uploadUrl);

                // Execute request
                Response response = httpClient.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    String publicUrl = getPublicUrl(AUDIO_BUCKET, fileName);
                    Log.d(TAG, "Audio upload successful. Public URL: " + publicUrl);
                    callback.onSuccess(publicUrl);
                } else {
                    Log.e(TAG, "Audio upload failed with code " + response.code() + ": " + responseBody);
                    callback.onError("Upload failed: " + responseBody);
                }
                response.close();

            } catch (Exception e) {
                Log.e(TAG, "Audio upload error", e);
                callback.onError("Upload error: " + e.getMessage());
            }
        }).start();
    }

    // =====================================
    // IMAGE FILE UPLOAD
    // =====================================

    public static void uploadImage(Context context, Uri imageUri, UploadCallback callback) {
        uploadImageWithProgress(context, imageUri, new ProgressCallback() {
            @Override
            public void onProgress(int progress) {
                // Progress updates can be ignored for simple callback
            }

            @Override
            public void onSuccess(String fileUrl) {
                callback.onSuccess(fileUrl);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static void uploadImageWithProgress(Context context, Uri imageUri, ProgressCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting image upload for URI: " + imageUri);

                // Generate unique filename
                String fileName = "image_" + UUID.randomUUID().toString() + ".jpg";

                // Get input stream from URI
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    callback.onError("Cannot read image file");
                    return;
                }

                // Read file data
                byte[] fileData = readInputStream(inputStream);
                inputStream.close();

                Log.d(TAG, "Image file size: " + fileData.length + " bytes");

                // Create request body with progress tracking
                RequestBody requestBody = new ProgressRequestBody(
                        RequestBody.create(MediaType.parse("image/jpeg"), fileData),
                        callback::onProgress
                );

                // Build upload request - PERBAIKAN: gunakan path yang benar
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + IMAGE_BUCKET + "/" + fileName;
                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "image/jpeg")
                        .post(requestBody)
                        .build();

                Log.d(TAG, "Uploading to: " + uploadUrl);

                // Execute request
                Response response = httpClient.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    String publicUrl = getPublicUrl(IMAGE_BUCKET, fileName);
                    Log.d(TAG, "Image upload successful. Public URL: " + publicUrl);
                    callback.onSuccess(publicUrl);
                } else {
                    Log.e(TAG, "Image upload failed with code " + response.code() + ": " + responseBody);
                    callback.onError("Upload failed: " + responseBody);
                }
                response.close();

            } catch (Exception e) {
                Log.e(TAG, "Image upload error", e);
                callback.onError("Upload error: " + e.getMessage());
            }
        }).start();
    }

    // =====================================
    // THUMBNAIL UPLOAD (ALIAS FOR IMAGE)
    // =====================================

    public static void uploadThumbnail(Context context, Uri thumbnailUri, UploadCallback callback) {
        uploadImage(context, thumbnailUri, callback);
    }

    // =====================================
    // UTILITY METHODS
    // =====================================

    private static String getPublicUrl(String bucket, String fileName) {
        return SUPABASE_URL + "/storage/v1/object/public/" + bucket + "/" + fileName;
    }

    private static byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }

        return output.toByteArray();
    }

    // =====================================
    // PROGRESS TRACKING REQUEST BODY
    // =====================================

    private static class ProgressRequestBody extends RequestBody {
        private final RequestBody requestBody;
        private final ProgressListener progressListener;

        public interface ProgressListener {
            void onProgress(int progress);
        }

        public ProgressRequestBody(RequestBody requestBody, ProgressListener progressListener) {
            this.requestBody = requestBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return requestBody.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            return requestBody.contentLength();
        }

        @Override
        public void writeTo(okio.BufferedSink sink) throws IOException {
            long totalBytes = contentLength();
            long uploadedBytes = 0;

            okio.Buffer buffer = new okio.Buffer();
            requestBody.writeTo(buffer);

            byte[] data = buffer.readByteArray();

            // Write data in chunks and report progress
            int chunkSize = 8192;
            int offset = 0;

            while (offset < data.length) {
                int bytesToWrite = Math.min(chunkSize, data.length - offset);
                sink.write(data, offset, bytesToWrite);
                sink.flush();

                uploadedBytes += bytesToWrite;
                offset += bytesToWrite;

                if (progressListener != null && totalBytes > 0) {
                    int progress = (int) ((uploadedBytes * 100) / totalBytes);
                    progressListener.onProgress(progress);
                }
            }
        }
    }

    // =====================================
    // DELETE FILE METHODS
    // =====================================

    public static void deleteAudioFile(String fileName, UploadCallback callback) {
        deleteFile(AUDIO_BUCKET, fileName, callback);
    }

    public static void deleteImageFile(String fileName, UploadCallback callback) {
        deleteFile(IMAGE_BUCKET, fileName, callback);
    }

    private static void deleteFile(String bucket, String fileName, UploadCallback callback) {
        new Thread(() -> {
            try {
                String deleteUrl = SUPABASE_URL + "/storage/v1/object/" + bucket + "/" + fileName;

                Request request = new Request.Builder()
                        .url(deleteUrl)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .delete()
                        .build();

                Response response = httpClient.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "File deleted successfully: " + fileName);
                    callback.onSuccess("File deleted");
                } else {
                    String error = response.body() != null ? response.body().string() : "Delete failed";
                    Log.e(TAG, "File delete failed: " + error);
                    callback.onError("Delete failed: " + error);
                }
                response.close();

            } catch (Exception e) {
                Log.e(TAG, "File delete error", e);
                callback.onError("Delete error: " + e.getMessage());
            }
        }).start();
    }

    // =====================================
    // STORAGE BUCKETS MANAGEMENT
    // =====================================

    public static void createBucketsIfNotExist() {
        createBucket(AUDIO_BUCKET, true);  // public bucket
        createBucket(IMAGE_BUCKET, true);  // public bucket
    }

    private static void createBucket(String bucketName, boolean isPublic) {
        new Thread(() -> {
            try {
                String createBucketUrl = SUPABASE_URL + "/storage/v1/bucket";

                String requestBody = "{"
                        + "\"id\":\"" + bucketName + "\","
                        + "\"name\":\"" + bucketName + "\","
                        + "\"public\":" + isPublic + ","
                        + "\"file_size_limit\":52428800,"  // 50MB limit
                        + "\"allowed_mime_types\":[\"image/jpeg\",\"image/png\",\"image/gif\",\"audio/mpeg\",\"audio/mp3\",\"audio/wav\"]"
                        + "}";

                Request request = new Request.Builder()
                        .url(createBucketUrl)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                        .build();

                Response response = httpClient.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Bucket created successfully: " + bucketName);
                    // Create policies after bucket creation
                    createBucketPolicies(bucketName);
                } else {
                    String error = response.body() != null ? response.body().string() : "Unknown error";
                    if (error.contains("already exists")) {
                        Log.d(TAG, "ℹ️ Bucket already exists: " + bucketName);
                    } else {
                        Log.e(TAG, "❌ Failed to create bucket " + bucketName + ": " + error);
                    }
                }
                response.close();

            } catch (Exception e) {
                Log.e(TAG, "❌ Error creating bucket " + bucketName, e);
            }
        }).start();
    }

    private static void createBucketPolicies(String bucketName) {
        // Note: Creating policies via API requires service_role key
        // These policies should be created manually in Supabase Dashboard
        Log.d(TAG, "ℹ️ Please create the following policies in Supabase Dashboard for bucket: " + bucketName);
        Log.d(TAG, "1. Policy name: 'Give users access to own folder'");
        Log.d(TAG, "   - Allowed operation: SELECT, INSERT, UPDATE, DELETE");
        Log.d(TAG, "   - Target roles: authenticated, anon");
        Log.d(TAG, "   - WITH CHECK expression: true");
        Log.d(TAG, "2. Policy name: 'Public Access'");
        Log.d(TAG, "   - Allowed operation: SELECT");
        Log.d(TAG, "   - Target roles: anon");
        Log.d(TAG, "   - USING expression: true");
    }

    // =====================================
    // LOGGING INTERCEPTOR
    // =====================================

    private static class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            long startTime = System.nanoTime();
            Log.d(TAG, "==> Sending request to: " + request.url());
            Log.d(TAG, "==> Headers: " + request.headers());

            Response response = chain.proceed(request);

            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds

            Log.d(TAG, "<== Received response: " + response.code() + " in " + duration + "ms");
            Log.d(TAG, "<== Response headers: " + response.headers());

            return response;
        }
    }

    // =====================================
    // FILE SIZE UTILS
    // =====================================

    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", sizeInBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", sizeInBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public static long getFileSize(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                byte[] data = readInputStream(inputStream);
                inputStream.close();
                return data.length;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size", e);
        }
        return 0;
    }

    // =====================================
    // VALIDATION METHODS
    // =====================================

    public static boolean isValidAudioFile(Context context, Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            return mimeType != null && mimeType.startsWith("audio/");
        } catch (Exception e) {
            Log.e(TAG, "Error checking audio file", e);
            return false;
        }
    }

    public static boolean isValidImageFile(Context context, Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            return mimeType != null && mimeType.startsWith("image/");
        } catch (Exception e) {
            Log.e(TAG, "Error checking image file", e);
            return false;
        }
    }

    // =====================================
    // BATCH UPLOAD METHODS
    // =====================================

    public static void uploadMultipleFiles(Context context, Uri[] fileUris, String bucket, ProgressCallback callback) {
        new Thread(() -> {
            int totalFiles = fileUris.length;
            int uploadedFiles = 0;
            StringBuilder urls = new StringBuilder();

            for (Uri uri : fileUris) {
                try {
                    // Upload each file
                    final boolean[] uploadComplete = {false};
                    final String[] uploadedUrl = {null};
                    final String[] uploadError = {null};

                    if (bucket.equals(AUDIO_BUCKET)) {
                        uploadAudioFile(context, uri, new UploadCallback() {
                            @Override
                            public void onSuccess(String fileUrl) {
                                uploadedUrl[0] = fileUrl;
                                uploadComplete[0] = true;
                            }

                            @Override
                            public void onError(String error) {
                                uploadError[0] = error;
                                uploadComplete[0] = true;
                            }
                        });
                    } else {
                        uploadImage(context, uri, new UploadCallback() {
                            @Override
                            public void onSuccess(String fileUrl) {
                                uploadedUrl[0] = fileUrl;
                                uploadComplete[0] = true;
                            }

                            @Override
                            public void onError(String error) {
                                uploadError[0] = error;
                                uploadComplete[0] = true;
                            }
                        });
                    }

                    // Wait for upload to complete
                    while (!uploadComplete[0]) {
                        Thread.sleep(100);
                    }

                    if (uploadError[0] != null) {
                        callback.onError("Failed to upload file: " + uploadError[0]);
                        return;
                    }

                    uploadedFiles++;
                    if (urls.length() > 0) urls.append(",");
                    urls.append(uploadedUrl[0]);

                    // Report progress
                    int progress = (uploadedFiles * 100) / totalFiles;
                    callback.onProgress(progress);

                } catch (Exception e) {
                    callback.onError("Upload error: " + e.getMessage());
                    return;
                }
            }

            callback.onSuccess(urls.toString());
        }).start();
    }

    // =====================================
    // TEST CONNECTION
    // =====================================

    public static void testConnection(Context context) {
        new Thread(() -> {
            try {
                // Test bucket list
                String bucketListUrl = SUPABASE_URL + "/storage/v1/bucket";
                Request request = new Request.Builder()
                        .url(bucketListUrl)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .get()
                        .build();

                Response response = httpClient.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "Bucket list response: " + response.code());
                Log.d(TAG, "Buckets: " + responseBody);

                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Test connection error", e);
            }
        }).start();
    }

    // =====================================
    // INITIALIZATION
    // =====================================

    /**
     * Initialize storage helper and create buckets if needed
     * Call this method in your Application class or MainActivity onCreate
     */
    public static void initialize(Context context) {
        Log.d(TAG, "🚀 Initializing SupabaseStorageHelper...");

        // Test connection first
        testConnection(context);

        // Create buckets if they don't exist
        createBucketsIfNotExist();

        Log.d(TAG, "✅ SupabaseStorageHelper initialized");
    }
}