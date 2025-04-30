package com.example.hello;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;


public class BloodSearchActivity extends AppCompatActivity {

    private Spinner spinnerBloodGroup;
    private Button btnSearch;
    private ListView lvResults;
    private String communityId;
    private DatabaseReference usersRef;
    private ArrayList<String> resultList;
    private BloodSearchAdapter adapter;
    private HashMap<String, String> userPhoneMap;
    private HashMap<String, String> userImageMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood_search);
        communityId = getIntent().getStringExtra("communityId"); // Retrieve community ID
        //toast community id
        Toast.makeText(this, "Community ID: " + communityId, Toast.LENGTH_SHORT).show();
        if (communityId == null) {
            Toast.makeText(this, "Community ID harai gese", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initialize views
        spinnerBloodGroup = findViewById(R.id.spinner_blood_group);
        btnSearch = findViewById(R.id.btn_search);
        lvResults = findViewById(R.id.lv_results);

        // Initialize Firebase reference
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Set up Spinner with blood group options
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(spinnerAdapter);

        // Initialize result list and adapter
        resultList = new ArrayList<>();
        userPhoneMap = new HashMap<>();
        userImageMap = new HashMap<>();
        adapter = new BloodSearchAdapter(this, resultList, userPhoneMap, userImageMap);
        lvResults.setAdapter(adapter);


        // Handle search button click
        btnSearch.setOnClickListener(v -> {
            String selectedBloodGroup = spinnerBloodGroup.getSelectedItem().toString();
            //give a toast message
            Toast.makeText(this, "Searching for " + selectedBloodGroup + " donors...", Toast.LENGTH_SHORT).show();
            searchBloodDonors(selectedBloodGroup);
        });

        // Handle list item click for calling
        lvResults.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = resultList.get(position);
            String phoneNumber = userPhoneMap.get(selectedName);
            if (phoneNumber != null) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Phone number not available.", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void searchBloodDonors(String bloodGroup) {
        resultList.clear();
        userPhoneMap.clear();
        userImageMap.clear();

        DatabaseReference communityMembersRef = FirebaseDatabase.getInstance()
                .getReference("Communities")
                .child(communityId)
                .child("members");

        communityMembersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot communitySnapshot) {
                if (communitySnapshot.exists()) {
                    for (DataSnapshot memberSnapshot : communitySnapshot.getChildren()) {
                        String userId = memberSnapshot.getKey();

                        Log.d("MemberUID", "Fetched Member UID: " + userId);

                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("Users").child(userId); // Remove "users" from the path

                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                if (userSnapshot.exists()) {
                                    Toast.makeText(BloodSearchActivity.this, "User found", Toast.LENGTH_SHORT).show();
                                    String userBloodGroup = userSnapshot.child("bloodGroup").getValue(String.class);
                                    Boolean isAvailable = userSnapshot.child("bloodDonate").getValue(Boolean.class);
                                    
                                    // Get name - either from firstName+lastName or legacy name field
                                    String name;
                                    if (userSnapshot.hasChild("firstName")) {
                                        String firstName = userSnapshot.child("firstName").getValue(String.class);
                                        String lastName = "";
                                        if (userSnapshot.hasChild("lastName")) {
                                            lastName = userSnapshot.child("lastName").getValue(String.class);
                                        }
                                        name = firstName + (lastName.isEmpty() ? "" : " " + lastName);
                                    } else {
                                        name = userSnapshot.child("name").getValue(String.class);
                                    }
                                    
                                    String phone = userSnapshot.child("phone").getValue(String.class);
                                    String profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String.class);

                                    if (userBloodGroup != null && userBloodGroup.equalsIgnoreCase(bloodGroup)
                                            && isAvailable != null && isAvailable) {
                                        resultList.add(name);
                                        userPhoneMap.put(name, phone);
                                        
                                        // Store profile image URL if available
                                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                            userImageMap.put(name, profileImageUrl);
                                        }

                                        Log.d("DonorFound", "Name: " + name + ", BloodGroup: " + userBloodGroup + 
                                              ", Phone: " + phone + ", Image: " + profileImageUrl);
                                    }
                                } else {
                                    Log.w("UserCheck", "User with ID " + userId + " does not exist in the database.");
                                }
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("FirebaseError", "Error fetching user data: " + error.getMessage());
                            }
                        });

                    }

                    if (resultList.isEmpty()) {
                        Toast.makeText(BloodSearchActivity.this, "No donors found for " + bloodGroup, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BloodSearchActivity.this, "Found " + resultList.size() + " donors for " + bloodGroup, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(BloodSearchActivity.this, "No members found in this community.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching community members: " + error.getMessage());
            }
        });
    }


}
