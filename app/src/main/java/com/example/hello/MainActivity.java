package com.example.hello;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;

import com.google.firebase.auth.FirebaseAuth;
import com.example.hello.utils.JoinRequestUpdater;
import com.example.hello.utils.FixSpecificJoinRequestActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnLogout;
    private Button btnCreateCommunity, btnJoinCommunity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        String communityId = sharedPreferences.getString("communityId", null);

        if (communityId != null) {
            // User is already a member of a community, redirect to DashboardActivity
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
            finish();
        } else {
            setContentView(R.layout.activity_main); // Load MainActivity layout
        }

        btnLogout = findViewById(R.id.btn_logout);
        btnCreateCommunity = findViewById(R.id.btn_create_community);
        btnJoinCommunity = findViewById(R.id.btn_join_community);

        // Set up Logout Button functionality
        btnLogout.setOnClickListener(v -> logout());

        // Navigate to CreateCommunityActivity
        btnCreateCommunity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateCommunityActivity.class);
            startActivity(intent);
        });

        // Navigate to JoinCommunityActivity
        btnJoinCommunity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, JoinCommunityActivity.class);
            startActivity(intent);
        });
    }


    private void logout() {
        FirebaseAuth.getInstance().signOut(); // Log out from Firebase
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        preferences.edit().clear().apply(); // Clear all preferences

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_update_request) {
            updateSpecificJoinRequest();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void updateSpecificJoinRequest() {
        // Update the specific join request mentioned by the user
        String requestId = "-OP3KdhmaPKVGnOvSGiS";
        String userId = "qTgC3NdvRtVB2pBE5Ryx71zyzvn2";
        
        JoinRequestUpdater.updateSpecificRequest(
                this,
                requestId,
                userId,
                () -> Toast.makeText(this, "Join request updated successfully", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
}

