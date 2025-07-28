package com.example.hello.models;

import com.google.firebase.database.PropertyName;
import java.util.HashMap;
import java.util.Map;

public class Event {
    private String id;
    private String title;
    private String description;
    private String communityId;
    private String creatorId;
    private long dateTime;
    private String location;
    private Map<String, Boolean> participants;
    private double latitude;
    private double longitude;

    public Event() {
        // Required empty constructor for Firebase
        participants = new HashMap<>();
    }

    public Event(String title, String description, String communityId, String creatorId, 
                long dateTime, String location, double latitude, double longitude) {
        this.title = title;
        this.description = description;
        this.communityId = communityId;
        this.creatorId = creatorId;
        this.dateTime = dateTime;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.participants = new HashMap<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCommunityId() { return communityId; }
    public void setCommunityId(String communityId) { this.communityId = communityId; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public long getDateTime() { return dateTime; }
    public void setDateTime(long dateTime) { this.dateTime = dateTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Map<String, Boolean> getParticipants() { return participants; }
    public void setParticipants(Map<String, Boolean> participants) { this.participants = participants; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
} 