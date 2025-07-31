package com.example.hello.models;

import java.io.Serializable;

/**
 * BluetoothMeshMessage - Message model for mesh network communication
 * 
 * This class represents a message in the Bluetooth mesh network with
 * routing information, message content, and metadata for network operations.
 */
public class BluetoothMeshMessage implements Serializable {
    private String messageId;
    private String type; // CHAT, DISCOVERY, TOPOLOGY, ROUTE_REQ, ROUTE_REPLY, HEARTBEAT
    private String sourceNodeId;
    private String targetNodeId; // null for broadcast
    private String content;
    private long timestamp;
    private int hopCount;
    private String senderName;
    private String senderAddress;

    // Default constructor for Gson
    public BluetoothMeshMessage() {}

    public BluetoothMeshMessage(String messageId, String type, String sourceNodeId, 
                               String targetNodeId, String content, long timestamp, int hopCount) {
        this.messageId = messageId;
        this.type = type;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.content = content;
        this.timestamp = timestamp;
        this.hopCount = hopCount;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    // Utility methods
    public boolean isChatMessage() {
        return "CHAT".equals(type);
    }

    public boolean isSystemMessage() {
        return !"CHAT".equals(type);
    }

    public boolean isBroadcast() {
        return targetNodeId == null;
    }

    @Override
    public String toString() {
        return "BluetoothMeshMessage{" +
                "messageId='" + messageId + '\'' +
                ", type='" + type + '\'' +
                ", sourceNodeId='" + sourceNodeId + '\'' +
                ", targetNodeId='" + targetNodeId + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", hopCount=" + hopCount +
                ", senderName='" + senderName + '\'' +
                '}';
    }
}
