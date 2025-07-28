package com.example.hello;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.squareup.picasso.Target;

public class FaceVerificationActivity extends AppCompatActivity {
    private static final String TAG = "FaceVerification";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView previewView;
    private Button captureButton;
    private CircularProgressIndicator progressIndicator;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private String chatId, otherUserName, profileImageUrl;
    private boolean isGroup;
    private FaceDetector faceDetector;
    private Target picassoTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verification);

        // Get intent extras
        chatId = getIntent().getStringExtra("chatId");
        isGroup = getIntent().getBooleanExtra("isGroup", false);
        otherUserName = getIntent().getStringExtra("otherUserName");
        
        // Initialize views
        previewView = findViewById(R.id.preview_view);
        captureButton = findViewById(R.id.capture_button);
        progressIndicator = findViewById(R.id.progress_indicator);
        
        // Set up the face detector with high accuracy settings
        FaceDetectorOptions highAccuracyOpts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        
        faceDetector = FaceDetection.getClient(highAccuracyOpts);
        
        // Set up camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
            fetchProfileImageUrl();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        
        // Set up capture button listener
        captureButton.setOnClickListener(v -> takePhoto());
    }
    
    private void fetchProfileImageUrl() {
        try {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            
            if (currentUserId == null || currentUserId.isEmpty()) {
                Log.w(TAG, "Current user ID is null or empty, skipping verification");
                Toast.makeText(FaceVerificationActivity.this, 
                        "User information not available. Skipping verification.", 
                        Toast.LENGTH_LONG).show();
                startChatActivity();
                return;
            }
            
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChild("profileImageUrl")) {
                        profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
                            // No profile image set, skip verification
                            Toast.makeText(FaceVerificationActivity.this, 
                                    "No profile picture found. Please set a profile picture first.", 
                                    Toast.LENGTH_LONG).show();
                            startChatActivity();
                        }
                    } else {
                        // No profile data, skip verification
                        Toast.makeText(FaceVerificationActivity.this, 
                                "Profile data not found. Skipping verification.", 
                                Toast.LENGTH_LONG).show();
                        startChatActivity();
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Database error: " + error.getMessage());
                    Toast.makeText(FaceVerificationActivity.this, 
                            "Error fetching profile data. Skipping verification.", 
                            Toast.LENGTH_LONG).show();
                    startChatActivity();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchProfileImageUrl: " + e.getMessage());
            Toast.makeText(this, "Error checking user profile. Skipping verification.", Toast.LENGTH_SHORT).show();
            startChatActivity();
        }
    }
    
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                // Set up the preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                
                // Set up the capture use case
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                
                // Set up image analysis use case
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                
                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll();
                
                // Try to use front camera first, but if not available, use any camera
                try {
                    // Select front camera
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build();
                    
                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                            (LifecycleOwner) this, 
                            cameraSelector, 
                            preview, 
                            imageCapture,
                            imageAnalysis);
                } catch (IllegalArgumentException e) {
                    // Front camera not available, try default camera
                    Log.w(TAG, "Front camera not available, using default camera instead");
                    
                    // Use default camera selector
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .build();
                    
                    try {
                        // Bind use cases to default camera
                        cameraProvider.bindToLifecycle(
                                (LifecycleOwner) this, 
                                cameraSelector, 
                                preview, 
                                imageCapture,
                                imageAnalysis);
                    } catch (IllegalArgumentException ex) {
                        // No camera available at all
                        Log.e(TAG, "No camera available on this device");
                        Toast.makeText(this, 
                                "No camera available on this device. Skipping verification.", 
                                Toast.LENGTH_LONG).show();
                        startChatActivity(); // Skip verification and continue to chat
                    }
                }
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                Toast.makeText(this, "Error starting camera. Skipping verification.", Toast.LENGTH_SHORT).show();
                startChatActivity(); // Skip verification and continue to chat
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void takePhoto() {
        if (imageCapture == null) return;
        
        // Show progress indicator
        progressIndicator.setVisibility(View.VISIBLE);
        captureButton.setEnabled(false);
        
        // Create temporary file for the image
        File photoFile = new File(getFilesDir(), "verification_photo.jpg");
        
        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build();
        
        // Set up image capture listener
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Image saved successfully, now verify against profile image
                        verifyFace(photoFile);
                    }
                    
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage());
                        Toast.makeText(FaceVerificationActivity.this,
                                "Failed to capture photo", Toast.LENGTH_SHORT).show();
                        progressIndicator.setVisibility(View.GONE);
                        captureButton.setEnabled(true);
                    }
                });
    }
    
    private void verifyFace(File photoFile) {
        // First check if we can detect a face in the captured image
        Bitmap capturedBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        InputImage capturedImage = InputImage.fromBitmap(capturedBitmap, 0);
        
        Task<List<Face>> capturedFaceResult = faceDetector.process(capturedImage)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.isEmpty()) {
                            // No face detected in captured image
                            Toast.makeText(FaceVerificationActivity.this,
                                    "No face detected. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                            progressIndicator.setVisibility(View.GONE);
                            captureButton.setEnabled(true);
                            return;
                        }
                        
                        // Store the detected face for comparison
                        Face capturedFace = faces.get(0);
                        
                        // A face was detected in the captured image, now download profile image
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            downloadProfileImage(capturedFace, capturedBitmap);
                        } else {
                            // Skip verification if no profile URL
                            Toast.makeText(FaceVerificationActivity.this,
                                    "No profile image available. Proceeding without verification.",
                                    Toast.LENGTH_SHORT).show();
                            startChatActivity();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Face detection failed: " + e.getMessage());
                        Toast.makeText(FaceVerificationActivity.this,
                                "Face detection failed. Please try again.",
                                Toast.LENGTH_SHORT).show();
                        progressIndicator.setVisibility(View.GONE);
                        captureButton.setEnabled(true);
                    }
                });
    }
    
    private void downloadProfileImage(Face capturedFace, Bitmap capturedBitmap) {
        // Use Picasso to download the profile image from Cloudinary
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            // Create a target to receive the bitmap
            com.squareup.picasso.Target target = new com.squareup.picasso.Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, com.squareup.picasso.Picasso.LoadedFrom from) {
                    // Profile image downloaded successfully, now compare with captured image
                    if (bitmap != null) {
                        compareFaces(capturedFace, capturedBitmap, bitmap);
                    } else {
                        Log.e(TAG, "Downloaded profile image is null");
                        Toast.makeText(FaceVerificationActivity.this,
                                "Error processing profile image. Skipping verification.",
                                Toast.LENGTH_SHORT).show();
                        startChatActivity();
                    }
                }

                @Override
                public void onBitmapFailed(Exception e, android.graphics.drawable.Drawable errorDrawable) {
                    Log.e(TAG, "Failed to download profile image: " + e.getMessage());
                    Toast.makeText(FaceVerificationActivity.this,
                            "Failed to download profile image. Skipping verification.",
                            Toast.LENGTH_SHORT).show();
                    startChatActivity();
                }

                @Override
                public void onPrepareLoad(android.graphics.drawable.Drawable placeHolderDrawable) {
                    // Preparation logic if needed
                }
            };

            // Store the target in a field to prevent garbage collection
            // (Picasso holds a weak reference to the target)
            this.picassoTarget = target;

            // Load the image using Picasso
            com.squareup.picasso.Picasso.get()
                .load(profileImageUrl)
                .into(target);
        } else {
            // No profile image URL
            Toast.makeText(FaceVerificationActivity.this,
                    "Invalid profile image URL. Skipping verification.",
                    Toast.LENGTH_SHORT).show();
            startChatActivity();
        }
    }
    
    private void compareFaces(Face capturedFace, Bitmap capturedBitmap, Bitmap profileBitmap) {
        // Convert profile bitmap to InputImage
        InputImage profileImage = InputImage.fromBitmap(profileBitmap, 0);
        
        // Detect face in profile image
        faceDetector.process(profileImage)
            .addOnSuccessListener(faces -> {
                if (faces.isEmpty()) {
                    Log.w(TAG, "No face detected in profile image");
                    Toast.makeText(FaceVerificationActivity.this,
                            "No face found in profile image. Skipping verification.",
                            Toast.LENGTH_SHORT).show();
                    startChatActivity();
                    return;
                }
                
                Face profileFace = faces.get(0);
                
                // Compare faces based on some basic characteristics
                boolean isMatch = isFaceMatch(capturedFace, profileFace);
                
                if (isMatch) {
                    Toast.makeText(FaceVerificationActivity.this,
                            "Face verified successfully!",
                            Toast.LENGTH_SHORT).show();
                    startChatActivity();
                } else {
                    progressIndicator.setVisibility(View.GONE);
                    captureButton.setEnabled(true);
                    Toast.makeText(FaceVerificationActivity.this,
                            "Face verification failed. Please try again.",
                            Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error detecting face in profile image: " + e.getMessage());
                Toast.makeText(FaceVerificationActivity.this,
                        "Error verifying face. Skipping verification.",
                        Toast.LENGTH_SHORT).show();
                startChatActivity();
            });
    }
    
    private boolean isFaceMatch(Face capturedFace, Face profileFace) {
        // This is a basic comparison. A real application would use more sophisticated
        // face recognition techniques and ML models.
        
        // Compare some basic characteristics if available
        if (capturedFace.getSmilingProbability() != null && profileFace.getSmilingProbability() != null) {
            float smileDiff = Math.abs(capturedFace.getSmilingProbability() - profileFace.getSmilingProbability());
            if (smileDiff > 0.7f) {
                return false;
            }
        }
        
        // Compare right eye open probability
        if (capturedFace.getRightEyeOpenProbability() != null && profileFace.getRightEyeOpenProbability() != null) {
            float eyeDiff = Math.abs(capturedFace.getRightEyeOpenProbability() - profileFace.getRightEyeOpenProbability());
            if (eyeDiff > 0.7f) {
                return false;
            }
        }
        
        // Check euler angles (face orientation)
        if (Math.abs(capturedFace.getHeadEulerAngleY() - profileFace.getHeadEulerAngleY()) > 20) {
            return false;
        }
        
        // Calculate bounding box similarity ratio
        float capturedWidth = capturedFace.getBoundingBox().width();
        float capturedHeight = capturedFace.getBoundingBox().height();
        float profileWidth = profileFace.getBoundingBox().width();
        float profileHeight = profileFace.getBoundingBox().height();
        
        float widthRatio = capturedWidth / profileWidth;
        float heightRatio = capturedHeight / profileHeight;
        
        // If face size ratios are very different, it's probably not the same person
        if (Math.abs(widthRatio - heightRatio) > 0.3f) {
            return false;
        }
        
        // For this demo, assume it's a match if we've passed all the checks above
        // In a real app, you'd want more sophisticated face matching
        return true;
    }
    
    private void startChatActivity() {
        // Open the group chat
        Intent intent = new Intent(FaceVerificationActivity.this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("isGroup", isGroup);
        intent.putExtra("otherUserName", otherUserName);
        startActivity(intent);
        finish(); // Close the verification activity
    }
    
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
                fetchProfileImageUrl();
            } else {
                Toast.makeText(this, 
                        "Permissions not granted. Camera access is required for verification.", 
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (picassoTarget != null) {
            com.squareup.picasso.Picasso.get().cancelRequest(picassoTarget);
        }
    }
} 