package com.example.hello.ui.joinrequests;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hello.R;
import com.example.hello.utils.JoinRequestUpdater;

public class JoinRequestUpdateActivity extends AppCompatActivity {
    
    public static final String EXTRA_REQUEST_ID = "requestId";
    public static final String EXTRA_USER_ID = "userId";
    
    private ProgressBar progressBar;
    private TextView statusText;
    private Button updateButton;
    private Button closeButton;
    
    private String requestId;
    private String userId;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_request_update);
        
        // Get request ID and user ID from intent
        requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        
        if (requestId == null || userId == null) {
            Toast.makeText(this, "No request ID or user ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize UI components
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.tv_status);
        updateButton = findViewById(R.id.btn_update);
        closeButton = findViewById(R.id.btn_close);
        
        // Set up click listeners
        updateButton.setOnClickListener(v -> updateJoinRequest());
        closeButton.setOnClickListener(v -> finish());
        
        // Display the request and user IDs
        TextView requestIdText = findViewById(R.id.tv_request_id);
        TextView userIdText = findViewById(R.id.tv_user_id);
        
        requestIdText.setText("Request ID: " + requestId);
        userIdText.setText("User ID: " + userId);
    }
    
    private void updateJoinRequest() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Updating join request...");
        updateButton.setEnabled(false);
        
        // Update the specific join request
        JoinRequestUpdater.updateSpecificRequest(
                this,
                requestId,
                userId,
                () -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Update complete! You can now close this screen.");
                    updateButton.setEnabled(false);
                }
        );
    }
} 