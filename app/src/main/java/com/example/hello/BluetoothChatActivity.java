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
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.adapters.BluetoothDeviceAdapter;
import com.example.hello.adapters.BluetoothMessageAdapter;
import com.example.hello.models.BluetoothMessage;
import com.example.hello.services.BluetoothChatService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothChatActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothChatActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothChatService chatService;
    private BluetoothDeviceAdapter deviceAdapter;
    private BluetoothMessageAdapter messageAdapter;
    private List<BluetoothDevice> discoveredDevices;
    private List<BluetoothMessage> messages;

    // UI Components
    private MaterialCardView deviceDiscoveryCard;
    private MaterialCardView chatCard;
    private RecyclerView devicesRecyclerView;
    private RecyclerView messagesRecyclerView;
    private MaterialButton btnStartDiscovery;
    private MaterialButton btnMakeDiscoverable;
    private CircularProgressIndicator progressIndicator;
    private TextInputEditText messageInput;
    private FloatingActionButton btnSendMessage;
    private MaterialToolbar toolbar;

    private boolean isConnected = false;
    private String connectedDeviceName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_chat);

        initializeViews();
        setupToolbar();
        initializeBluetooth();
        setupRecyclerViews();
        setupClickListeners();
        requestBluetoothPermissions();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        deviceDiscoveryCard = findViewById(R.id.deviceDiscoveryCard);
        chatCard = findViewById(R.id.chatCard);
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        btnStartDiscovery = findViewById(R.id.btnStartDiscovery);
        btnMakeDiscoverable = findViewById(R.id.btnMakeDiscoverable);
        progressIndicator = findViewById(R.id.progressIndicator);
        messageInput = findViewById(R.id.messageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        // Initially show device discovery and hide chat
        deviceDiscoveryCard.setVisibility(View.VISIBLE);
        chatCard.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Chat Offline");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        discoveredDevices = new ArrayList<>();
        messages = new ArrayList<>();
    }

    private void setupRecyclerViews() {
        // Setup devices RecyclerView
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new BluetoothDeviceAdapter(discoveredDevices, this::onDeviceSelected);
        devicesRecyclerView.setAdapter(deviceAdapter);

        // Setup messages RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new BluetoothMessageAdapter(messages);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnStartDiscovery.setOnClickListener(v -> startDeviceDiscovery());
        btnMakeDiscoverable.setOnClickListener(v -> makeDeviceDiscoverable());
        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void requestBluetoothPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        } else {
            // Pre-Android 12 permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            enableBluetoothIfNeeded();
        }
    }

    private void enableBluetoothIfNeeded() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            initializeChatService();
            loadPairedDevices();
        }
    }

    private void initializeChatService() {
        chatService = new BluetoothChatService(this, new BluetoothChatService.BluetoothChatListener() {
            @Override
            public void onMessageReceived(BluetoothMessage message) {
                runOnUiThread(() -> {
                    messages.add(message);
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onConnectionEstablished(String deviceName) {
                runOnUiThread(() -> {
                    isConnected = true;
                    connectedDeviceName = deviceName;
                    updateUI();
                    Toast.makeText(BluetoothChatActivity.this, 
                        "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onConnectionLost() {
                runOnUiThread(() -> {
                    isConnected = false;
                    connectedDeviceName = "";
                    updateUI();
                    Toast.makeText(BluetoothChatActivity.this, 
                        "Connection lost", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onConnectionFailed() {
                runOnUiThread(() -> {
                    Toast.makeText(BluetoothChatActivity.this, 
                        "Failed to connect", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            discoveredDevices.clear();
            discoveredDevices.addAll(pairedDevices);
            deviceAdapter.notifyDataSetChanged();
        }
    }

    private void startDeviceDiscovery() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        progressIndicator.setVisibility(View.VISIBLE);
        btnStartDiscovery.setEnabled(false);

        // Register broadcast receiver for discovery
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);

        bluetoothAdapter.startDiscovery();
        
        // Auto-stop discovery after 12 seconds
        btnStartDiscovery.postDelayed(() -> {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        }, 12000);
    }

    private void makeDeviceDiscoverable() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(discoverableIntent);
            Toast.makeText(this, "Device is now discoverable for 5 minutes", Toast.LENGTH_LONG).show();
        }
    }

    private void onDeviceSelected(BluetoothDevice device) {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        if (chatService != null) {
            chatService.connect(device);
            Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty() || !isConnected || chatService == null) {
            return;
        }

        BluetoothMessage message = new BluetoothMessage(
            messageText, 
            true, // isOutgoing
            System.currentTimeMillis()
        );

        chatService.sendMessage(messageText);
        messages.add(message);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecyclerView.scrollToPosition(messages.size() - 1);
        messageInput.setText("");
    }

    private void updateUI() {
        if (isConnected) {
            deviceDiscoveryCard.setVisibility(View.GONE);
            chatCard.setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle("Connected to " + connectedDeviceName);
        } else {
            deviceDiscoveryCard.setVisibility(View.VISIBLE);
            chatCard.setVisibility(View.GONE);
            getSupportActionBar().setTitle("Chat Offline");
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    deviceAdapter.notifyItemInserted(discoveredDevices.size() - 1);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressIndicator.setVisibility(View.GONE);
                btnStartDiscovery.setEnabled(true);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                enableBluetoothIfNeeded();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required for this feature", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                initializeChatService();
                loadPairedDevices();
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to use this feature", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        try {
            unregisterReceiver(discoveryReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
        
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
        
        if (chatService != null) {
            chatService.stop();
        }
    }
}
