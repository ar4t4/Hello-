package com.example.hello;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private String communityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Retrieve the community ID passed from the JoinCommunityActivity
        communityId = getIntent().getStringExtra("communityId");

        if (communityId == null) {
            Toast.makeText(this, "Community ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup click listeners for sections
        findViewById(R.id.section_members).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AbcdActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });

        findViewById(R.id.section_chat).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatListActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });

        findViewById(R.id.section_chat_offline).setOnClickListener(v -> {
            Intent intent = new Intent(this, BluetoothMeshChatActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.section_blood_search).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, BloodSearchActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });

        findViewById(R.id.section_fundraise).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, FundraiseActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });

        findViewById(R.id.section_locations).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LocationsActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });

        findViewById(R.id.btn_personal_details).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, PersonalDetailsActivity.class);
            startActivity(intent);
        });

        // Add community details button
        findViewById(R.id.btn_community_details).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CommunityDetailActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });

        findViewById(R.id.btn_leave_community).setOnClickListener(v -> leaveCommunity());

        // Add Community Assistant click listener
        findViewById(R.id.community_assistant_card).setOnClickListener(v -> {
            // Get current user ID
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // Create a unique chat ID for AI chat
            String aiChatId = "ai_chat_" + currentUserId;
            
            // Start chat activity with AI
            Intent intent = new Intent(DashboardActivity.this, ChatActivity.class);
            intent.putExtra("chatId", aiChatId);
            intent.putExtra("isAIChat", true);
            startActivity(intent);
        });

        setupClickListeners();
        
        // Load dynamic counts
        loadMembersCount();
        loadEventsCount();
        
        // Load user profile information
        loadUserProfile();
        
        // Check if the user is an admin to show pending requests notification
        checkForPendingRequests();
    }

    private void leaveCommunity() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        sharedPreferences.edit().remove("communityId").apply();

        Toast.makeText(this, "You have left the community.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupClickListeners() {
        findViewById(R.id.eventsCard).setOnClickListener(v -> {
            // Use the existing communityId
            Intent intent = new Intent(DashboardActivity.this, EventsActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });
    }
    
    private void checkForPendingRequests() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Check if user is an admin
        DatabaseReference adminRef = FirebaseDatabase.getInstance()
                .getReference("Communities")
                .child(communityId)
                .child("admins")
                .child(userId);
                
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User is an admin, check for pending requests
                    FirebaseFirestore.getInstance()
                            .collection("join_requests")
                            .whereEqualTo("communityId", communityId)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                int count = queryDocumentSnapshots.size();
                                if (count > 0) {
                                    // Show a notification or badge that there are pending requests
                                    Toast.makeText(DashboardActivity.this, 
                                            "You have " + count + " pending join requests to review", 
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Do nothing
            }
        });
    }
    
    private void loadMembersCount() {
        DatabaseReference membersRef = FirebaseDatabase.getInstance()
                .getReference("Communities")
                .child(communityId)
                .child("members");
                
        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                TextView membersCount = findViewById(R.id.membersCount);
                membersCount.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Keep default value
            }
        });
    }
    
    private void loadEventsCount() {
        DatabaseReference eventsRef = FirebaseDatabase.getInstance()
                .getReference("Events");
                
        eventsRef.orderByChild("communityId").equalTo(communityId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                TextView eventsCount = findViewById(R.id.eventsCount);
                eventsCount.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Keep default value
            }
        });
    }
    
    private void loadUserProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get user information
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String lastName = snapshot.child("lastName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String college = snapshot.child("college").getValue(String.class);
                    String university = snapshot.child("university").getValue(String.class);
                    
                    // Update UI elements
                    TextView tvUserName = findViewById(R.id.tv_user_name);
                    TextView tvUserEmail = findViewById(R.id.tv_user_email);
                    TextView tvUserCollege = findViewById(R.id.tv_user_college);
                    
                    // Set name
                    if (firstName != null && lastName != null) {
                        tvUserName.setText(firstName + " " + lastName);
                    } else if (firstName != null) {
                        tvUserName.setText(firstName);
                    } else {
                        tvUserName.setText("User");
                    }
                    
                    // Set email
                    if (email != null) {
                        tvUserEmail.setText(email);
                    } else {
                        tvUserEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                    }
                    
                    // Set college/university
                    if (college != null && !college.isEmpty()) {
                        tvUserCollege.setText(college);
                        tvUserCollege.setVisibility(android.view.View.VISIBLE);
                    } else if (university != null && !university.isEmpty()) {
                        tvUserCollege.setText(university);
                        tvUserCollege.setVisibility(android.view.View.VISIBLE);
                    } else {
                        tvUserCollege.setVisibility(android.view.View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Keep default values
                TextView tvUserName = findViewById(R.id.tv_user_name);
                TextView tvUserEmail = findViewById(R.id.tv_user_email);
                
                tvUserName.setText("User");
                tvUserEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
            }
        });
    }
}
