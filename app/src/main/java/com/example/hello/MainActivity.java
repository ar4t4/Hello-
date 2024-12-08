package com.example.hello;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Button btnLogout;
    private Button btnCreateCommunity, btnJoinCommunity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLogout = findViewById(R.id.btn_logout);
        btnCreateCommunity = findViewById(R.id.btn_create_community);
        btnJoinCommunity = findViewById(R.id.btn_join_community);

        // Check for first launch
        if (isFirstLaunch()) {
            Intent intent = new Intent(this, HelloActivity.class);
            startActivity(intent);
            finish(); // Close MainActivity during the first launch
        }

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

    private boolean isFirstLaunch() {
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        boolean isFirstLaunch = preferences.getBoolean("FirstLaunch", true);
        if (isFirstLaunch) {
            preferences.edit().putBoolean("FirstLaunch", false).apply();
        }
        return isFirstLaunch;
    }

    private void logout() {
        // Log out from Firebase (if used)
        FirebaseAuth.getInstance().signOut();

        // Clear shared preferences (optional, if needed for user data)
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        preferences.edit().clear().apply();

        // Redirect to login screen
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        finish(); // Close MainActivity after logout
    }
}
