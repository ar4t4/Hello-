package com.example.hello.utils;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.hello.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Special activity specifically for fixing the join request mentioned by the user
 */
public class FixSpecificJoinRequestActivity extends AppCompatActivity {
    
    private static final String TAG = "FixSpecificRequest";
    
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView userDataText;
    private ImageView profileImageView;
    private Button fixButton;
    private Button directUpdateButton;
    
    // The specific data from the user's message
    private static final String REQUEST_ID = "-OP3KdhmaPKVGnOvSGiS";
    private static final String USER_ID = "qTgC3NdvRtVB2pBE5Ryx71zyzvn2";
    
    private String retrievedUserName = "";
    private String retrievedProfileUrl = "";
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fix_specific_request);
        
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.tv_status);
        userDataText = findViewById(R.id.tv_user_data);
        fixButton = findViewById(R.id.btn_fix);
        directUpdateButton = findViewById(R.id.btn_direct_update);
        
        profileImageView = findViewById(R.id.img_profile);
        if (profileImageView == null) {
            // If the layout doesn't have an image view yet, we'll just log it
            Log.w(TAG, "Profile image view not found in layout");
        }
        
        // Display request info
        TextView requestInfoText = findViewById(R.id.tv_request_info);
        requestInfoText.setText("Request ID: " + REQUEST_ID + "\nUser ID: " + USER_ID);
        
        // Set up click listeners
        fixButton.setOnClickListener(v -> directUpdateWithRealtimeData());
        directUpdateButton.setOnClickListener(v -> directUpdateWithRealtimeData());
        
        // Immediately retrieve user data from Realtime Database
        retrieveUserDataFromRealtime();
    }
    
    private void retrieveUserDataFromRealtime() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Retrieving user data from Realtime Database...");
        
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(USER_ID);
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Found user data in Realtime Database: " + snapshot.getValue());
                    
                    StringBuilder userData = new StringBuilder("User Data from Realtime Database:\n\n");
                    
                    // Check for first name and last name
                    String firstName = snapshot.child("firstName").exists() ? 
                            snapshot.child("firstName").getValue(String.class) : null;
                    String lastName = snapshot.child("lastName").exists() ? 
                            snapshot.child("lastName").getValue(String.class) : null;
                    
                    // Check for profile image
                    String profileImageUrl = null;
                    if (snapshot.child("profileImageUrl").exists()) {
                        profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    } else if (snapshot.child("photoUrl").exists()) {
                        profileImageUrl = snapshot.child("photoUrl").getValue(String.class);
                    } else if (snapshot.child("imageUrl").exists()) {
                        profileImageUrl = snapshot.child("imageUrl").getValue(String.class);
                    }
                    
                    // Try to build a username and save it for update
                    if (firstName != null && lastName != null) {
                        retrievedUserName = firstName + " " + lastName;
                    } else if (snapshot.child("name").exists()) {
                        retrievedUserName = snapshot.child("name").getValue(String.class);
                    } else if (snapshot.child("displayName").exists()) {
                        retrievedUserName = snapshot.child("displayName").getValue(String.class);
                    } else if (snapshot.child("username").exists()) {
                        retrievedUserName = snapshot.child("username").getValue(String.class);
                    }
                    
                    // Save profile URL
                    retrievedProfileUrl = profileImageUrl;
                    
                    // Add all data to our display
                    for (DataSnapshot child : snapshot.getChildren()) {
                        userData.append("- ").append(child.getKey()).append(": ")
                                .append(child.getValue()).append("\n");
                    }
                    
                    // Update UI
                    userDataText.setText(userData.toString());
                    statusText.setText("User data retrieved. Click 'Update Join Request' to fix.");
                    
                    // Load profile image if available
                    if (profileImageView != null && profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(FixSpecificJoinRequestActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profileImageView);
                    }
                    
                } else {
                    userDataText.setText("No user data found in Realtime Database.\nUsing Asif Rahman data from previous message.");
                    statusText.setText("Using hardcoded data as fallback.");
                    
                    // Fallback to hardcoded values from user message
                    retrievedUserName = "Asif Rahman";
                    retrievedProfileUrl = "https://res.cloudinary.com/dxcsinlkj/image/upload/v1745976961/users/user_1745976955151.jpg";
                }
                
                progressBar.setVisibility(View.GONE);
                fixButton.setEnabled(true);
                directUpdateButton.setEnabled(true);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                userDataText.setText("Error retrieving user data: " + error.getMessage() + 
                        "\n\nUsing Asif Rahman data from previous message as fallback.");
                
                // Fallback to hardcoded values
                retrievedUserName = "Asif Rahman";
                retrievedProfileUrl = "https://res.cloudinary.com/dxcsinlkj/image/upload/v1745976961/users/user_1745976955151.jpg";
                
                progressBar.setVisibility(View.GONE);
                statusText.setText("Using hardcoded data as fallback.");
                fixButton.setEnabled(true);
                directUpdateButton.setEnabled(true);
            }
        });
    }
    
    private void directUpdateWithRealtimeData() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Updating join request with retrieved user data...");
        fixButton.setEnabled(false);
        directUpdateButton.setEnabled(false);
        
        // Make sure we have data to use (either from Realtime DB or fallback)
        if (retrievedUserName.isEmpty()) {
            retrievedUserName = "Asif Rahman"; // Final fallback
        }
        
        if (retrievedProfileUrl == null || retrievedProfileUrl.isEmpty()) {
            retrievedProfileUrl = "https://res.cloudinary.com/dxcsinlkj/image/upload/v1745976961/users/user_1745976955151.jpg";
        }
        
        // Create the update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("userName", retrievedUserName);
        updates.put("userImageUrl", retrievedProfileUrl);
        
        // Update the join request document in Firestore
        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection("join_requests")
                .document(REQUEST_ID);
        
        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Join request updated successfully with user data:\n" + 
                            "Name: " + retrievedUserName + "\n" +
                            "Image URL: " + retrievedProfileUrl);
                    Toast.makeText(this, "Join request fixed!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    fixButton.setEnabled(true);
                    directUpdateButton.setEnabled(true);
                });
    }
} 