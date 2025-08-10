package com.example.hello.models;

import org.json.JSONObject;

public class BluetoothMessage {
    private String content;
    private boolean isOutgoing;
    private boolean isSent;
    private long timestamp;
    private String senderName;

    public BluetoothMessage() {
        // Empty constructor
    }

    public BluetoothMessage(String content, boolean isOutgoing, long timestamp) {
        this.content = content;
        this.isOutgoing = isOutgoing;
        this.isSent = isOutgoing;
        this.timestamp = timestamp;
        this.senderName = isOutgoing ? "You" : "Friend";
    }

    public BluetoothMessage(String content, String senderName, long timestamp, boolean isSent) {
        this.content = content;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.isSent = isSent;
        this.isOutgoing = isSent;
    }

    // Convert to JSON string
    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("content", content);
            json.put("senderName", senderName);
            json.put("timestamp", timestamp);
            json.put("isSent", isSent);
            return json.toString();
        } catch (Exception e) {
            return "{\"content\":\"" + content + "\",\"senderName\":\"" + senderName + "\",\"timestamp\":" + timestamp + ",\"isSent\":" + isSent + "}";
        }
    }

    // Create from JSON string
    public static BluetoothMessage fromJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            BluetoothMessage message = new BluetoothMessage();
            message.content = json.getString("content");
            message.senderName = json.getString("senderName");
            message.timestamp = json.getLong("timestamp");
            message.isSent = json.getBoolean("isSent");
            message.isOutgoing = false; // Received message
            return message;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON message", e);
        }
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

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
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
