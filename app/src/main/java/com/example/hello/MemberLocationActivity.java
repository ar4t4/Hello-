package com.example.hello;

import android.app.AlertDialog;
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

public class MemberLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String memberId;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_location);

        memberId = getIntent().getStringExtra("memberId");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        checkLocationSharingStatus();
    }

    private void checkLocationSharingStatus() {
        usersRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean sharingLocation = snapshot.child("sharingLocation").getValue(Boolean.class);
                
                if (sharingLocation == null || !sharingLocation) {
                    showLocationDisabledDialog();
                    return;
                }
                
                fetchMemberLocation();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MemberLocationActivity.this, 
                    "Error checking location status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLocationDisabledDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Location Not Available")
            .setMessage("This user has disabled location sharing")
            .setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
                finish();
            })
            .setCancelable(false)
            .show();
    }

    private void fetchMemberLocation() {
        usersRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Member member = snapshot.getValue(Member.class);
                if (member != null && member.getLocation() != null) {
                    LatLng memberLatLng = new LatLng(
                            member.getLocation().getLatitude(),
                            member.getLocation().getLongitude());
                    mMap.addMarker(new MarkerOptions()
                            .position(memberLatLng)
                            .title(member.getName()));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(memberLatLng, 15));
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
    }
}
