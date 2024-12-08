package com.example.hello;

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
    private EditText emailEditText, passwordEditText, nameEditText, universityEditText, collegeEditText, schoolEditText, homeEditText, districtEditText;
    private Spinner bloodGroupSpinner;
    private Button signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        nameEditText = findViewById(R.id.nameEditText);
        universityEditText = findViewById(R.id.universityEditText);
        collegeEditText = findViewById(R.id.collegeEditText);
        schoolEditText = findViewById(R.id.schoolEditText);
        homeEditText = findViewById(R.id.homeEditText);
        districtEditText = findViewById(R.id.districtEditText);
        bloodGroupSpinner = findViewById(R.id.bloodGroupSpinner);
        signUpButton = findViewById(R.id.signUpButton);

        // Initialize blood group options
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bloodGroupSpinner.setAdapter(adapter);

        signUpButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        // Retrieve inputs
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        String university = universityEditText.getText().toString().trim();
        String college = collegeEditText.getText().toString().trim();
        String school = schoolEditText.getText().toString().trim();
        String home = homeEditText.getText().toString().trim();
        String district = districtEditText.getText().toString().trim();
        String bloodGroup = bloodGroupSpinner.getSelectedItem().toString();

        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || university.isEmpty() || college.isEmpty() || school.isEmpty() || home.isEmpty() || district.isEmpty() || bloodGroup.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();

                        // Create a map for user data
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email);
                        userData.put("name", name);
                        userData.put("university", university);
                        userData.put("college", college);
                        userData.put("school", school);
                        userData.put("home", home);
                        userData.put("district", district);
                        userData.put("bloodGroup", bloodGroup);

                        // Save data to Firebase
                        database.child(userId).setValue(userData)
                                .addOnCompleteListener(databaseTask -> {
                                    if (databaseTask.isSuccessful()) {
                                        Toast.makeText(SignupActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(SignupActivity.this, "Error: " + databaseTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(SignupActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
