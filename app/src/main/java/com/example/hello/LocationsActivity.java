package com.example.hello;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MemberAdapter memberAdapter;
    private List<Member> memberList;
    private DatabaseReference membersRef;
    private DatabaseReference usersRef;
    private MaterialButton btnStopSharingLocation;
    private String communityId;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locations);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        communityId = getIntent().getStringExtra("communityId");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewMembers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        btnStopSharingLocation = findViewById(R.id.btnStopSharingLocation);

        // Initialize adapter
        memberList = new ArrayList<>();
        memberAdapter = new MemberAdapter(this, memberList, this::onCallButtonClick);
        recyclerView.setAdapter(memberAdapter);

        // Initialize Firebase references
        membersRef = FirebaseDatabase.getInstance().getReference("Communities")
            .child(communityId).child("members");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Set up click listeners
        btnStopSharingLocation.setOnClickListener(v -> {
            if (btnStopSharingLocation.getText().toString().equals("Stop Sharing Location")) {
                stopSharingLocation();
            } else {
                startSharingLocation();
            }
        });

        // Check location permissions and start location updates
        checkLocationPermission();
        fetchMemberData();

        // Check current sharing status
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Users")
            .child(currentUserId)
            .child("sharingLocation")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean isSharing = snapshot.getValue(Boolean.class);
                    if (isSharing != null && isSharing) {
                        btnStopSharingLocation.setText("Stop Sharing Location");
                    } else {
                        btnStopSharingLocation.setText("Start Sharing Location");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchAndUpdateLocation();
        }
    }

    private void fetchAndUpdateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    usersRef.child(currentUserId).child("location")
                            .setValue(new LocationModel(latitude, longitude))
                            .addOnSuccessListener(aVoid -> Log.d("LocationUpdate", "Location updated successfully"))
                            .addOnFailureListener(e -> Log.e("LocationUpdate", "Failed to update location", e));
                } else {
                    Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchMemberData() {
        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberList.clear();
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String userId = memberSnapshot.getKey();
                    fetchUserDetails(userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to load member data", error.toException());
            }
        });
    }

    private void fetchUserDetails(String userId) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Member member = snapshot.getValue(Member.class);
                    if (member != null) {
                        member.setUid(userId);
                        memberList.add(member);
                        memberAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to load user data", error.toException());
            }
        });
    }

    private void stopSharingLocation() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
        
        // Set both location and sharingLocation status
        Map<String, Object> updates = new HashMap<>();
        updates.put("location", null);
        updates.put("sharingLocation", false);
        
        userRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Location sharing disabled", Toast.LENGTH_SHORT).show();
                btnStopSharingLocation.setText("Start Sharing Location");
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to stop sharing location", Toast.LENGTH_SHORT).show());
    }

    private void startSharingLocation() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
        
        userRef.child("sharingLocation").setValue(true)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Location sharing enabled", Toast.LENGTH_SHORT).show();
                btnStopSharingLocation.setText("Stop Sharing Location");
                // Update location immediately
                fetchAndUpdateLocation();
            });
    }

    private void onCallButtonClick(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchAndUpdateLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
