package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HelloActivity extends AppCompatActivity {
    private TextView tvHello, tvWorldUpsideDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set full-screen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_hello);

        tvHello = findViewById(R.id.tv_hello);
        tvWorldUpsideDown = findViewById(R.id.tv_world_upside_down);

        // Delay to hide "hello" and show "Let's make the world upside down"
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Hide "hello" and show "Let's make the world upside down"
                tvHello.setVisibility(View.GONE);
                tvWorldUpsideDown.setVisibility(View.VISIBLE);

                // Delay for another 3 seconds before navigating back to the main page
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Start the main activity (MainActivity or whatever your main activity is)
                        Intent intent = new Intent(HelloActivity.this, MainActivity.class);
                        startActivity(intent);

                        // Finish the HelloActivity so it's removed from the back stack
                        finish();
                    }
                }, 3000);  // Additional 3-second delay before transitioning to the main activity
            }
        }, 3000);  // Initial 3-second delay to show "hello"
    }
}
