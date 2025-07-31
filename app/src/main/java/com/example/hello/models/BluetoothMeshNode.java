package com.example.hello.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * BluetoothMeshNode - Represents a node in the Bluetooth mesh network
 * 
 * This class contains information about each device in the mesh network
 * including connection status, capabilities, and routing information.
 */
public class BluetoothMeshNode implements Serializable {
    private String nodeId;
    private String deviceName;
    private String bluetoothAddress;
    private long firstSeen;
    private long lastSeen;
    private boolean isDirectlyConnected;
    private int hopDistance; // Number of hops to reach this node
    private String nextHopNodeId; // Next hop to reach this node
    private List<String> connectedNodes; // Nodes directly connected to this node
    private NodeStatus status;
    private long lastHeartbeat;
    
    public enum NodeStatus {
        ONLINE,
        OFFLINE,
        CONNECTING,
        UNREACHABLE
    }

    // Default constructor for Gson
    public BluetoothMeshNode() {
        this.connectedNodes = new ArrayList<>();
        this.status = NodeStatus.OFFLINE;
    }

    public BluetoothMeshNode(String nodeId, String deviceName, String bluetoothAddress, long timestamp) {
        this.nodeId = nodeId;
        this.deviceName = deviceName;
        this.bluetoothAddress = bluetoothAddress;
        this.firstSeen = timestamp;
        this.lastSeen = timestamp;
        this.lastHeartbeat = timestamp;
        this.isDirectlyConnected = false;
        this.hopDistance = Integer.MAX_VALUE; // Unknown distance initially
        this.connectedNodes = new ArrayList<>();
        this.status = NodeStatus.ONLINE;
    }

    // Getters and Setters
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public long getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(long firstSeen) {
        this.firstSeen = firstSeen;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
        this.lastHeartbeat = lastSeen;
    }

    public boolean isDirectlyConnected() {
        return isDirectlyConnected;
    }

    public void setDirectlyConnected(boolean directlyConnected) {
        isDirectlyConnected = directlyConnected;
        if (directlyConnected) {
            this.hopDistance = 1;
            this.nextHopNodeId = null; // Direct connection
        }
    }

    public int getHopDistance() {
        return hopDistance;
    }

    public void setHopDistance(int hopDistance) {
        this.hopDistance = hopDistance;
    }

    public String getNextHopNodeId() {
        return nextHopNodeId;
    }

    public void setNextHopNodeId(String nextHopNodeId) {
        this.nextHopNodeId = nextHopNodeId;
    }

    public List<String> getConnectedNodes() {
        return connectedNodes;
    }

    public void setConnectedNodes(List<String> connectedNodes) {
        this.connectedNodes = connectedNodes;
    }

    public void addConnectedNode(String nodeId) {
        if (!connectedNodes.contains(nodeId)) {
            connectedNodes.add(nodeId);
        }
    }

    public void removeConnectedNode(String nodeId) {
        connectedNodes.remove(nodeId);
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    // Utility methods
    public boolean isOnline() {
        return status == NodeStatus.ONLINE;
    }

    public boolean isReachable() {
        return hopDistance != Integer.MAX_VALUE && status != NodeStatus.UNREACHABLE;
    }

    public long getTimeSinceLastSeen() {
        return System.currentTimeMillis() - lastSeen;
    }

    public long getTimeSinceLastHeartbeat() {
        return System.currentTimeMillis() - lastHeartbeat;
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.lastSeen = lastHeartbeat;
        if (status == NodeStatus.OFFLINE || status == NodeStatus.UNREACHABLE) {
            status = NodeStatus.ONLINE;
        }
    }

    public String getDisplayName() {
        if (deviceName != null && !deviceName.trim().isEmpty()) {
            return deviceName;
        }
        return "Unknown Device (" + bluetoothAddress + ")";
    }

    public String getConnectionInfo() {
        if (isDirectlyConnected) {
            return "Direct Connection";
        } else if (hopDistance == Integer.MAX_VALUE) {
            return "Unreachable";
        } else {
            return hopDistance + " hop" + (hopDistance > 1 ? "s" : "") + " away";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BluetoothMeshNode that = (BluetoothMeshNode) obj;
        return nodeId != null ? nodeId.equals(that.nodeId) : that.nodeId == null;
    }

    @Override
    public int hashCode() {
        return nodeId != null ? nodeId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BluetoothMeshNode{" +
                "nodeId='" + nodeId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", bluetoothAddress='" + bluetoothAddress + '\'' +
                ", isDirectlyConnected=" + isDirectlyConnected +
                ", hopDistance=" + hopDistance +
                ", status=" + status +
                ", connectedNodes=" + connectedNodes.size() +
                '}';
    }
}
