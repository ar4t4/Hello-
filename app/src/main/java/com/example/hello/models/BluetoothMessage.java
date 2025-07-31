package com.example.hello.models;

public class BluetoothMessage {
    private String content;
    private boolean isOutgoing;
    private long timestamp;
    private String senderName;

    public BluetoothMessage() {
        // Empty constructor for Firebase
    }

    public BluetoothMessage(String content, boolean isOutgoing, long timestamp) {
        this.content = content;
        this.isOutgoing = isOutgoing;
        this.timestamp = timestamp;
        this.senderName = isOutgoing ? "You" : "Friend";
    }

    public BluetoothMessage(String content, boolean isOutgoing, long timestamp, String senderName) {
        this.content = content;
        this.isOutgoing = isOutgoing;
        this.timestamp = timestamp;
        this.senderName = senderName;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public void setOutgoing(boolean outgoing) {
        isOutgoing = outgoing;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
