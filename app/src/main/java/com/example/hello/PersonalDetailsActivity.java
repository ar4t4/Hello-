package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.ProgressDialog;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    private EditText etName, etCollege, etDistrict, etHome, etUniversity;
    private SwitchMaterial switchBloodDonate;
    private Button btnSave;
    private DatabaseReference userRef;
    private FirebaseAuth auth;
    private ProgressDialog progressDialog;

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
        etName = findViewById(R.id.et_name);
        etCollege = findViewById(R.id.et_college);
        etDistrict = findViewById(R.id.et_district);
        etHome = findViewById(R.id.et_home);
        etUniversity = findViewById(R.id.et_university);
        switchBloodDonate = findViewById(R.id.switch_blood_donate);
        btnSave = findViewById(R.id.btn_save);

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
                    String name = snapshot.child("name").getValue(String.class);
                    String college = snapshot.child("college").getValue(String.class);
                    String district = snapshot.child("district").getValue(String.class);
                    String home = snapshot.child("home").getValue(String.class);
                    String university = snapshot.child("university").getValue(String.class);
                    Boolean bloodDonate = snapshot.child("bloodDonate").getValue(Boolean.class);

                    if (name != null) etName.setText(name);
                    if (college != null) etCollege.setText(college);
                    if (district != null) etDistrict.setText(district);
                    if (home != null) etHome.setText(home);
                    if (university != null) etUniversity.setText(university);
                    if (bloodDonate != null) switchBloodDonate.setChecked(bloodDonate);
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

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", etName.getText().toString().trim());
        updates.put("college", etCollege.getText().toString().trim());
        updates.put("district", etDistrict.getText().toString().trim());
        updates.put("home", etHome.getText().toString().trim());
        updates.put("university", etUniversity.getText().toString().trim());
        updates.put("bloodDonate", switchBloodDonate.isChecked());

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
