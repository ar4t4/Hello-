package com.example.hello;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 3000; // 3 seconds
    private static final String PREF_FIRST_TIME = "first_time_launch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Check if it's the first time launch
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean(PREF_FIRST_TIME, true);

        if (isFirstTime) {
            // Animate the hello text
            TextView helloText = findViewById(R.id.helloText);
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1500);
            helloText.startAnimation(fadeIn);

            // Wait for 3 seconds then go to LoginActivity
            new Handler().postDelayed(() -> {
                // Mark first time as done
                prefs.edit().putBoolean(PREF_FIRST_TIME, false).apply();
                
                // Start LoginActivity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }, SPLASH_DURATION);
        } else {
            // If not first time, go directly to LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
} 