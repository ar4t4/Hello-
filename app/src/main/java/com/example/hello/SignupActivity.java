package com.example.hello;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText, nameEditText, universityEditText, 
                    collegeEditText, schoolEditText, homeEditText, districtEditText, phoneEditText;
    private Spinner bloodGroupSpinner;
    private Button signUpButton;
    private DatabaseReference database;
    private FirebaseAuth auth;

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
        nameEditText = findViewById(R.id.nameEditText);
        universityEditText = findViewById(R.id.universityEditText);
        collegeEditText = findViewById(R.id.collegeEditText);
        schoolEditText = findViewById(R.id.schoolEditText);
        homeEditText = findViewById(R.id.homeEditText);
        districtEditText = findViewById(R.id.districtEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        bloodGroupSpinner = findViewById(R.id.bloodGroupSpinner);
        signUpButton = findViewById(R.id.signUpButton);

        // Set up blood group spinner
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bloodGroupSpinner.setAdapter(adapter);

        signUpButton.setOnClickListener(v -> signUp());
    }

    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        String university = universityEditText.getText().toString().trim();
        String college = collegeEditText.getText().toString().trim();
        String school = schoolEditText.getText().toString().trim();
        String home = homeEditText.getText().toString().trim();
        String district = districtEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String bloodGroup = bloodGroupSpinner.getSelectedItem().toString();

        // Validate inputs
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Email, password and name are required", Toast.LENGTH_SHORT).show();
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

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", email);
                    userData.put("name", name);
                    userData.put("university", university);
                    userData.put("college", college);
                    userData.put("school", school);
                    userData.put("home", home);
                    userData.put("district", district);
                    userData.put("phone", phone);
                    userData.put("bloodGroup", bloodGroup);
                    userData.put("bloodDonate", true); // Default value

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
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(SignupActivity.this, 
                        "Failed to create account: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
}
