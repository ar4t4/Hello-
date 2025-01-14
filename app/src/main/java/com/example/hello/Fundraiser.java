package com.example.hello;

public class Fundraiser {
    private String fundraiseId;
    private String communityId;
    private String title;
    private String amountNeeded;
    private String donationMethod;
    private String creatorId;

    // Empty constructor required for Firebase
    public Fundraiser() {
    }

    // Constructor to initialize the Fundraiser object
    public Fundraiser(String fundraiseId, String communityId, String title, String amountNeeded, String donationMethod, String creatorId) {
        this.fundraiseId = fundraiseId;
        this.communityId = communityId;
        this.title = title;
        this.amountNeeded = amountNeeded;
        this.donationMethod = donationMethod;
        this.creatorId = creatorId;
    }

    // Getters and Setters
    public String getFundraiseId() {
        return fundraiseId;
    }

    public void setFundraiseId(String fundraiseId) {
        this.fundraiseId = fundraiseId;
    }

    public String getCommunityId() {
        return communityId;
    }

    public void setCommunityId(String communityId) {
        this.communityId = communityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAmountNeeded() {
        return amountNeeded;
    }

    public void setAmountNeeded(String amountNeeded) {
        this.amountNeeded = amountNeeded;
    }

    public String getDonationMethod() {
        return donationMethod;
    }

    public void setDonationMethod(String donationMethod) {
        this.donationMethod = donationMethod;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
}
