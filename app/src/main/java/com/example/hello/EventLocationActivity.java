package com.example.hello;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class EventLocationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private double latitude, longitude;
    private String eventTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_location);

        // Get event details from intent
        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);
        eventTitle = getIntent().getStringExtra("title");

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng eventLocation = new LatLng(latitude, longitude);
        
        mMap.addMarker(new MarkerOptions()
                .position(eventLocation)
                .title(eventTitle));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 15));
    }
} 