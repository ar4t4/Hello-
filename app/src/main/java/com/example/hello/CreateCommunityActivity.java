package com.example.hello;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class CreateCommunityActivity extends AppCompatActivity {
    private EditText communityNameEditText, communityPasswordEditText;
    private Button createCommunityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_community);

        communityNameEditText = findViewById(R.id.et_community_name);
        communityPasswordEditText = findViewById(R.id.et_community_password);
        createCommunityButton = findViewById(R.id.btn_create_community);

        // Set up back button
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        createCommunityButton.setOnClickListener(v -> createCommunity());
    }

    private void createCommunity() {
        String communityName = communityNameEditText.getText().toString().trim();
        String communityPassword = communityPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(communityName) || TextUtils.isEmpty(communityPassword)) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference communitiesRef = FirebaseDatabase.getInstance().getReference("Communities");
        String communityId = communitiesRef.push().getKey(); // Generate unique ID for the community

        // Create a new community map
        Map<String, Object> communityData = new HashMap<>();
        communityData.put("name", communityName);
        communityData.put("creator", FirebaseAuth.getInstance().getCurrentUser().getUid());
        communityData.put("password", communityPassword.hashCode()); // Hash the password

        // Initialize an empty "members" node
        Map<String, Boolean> members = new HashMap<>();
        members.put(FirebaseAuth.getInstance().getCurrentUser().getUid(), true); // Add creator as the first member
        communityData.put("members", members);

        assert communityId != null;
        communitiesRef.child(communityId).setValue(communityData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Community created successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to create community!", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
