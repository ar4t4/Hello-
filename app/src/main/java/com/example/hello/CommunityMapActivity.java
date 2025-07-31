package com.example.hello;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CommunityMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String communityId;
    private DatabaseReference membersRef;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_map);

        communityId = getIntent().getStringExtra("communityId");
        
        // Initialize Firebase references
        membersRef = FirebaseDatabase.getInstance().getReference("Communities")
            .child(communityId != null ? communityId : "default").child("members");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        loadMemberLocations();
    }

    private void loadMemberLocations() {
        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String userId = memberSnapshot.getKey();
                    loadUserLocation(userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CommunityMap", "Failed to load members", error.toException());
                Toast.makeText(CommunityMapActivity.this, "Failed to load member locations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserLocation(String userId) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Member member = snapshot.getValue(Member.class);
                    if (member != null && member.getLocation() != null) {
                        // Add marker for this member
                        LatLng memberLocation = new LatLng(
                            member.getLocation().getLatitude(),
                            member.getLocation().getLongitude()
                        );
                        
                        String title = member.getFirstName();
                        String snippet = member.getBloodGroup() != null ? 
                            "Blood Group: " + member.getBloodGroup() : "Community Member";
                        
                        mMap.addMarker(new MarkerOptions()
                            .position(memberLocation)
                            .title(title)
                            .snippet(snippet));
                        
                        // Move camera to first location found
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(memberLocation, 12f));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CommunityMap", "Failed to load user location", error.toException());
            }
        });
    }
}
