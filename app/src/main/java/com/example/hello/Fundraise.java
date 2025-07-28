package com.example.hello;

public class Fundraise {
    private String id;
    private String title;
    private String description;
    private String amountNeeded;
    private double raisedAmount;
    private String imageUrl;
    private String creatorId;
    private long createdAt;
    private String donationMethod;

    public Fundraise() {
        // Required empty constructor for Firebase
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAmountNeeded() { return amountNeeded; }
    public void setAmountNeeded(String amountNeeded) { this.amountNeeded = amountNeeded; }

    public double getRaisedAmount() { return raisedAmount; }
    public void setRaisedAmount(double raisedAmount) { this.raisedAmount = raisedAmount; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getDonationMethod() { return donationMethod; }
    public void setDonationMethod(String donationMethod) { this.donationMethod = donationMethod; }
} 