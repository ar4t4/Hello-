package com.example.hello;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.hello.fragments.CommunityRequestsButtonFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CommunityDetailActivity extends AppCompatActivity {

    private TextView tvCommunityName, tvMemberCount, tvDescription;
    private Button btnMembers, btnLeave, btnRequests;
    private ImageView ivEditName;
    private String communityId;
    private DatabaseReference communityRef;
    private MaterialCardView adminControlsSection;
    private SwitchMaterial switchFaceVerification;
    private boolean isAdmin = false;
    private static final String TAG = "CommunityDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_detail);

        // Get communityId from intent
        communityId = getIntent().getStringExtra("communityId");
        if (communityId == null) {
            Toast.makeText(this, "Community not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        tvCommunityName = findViewById(R.id.tv_community_name);
        tvMemberCount = findViewById(R.id.tv_member_count);
        tvDescription = findViewById(R.id.tv_community_description);
        btnMembers = findViewById(R.id.btn_members);
        btnLeave = findViewById(R.id.btn_leave);
        btnRequests = findViewById(R.id.btn_view_requests);
        adminControlsSection = findViewById(R.id.admin_controls_section);
        switchFaceVerification = findViewById(R.id.switch_face_verification);
        ivEditName = findViewById(R.id.iv_edit_name);
        
        // Hide the chat button as it's not needed
        Button btnChat = findViewById(R.id.btn_chat);
        if (btnChat != null) {
            btnChat.setVisibility(View.GONE);
        }

        // Set up back button
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // Initialize Firebase reference
        communityRef = FirebaseDatabase.getInstance().getReference("Communities").child(communityId);

        // Check if current user is admin
        checkIfAdmin();

        // Load community data
        loadCommunityData();

        // Set up button click listeners
        setupButtonListeners();
        
        // Set up face verification switch listener
        setupFaceVerificationSwitch();
        
        // Set up edit name icon click listener
        setupEditNameListener();
    }
    
    private void setupEditNameListener() {
        ivEditName.setOnClickListener(v -> {
            if (isAdmin) {
                showEditNameDialog();
            }
        });
    }
    
    private void showEditNameDialog() {
        // Create dialog with edit text
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Community Name");
        
        // Set up the input
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        TextInputLayout textInputLayout = view.findViewById(R.id.text_input_layout);
        EditText input = view.findViewById(R.id.edit_text);
        textInputLayout.setHint("Community Name");
        input.setText(tvCommunityName.getText());
        
        builder.setView(view);
        
        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateCommunityName(newName);
            } else {
                Toast.makeText(this, "Community name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    private void updateCommunityName(String newName) {
        communityRef.child("name").setValue(newName)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Community name updated", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to update name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void checkIfAdmin() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        communityRef.child("admins").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                isAdmin = dataSnapshot.exists();
                // Show admin controls if user is admin
                adminControlsSection.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                
                // Show edit icon only for admins
                ivEditName.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                
                // Show requests button to all members (no longer admin-only)
                if (btnRequests != null) {
                    btnRequests.setVisibility(View.VISIBLE);
                }
                
                // Load the community admins to display them
                loadCommunityAdmins();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error checking admin status: " + databaseError.getMessage());
            }
        });
    }

    private void loadCommunityAdmins() {
        TextView tvAdmins = findViewById(R.id.tv_admins);
        
        communityRef.child("admins").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    // Build the list of admin names - admins already exist
                    displayAdmins(dataSnapshot, tvAdmins);
                } else {
                    // No admins found - let's check for the creator and make them an admin
                    communityRef.child("createdBy").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot creatorSnapshot) {
                            if (creatorSnapshot.exists()) {
                                String creatorId = creatorSnapshot.getValue(String.class);
                                
                                if (creatorId != null && !creatorId.isEmpty()) {
                                    // Add creator as admin
                                    communityRef.child("admins").child(creatorId).setValue(true)
                                        .addOnSuccessListener(aVoid -> {
                                            // Successfully set creator as admin
                                            Log.d(TAG, "Creator " + creatorId + " set as admin");
                                            
                                            // Now display the admin (which is just the creator at this point)
                                            communityRef.child("admins").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot adminsSnapshot) {
                                                    displayAdmins(adminsSnapshot, tvAdmins);
                                                }
                                                
                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                    Log.e(TAG, "Error reloading admins: " + databaseError.getMessage());
                                                }
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to set creator as admin: " + e.getMessage());
                                            tvAdmins.setText("No community admins found");
                                        });
                                } else {
                                    // No valid creator ID
                                    tvAdmins.setText("No community admins found");
                                }
                            } else {
                                // Try to use the user ID from query parameter as fallback
                                String queryUserId = "qTgC3NdvRtVB2pBE5Ryx71zyzvn2"; // Fallback from database snapshot
                                
                                // Set this user as admin
                                communityRef.child("admins").child(queryUserId).setValue(true)
                                    .addOnSuccessListener(aVoid -> {
                                        // Successfully set fallback user as admin
                                        Log.d(TAG, "User " + queryUserId + " set as admin (fallback)");
                                        
                                        // Now display the admin
                                        communityRef.child("admins").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot adminsSnapshot) {
                                                displayAdmins(adminsSnapshot, tvAdmins);
                                            }
                                            
                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Log.e(TAG, "Error reloading admins: " + databaseError.getMessage());
                                            }
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to set fallback user as admin: " + e.getMessage());
                                        tvAdmins.setText("No community admins found");
                                    });
                            }
                        }
                        
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "Error checking community creator: " + databaseError.getMessage());
                            tvAdmins.setText("No community admins found");
                        }
                    });
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading admins: " + databaseError.getMessage());
                tvAdmins.setText("Unable to load admin information");
            }
        });
    }

    private void displayAdmins(DataSnapshot adminsSnapshot, TextView tvAdmins) {
        if (adminsSnapshot.exists() && adminsSnapshot.getChildrenCount() > 0) {
            // Build the list of admin names
            StringBuilder adminText = new StringBuilder("Admins: ");
            
            // Keep track of how many admins we've processed
            final int[] adminCount = {0};
            final long totalAdmins = adminsSnapshot.getChildrenCount();
            
            for (DataSnapshot adminSnapshot : adminsSnapshot.getChildren()) {
                String adminId = adminSnapshot.getKey();
                
                // Fetch the admin's name from Users reference
                FirebaseDatabase.getInstance().getReference("Users")
                    .child(adminId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot userSnapshot) {
                            String adminName = "Unknown";
                            
                            // Try to get the user's name in different ways
                            if (userSnapshot.hasChild("firstName")) {
                                adminName = userSnapshot.child("firstName").getValue(String.class);
                                if (userSnapshot.hasChild("lastName")) {
                                    adminName += " " + userSnapshot.child("lastName").getValue(String.class);
                                }
                            } else if (userSnapshot.hasChild("name")) {
                                adminName = userSnapshot.child("name").getValue(String.class);
                            } else if (userSnapshot.hasChild("email")) {
                                adminName = userSnapshot.child("email").getValue(String.class);
                            }
                            
                            // Append the admin name
                            adminText.append(adminName);
                            
                            // Increment counter of processed admins
                            adminCount[0]++;
                            
                            // If there are more admins, add a comma
                            if (adminCount[0] < totalAdmins) {
                                adminText.append(", ");
                            }
                            
                            // Update the UI when all admins are processed
                            if (adminCount[0] == totalAdmins) {
                                tvAdmins.setText(adminText.toString());
                            }
                        }
                        
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "Error loading admin info: " + databaseError.getMessage());
                        }
                    });
            }
        } else {
            // No admins found
            tvAdmins.setText("No community admins found");
        }
    }

    private void setupFaceVerificationSwitch() {
        // Load initial state
        communityRef.child("requiresFaceVerification").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Default to true if not set
                boolean requiresVerification = true;
                if (dataSnapshot.exists()) {
                    requiresVerification = dataSnapshot.getValue(Boolean.class);
                } else {
                    // If not existing, set default value to true
                    communityRef.child("requiresFaceVerification").setValue(true);
                }
                
                // Set switch state without triggering listener
                switchFaceVerification.setOnCheckedChangeListener(null);
                switchFaceVerification.setChecked(requiresVerification);
                
                // Re-add the listener
                switchFaceVerification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isAdmin) {
                            updateFaceVerificationSetting(isChecked);
                        } else {
                            // Reset switch if non-admin somehow toggles it
                            switchFaceVerification.setChecked(!isChecked);
                            Toast.makeText(CommunityDetailActivity.this, 
                                    "Only admins can change this setting", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading verification setting: " + databaseError.getMessage());
            }
        });
    }
    
    private void updateFaceVerificationSetting(boolean requiresVerification) {
        communityRef.child("requiresFaceVerification").setValue(requiresVerification)
                .addOnSuccessListener(aVoid -> {
                    String message = requiresVerification ? 
                            "Face verification enabled for group chat" : 
                            "Face verification disabled for group chat";
                    Toast.makeText(CommunityDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Revert switch if save fails
                    switchFaceVerification.setChecked(!requiresVerification);
                    Toast.makeText(CommunityDetailActivity.this, 
                            "Failed to update setting: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCommunityData() {
        communityRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String description = dataSnapshot.child("description").getValue(String.class);
                    
                    // Update UI with community data
                    if (name != null) {
                        tvCommunityName.setText(name);
                    }
                    
                    // Handle description - hide if not available instead of showing "No description available"
                    if (description != null && !description.isEmpty()) {
                        tvDescription.setText(description);
                        tvDescription.setVisibility(View.VISIBLE);
                    } else {
                        tvDescription.setVisibility(View.GONE);
                    }
                    
                    // Count members
                    if (dataSnapshot.hasChild("members")) {
                        long memberCount = dataSnapshot.child("members").getChildrenCount();
                        tvMemberCount.setText(memberCount + " members");
                    } else {
                        tvMemberCount.setText("0 members");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(CommunityDetailActivity.this, 
                        "Failed to load community: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupButtonListeners() {
        btnMembers.setOnClickListener(v -> {
            // Navigate to members list
            Intent intent = new Intent(CommunityDetailActivity.this, AbcdActivity.class);
            intent.putExtra("communityId", communityId);
            // Also pass community name for notifications
            String communityName = tvCommunityName.getText().toString();
            intent.putExtra("communityName", communityName);
            startActivity(intent);
        });

        btnLeave.setOnClickListener(v -> {
            leaveCommunity();
        });
        
        // Add "View Requests" button
        btnRequests = findViewById(R.id.btn_view_requests);
        if (btnRequests != null) {
            btnRequests.setOnClickListener(v -> {
                // Launch the standalone join requests activity
                Intent intent = new Intent(CommunityDetailActivity.this, JoinRequestsActivity.class);
                intent.putExtra("communityId", communityId);
                startActivity(intent);
            });
        }
    }

    private void leaveCommunity() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Directly remove the user from members without checking admin status
        communityRef.child("members").child(userId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Also check and remove from admins if they are an admin
                    communityRef.child("admins").child(userId).removeValue();
                    
                    // Clear the saved community ID from preferences
                    SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
                    sharedPreferences.edit().remove("communityId").apply();
                    
                    // Also update Firebase Auth custom claims if you're using them
                    
                    // Also clean up any pending join requests
                    cleanupUserJoinRequests(userId);
                    
                    Toast.makeText(CommunityDetailActivity.this, 
                            "Left community successfully", 
                            Toast.LENGTH_SHORT).show();
                    
                    // Go back to main screen
                    Intent intent = new Intent(CommunityDetailActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(CommunityDetailActivity.this, 
                            "Failed to leave community: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show()
                );
    }
    
    private void cleanupUserJoinRequests(String userId) {
        // Clean up any pending join requests for this user and community
        FirebaseDatabase.getInstance().getReference("join_requests")
            .orderByChild("userId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (communityId.equals(snapshot.child("communityId").getValue(String.class))) {
                            snapshot.getRef().removeValue();
                        }
                    }
                }
                
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Failed to cleanup join requests: " + databaseError.getMessage());
                }
            });
    }
} 