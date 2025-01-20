package com.example.hello;

public class Member {
    private String name;
    private String home;
    private String college;

    public Member() {
        // Default constructor for Firebase deserialization
    }

    public Member(String name, String home, String college) {
        this.name = name;
        this.home = home;
        this.college = college;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }
}
