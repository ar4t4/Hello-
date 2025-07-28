package com.example.hello;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.text.NumberFormat;

public class EditFundraiseActivity extends AppCompatActivity {

    private TextInputEditText titleInput, descriptionInput, targetAmountInput, raisedAmountInput, donationMethodInput;
    private MaterialButton btnUpdate;
    private DatabaseReference databaseRef;
    private String fundraiseId;
    private String communityId;
    private TextView amountNeededText;
    private TextInputLayout targetAmountLayout, raisedAmountLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_fundraise);

        // Get fundraise ID and community ID from intent
        fundraiseId = getIntent().getStringExtra("fundraiseId");
        communityId = getIntent().getStringExtra("communityId");
        
        if (fundraiseId == null || communityId == null) {
            Toast.makeText(this, "Required data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        targetAmountInput = findViewById(R.id.targetAmountInput);
        raisedAmountInput = findViewById(R.id.raisedAmountInput);
        donationMethodInput = findViewById(R.id.donationMethodInput);
        btnUpdate = findViewById(R.id.btnUpdate);
        targetAmountLayout = findViewById(R.id.targetAmountLayout);
        raisedAmountLayout = findViewById(R.id.raisedAmountLayout);
        amountNeededText = findViewById(R.id.amountNeededText);

        // Set up toolbar with back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance()
            .getReference("Fundraisers")
            .child(communityId)
            .child(fundraiseId);

        // Load current fundraiser data
        loadFundraiserData();

        // Set up update button
        btnUpdate.setOnClickListener(v -> updateFundraiser());

        // Add text change listeners
        targetAmountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateAmountNeeded();
            }
        });

        raisedAmountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateAmountNeeded();
            }
        });
    }

    private void loadFundraiserData() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Fundraise fundraise = snapshot.getValue(Fundraise.class);
                if (fundraise != null) {
                    titleInput.setText(fundraise.getTitle());
                    descriptionInput.setText(fundraise.getDescription());
                    targetAmountInput.setText(fundraise.getAmountNeeded());
                    raisedAmountInput.setText(String.valueOf(fundraise.getRaisedAmount()));
                    donationMethodInput.setText(fundraise.getDonationMethod());
                    
                    updateAmountNeeded();
                } else {
                    Toast.makeText(EditFundraiseActivity.this, 
                        "Failed to load fundraiser data", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditFundraiseActivity.this, 
                    "Error loading fundraiser: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateFundraiser() {
        String title = titleInput.getText().toString().trim();
        String targetAmountStr = targetAmountInput.getText().toString().trim();
        String raisedAmountStr = raisedAmountInput.getText().toString().trim();
        String donationMethod = donationMethodInput.getText().toString().trim();

        if (title.isEmpty() || targetAmountStr.isEmpty() || 
            raisedAmountStr.isEmpty() || donationMethod.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double targetAmount, raisedAmount;
        try {
            targetAmount = Double.parseDouble(targetAmountStr);
            raisedAmount = Double.parseDouble(raisedAmountStr);

            if (raisedAmount > targetAmount) {
                Toast.makeText(this, "Raised amount cannot exceed target amount", 
                    Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid amounts", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating fundraiser...");
        progressDialog.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("amountNeeded", targetAmountStr);  // Store as string to match database
        updates.put("raisedAmount", raisedAmount);
        updates.put("donationMethod", donationMethod);

        databaseRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Fundraiser updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to update fundraiser: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void updateAmountNeeded() {
        try {
            double targetAmount = Double.parseDouble(targetAmountInput.getText().toString().trim());
            double raisedAmount = Double.parseDouble(raisedAmountInput.getText().toString().trim());
            double remainingAmount = targetAmount - raisedAmount;

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            
            // Update helper texts
            targetAmountLayout.setHelperText("Total target: " + format.format(targetAmount));
            raisedAmountLayout.setHelperText("Currently raised: " + format.format(raisedAmount));
            
            // Update amount needed text
            if (remainingAmount > 0) {
                amountNeededText.setTextColor(getResources().getColor(R.color.textSecondary));
                amountNeededText.setText("Still needed: " + format.format(remainingAmount));
            } else if (remainingAmount == 0) {
                amountNeededText.setTextColor(getResources().getColor(R.color.success));
                amountNeededText.setText("Target amount reached! 🎉");
            } else {
                amountNeededText.setTextColor(getResources().getColor(R.color.error));
                amountNeededText.setText("Warning: Raised amount exceeds target by " + 
                    format.format(Math.abs(remainingAmount)));
            }
        } catch (NumberFormatException e) {
            targetAmountLayout.setHelperText("Enter valid amount");
            raisedAmountLayout.setHelperText("Enter valid amount");
            amountNeededText.setText("");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 