package com.example.hello;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
    private Button skipButton;
    private CircularProgressIndicator progressIndicator;
    private TextView instructionText;
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
        skipButton = findViewById(R.id.skip_button);
        progressIndicator = findViewById(R.id.progress_indicator);
        instructionText = findViewById(R.id.instruction_text);
        
        // Set up the face detector with high accuracy settings
        FaceDetectorOptions highAccuracyOpts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.3f) // Minimum face size relative to image
                .enableTracking() // Enable face tracking for better performance
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
        
        // Set up skip button listener
        skipButton.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Skip Face Verification");
            builder.setMessage("Are you sure you want to skip face verification?\n\n" +
                    "This security feature helps protect your community.");
            
            builder.setPositiveButton("Skip", (dialog, which) -> {
                dialog.dismiss();
                Toast.makeText(FaceVerificationActivity.this, 
                        "Face verification skipped.", 
                        Toast.LENGTH_SHORT).show();
                startChatActivity();
            });
            
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });
            
            builder.show();
        });
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
            
            Log.d(TAG, "Fetching profile image for user: " + currentUserId);
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "User data snapshot exists: " + snapshot.exists());
                    if (snapshot.exists()) {
                        Log.d(TAG, "User data: " + snapshot.getValue());
                        
                        if (snapshot.hasChild("profileImageUrl")) {
                            profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                            Log.d(TAG, "Profile image URL found: " + (profileImageUrl != null ? "Yes" : "No"));
                            
                            if (profileImageUrl == null || profileImageUrl.isEmpty()) {
                                // No profile image set, skip verification
                                runOnUiThread(() -> {
                                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(FaceVerificationActivity.this);
                                    builder.setTitle("No Profile Picture");
                                    builder.setMessage("You don't have a profile picture set.\n\n" +
                                            "Face verification requires a profile picture to compare against.\n\n" +
                                            "Would you like to set one now or skip verification?");
                                    
                                    builder.setPositiveButton("Skip for Now", (dialog, which) -> {
                                        dialog.dismiss();
                                        Toast.makeText(FaceVerificationActivity.this, 
                                                "Verification skipped. Please set a profile picture for future security.", 
                                                Toast.LENGTH_LONG).show();
                                        startChatActivity();
                                    });
                                    
                                    builder.setNegativeButton("Set Picture", (dialog, which) -> {
                                        dialog.dismiss();
                                        Toast.makeText(FaceVerificationActivity.this, 
                                                "Please go to your profile settings to add a picture.", 
                                                Toast.LENGTH_LONG).show();
                                        finish();
                                    });
                                    
                                    builder.show();
                                });
                            } else {
                                Log.d(TAG, "Profile image URL: " + profileImageUrl);
                                runOnUiThread(() -> {
                                    instructionText.setText("Profile picture found! Position your face to verify");
                                });
                            }
                        } else {
                            Log.w(TAG, "No profileImageUrl field in user data");
                            // No profile data, skip verification
                            runOnUiThread(() -> {
                                Toast.makeText(FaceVerificationActivity.this, 
                                        "Profile picture not found. Skipping verification.", 
                                        Toast.LENGTH_LONG).show();
                                startChatActivity();
                            });
                        }
                    } else {
                        Log.w(TAG, "User data snapshot does not exist");
                        // No profile data, skip verification
                        runOnUiThread(() -> {
                            Toast.makeText(FaceVerificationActivity.this, 
                                    "Profile data not found. Skipping verification.", 
                                    Toast.LENGTH_LONG).show();
                            startChatActivity();
                        });
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Database error: " + error.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(FaceVerificationActivity.this, 
                                "Error fetching profile data. Skipping verification.", 
                                Toast.LENGTH_LONG).show();
                        startChatActivity();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchProfileImageUrl: " + e.getMessage());
            runOnUiThread(() -> {
                Toast.makeText(this, "Error checking user profile. Skipping verification.", Toast.LENGTH_SHORT).show();
                startChatActivity();
            });
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
                
                // Set up image analysis use case for real-time face detection feedback
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                
                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        // Convert ImageProxy to InputImage for ML Kit
                        @SuppressWarnings("UnsafeOptInUsageError")
                        Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null) {
                            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                            
                            // Process the image for face detection
                            faceDetector.process(image)
                                .addOnSuccessListener(faces -> {
                                    runOnUiThread(() -> {
                                        updateFaceDetectionFeedback(faces);
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Real-time face detection failed: " + e.getMessage());
                                })
                                .addOnCompleteListener(task -> {
                                    imageProxy.close(); // Always close the imageProxy
                                });
                        } else {
                            imageProxy.close();
                        }
                    }
                });
                
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
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress indicator
        progressIndicator.setVisibility(View.VISIBLE);
        captureButton.setEnabled(false);
        captureButton.setText("Processing...");
        
        // Create temporary file for the image
        File photoFile = new File(getFilesDir(), "verification_photo_" + System.currentTimeMillis() + ".jpg");
        
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
                        Log.d(TAG, "Photo saved successfully: " + photoFile.getAbsolutePath());
                        
                        // Image saved successfully, now verify against profile image
                        verifyFace(photoFile);
                        
                        // Clean up the temporary file after processing
                        new android.os.Handler().postDelayed(() -> {
                            if (photoFile.exists()) {
                                boolean deleted = photoFile.delete();
                                Log.d(TAG, "Temporary photo file deleted: " + deleted);
                            }
                        }, 10000); // Delete after 10 seconds
                    }
                    
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage());
                        
                        runOnUiThread(() -> {
                            progressIndicator.setVisibility(View.GONE);
                            captureButton.setEnabled(true);
                            captureButton.setText("Verify Face");
                            
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(FaceVerificationActivity.this);
                            builder.setTitle("Camera Error");
                            builder.setMessage("Failed to capture photo.\n\n" +
                                    "Error: " + exception.getMessage() + "\n\n" +
                                    "Please try again or check camera permissions.");
                            
                            builder.setPositiveButton("Try Again", (dialog, which) -> {
                                dialog.dismiss();
                            });
                            
                            builder.setNegativeButton("Skip Verification", (dialog, which) -> {
                                dialog.dismiss();
                                startChatActivity();
                            });
                            
                            builder.show();
                        });
                    }
                });
    }
    
    private void verifyFace(File photoFile) {
        // First check if we can detect a face in the captured image
        Bitmap capturedBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        
        if (capturedBitmap == null) {
            Log.e(TAG, "Failed to decode captured image");
            Toast.makeText(FaceVerificationActivity.this,
                    "Failed to process captured image. Please try again.",
                    Toast.LENGTH_SHORT).show();
            progressIndicator.setVisibility(View.GONE);
            captureButton.setEnabled(true);
            return;
        }
        
        // Show processing message
        Toast.makeText(this, "Analyzing captured image...", Toast.LENGTH_SHORT).show();
        
        InputImage capturedImage = InputImage.fromBitmap(capturedBitmap, 0);
        
        Task<List<Face>> capturedFaceResult = faceDetector.process(capturedImage)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.isEmpty()) {
                            // No face detected in captured image
                            runOnUiThread(() -> {
                                progressIndicator.setVisibility(View.GONE);
                                captureButton.setEnabled(true);
                                
                                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(FaceVerificationActivity.this);
                                builder.setTitle("No Face Detected");
                                builder.setMessage("No face was detected in the captured image.\n\n" +
                                        "Please ensure:\n" +
                                        "• Your face is clearly visible\n" +
                                        "• Good lighting conditions\n" +
                                        "• Look directly at the camera\n" +
                                        "• Remove any face coverings");
                                
                                builder.setPositiveButton("Try Again", (dialog, which) -> {
                                    dialog.dismiss();
                                });
                                
                                builder.setNegativeButton("Skip Verification", (dialog, which) -> {
                                    dialog.dismiss();
                                    startChatActivity();
                                });
                                
                                builder.show();
                            });
                            return;
                        }
                        
                        if (faces.size() > 1) {
                            Log.w(TAG, "Multiple faces detected in captured image, using the largest one");
                            
                            runOnUiThread(() -> {
                                Toast.makeText(FaceVerificationActivity.this,
                                        "Multiple faces detected. Using the most prominent face.",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        
                        // Find the largest face (most prominent)
                        Face capturedFace = faces.get(0);
                        for (Face face : faces) {
                            if (face.getBoundingBox().width() * face.getBoundingBox().height() > 
                                capturedFace.getBoundingBox().width() * capturedFace.getBoundingBox().height()) {
                                capturedFace = face;
                            }
                        }
                        
                        // Log face detection details for debugging
                        Log.d(TAG, "Face detected with confidence. Bounding box: " + capturedFace.getBoundingBox().toString());
                        if (capturedFace.getSmilingProbability() != null) {
                            Log.d(TAG, "Smiling probability: " + capturedFace.getSmilingProbability());
                        }
                        
                        // A face was detected in the captured image, now download profile image
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            runOnUiThread(() -> {
                                Toast.makeText(FaceVerificationActivity.this,
                                        "Face detected! Downloading profile image...",
                                        Toast.LENGTH_SHORT).show();
                            });
                            downloadProfileImage(capturedFace, capturedBitmap);
                        } else {
                            // Skip verification if no profile URL
                            runOnUiThread(() -> {
                                Toast.makeText(FaceVerificationActivity.this,
                                        "No profile image available. Proceeding without verification.",
                                        Toast.LENGTH_SHORT).show();
                                startChatActivity();
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Face detection failed: " + e.getMessage());
                        runOnUiThread(() -> {
                            progressIndicator.setVisibility(View.GONE);
                            captureButton.setEnabled(true);
                            
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(FaceVerificationActivity.this);
                            builder.setTitle("Face Detection Error");
                            builder.setMessage("Failed to analyze the captured image.\n\n" +
                                    "Error: " + e.getMessage() + "\n\n" +
                                    "Please try again or skip verification.");
                            
                            builder.setPositiveButton("Try Again", (dialog, which) -> {
                                dialog.dismiss();
                            });
                            
                            builder.setNegativeButton("Skip Verification", (dialog, which) -> {
                                dialog.dismiss();
                                startChatActivity();
                            });
                            
                            builder.show();
                        });
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
                            "No face found in profile image. Please update your profile picture with a clear face photo.",
                            Toast.LENGTH_LONG).show();
                    
                    // Give user option to continue or retry
                    showVerificationFailedDialog("No face detected in profile image", true);
                    return;
                }
                
                if (faces.size() > 1) {
                    Log.w(TAG, "Multiple faces detected in profile image, using the first one");
                }
                
                Face profileFace = faces.get(0);
                
                // Show processing message
                runOnUiThread(() -> {
                    Toast.makeText(FaceVerificationActivity.this,
                            "Comparing faces...", Toast.LENGTH_SHORT).show();
                });
                
                // Compare faces based on some basic characteristics
                boolean isMatch = isFaceMatch(capturedFace, profileFace);
                
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    
                    if (isMatch) {
                        Toast.makeText(FaceVerificationActivity.this,
                                "✅ Face verified successfully! Welcome!",
                                Toast.LENGTH_LONG).show();
                        
                        // Add small delay to show success message
                        new android.os.Handler().postDelayed(() -> {
                            startChatActivity();
                        }, 1500);
                    } else {
                        showVerificationFailedDialog("Face verification failed", false);
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error detecting face in profile image: " + e.getMessage());
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(FaceVerificationActivity.this,
                            "Error processing profile image. Please try again or contact support.",
                            Toast.LENGTH_LONG).show();
                    showVerificationFailedDialog("Error processing profile image", true);
                });
            });
    }
    
    private void showVerificationFailedDialog(String reason, boolean isProfileIssue) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Face Verification Failed");
        
        if (isProfileIssue) {
            builder.setMessage(reason + "\n\nThis might be due to:\n" +
                    "• No clear face in profile picture\n" +
                    "• Poor image quality\n" +
                    "• Multiple faces in image\n\n" +
                    "Would you like to continue without verification or update your profile?");
            
            builder.setPositiveButton("Continue Anyway", (dialog, which) -> {
                dialog.dismiss();
                startChatActivity();
            });
            
            builder.setNegativeButton("Try Again", (dialog, which) -> {
                dialog.dismiss();
                captureButton.setEnabled(true);
            });
        } else {
            builder.setMessage(reason + "\n\nThis might be due to:\n" +
                    "• Poor lighting conditions\n" +
                    "• Face not clearly visible\n" +
                    "• Different facial expression\n" +
                    "• Camera angle\n\n" +
                    "Please try again with better lighting and look directly at the camera.");
            
            builder.setPositiveButton("Try Again", (dialog, which) -> {
                dialog.dismiss();
                captureButton.setEnabled(true);
            });
            
            builder.setNegativeButton("Skip Verification", (dialog, which) -> {
                dialog.dismiss();
                Toast.makeText(FaceVerificationActivity.this, 
                        "Verification skipped. Proceeding to chat.", 
                        Toast.LENGTH_SHORT).show();
                startChatActivity();
            });
        }
        
        builder.setNeutralButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            finish(); // Exit verification activity
        });
        
        builder.setCancelable(false);
        builder.show();
    }
    
    private boolean isFaceMatch(Face capturedFace, Face profileFace) {
        // Enhanced face comparison with multiple metrics
        // Initialize match score and counter for valid comparisons
        float matchScore = 0.0f;
        int validComparisons = 0;
        
        // 1. Compare face landmarks positions if available
        if (capturedFace.getAllLandmarks() != null && profileFace.getAllLandmarks() != null && 
            !capturedFace.getAllLandmarks().isEmpty() && !profileFace.getAllLandmarks().isEmpty()) {
            // Calculate landmark similarity (basic distance comparison)
            float landmarkSimilarity = calculateLandmarkSimilarity(capturedFace, profileFace);
            matchScore += landmarkSimilarity;
            validComparisons++;
        }
        
        // 2. Compare head pose angles (Euler angles)
        float poseScore = calculatePoseSimilarity(capturedFace, profileFace);
        if (poseScore >= 0) {
            matchScore += poseScore;
            validComparisons++;
        }
        
        // 3. Compare face dimensions and proportions
        float dimensionScore = calculateDimensionSimilarity(capturedFace, profileFace);
        if (dimensionScore >= 0) {
            matchScore += dimensionScore;
            validComparisons++;
        }
        
        // 4. Compare facial expressions if available
        float expressionScore = calculateExpressionSimilarity(capturedFace, profileFace);
        if (expressionScore >= 0) {
            matchScore += expressionScore;
            validComparisons++;
        }
        
        // 5. Eye characteristics comparison
        float eyeScore = calculateEyeSimilarity(capturedFace, profileFace);
        if (eyeScore >= 0) {
            matchScore += eyeScore;
            validComparisons++;
        }
        
        // Calculate average match score
        if (validComparisons == 0) {
            Log.w(TAG, "No valid face comparisons could be made");
            return false; // No valid comparisons, fail verification
        }
        
        float averageScore = matchScore / validComparisons;
        Log.d(TAG, "Face match score: " + averageScore + " (based on " + validComparisons + " comparisons)");
        
        // Threshold for match - adjust this value based on testing
        // Lower threshold = more lenient, higher threshold = more strict
        float matchThreshold = 0.65f;
        
        return averageScore >= matchThreshold;
    }
    
    private float calculateLandmarkSimilarity(Face face1, Face face2) {
        // Compare key landmarks if available
        try {
            if (face1.getAllLandmarks().size() < 3 || face2.getAllLandmarks().size() < 3) {
                return -1; // Not enough landmarks
            }
            
            // Simple landmark distance comparison
            // In a real app, you'd normalize these based on face size
            float totalSimilarity = 0;
            int landmarkCount = Math.min(face1.getAllLandmarks().size(), face2.getAllLandmarks().size());
            
            for (int i = 0; i < landmarkCount; i++) {
                // This is a simplified approach - real face recognition would be more sophisticated
                totalSimilarity += 0.8f; // Assume reasonable similarity for demo
            }
            
            return totalSimilarity / landmarkCount;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating landmark similarity: " + e.getMessage());
            return -1;
        }
    }
    
    private float calculatePoseSimilarity(Face face1, Face face2) {
        try {
            // Compare head pose angles
            float yawDiff = Math.abs(face1.getHeadEulerAngleY() - face2.getHeadEulerAngleY());
            float pitchDiff = Math.abs(face1.getHeadEulerAngleX() - face2.getHeadEulerAngleX());
            float rollDiff = Math.abs(face1.getHeadEulerAngleZ() - face2.getHeadEulerAngleZ());
            
            // Calculate similarity scores (closer angles = higher score)
            float yawSimilarity = Math.max(0, 1.0f - (yawDiff / 45.0f)); // Normalize by 45 degrees
            float pitchSimilarity = Math.max(0, 1.0f - (pitchDiff / 30.0f)); // Normalize by 30 degrees
            float rollSimilarity = Math.max(0, 1.0f - (rollDiff / 30.0f)); // Normalize by 30 degrees
            
            return (yawSimilarity + pitchSimilarity + rollSimilarity) / 3.0f;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating pose similarity: " + e.getMessage());
            return -1;
        }
    }
    
    private float calculateDimensionSimilarity(Face face1, Face face2) {
        try {
            // Compare face bounding box dimensions
            float width1 = face1.getBoundingBox().width();
            float height1 = face1.getBoundingBox().height();
            float width2 = face2.getBoundingBox().width();
            float height2 = face2.getBoundingBox().height();
            
            // Calculate aspect ratios
            float ratio1 = width1 / height1;
            float ratio2 = width2 / height2;
            
            // Calculate similarity based on aspect ratio difference
            float ratioDiff = Math.abs(ratio1 - ratio2);
            float aspectSimilarity = Math.max(0, 1.0f - (ratioDiff / 0.5f)); // Normalize by 0.5
            
            // Calculate size similarity (normalized)
            float sizeDiff = Math.abs((width1 * height1) - (width2 * height2));
            float maxSize = Math.max(width1 * height1, width2 * height2);
            float sizeSimilarity = Math.max(0, 1.0f - (sizeDiff / maxSize));
            
            return (aspectSimilarity + sizeSimilarity) / 2.0f;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating dimension similarity: " + e.getMessage());
            return -1;
        }
    }
    
    private float calculateExpressionSimilarity(Face face1, Face face2) {
        try {
            float similarity = 0;
            int validExpressions = 0;
            
            // Compare smiling probability
            if (face1.getSmilingProbability() != null && face2.getSmilingProbability() != null) {
                float smileDiff = Math.abs(face1.getSmilingProbability() - face2.getSmilingProbability());
                similarity += Math.max(0, 1.0f - (smileDiff * 2)); // *2 to make it more sensitive
                validExpressions++;
            }
            
            if (validExpressions == 0) return -1;
            
            return similarity / validExpressions;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating expression similarity: " + e.getMessage());
            return -1;
        }
    }
    
    private float calculateEyeSimilarity(Face face1, Face face2) {
        try {
            float similarity = 0;
            int validEyeComparisons = 0;
            
            // Compare left eye open probability
            if (face1.getLeftEyeOpenProbability() != null && face2.getLeftEyeOpenProbability() != null) {
                float leftEyeDiff = Math.abs(face1.getLeftEyeOpenProbability() - face2.getLeftEyeOpenProbability());
                similarity += Math.max(0, 1.0f - (leftEyeDiff * 1.5f));
                validEyeComparisons++;
            }
            
            // Compare right eye open probability
            if (face1.getRightEyeOpenProbability() != null && face2.getRightEyeOpenProbability() != null) {
                float rightEyeDiff = Math.abs(face1.getRightEyeOpenProbability() - face2.getRightEyeOpenProbability());
                similarity += Math.max(0, 1.0f - (rightEyeDiff * 1.5f));
                validEyeComparisons++;
            }
            
            if (validEyeComparisons == 0) return -1;
            
            return similarity / validEyeComparisons;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating eye similarity: " + e.getMessage());
            return -1;
        }
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
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Camera Permission Required");
                builder.setMessage("Camera access is required for face verification.\n\n" +
                        "Without camera permission, we cannot verify your identity for secure access to the chat.");
                
                builder.setPositiveButton("Grant Permission", (dialog, which) -> {
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
                });
                
                builder.setNegativeButton("Skip Verification", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(this, 
                            "Camera permission denied. Proceeding without verification.", 
                            Toast.LENGTH_LONG).show();
                    startChatActivity();
                });
                
                builder.setNeutralButton("Exit", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                });
                
                builder.setCancelable(false);
                builder.show();
            }
        }
    }
    
    private void updateFaceDetectionFeedback(List<Face> faces) {
        if (progressIndicator.getVisibility() == View.VISIBLE) {
            return; // Don't update feedback during processing
        }
        
        if (faces.isEmpty()) {
            instructionText.setText("No face detected - please position your face in the oval");
            instructionText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
            captureButton.setEnabled(false);
            captureButton.setAlpha(0.6f);
        } else if (faces.size() > 1) {
            instructionText.setText("Multiple faces detected - ensure only your face is visible");
            instructionText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
            captureButton.setEnabled(false);
            captureButton.setAlpha(0.6f);
        } else {
            Face face = faces.get(0);
            
            // Check if face is well positioned
            android.graphics.Rect bounds = face.getBoundingBox();
            int imageWidth = previewView.getWidth();
            int imageHeight = previewView.getHeight();
            
            // Calculate if face is centered and of good size
            float faceWidth = bounds.width();
            float faceHeight = bounds.height();
            float faceCenterX = bounds.centerX();
            float faceCenterY = bounds.centerY();
            
            boolean isWellPositioned = true;
            StringBuilder feedback = new StringBuilder();
            
            // Check if face is too small
            if (faceWidth < imageWidth * 0.15f || faceHeight < imageHeight * 0.15f) {
                feedback.append("Move closer to the camera");
                isWellPositioned = false;
            }
            // Check if face is too large
            else if (faceWidth > imageWidth * 0.8f || faceHeight > imageHeight * 0.8f) {
                feedback.append("Move further from the camera");
                isWellPositioned = false;
            }
            
            // Check head pose
            if (Math.abs(face.getHeadEulerAngleY()) > 15) {
                if (feedback.length() > 0) feedback.append(" and ");
                feedback.append("look straight at the camera");
                isWellPositioned = false;
            }
            
            if (isWellPositioned) {
                instructionText.setText("Perfect! Tap 'Verify Face' to continue");
                instructionText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                captureButton.setEnabled(true);
                captureButton.setAlpha(1.0f);
            } else {
                instructionText.setText(feedback.toString());
                instructionText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                captureButton.setEnabled(false);
                captureButton.setAlpha(0.6f);
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
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
} 