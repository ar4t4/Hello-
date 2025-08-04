package com.example.hello.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.hello.models.BluetoothMeshMessage;
import com.example.hello.models.BluetoothMeshNode;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * BluetoothMeshService - Advanced Bluetooth Mesh Networking Service
 * 
 * This service creates a mesh network where devices can communicate through
 * multiple hops via intermediate devices, forming a decentralized network.
 * 
 * Features:
 * - Multi-device connections
 * - Message routing and forwarding
 * - Network topology discovery
 * - Automatic route optimization
 * - Duplicate message prevention
 * - Network healing (reconnection)
 */
public class BluetoothMeshService {
    private static final String TAG = "BluetoothMeshService";
    private static final String SERVICE_NAME = "BluetoothMeshChat";
    private static final UUID MESH_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    
    // Message types for mesh protocol
    public static final String MSG_TYPE_CHAT = "CHAT";
    public static final String MSG_TYPE_DISCOVERY = "DISCOVERY";
    public static final String MSG_TYPE_TOPOLOGY = "TOPOLOGY";
    public static final String MSG_TYPE_ROUTE_REQUEST = "ROUTE_REQ";
    public static final String MSG_TYPE_ROUTE_REPLY = "ROUTE_REPLY";
    public static final String MSG_TYPE_HEARTBEAT = "HEARTBEAT";
    
    // Network states
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTENING = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_DISCOVERING = 4;
    
    // Network maintenance constants
    private static final long HEARTBEAT_INTERVAL = 10000; // 10 seconds
    private static final long DISCOVERY_INTERVAL = 30000; // 30 seconds
    private static final long STALE_NODE_THRESHOLD = 60000; // 1 minute
    private static final long AUTO_DISCOVERY_INTERVAL = 45000; // 45 seconds
    private static final int MAX_MESH_CONNECTIONS = 7; // Maximum connections
    
    public interface BluetoothMeshListener {
        void onMessageReceived(BluetoothMeshMessage message);
        void onNodeJoined(BluetoothMeshNode node);
        void onNodeLeft(BluetoothMeshNode node);
        void onNetworkTopologyChanged(List<BluetoothMeshNode> nodes);
        void onConnectionStateChanged(int state);
        void onDeviceDiscovered(BluetoothDevice device); // Add discovery callback
    }
    
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothMeshListener listener;
    private final Handler mainHandler;
    private final Context context;
    private final Gson gson;
    
    // Network state
    private int currentState;
    private String localNodeId;
    private BluetoothMeshNode localNode;
    
    // Connection management
    private AcceptThread acceptThread;
    private final Map<String, ConnectedThread> connectedDevices;
    private final Map<String, BluetoothMeshNode> networkNodes;
    private final Set<String> seenMessageIds;
    
    // Routing table: nodeId -> next hop nodeId
    private final Map<String, String> routingTable;
    
    // Auto-discovery optimization
    private int consecutiveEmptyDiscoveries = 0;
    private long lastSuccessfulDiscovery = 0;
    
    // Message queues and handling
    private final List<BluetoothMeshMessage> pendingMessages;
    private final Handler networkHandler;
    
    public BluetoothMeshService(Context context, BluetoothMeshListener listener) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
        
        // Initialize collections
        this.connectedDevices = new ConcurrentHashMap<>();
        this.networkNodes = new ConcurrentHashMap<>();
        this.seenMessageIds = new HashSet<>();
        this.routingTable = new ConcurrentHashMap<>();
        this.pendingMessages = new CopyOnWriteArrayList<>();
        
        // Create background thread for network operations
        this.networkHandler = new Handler(Looper.getMainLooper());
        
        // Initialize local node
        this.localNodeId = generateNodeId();
        
        // Get device name and address with permission check
        String deviceName = getDeviceName();
        String deviceAddress = getDeviceAddress();
        
        this.localNode = new BluetoothMeshNode(localNodeId, deviceName, 
                deviceAddress, System.currentTimeMillis());
        
        // Add local node to network
        networkNodes.put(localNodeId, localNode);
        
        this.currentState = STATE_NONE;
        
