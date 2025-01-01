package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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
            Toast.makeText(this, "Chat feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.section_blood_search).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, BloodSearchActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });

        findViewById(R.id.section_fundraise).setOnClickListener(v -> {
            Toast.makeText(this, "Fundraise feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.section_locations).setOnClickListener(v -> {
            Toast.makeText(this, "Locations feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        // Add this to DashboardActivity.java
        findViewById(R.id.btn_personal_details).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, PersonalDetailsActivity.class);
            startActivity(intent);
        });

    }
}
