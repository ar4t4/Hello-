package com.example.hello;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreateFundraiseActivity extends AppCompatActivity {

    private EditText etTitle, etAmountNeeded, etDonationMethod;
    private Button btnCreateFundraise;

    private DatabaseReference fundraisersRef;
    private String communityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_fundraise);

        // Initialize views
        etTitle = findViewById(R.id.etTitle);
        etAmountNeeded = findViewById(R.id.etAmountNeeded);
        etDonationMethod = findViewById(R.id.etDonationMethod);
        btnCreateFundraise = findViewById(R.id.btnCreateFundraise);

        // Get the communityId from the intent
        communityId = getIntent().getStringExtra("communityId");

        // Validate that communityId is not null or empty
        if (communityId == null || communityId.isEmpty()) {
            Toast.makeText(this, "Community ID is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase reference for the current community's fundraisers
        fundraisersRef = FirebaseDatabase.getInstance().getReference("Fundraisers").child(communityId);

        // Set up button click listener
        btnCreateFundraise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFundraise();
            }
        });
    }

    // Method to create a new fundraiser
    private void createFundraise() {
        String title = etTitle.getText().toString().trim();
        String amountNeeded = etAmountNeeded.getText().toString().trim();
        String donationMethod = etDonationMethod.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(amountNeeded) || TextUtils.isEmpty(donationMethod)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user's UID
        String creatorId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Generate a unique ID for the fundraiser
        String fundraiserId = fundraisersRef.push().getKey();

       //  Create a new Fundraiser object
        Fundraiser fundraiser = new Fundraiser(title, amountNeeded, donationMethod, creatorId);

        // Save the fundraiser to Firebase
        if (fundraiserId != null) {
            fundraisersRef.child(fundraiserId).setValue(fundraiser)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateFundraiseActivity.this, "Fundraiser created successfully!", Toast.LENGTH_SHORT).show();
                        finish();  // Close the activity after successful creation
                    })
                    .addOnFailureListener(e -> Toast.makeText(CreateFundraiseActivity.this, "Failed to create fundraiser.", Toast.LENGTH_SHORT).show());
        }
    }
}