        Log.d(TAG, "BluetoothMeshService initialized with node ID: " + localNodeId);
    }
    
    private String getDeviceName() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return bluetoothAdapter.getName();
        }
        return "Unknown Device";
    }
    
    private String getDeviceAddress() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return bluetoothAdapter.getAddress();
        }
        return "00:00:00:00:00:00";
    }
    
    private String generateNodeId() {
        String address = getDeviceAddress();
        return address.replace(":", "") + "_" + System.currentTimeMillis();
    }
    
    public synchronized void startMeshNetwork() {
        Log.d(TAG, "Starting mesh network");
        
        setState(STATE_LISTENING);
        
        // Start accepting incoming connections
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        
        // Start network maintenance tasks
        startNetworkMaintenance();
    }
    
    public synchronized void connectToDevice(BluetoothDevice device) {
        Log.d(TAG, "Connecting to device: " + device.getName());
        
        // Check if already connected to this device
        String deviceAddress = device.getAddress();
        if (connectedDevices.containsKey(deviceAddress)) {
            Log.w(TAG, "Already connected to device: " + device.getName());
            return;
        }
        
        // Start connection thread
        ConnectThread connectThread = new ConnectThread(device);
        connectThread.start();
    }
    
    public void sendChatMessage(String messageText) {
        BluetoothMeshMessage message = new BluetoothMeshMessage(
                generateMessageId(),
                MSG_TYPE_CHAT,
                localNodeId,
                null, // broadcast to all
                messageText,
                System.currentTimeMillis(),
                0 // initial hop count
        );
        
        broadcastMessage(message);
    }
    
    private void broadcastMessage(BluetoothMeshMessage message) {
        Log.d(TAG, "Broadcasting message: " + message.getMessageId());
        
        // Add to seen messages to prevent loops
        seenMessageIds.add(message.getMessageId());
        
        // Send to all connected devices
        for (ConnectedThread connectedThread : connectedDevices.values()) {
            connectedThread.write(gson.toJson(message));
        }
        
        // If it's a chat message from this node, notify UI
        if (MSG_TYPE_CHAT.equals(message.getType()) && localNodeId.equals(message.getSourceNodeId())) {
            mainHandler.post(() -> listener.onMessageReceived(message));
        }
    }
    
    private void forwardMessage(BluetoothMeshMessage message, String fromDeviceId) {
        // Increment hop count
        message.setHopCount(message.getHopCount() + 1);
        
        // Check if we've seen this message before (loop prevention)
        if (seenMessageIds.contains(message.getMessageId())) {
            Log.d(TAG, "Dropping duplicate message: " + message.getMessageId());
            return;
        }
        
        // Check hop limit
        if (message.getHopCount() > 10) { // Max 10 hops
            Log.d(TAG, "Dropping message due to hop limit: " + message.getMessageId());
            return;
        }
        
        seenMessageIds.add(message.getMessageId());
        
        // Forward to all connected devices except the one it came from
        for (Map.Entry<String, ConnectedThread> entry : connectedDevices.entrySet()) {
            if (!entry.getKey().equals(fromDeviceId)) {
                entry.getValue().write(gson.toJson(message));
            }
        }
        
        // If it's a chat message, notify UI
        if (MSG_TYPE_CHAT.equals(message.getType())) {
            mainHandler.post(() -> listener.onMessageReceived(message));
        }
    }
    
    private void handleIncomingMessage(String messageJson, String fromDeviceId) {
        try {
            BluetoothMeshMessage message = gson.fromJson(messageJson, BluetoothMeshMessage.class);
            
            switch (message.getType()) {
                case MSG_TYPE_CHAT:
                    handleChatMessage(message, fromDeviceId);
                    break;
                case MSG_TYPE_DISCOVERY:
                    handleDiscoveryMessage(message, fromDeviceId);
                    break;
                case MSG_TYPE_TOPOLOGY:
                    handleTopologyMessage(message, fromDeviceId);
                    break;
                case MSG_TYPE_HEARTBEAT:
                    handleHeartbeatMessage(message, fromDeviceId);
                    break;
                default:
                    Log.w(TAG, "Unknown message type: " + message.getType());
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing message JSON", e);
        }
    }
    
    private void handleChatMessage(BluetoothMeshMessage message, String fromDeviceId) {
        // If message is for us or broadcast, process it
        if (message.getTargetNodeId() == null || localNodeId.equals(message.getTargetNodeId())) {
            mainHandler.post(() -> listener.onMessageReceived(message));
        }
        
        // Forward the message if it's not from us
        if (!localNodeId.equals(message.getSourceNodeId())) {
            forwardMessage(message, fromDeviceId);
        }
    }
    
    private void handleDiscoveryMessage(BluetoothMeshMessage message, String fromDeviceId) {
        // Update network topology with discovered node
        BluetoothMeshNode node = gson.fromJson(message.getContent(), BluetoothMeshNode.class);
        
        if (!networkNodes.containsKey(node.getNodeId())) {
            networkNodes.put(node.getNodeId(), node);
            updateRoutingTable();
            
            mainHandler.post(() -> listener.onNodeJoined(node));
            mainHandler.post(() -> listener.onNetworkTopologyChanged(new ArrayList<>(networkNodes.values())));
        }
        
        // Forward discovery message
        forwardMessage(message, fromDeviceId);
    }
    
    private void handleTopologyMessage(BluetoothMeshMessage message, String fromDeviceId) {
        // Handle network topology updates
        forwardMessage(message, fromDeviceId);
    }
    
    private void handleHeartbeatMessage(BluetoothMeshMessage message, String fromDeviceId) {
        // Update node last seen time
        BluetoothMeshNode node = networkNodes.get(message.getSourceNodeId());
        if (node != null) {
            node.setLastSeen(System.currentTimeMillis());
        }
    }
    
    private void updateRoutingTable() {
        // Simple routing: direct connections have priority
        routingTable.clear();
        
        for (String deviceId : connectedDevices.keySet()) {
            for (BluetoothMeshNode node : networkNodes.values()) {
                if (node.getBluetoothAddress().equals(deviceId)) {
                    routingTable.put(node.getNodeId(), deviceId);
                    break;
                }
            }
        }
    }
    
    private void startNetworkMaintenance() {
        // Send periodic heartbeats
        networkHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendHeartbeat();
                cleanupStaleNodes();
                networkHandler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        }, HEARTBEAT_INTERVAL);
        
        // Send discovery announcement
        sendDiscoveryAnnouncement();
        
        // Start automatic device discovery
        startAutomaticDiscovery();
        
        // Start automatic mesh expansion
        startMeshExpansion();
    }
    
    private void startAutomaticDiscovery() {
        networkHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (connectedDevices.size() < MAX_MESH_CONNECTIONS) {
                    // Smart discovery: increase interval after consecutive empty discoveries
                    long timeSinceLastSuccess = System.currentTimeMillis() - lastSuccessfulDiscovery;
                    
                    // If we've had many empty discoveries recently, reduce frequency
                    if (consecutiveEmptyDiscoveries >= 3 && timeSinceLastSuccess > 300000) { // 5 minutes
                        Log.d(TAG, "Reducing auto-discovery frequency due to consecutive empty discoveries");
                        networkHandler.postDelayed(this, AUTO_DISCOVERY_INTERVAL * 3); // 2.25 minutes
                    } else if (consecutiveEmptyDiscoveries >= 5) {
                        Log.d(TAG, "Pausing auto-discovery due to excessive empty discoveries");
                        networkHandler.postDelayed(this, AUTO_DISCOVERY_INTERVAL * 6); // 4.5 minutes
                    } else {
                        discoverNearbyMeshDevices();
                        networkHandler.postDelayed(this, AUTO_DISCOVERY_INTERVAL);
                    }
                } else {
                    // We have enough connections, check again later
                    networkHandler.postDelayed(this, AUTO_DISCOVERY_INTERVAL * 2);
                }
            }
        }, 5000); // Start after 5 seconds
    }
    
    private void startMeshExpansion() {
        networkHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                expandMeshNetwork();
                networkHandler.postDelayed(this, DISCOVERY_INTERVAL * 2); // Every minute
            }
        }, 15000); // Start after 15 seconds
    }
    
    private void discoverNearbyMeshDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth adapter not available for discovery");
            return;
        }
        
        // Check permissions first
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BLUETOOTH_SCAN permission not granted");
            return;
        }
        
        Log.d(TAG, "Starting automatic device discovery for mesh expansion");
        
        // Get paired devices first
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (device != null && !isAlreadyConnected(device)) {
                    Log.d(TAG, "Found paired device: " + getDeviceName(device));
                    attemptMeshConnection(device);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception accessing paired devices: " + e.getMessage());
        }
        
        // Start discovery for new devices
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            
            boolean discoveryStarted = bluetoothAdapter.startDiscovery();
            if (discoveryStarted) {
                Log.d(TAG, "Device discovery started successfully");
                currentState = STATE_DISCOVERING;
                mainHandler.post(() -> listener.onConnectionStateChanged(currentState));
                
                // Stop discovery after 12 seconds
                networkHandler.postDelayed(() -> {
                    try {
                        if (bluetoothAdapter.isDiscovering()) {
                            bluetoothAdapter.cancelDiscovery();
                            Log.d(TAG, "Discovery stopped after timeout");
                        }
                        // Track discovery completion
                        trackDiscoveryCompletion();
                    } catch (SecurityException e) {
                        Log.e(TAG, "Error stopping discovery: " + e.getMessage());
                    }
                }, 12000);
            } else {
                Log.w(TAG, "Failed to start device discovery");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting discovery: " + e.getMessage());
        }
    }
    
    private void expandMeshNetwork() {
        // Try to connect to devices discovered by connected nodes
        for (BluetoothMeshNode node : networkNodes.values()) {
            if (node != null && !node.getNodeId().equals(localNodeId)) {
                // Request topology information from this node
                requestNodeTopology(node.getNodeId());
            }
        }
    }
    
    private void requestNodeTopology(String nodeId) {
        BluetoothMeshMessage topologyRequest = new BluetoothMeshMessage(
                generateMessageId(),
                MSG_TYPE_TOPOLOGY,
                localNodeId,
                nodeId,
                "REQUEST",
                System.currentTimeMillis(),
                0
        );
        
        broadcastMessage(topologyRequest);
    }
    
    private void trackDiscoveryCompletion() {
        // Check if any new devices were discovered in this cycle
        boolean foundNewDevices = false;
        
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (!isAlreadyConnected(device) && connectedDevices.size() < MAX_MESH_CONNECTIONS) {
                    foundNewDevices = true;
                    break;
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception checking paired devices: " + e.getMessage());
        }
        
        if (foundNewDevices) {
            consecutiveEmptyDiscoveries = 0;
            lastSuccessfulDiscovery = System.currentTimeMillis();
            Log.d(TAG, "Discovery successful - found potential devices");
        } else {
            consecutiveEmptyDiscoveries++;
            Log.d(TAG, "Empty discovery cycle #" + consecutiveEmptyDiscoveries);
        }
    }
    
    private boolean isAlreadyConnected(BluetoothDevice device) {
        String deviceAddress = device.getAddress();
        return connectedDevices.containsKey(deviceAddress);
    }
    
    private void attemptMeshConnection(BluetoothDevice device) {
        if (connectedDevices.size() >= MAX_MESH_CONNECTIONS) {
            Log.d(TAG, "Maximum connections reached, skipping device: " + getDeviceName(device));
            return;
        }
        
        if (isAlreadyConnected(device)) {
            Log.d(TAG, "Already connected to device: " + getDeviceName(device));
            return;
        }
        
        Log.d(TAG, "Attempting mesh connection to: " + getDeviceName(device));
        
        // Notify listener about discovered device
        mainHandler.post(() -> listener.onDeviceDiscovered(device));
        
        // Try to connect after a small delay to avoid overwhelming
        networkHandler.postDelayed(() -> {
            connectToDevice(device);
        }, 2000);
    }
    
    public void onDeviceDiscovered(BluetoothDevice device) {
        if (device != null && !isAlreadyConnected(device)) {
            Log.d(TAG, "New device discovered: " + getDeviceName(device));
            attemptMeshConnection(device);
        }
    }
    
    private String getDeviceName(BluetoothDevice device) {
        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                String name = device.getName();
                return name != null ? name : "Unknown Device";
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting device name: " + e.getMessage());
        }
        return "Unknown Device";
    }
    
    private void sendHeartbeat() {
        BluetoothMeshMessage heartbeat = new BluetoothMeshMessage(
                generateMessageId(),
                MSG_TYPE_HEARTBEAT,
                localNodeId,
                null,
                "",
                System.currentTimeMillis(),
                0
        );
        
        broadcastMessage(heartbeat);
    }
    
    private void sendDiscoveryAnnouncement() {
        BluetoothMeshMessage discovery = new BluetoothMeshMessage(
                generateMessageId(),
                MSG_TYPE_DISCOVERY,
                localNodeId,
                null,
                gson.toJson(localNode),
                System.currentTimeMillis(),
                0
        );
        
        broadcastMessage(discovery);
    }
    
    private void cleanupStaleNodes() {
        long currentTime = System.currentTimeMillis();
        long staleThreshold = 60000; // 1 minute
        
        List<String> staleNodes = new ArrayList<>();
        for (BluetoothMeshNode node : networkNodes.values()) {
            if (!node.getNodeId().equals(localNodeId) && 
                (currentTime - node.getLastSeen()) > staleThreshold) {
                staleNodes.add(node.getNodeId());
            }
        }
        
        for (String nodeId : staleNodes) {
            BluetoothMeshNode removedNode = networkNodes.remove(nodeId);
            if (removedNode != null) {
                mainHandler.post(() -> listener.onNodeLeft(removedNode));
                mainHandler.post(() -> listener.onNetworkTopologyChanged(new ArrayList<>(networkNodes.values())));
            }
        }
        
        if (!staleNodes.isEmpty()) {
            updateRoutingTable();
        }
    }
    
    private String generateMessageId() {
        return localNodeId + "_" + System.currentTimeMillis() + "_" + Math.random();
    }
    
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + currentState + " -> " + state);
        currentState = state;
        mainHandler.post(() -> listener.onConnectionStateChanged(state));
    }
    
    public synchronized int getState() {
        return currentState;
    }
    
    public List<BluetoothMeshNode> getNetworkNodes() {
        return new ArrayList<>(networkNodes.values());
    }
    
    public int getConnectedDeviceCount() {
        return connectedDevices.size();
    }
    
    // Thread for accepting incoming connections
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;
        
        public AcceptThread() {
            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted");
                    serverSocket = null;
                    return;
                }
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, MESH_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
                serverSocket = null;
            }
        }
        
        public void run() {
            Log.d(TAG, "AcceptThread started");
            setName("AcceptThread");
            
            if (serverSocket == null) {
                Log.e(TAG, "ServerSocket is null, cannot accept connections");
                return;
            }
            
            BluetoothSocket socket;
            
            while (currentState != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                
                if (socket != null) {
                    synchronized (BluetoothMeshService.this) {
                        handleNewConnection(socket);
                    }
                }
            }
            Log.d(TAG, "AcceptThread ended");
        }
        
        public void cancel() {
            Log.d(TAG, "AcceptThread cancel");
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }
    
    // Thread for connecting to a device
    private class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private final BluetoothDevice device;
        
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            
            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted");
                    socket = null;
                    return;
                }
                socket = device.createInsecureRfcommSocketToServiceRecord(MESH_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
                socket = null;
            }
        }
        
        public void run() {
            Log.d(TAG, "ConnectThread started");
            setName("ConnectThread");
            
            if (socket == null) {
                Log.e(TAG, "Socket is null, cannot connect");
                return;
            }
            
            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted");
                    return;
                }
                socket.connect();
            } catch (IOException e) {
                Log.e(TAG, "unable to connect() socket", e);
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }
            
            synchronized (BluetoothMeshService.this) {
                handleNewConnection(socket);
            }
        }
        
        public void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
    private void handleNewConnection(BluetoothSocket socket) {
        Log.d(TAG, "handleNewConnection");
        
        String deviceAddress = socket.getRemoteDevice().getAddress();
        
        // Check if already connected to this device
        if (connectedDevices.containsKey(deviceAddress)) {
            Log.w(TAG, "Already connected to device: " + deviceAddress);
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing duplicate socket", e);
            }
            return;
        }
        
        // Create and start connected thread
        ConnectedThread connectedThread = new ConnectedThread(socket);
        connectedDevices.put(deviceAddress, connectedThread);
        connectedThread.start();
        
        setState(STATE_CONNECTED);
        
        // Reset discovery counters on successful connection
        consecutiveEmptyDiscoveries = 0;
        lastSuccessfulDiscovery = System.currentTimeMillis();
        Log.d(TAG, "Successful mesh connection - resetting discovery counters");
        
        // Send discovery announcement to new connection
        sendDiscoveryAnnouncement();
        
        // Update routing table
        updateRoutingTable();
    }
    
    // Thread for managing connected socket
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final String deviceAddress;
        
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            this.socket = socket;
            this.deviceAddress = socket.getRemoteDevice().getAddress();
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            
            inputStream = tmpIn;
            outputStream = tmpOut;
        }
        
        public void run() {
            Log.d(TAG, "ConnectedThread started for device: " + deviceAddress);
            byte[] buffer = new byte[1024];
            int bytes;
            
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String receivedMessage = new String(buffer, 0, bytes);
                    
                    // Handle the received message
                    handleIncomingMessage(receivedMessage, deviceAddress);
                    
                } catch (IOException e) {
                    Log.e(TAG, "disconnected from device: " + deviceAddress, e);
                    connectionLost();
                    break;
                }
            }
        }
        
        public void write(String message) {
            try {
                outputStream.write(message.getBytes());
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        
        public void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        
        private void connectionLost() {
            // Remove from connected devices
            connectedDevices.remove(deviceAddress);
            
            // Update routing table
            updateRoutingTable();
            
            // If no more connections, go back to listening
            if (connectedDevices.isEmpty()) {
                setState(STATE_LISTENING);
            }
        }
    }
    
    public synchronized void stop() {
        Log.d(TAG, "stop");
        
        setState(STATE_NONE);
        
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        
        for (ConnectedThread connectedThread : connectedDevices.values()) {
            connectedThread.cancel();
        }
        connectedDevices.clear();
        
        networkNodes.clear();
        routingTable.clear();
        seenMessageIds.clear();
    }
}
