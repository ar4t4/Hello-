package com.example.hello.models;

public class User {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String imageUrl;
    private boolean isGroupChat;

    public User() {
        // Required empty constructor for Firebase
        this.isGroupChat = false; // Default to false
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isGroupChat() { return isGroupChat; }
    public void setIsGroupChat(boolean isGroupChat) { this.isGroupChat = isGroupChat; }
} 