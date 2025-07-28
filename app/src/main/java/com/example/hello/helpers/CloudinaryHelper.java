package com.example.hello.helpers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.hello.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryHelper {
    private static final String TAG = "CloudinaryHelper";
    private static boolean isInitialized = false;

    /**
     * Initialize Cloudinary SDK with your credentials
     * Call this method in your Application class
     */
    public static void initCloudinary(Context context) {
        if (isInitialized) return;
        
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dxcsinlkj");
            config.put("api_key", "385124263985969");
            config.put("api_secret", "qNCINGRonpEURm6UsMQy05vYrXM");
            
            MediaManager.init(context, config);
            isInitialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Upload an image to Cloudinary
     * @param context Context
     * @param imageUri Uri of the image to upload
     * @param folder Folder in Cloudinary to upload to
     * @param callback Callback to handle success/failure
     */
    public static void uploadImage(Context context, Uri imageUri, String folder, final CloudinaryUploadCallback callback) {
        if (!isInitialized) {
            initCloudinary(context);
        }

        String requestId = MediaManager.get().upload(imageUri)
                .option("folder", folder)
                .option("public_id", "user_" + System.currentTimeMillis()) // Generate unique ID
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        double progress = (double) bytes / totalBytes;
                        Log.d(TAG, "Upload progress: " + progress);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        Log.d(TAG, "Upload success: " + imageUrl);
                        callback.onSuccess(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload error: " + error.getDescription());
                        callback.onFailure(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }

    /**
     * Load image from URL into ImageView using Picasso
     */
    public static void loadImage(Context context, String imageUrl, ImageView imageView) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.profile_placeholder) // Create a placeholder image
                    .error(R.drawable.profile_placeholder)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.profile_placeholder);
        }
    }

    public interface CloudinaryUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }
} 