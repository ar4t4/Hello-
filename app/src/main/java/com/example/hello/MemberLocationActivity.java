package com.example.hello;

import android.os.Bundle;
import android.util.Log;

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

public class MemberLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String communityId;
    private DatabaseReference membersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_location);

        communityId = getIntent().getStringExtra("communityId");
        membersRef = FirebaseDatabase.getInstance().getReference("Communities").child(communityId).child("members");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void fetchMemberData() {
        membersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    Member member = memberSnapshot.getValue(Member.class);
                    if (member != null && member.getLocation() != null) {
                        LatLng memberLatLng = new LatLng(
                                member.getLocation().getLatitude(),
                                member.getLocation().getLongitude());
                        mMap.addMarker(new MarkerOptions()
                                .position(memberLatLng)
                                .title(member.getName())
                                .snippet("Home: " + member.getHome() + ", College: " + member.getCollege()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to load member data", error.toException());
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        fetchMemberData();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(23.810331, 90.412521), 10)); // Default location
    }
}
