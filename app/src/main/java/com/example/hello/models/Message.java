package com.example.hello.models;

public class Message {
    private String id;
    private String senderId;
    private String content;
    private long timestamp;
    private String chatId;
    private int messageType; // 0 for text, 1 for image, etc.

    public Message() {
        // Required empty constructor for Firebase
    }

    public Message(String senderId, String content, String chatId) {
        this.senderId = senderId;
        this.content = content;
        this.chatId = chatId;
        this.timestamp = System.currentTimeMillis();
        this.messageType = 0; // Default to text
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public int getMessageType() { return messageType; }
    public void setMessageType(int messageType) { this.messageType = messageType; }
} 