package com.example.hello;

public class Member {
    private String firstName;
    private String lastName;
    private String email;
    private String home;
    private String college;
    private String school;
    private String district;
    private String university;
    private Location location;
    private String uid; // UID field
    private String bloodGroup;
    private String phone;
    private boolean bloodDonate;
    private String profileImageUrl; // Profile image URL from Cloudinary

    // Default constructor for Firebase
    public Member() {
    }

    // Constructor with UID
    public Member(String uid, String firstName, String lastName, String email, String home, String college, String school, String district, String university, Location location) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.home = home;
        this.college = college;
        this.school = school;
        this.district = district;
        this.university = university;
        this.location = location;
    }

    // Getter and Setter for UID
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    // Getter and Setter for FirstName
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    // Getter and Setter for LastName
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    // Getter for full name (for compatibility)
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

    // Setter for full name (for compatibility)
    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : "";
        }
    }

    // Getter and Setter for Email
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Getter and Setter for Home
    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    // Getter and Setter for College
    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    // Getter and Setter for University
    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    // Getter and Setter for School
    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    // Getter and Setter for District
    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    // Getter and Setter for Location
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    // Getter and Setter for Blood Group
    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    // Getter and Setter for Phone
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // Getter and Setter for Blood Donate
    public boolean isBloodDonate() {
        return bloodDonate;
    }

    public void setBloodDonate(boolean bloodDonate) {
        this.bloodDonate = bloodDonate;
    }
    
    // Getter and Setter for Profile Image URL
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // Nested Location class
    public static class Location {
        private double latitude;
        private double longitude;

        // Default constructor for Firebase
        public Location() {
        }

        // Constructor with parameters
        public Location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        // Getter and Setter for Latitude
        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        // Getter and Setter for Longitude
        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }
}
