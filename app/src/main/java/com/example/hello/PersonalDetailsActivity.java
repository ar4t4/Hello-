package com.example.hello;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.ProgressDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hello.helpers.CloudinaryHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class PersonalDetailsActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etCollege, etDistrict, etHome, etUniversity;
    private SwitchMaterial switchBloodDonate;
    private Button btnSave;
    private ImageView profileImageView;
    private FloatingActionButton btnChangeImage;
    private DatabaseReference userRef;
    private FirebaseAuth auth;
    private ProgressDialog progressDialog;
    private Uri selectedImageUri = null;
    private String currentProfileImageUrl = null;
    
    // Activity result launcher for image picking
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                profileImageView.setImageURI(uri);
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");

        String userId = auth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        // Initialize views
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etCollege = findViewById(R.id.et_college);
        etDistrict = findViewById(R.id.et_district);
        etHome = findViewById(R.id.et_home);
        etUniversity = findViewById(R.id.et_university);
        switchBloodDonate = findViewById(R.id.switch_blood_donate);
        btnSave = findViewById(R.id.btn_save);
        profileImageView = findViewById(R.id.profileImageView);
        btnChangeImage = findViewById(R.id.btnChangeImage);

        // Set up back button
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        // Setup profile image change button
        btnChangeImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        loadUserData();
        btnSave.setOnClickListener(v -> saveUserData());
    }

    private void loadUserData() {
        progressDialog.show();
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();
                if (snapshot.exists()) {
                    // Check for first name, last name, or legacy name field
                    if (snapshot.hasChild("firstName")) {
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        if (firstName != null) etFirstName.setText(firstName);
                        
                        if (snapshot.hasChild("lastName")) {
                            String lastName = snapshot.child("lastName").getValue(String.class);
                            if (lastName != null) etLastName.setText(lastName);
                        }
                    } else if (snapshot.hasChild("name")) {
                        // Handle legacy data format - split name into first and last name
                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null) {
                            String[] parts = name.split(" ", 2);
                            etFirstName.setText(parts[0]);
                            if (parts.length > 1) {
                                etLastName.setText(parts[1]);
                            }
                        }
                    }
                    
                    String college = snapshot.child("college").getValue(String.class);
                    String district = snapshot.child("district").getValue(String.class);
                    String home = snapshot.child("home").getValue(String.class);
                    String university = snapshot.child("university").getValue(String.class);
                    Boolean bloodDonate = snapshot.child("bloodDonate").getValue(Boolean.class);
                    currentProfileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    if (college != null) etCollege.setText(college);
                    if (district != null) etDistrict.setText(district);
                    if (home != null) etHome.setText(home);
                    if (university != null) etUniversity.setText(university);
                    if (bloodDonate != null) switchBloodDonate.setChecked(bloodDonate);
                    
                    // Load profile image if available
                    if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                        CloudinaryHelper.loadImage(PersonalDetailsActivity.this, 
                                                  currentProfileImageUrl, 
                                                  profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(PersonalDetailsActivity.this, 
                    "Error loading data: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData() {
        progressDialog.setMessage("Saving...");
        progressDialog.show();
        
        if (selectedImageUri != null) {
            // Upload new image to Cloudinary first
            uploadAndSaveData();
        } else {
            // No new image, just save the user data
            updateUserData(currentProfileImageUrl);
        }
    }
    
    private void uploadAndSaveData() {
        CloudinaryHelper.uploadImage(
            this, 
            selectedImageUri, 
            "users",
            new CloudinaryHelper.CloudinaryUploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    // Successfully uploaded, now save user data with new image URL
                    updateUserData(imageUrl);
                }

                @Override
                public void onFailure(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(PersonalDetailsActivity.this, 
                        "Failed to upload image: " + error, 
                        Toast.LENGTH_SHORT).show();
                    
                    // Continue with the old image URL
                    updateUserData(currentProfileImageUrl);
                }
            }
        );
    }

    private void updateUserData(String profileImageUrl) {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("college", etCollege.getText().toString().trim());
        updates.put("district", etDistrict.getText().toString().trim());
        updates.put("home", etHome.getText().toString().trim());
        updates.put("university", etUniversity.getText().toString().trim());
        updates.put("bloodDonate", switchBloodDonate.isChecked());
        
        // Update profile image URL if provided
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            updates.put("profileImageUrl", profileImageUrl);
        }

        userRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Details updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Error updating details: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
