package com.example.hello.models;

import java.util.HashMap;
import java.util.Map;

public class JoinRequest {
    private String id;
    private String userId;
    private String userName;
    private String userImageUrl;
    private String communityId;
    private String communityName;
    private long timestamp;
    private String status; // "pending", "accepted", "rejected"
    
    public JoinRequest() {
        // Required empty constructor for Firebase
    }
    
    public JoinRequest(String userId, String userName, String userImageUrl, 
                      String communityId, String communityName) {
        this.userId = userId;
        this.userName = userName;
        this.userImageUrl = userImageUrl;
        this.communityId = communityId;
        this.communityName = communityName;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
    }
    
    // Convert to a Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("userImageUrl", userImageUrl);
        map.put("communityId", communityId);
        map.put("communityName", communityName);
        map.put("timestamp", timestamp);
        map.put("status", status);
        return map;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserImageUrl() { return userImageUrl; }
    public void setUserImageUrl(String userImageUrl) { this.userImageUrl = userImageUrl; }
    
    public String getCommunityId() { return communityId; }
    public void setCommunityId(String communityId) { this.communityId = communityId; }
    
    public String getCommunityName() { return communityName; }
    public void setCommunityName(String communityName) { this.communityName = communityName; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
} 