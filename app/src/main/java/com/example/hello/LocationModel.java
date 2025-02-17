package com.example.hello;

public class LocationModel {
    private double latitude;
    private double longitude;

    public LocationModel() {
        // Default constructor required for Firebase
    }

    public LocationModel(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
