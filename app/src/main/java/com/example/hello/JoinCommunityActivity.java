package com.example.hello;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class JoinCommunityActivity extends AppCompatActivity {

    private EditText etCommunityName, etCommunityPassword;
    private Button btnJoinCommunity;
    private DatabaseReference communitiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_community);


        // Initialize views
        etCommunityName = findViewById(R.id.joinname);
        etCommunityPassword = findViewById(R.id.joinpass);
        btnJoinCommunity = findViewById(R.id.btn_join_community);

        // Initialize Firebase reference
        communitiesRef = FirebaseDatabase.getInstance().getReference("Communities");

        // Set click listener for Join Community button
        btnJoinCommunity.setOnClickListener(v -> joinCommunity());
    }

    private void joinCommunity() {
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
                            Log.d("JoinCommunityActivity", "Found community: " + communityName + " with ID: " + communityId);

                            // Add the current user to the members list
                            DatabaseReference membersRef = communitySnapshot.child("members").getRef();
                            membersRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(true)
                                    .addOnCompleteListener(joinTask -> {
                                        if (joinTask.isSuccessful()) {
                                            Log.d("JoinCommunityActivity", "User successfully joined community!");

                                            // Save communityId in SharedPreferences
                                            SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
                                            sharedPreferences.edit().putString("communityId", communityId).apply();

                                            Toast.makeText(this, "Joined community successfully!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(JoinCommunityActivity.this, DashboardActivity.class);
                                            intent.putExtra("communityId", communityId);
                                            startActivity(intent);
                                            finish(); // Close the current activity
                                        } else {
                                            Toast.makeText(this, "Failed to join the community.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
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
            Log.e("JoinCommunityActivity", "Firebase error: " + e.getMessage(), e);
        });
    }



}
