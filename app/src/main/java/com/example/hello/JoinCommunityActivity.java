package com.example.hello;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hello.models.JoinRequest;
import com.example.hello.repositories.JoinRequestRepository;
import com.example.hello.utils.JoinRequestDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class JoinCommunityActivity extends AppCompatActivity {

    private EditText etCommunityName, etCommunityPassword;
    private Button btnJoinCommunity;
    private DatabaseReference communitiesRef;
    private JoinRequestRepository joinRequestRepository;
    private static final String TAG = "JoinCommunityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_community);

        // Initialize views
        etCommunityName = findViewById(R.id.joinname);
        etCommunityPassword = findViewById(R.id.joinpass);
        btnJoinCommunity = findViewById(R.id.btn_join_community);

        // Set up back button
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        // Initialize Firebase reference and repository
        communitiesRef = FirebaseDatabase.getInstance().getReference("Communities");
        joinRequestRepository = new JoinRequestRepository();

        // Set click listener for Join Community button
        btnJoinCommunity.setOnClickListener(v -> verifyAndRequestJoin());
    }

    private void verifyAndRequestJoin() {
        String communityName = etCommunityName.getText().toString().trim();
        String communityPassword = etCommunityPassword.getText().toString().trim();

        if (communityName.isEmpty() || communityPassword.isEmpty()) {
            Toast.makeText(this, "Please enter community name and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        communitiesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                boolean communityFound = false;

                for (DataSnapshot communitySnapshot : snapshot.getChildren()) {
                    String name = communitySnapshot.child("name").getValue(String.class);
                    Integer password = communitySnapshot.child("password").getValue(Integer.class);

                    if (name != null && name.equals(communityName)) {
                        communityFound = true;

                        if (password != null && password.equals(communityPassword.hashCode())) {
                            String communityId = communitySnapshot.getKey();
                            Log.d(TAG, "Found community: " + communityName + " with ID: " + communityId);
                            
                            // Check if user is already a member
                            if (communitySnapshot.child("members").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                Toast.makeText(this, "You are already a member of this community.", Toast.LENGTH_SHORT).show();
                                navigateToDashboard(communityId);
                                return;
                            }
                            
                            // Show join request dialog instead of direct joining
                            showJoinRequestDialog(communityId, name);
                            return;
                        } else {
                            Toast.makeText(this, "Incorrect community password.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                if (!communityFound) {
                    Toast.makeText(this, "Community not found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to connect to the database.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Firebase error: " + e.getMessage(), e);
        });
    }

    private void showJoinRequestDialog(String communityId, String communityName) {
        JoinRequestDialog.show(this, communityId, communityName, () -> {
            // When request is sent successfully
            Toast.makeText(this, "Join request sent. You'll be notified when approved.", Toast.LENGTH_LONG).show();
            
            // Navigate back to dashboard or main screen
            Intent intent = new Intent(JoinCommunityActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void navigateToDashboard(String communityId) {
        // Save communityId in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        sharedPreferences.edit().putString("communityId", communityId).apply();
        
        Intent intent = new Intent(JoinCommunityActivity.this, DashboardActivity.class);
        intent.putExtra("communityId", communityId);
        startActivity(intent);
        finish();
    }
}
