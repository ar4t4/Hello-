package com.example.hello.models;

public class User {
    private String id;
    private String firstName;
    private String lastName;
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

    public String getFirstName() { return firstName != null ? firstName : ""; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName != null ? lastName : ""; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    // For compatibility with existing code
    public String getName() { 
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return "Unknown";
    }
    
    // For compatibility with existing code
    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : "";
        }
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    // Alias for getImageUrl for consistency with JoinRequestDialog
    public String getProfileImage() { return imageUrl; }

    public boolean isGroupChat() { return isGroupChat; }
    public void setIsGroupChat(boolean isGroupChat) { this.isGroupChat = isGroupChat; }
} 