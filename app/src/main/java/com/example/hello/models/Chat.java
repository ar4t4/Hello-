package com.example.hello.models;

import java.util.List;

public class Chat {
    private String id;
    private String name; // For group chats
    private List<String> participants;
    private String lastMessage;
    private long lastMessageTime;
    private boolean isGroup;
    private String communityId;

    public Chat() {
        // Required empty constructor for Firebase
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public boolean isGroup() { return isGroup; }
    public void setGroup(boolean group) { isGroup = group; }

    public String getCommunityId() { return communityId; }
    public void setCommunityId(String communityId) { this.communityId = communityId; }
} 