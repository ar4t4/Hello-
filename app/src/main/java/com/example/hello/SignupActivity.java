package com.example.hello;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hello.helpers.CloudinaryHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText, firstNameEditText, lastNameEditText, universityEditText, 
                    collegeEditText, schoolEditText, homeEditText, districtEditText, phoneEditText;
    private Spinner bloodGroupSpinner;
    private Button signUpButton;
    private ImageView profileImageView;
    private FloatingActionButton addImageButton;
    private DatabaseReference database;
    private FirebaseAuth auth;
    private Uri selectedImageUri = null;
    
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
        setContentView(R.layout.activity_signup);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        universityEditText = findViewById(R.id.universityEditText);
        collegeEditText = findViewById(R.id.collegeEditText);
        schoolEditText = findViewById(R.id.schoolEditText);
        homeEditText = findViewById(R.id.homeEditText);
        districtEditText = findViewById(R.id.districtEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        bloodGroupSpinner = findViewById(R.id.bloodGroupSpinner);
        signUpButton = findViewById(R.id.signUpButton);
        profileImageView = findViewById(R.id.profileImageView);
        addImageButton = findViewById(R.id.addImageButton);

        // Set up blood group spinner
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bloodGroupSpinner.setAdapter(adapter);

        // Set up image selection
        addImageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Set up sign up button
        signUpButton.setOnClickListener(v -> signUp());
    }

    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String university = universityEditText.getText().toString().trim();
        String college = collegeEditText.getText().toString().trim();
        String school = schoolEditText.getText().toString().trim();
        String home = homeEditText.getText().toString().trim();
        String district = districtEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String bloodGroup = bloodGroupSpinner.getSelectedItem().toString();

        // Validate inputs
        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty()) {
            Toast.makeText(this, "Email, password and first name are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.show();

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String userId = auth.getCurrentUser().getUid();
                    
                    if (selectedImageUri != null) {
                        // Upload image to Cloudinary first
                        uploadImageAndSaveUser(userId, progressDialog, email, firstName, lastName, university, 
                                college, school, home, district, phone, bloodGroup);
                    } else {
                        // No image selected, save user without image
                        saveUserToFirebase(userId, progressDialog, email, firstName, lastName, university, 
                                college, school, home, district, phone, bloodGroup, null);
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(SignupActivity.this, 
                        "Failed to create account: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void uploadImageAndSaveUser(String userId, ProgressDialog progressDialog, 
                                       String email, String firstName, String lastName, String university, 
                                       String college, String school, String home, 
                                       String district, String phone, String bloodGroup) {
        // Upload image to Cloudinary
        CloudinaryHelper.uploadImage(this, selectedImageUri, "users", new CloudinaryHelper.CloudinaryUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                // Save user data with image URL
                saveUserToFirebase(userId, progressDialog, email, firstName, lastName, university, 
                        college, school, home, district, phone, bloodGroup, imageUrl);
            }

            @Override
            public void onFailure(String error) {
                progressDialog.dismiss();
                Toast.makeText(SignupActivity.this, 
                    "Failed to upload image: " + error, Toast.LENGTH_SHORT).show();
                
                // Continue without image
                saveUserToFirebase(userId, progressDialog, email, firstName, lastName, university, 
                        college, school, home, district, phone, bloodGroup, null);
            }
        });
    }
    
    private void saveUserToFirebase(String userId, ProgressDialog progressDialog, 
                                   String email, String firstName, String lastName, String university, 
                                   String college, String school, String home, 
                                   String district, String phone, String bloodGroup, 
                                   String profileImageUrl) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("university", university);
        userData.put("college", college);
        userData.put("school", school);
        userData.put("home", home);
        userData.put("district", district);
        userData.put("phone", phone);
        userData.put("bloodGroup", bloodGroup);
        userData.put("bloodDonate", true); // Default value
        
        // Add profile image URL if available
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            userData.put("profileImageUrl", profileImageUrl);
        }

        database.child(userId).setValue(userData)
            .addOnCompleteListener(dbTask -> {
                progressDialog.dismiss();
                if (dbTask.isSuccessful()) {
                    Toast.makeText(SignupActivity.this, 
                        "Account created successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(SignupActivity.this, 
                        "Failed to save user data: " + dbTask.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
}
