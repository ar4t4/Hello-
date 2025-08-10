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

import com.example.hello.models.BluetoothMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChatService {
    private static final String TAG = "BluetoothChatService";
    private static final String NAME = "HELLO_BluetoothChat";
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public interface BluetoothChatListener {
        void onMessageReceived(BluetoothMessage message);
        void onConnectionEstablished(String deviceName);
        void onConnectionLost();
        void onConnectionFailed();
        void onStateChanged(int state);
    }

    // Connection states
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothChatListener listener;
    private final Handler mainHandler;
    private final Context context;

    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int currentState;
    private String connectedDeviceName;

    public BluetoothChatService(Context context, BluetoothChatListener listener) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.currentState = STATE_NONE;
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + currentState + " -> " + state);
        currentState = state;
        mainHandler.post(() -> listener.onStateChanged(state));
    }

    public synchronized int getState() {
        return currentState;
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        if (currentState == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        setState(STATE_NONE);
    }

    public void sendMessage(String message) {
        ConnectedThread r;
        synchronized (this) {
            if (currentState != STATE_CONNECTED) return;
            r = connectedThread;
        }
        r.write(message.getBytes());
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        connectedThread = new ConnectedThread(socket, socketType);
        connectedThread.start();

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            connectedDeviceName = device.getName();
        } else {
            connectedDeviceName = "Unknown Device";
        }

        setState(STATE_CONNECTED);
        mainHandler.post(() -> listener.onConnectionEstablished(connectedDeviceName));
    }

    private void connectionFailed() {
        mainHandler.post(() -> listener.onConnectionFailed());
        BluetoothChatService.this.start();
    }

    private void connectionLost() {
        mainHandler.post(() -> listener.onConnectionLost());
        BluetoothChatService.this.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;
        private String socketType;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            socketType = "Insecure";

            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + socketType + "listen() failed", e);
            }
            serverSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + socketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + socketType);

            BluetoothSocket socket = null;

            while (currentState != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + socketType + "accept() failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (currentState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(), socketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + socketType);
        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + socketType + "cancel " + this);
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + socketType + "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        private String socketType;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            socketType = "Insecure";

            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + socketType + "create() failed", e);
            }
            socket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + socketType);
            setName("ConnectThread" + socketType);

            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }

            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + socketType + " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothChatService.this) {
                connectThread = null;
            }

            connected(socket, device, socketType);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            this.socket = socket;
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
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (currentState == STATE_CONNECTED) {
                try {
                    bytes = inputStream.read(buffer);
                    String receivedMessage = new String(buffer, 0, bytes);
                    
                    BluetoothMessage message = new BluetoothMessage(
                        receivedMessage,
                        false,
                        System.currentTimeMillis()
                    );

                    mainHandler.post(() -> listener.onMessageReceived(message));
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
