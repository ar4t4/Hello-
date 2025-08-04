package com.example.hello;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.adapters.BluetoothMeshMessageAdapter;
import com.example.hello.adapters.BluetoothMeshNodeAdapter;
import com.example.hello.adapters.BluetoothMeshDeviceAdapter;
import com.example.hello.models.BluetoothMeshMessage;
import com.example.hello.models.BluetoothMeshNode;
import com.example.hello.services.BluetoothMeshService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * BluetoothMeshChatActivity - Advanced Bluetooth Mesh Chat Interface
 * 
 * This activity provides a modern UI for mesh networking chat where users can
 * communicate through multiple hops via intermediate devices.
 * 
 * Features:
 * - Real-time group messaging through mesh network
 * - Network topology visualization
 * - Device discovery and connection
 * - Multi-hop message routing
 * - Connection status monitoring
 */
public class BluetoothMeshChatActivity extends AppCompatActivity 
        implements BluetoothMeshService.BluetoothMeshListener {
    
    private static final String TAG = "BluetoothMeshChat";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    private static final int REQUEST_DISCOVERABLE = 3;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothMeshService meshService;
    private BluetoothMeshNodeAdapter nodeAdapter;
    private BluetoothMeshMessageAdapter messageAdapter;
    private List<BluetoothDevice> discoveredDevices;
    private List<BluetoothMeshMessage> messages;
    private List<BluetoothMeshNode> networkNodes;
    
    // Discovery state tracking
    private boolean isManualDiscovery = false;

    // UI Components
    private MaterialCardView deviceDiscoveryCard;
    private MaterialCardView chatCard;
    private MaterialCardView networkTopologyCard;
    private RecyclerView devicesRecyclerView;
    private RecyclerView messagesRecyclerView;
    private RecyclerView nodesRecyclerView;
    private MaterialButton btnStartDiscovery;
    private MaterialButton btnMakeDiscoverable;
    private MaterialButton btnShowTopology;
    private CircularProgressIndicator progressIndicator;
    private EditText messageInput;
    private FloatingActionButton btnSendMessage;
    private MaterialToolbar toolbar;

    private boolean isNetworkActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_mesh_chat_simple);

        initializeViews();
        setupToolbar();
        setupRecyclerViews();
        setupClickListeners();
        requestBluetoothPermissions(); // Request permissions first, then initialize Bluetooth
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        deviceDiscoveryCard = findViewById(R.id.device_discovery_card);
        chatCard = findViewById(R.id.chat_card);
        networkTopologyCard = findViewById(R.id.network_topology_card);
        devicesRecyclerView = findViewById(R.id.devices_recycler_view);
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        nodesRecyclerView = findViewById(R.id.nodes_recycler_view);
        btnStartDiscovery = findViewById(R.id.btn_start_discovery);
        btnMakeDiscoverable = findViewById(R.id.btn_make_discoverable);
        btnShowTopology = findViewById(R.id.btn_show_topology);
        progressIndicator = findViewById(R.id.progress_indicator);
        messageInput = findViewById(R.id.message_input);
        btnSendMessage = findViewById(R.id.btn_send_message);

        // Initialize data lists
        discoveredDevices = new ArrayList<>();
        messages = new ArrayList<>();
        networkNodes = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mesh Chat Network");
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize mesh service
        meshService = new BluetoothMeshService(this, this);
    }

    private void setupRecyclerViews() {
        // Device discovery RecyclerView
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Messages RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new BluetoothMeshMessageAdapter(messages);
        messagesRecyclerView.setAdapter(messageAdapter);
        
        // Network nodes RecyclerView
        nodesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        nodeAdapter = new BluetoothMeshNodeAdapter(networkNodes, this::connectToNode);
        nodesRecyclerView.setAdapter(nodeAdapter);
    }

    private void setupClickListeners() {
        btnStartDiscovery.setOnClickListener(v -> startDeviceDiscovery());
        btnMakeDiscoverable.setOnClickListener(v -> makeDiscoverable());
        btnShowTopology.setOnClickListener(v -> showNetworkTopology());
        btnSendMessage.setOnClickListener(v -> sendMessage());
        
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void requestBluetoothPermissions() {
        List<String> permissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        } else {
            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), 
                REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            enableBluetoothAndStartMesh();
        }
    }

    private void enableBluetoothAndStartMesh() {
        // Initialize Bluetooth now that we have permissions
        initializeBluetooth();
        
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startMeshNetwork();
        }
    }

    private void startMeshNetwork() {
        Log.d(TAG, "Starting mesh network");
        meshService.startMeshNetwork();
        isNetworkActive = true;
        updateUI();
        showPairedDevices();
    }

    private void showPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        discoveredDevices.clear();
        discoveredDevices.addAll(pairedDevices);
        
        // Create adapter for paired devices with mesh connection option
        BluetoothMeshDeviceAdapter deviceAdapter = new BluetoothMeshDeviceAdapter(
            discoveredDevices, this::connectToDevice);
        devicesRecyclerView.setAdapter(deviceAdapter);
    }

    private void startDeviceDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth scan permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set flag for manual discovery to show toast
        isManualDiscovery = true;
        
        progressIndicator.setVisibility(View.VISIBLE);
        btnStartDiscovery.setEnabled(false);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);

        // Start discovery
        bluetoothAdapter.startDiscovery();
        
        Toast.makeText(this, "Searching for nearby devices...", Toast.LENGTH_SHORT).show();
    }

    private void makeDiscoverable() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth advertise permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
    }

    private void connectToDevice(BluetoothDevice device) {
        Log.d(TAG, "Connecting to device: " + device.getName());
        
        // Cancel discovery if running
        if (bluetoothAdapter.isDiscovering()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.cancelDiscovery();
        }
        
        // Connect via mesh service
        meshService.connectToDevice(device);
        
        Toast.makeText(this, "Connecting to " + device.getName() + "...", Toast.LENGTH_SHORT).show();
    }

    private void connectToNode(BluetoothMeshNode node) {
        // This is for connecting to nodes that are not directly connected
        // but reachable through the mesh network
        Toast.makeText(this, "Node: " + node.getDisplayName() + 
            " (" + node.getConnectionInfo() + ")", Toast.LENGTH_SHORT).show();
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            return;
        }
        
        if (!isNetworkActive || meshService.getConnectedDeviceCount() == 0) {
            Toast.makeText(this, "No devices connected to mesh network", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Send message through mesh network
        meshService.sendChatMessage(messageText);
        
        // Clear input
        messageInput.setText("");
    }

    private void showNetworkTopology() {
        if (networkNodes.isEmpty()) {
            Toast.makeText(this, "No nodes in network", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog showing network topology
        StringBuilder topology = new StringBuilder();
        topology.append("Network Topology:\n\n");
        
        for (BluetoothMeshNode node : networkNodes) {
            topology.append("• ").append(node.getDisplayName()).append("\n");
            topology.append("  ").append(node.getConnectionInfo()).append("\n");
            topology.append("  Status: ").append(node.getStatus()).append("\n");
            if (!node.getConnectedNodes().isEmpty()) {
                topology.append("  Connected to: ").append(node.getConnectedNodes().size()).append(" nodes\n");
            }
            topology.append("\n");
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Network Topology")
                .setMessage(topology.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateUI() {
        if (isNetworkActive && meshService.getConnectedDeviceCount() > 0) {
            // Show chat interface
            deviceDiscoveryCard.setVisibility(View.GONE);
            chatCard.setVisibility(View.VISIBLE);
            networkTopologyCard.setVisibility(View.VISIBLE);
            
            // Update toolbar subtitle
            String subtitle = meshService.getConnectedDeviceCount() + " connected, " + 
                            networkNodes.size() + " nodes in network";
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(subtitle);
            }
        } else {
            // Show discovery interface
            deviceDiscoveryCard.setVisibility(View.VISIBLE);
            chatCard.setVisibility(View.GONE);
            networkTopologyCard.setVisibility(View.GONE);
            
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle("Not connected");
            }
        }
    }

    // BluetoothMeshService.BluetoothMeshListener implementation
    @Override
    public void onMessageReceived(BluetoothMeshMessage message) {
        Log.d(TAG, "Message received: " + message.getContent());
        
        runOnUiThread(() -> {
            messages.add(message);
            messageAdapter.notifyItemInserted(messages.size() - 1);
            messagesRecyclerView.scrollToPosition(messages.size() - 1);
        });
    }

    @Override
    public void onNodeJoined(BluetoothMeshNode node) {
        Log.d(TAG, "Node joined: " + node.getDisplayName());
        
        runOnUiThread(() -> {
            Toast.makeText(this, node.getDisplayName() + " joined the network", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onNodeLeft(BluetoothMeshNode node) {
        Log.d(TAG, "Node left: " + node.getDisplayName());
        
        runOnUiThread(() -> {
            Toast.makeText(this, node.getDisplayName() + " left the network", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onNetworkTopologyChanged(List<BluetoothMeshNode> nodes) {
        Log.d(TAG, "Network topology changed. Nodes: " + nodes.size());
        
        runOnUiThread(() -> {
            networkNodes.clear();
            networkNodes.addAll(nodes);
            nodeAdapter.notifyDataSetChanged();
            updateUI();
        });
    }

    @Override
    public void onConnectionStateChanged(int state) {
        Log.d(TAG, "Connection state changed: " + state);
        
        runOnUiThread(() -> {
            switch (state) {
                case BluetoothMeshService.STATE_NONE:
                    Toast.makeText(this, "Network stopped", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.GONE);
                    break;
                case BluetoothMeshService.STATE_LISTENING:
                    Toast.makeText(this, "Listening for connections...", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.GONE);
                    break;
                case BluetoothMeshService.STATE_CONNECTING:
                    Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.VISIBLE);
                    break;
                case BluetoothMeshService.STATE_CONNECTED:
                    Toast.makeText(this, "Connected to mesh network!", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.GONE);
                    break;
                case BluetoothMeshService.STATE_DISCOVERING:
                    Toast.makeText(this, "Auto-discovering devices...", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.VISIBLE);
                    break;
                default:
                    progressIndicator.setVisibility(View.GONE);
                    break;
            }
            updateUI();
        });
    }
    
    // Add new method for device discovery callback
    public void onDeviceDiscovered(BluetoothDevice device) {
        runOnUiThread(() -> {
            if (device != null && !discoveredDevices.contains(device)) {
                discoveredDevices.add(device);
                
                // Check if device name suggests it's a mesh-compatible device
                String deviceName = "Unknown Device";
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        deviceName = device.getName();
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "Error getting device name: " + e.getMessage());
                }
                
                Log.d(TAG, "Auto-discovered device: " + deviceName);
                
                // Update the adapter
                BluetoothMeshDeviceAdapter deviceAdapter = new BluetoothMeshDeviceAdapter(
                    discoveredDevices, this::connectToDevice);
                devicesRecyclerView.setAdapter(deviceAdapter);
                
                // Show a brief notification for automatic discovery
                Toast.makeText(this, "📱 Found: " + deviceName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // BroadcastReceiver for device discovery
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    
                    BluetoothMeshDeviceAdapter deviceAdapter = new BluetoothMeshDeviceAdapter(
                        discoveredDevices, BluetoothMeshChatActivity.this::connectToDevice);
                    devicesRecyclerView.setAdapter(deviceAdapter);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressIndicator.setVisibility(View.GONE);
                btnStartDiscovery.setEnabled(true);
                
                // Only show toast for manual discovery, not automatic background discovery
                if (isManualDiscovery) {
                    Toast.makeText(context, "Discovery finished. Found " + 
                        discoveredDevices.size() + " devices", Toast.LENGTH_SHORT).show();
                    isManualDiscovery = false; // Reset flag
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                enableBluetoothAndStartMesh();
            } else {
                Toast.makeText(this, "Bluetooth permissions required for mesh networking", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    startMeshNetwork();
                } else {
                    Toast.makeText(this, "Bluetooth is required for mesh networking", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
                
            case REQUEST_DISCOVERABLE:
                if (resultCode > 0) {
                    Toast.makeText(this, "Device is now discoverable for " + resultCode + " seconds", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Device discoverability declined", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(discoveryReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
        
        // Stop mesh service
        if (meshService != null) {
            meshService.stop();
        }
    }
}
