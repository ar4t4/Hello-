package com.example.hello;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PersonalDetailsActivity extends AppCompatActivity {

    private EditText etName, etCollege, etDistrict, etHome, etUniversity;
    private Switch switchBloodDonate;
    private Button btnSave;

    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        // Initialize views
        etName = findViewById(R.id.et_name);
        etCollege = findViewById(R.id.et_college);
        etDistrict = findViewById(R.id.et_district);
        etHome = findViewById(R.id.et_home);
        etUniversity = findViewById(R.id.et_university);
        switchBloodDonate = findViewById(R.id.switch_blood_donate);
        btnSave = findViewById(R.id.btn_save);

        // Initialize Firebase reference
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        // Load existing user data
        loadUserData();

        // Save button click listener
        btnSave.setOnClickListener(v -> saveUserData());
    }

    private void loadUserData() {
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                etName.setText(task.getResult().child("name").getValue(String.class));
                etCollege.setText(task.getResult().child("college").getValue(String.class));
                etDistrict.setText(task.getResult().child("district").getValue(String.class));
                etHome.setText(task.getResult().child("home").getValue(String.class));
                etUniversity.setText(task.getResult().child("university").getValue(String.class));
                Boolean bloodDonate = task.getResult().child("bloodDonate").getValue(Boolean.class);
                switchBloodDonate.setChecked(bloodDonate != null && bloodDonate);
            } else {
                Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData() {
        String name = etName.getText().toString().trim();
        String college = etCollege.getText().toString().trim();
        String district = etDistrict.getText().toString().trim();
        String home = etHome.getText().toString().trim();
        String university = etUniversity.getText().toString().trim();
        boolean bloodDonate = switchBloodDonate.isChecked();

        // Update user data in Firebase
        userRef.child("name").setValue(name);
        userRef.child("college").setValue(college);
        userRef.child("district").setValue(district);
        userRef.child("home").setValue(home);
        userRef.child("university").setValue(university);
        userRef.child("bloodDonate").setValue(bloodDonate).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Details updated successfully.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update details.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
