package com.example.hello;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isFirstLaunch()) {
            // Launch HelloActivity
            Intent intent = new Intent(this, HelloActivity.class);
            startActivity(intent);
        }
    }

    private boolean isFirstLaunch() {
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        boolean isFirstLaunch = preferences.getBoolean("FirstLaunch", true);
        if (isFirstLaunch) {
            // Set FirstLaunch to false so it doesn't show again
            preferences.edit().putBoolean("FirstLaunch", false).apply();
        }
        return isFirstLaunch;
    }
}