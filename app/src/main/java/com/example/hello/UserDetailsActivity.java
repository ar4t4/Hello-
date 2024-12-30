package com.example.hello;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class UserDetailsActivity extends AppCompatActivity {

    private TextView tvUserDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        tvUserDetails = findViewById(R.id.tv_user_details);

        String userDetails = getIntent().getStringExtra("userDetails");

        if (userDetails != null) {
            tvUserDetails.setText(userDetails);
        }
    }
}
