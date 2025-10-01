package com.emergencymesh.app.services;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.gson.Gson;
import com.emergencymesh.app.models.Message;
import com.emergencymesh.app.utils.MessageStorage;
import com.emergencymesh.app.utils.SharedPrefsHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emergency Mesh Service with:
 * - Bidirectional messaging
 * - Emergency role support
 * - Auto-send features
 * - Delivery status tracking
 */
public class BluetoothMeshService {
    private static final String TAG = "EmergencyMesh";

    private static final UUID MESH_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String SERVICE_NAME = "EmergencyMesh";

    // Broadcast actions for UI updates
    public static final String ACTION_MESSAGE_RECEIVED = "com.emergencymesh.MESSAGE_RECEIVED";
    public static final String ACTION_MESSAGE_SENT = "com.emergencymesh.MESSAGE_SENT";
    public static final String ACTION_MESSAGE_FAILED = "com.emergencymesh.MESSAGE_FAILED";
    public static final String ACTION_DEVICE_CONNECTED = "com.emergencymesh.DEVICE_CONNECTED";
    public static final String ACTION_DEVICE_DISCONNECTED = "com.emergencymesh.DEVICE_DISCONNECTED";
    public static final String ACTION_DISCOVERY_DEVICE = "com.emergencymesh.DISCOVERY_DEVICE";

    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_ERROR = "error";

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread serverThread;
    private ConcurrentHashMap<String, SimpleConnection> activeConnections;
    private List<BluetoothDevice> discoveredDevices;
    private boolean isRunning = false;

    private MessageStorage messageStorage;
    private SharedPrefsHelper prefsHelper;
    private Handler mainHandler;
    private Gson gson;
    private LocalBroadcastManager broadcastManager;

    private class SimpleConnection {
        private final BluetoothSocket socket;
        private final String deviceAddress;
        private final String deviceName;
        private final PrintWriter writer;
        private final BufferedReader reader;
        private final Thread readerThread;
        private volatile boolean isActive = true;

        public SimpleConnection(BluetoothSocket socket) throws IOException {
            this.socket = socket;
            this.deviceAddress = socket.getRemoteDevice().getAddress();
            // FIXED: Call the outer class method explicitly
            this.deviceName = BluetoothMeshService.this.getDeviceName(socket.getRemoteDevice());

            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.readerThread = new Thread(this::readMessages);
            this.readerThread.start();

            Log.d(TAG, "Connection established with " + deviceName);
            broadcastDeviceConnected(deviceAddress, deviceName);

            // AUTO-SEND EMERGENCY ALERT IF IN EMERGENCY ROLE
            autoSendEmergencyAlert();
        }

        private void autoSendEmergencyAlert() {
            String role = prefsHelper.getUserRole();
            if ("emergency".equals(role)) {
                // Auto-send emergency alert
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Message alertMsg = new Message(
                            UUID.randomUUID().toString(),
                            prefsHelper.getName(),
                            prefsHelper.getPhone(),
                            "AUTO",
                            "🚨 AUTOMATIC EMERGENCY ALERT\n\nI need help! This is an automatic distress signal.\n\nFrom: " +
                                    prefsHelper.getName() + "\nPhone: " + prefsHelper.getPhone(),
                            "alert"
                    );
                    sendMessage(alertMsg);

                    // Auto-send location after 2 seconds
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        sendAutoLocation();
                    }, 2000);
                }, 1000);
            }
        }

        private void sendAutoLocation() {
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager == null) return;

                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (location != null) {
                    String locationMsg = String.format(
                            "📍 AUTOMATIC LOCATION SHARE\n\n" +
                                    "From: %s\nPhone: %s\n\n" +
                                    "Coordinates: %.6f, %.6f\n" +
                                    "Accuracy: %.0fm\n\n" +
                                    "https://maps.google.com/?q=%.6f,%.6f",
                            prefsHelper.getName(),
                            prefsHelper.getPhone(),
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getAccuracy(),
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    Message locMsg = new Message(
                            UUID.randomUUID().toString(),
                            prefsHelper.getName(),
                            prefsHelper.getPhone(),
                            "AUTO",
                            locationMsg,
                            "location"
                    );
                    sendMessage(locMsg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending auto location", e);
            }
        }

        private void readMessages() {
            try {
                String line;
                while (isActive && (line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        handleReceivedMessage(line.trim());
                    }
                }
            } catch (IOException e) {
                Log.d(TAG, "Connection ended with " + deviceAddress);
            } finally {
                disconnect();
            }
        }

        private void handleReceivedMessage(String jsonMessage) {
            try {
                if ("PING".equals(jsonMessage)) return;

                Message message = gson.fromJson(jsonMessage, Message.class);
                if (message != null && !"ack".equals(message.getMessageType())) {
                    // Store incoming message
                    messageStorage.storeIncomingMessage(message);

                    // Broadcast to UI
                    broadcastMessageReceived(message, deviceAddress);

                    // Send acknowledgment back
                    sendAck(message.getId());

                    Log.d(TAG, "Message received: " + message.getMessageType() + " from " + deviceAddress);
                } else if (message != null && "ack".equals(message.getMessageType())) {
                    // Mark original message as delivered
                    String originalMsgId = message.getContent().replace("ACK:", "");
                    messageStorage.markMessageAsDelivered(originalMsgId);
                    broadcastMessageSent(originalMsgId, deviceAddress);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing message", e);
            }
        }

        private void sendAck(String originalMessageId) {
            try {
                Message ack = new Message(
                        UUID.randomUUID().toString(),
                        prefsHelper.getName(),
                        prefsHelper.getPhone(),
                        "",
                        "ACK:" + originalMessageId,
                        "ack"
                );
                String json = gson.toJson(ack);
                writer.println(json);
            } catch (Exception e) {
                Log.e(TAG, "Error sending ACK", e);
            }
        }

        public boolean sendMessage(Message message) {
            if (!isActive || writer == null) return false;

            try {
                String jsonMessage = gson.toJson(message);
                writer.println(jsonMessage);

                if (writer.checkError()) {
                    Log.e(TAG, "Error sending to " + deviceAddress);
                    return false;
                }

                Log.d(TAG, "Message sent to " + deviceAddress);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to send to " + deviceAddress, e);
                return false;
            }
        }

        public void disconnect() {
            isActive = false;
            try {
                if (socket != null) socket.close();
                if (writer != null) writer.close();
                if (reader != null) reader.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing connection", e);
            }

            activeConnections.remove(deviceAddress);
            broadcastDeviceDisconnected(deviceAddress);
        }

        public boolean isConnected() {
            return isActive && socket != null && socket.isConnected();
        }

        public String getDeviceAddress() {
            return deviceAddress;
        }

        public String getDeviceName() {
            return deviceName;
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                if (hasBluetoothPermission()) {
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, MESH_UUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error creating server", e);
            }
            serverSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Emergency server started");

            while (isRunning && serverSocket != null) {
                try {
                    BluetoothSocket socket = serverSocket.accept();
                    if (socket != null) {
                        handleNewConnection(socket);
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        Log.e(TAG, "Accept error", e);
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing server", e);
            }
        }
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    broadcastDiscoveredDevice(device);
                }
            }
        }
    };

    public BluetoothMeshService(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.activeConnections = new ConcurrentHashMap<>();
        this.discoveredDevices = new ArrayList<>();
        this.messageStorage = new MessageStorage(context);
        this.prefsHelper = new SharedPrefsHelper(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
        this.broadcastManager = LocalBroadcastManager.getInstance(context);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(bluetoothReceiver, filter);
    }

    // Public API
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void startServer() {
        if (isRunning || !isBluetoothEnabled()) return;

        isRunning = true;
        serverThread = new AcceptThread();
        serverThread.start();
        Log.d(TAG, "Server started");
    }

    public void stopServer() {
        isRunning = false;
        if (serverThread != null) serverThread.cancel();

        for (SimpleConnection conn : activeConnections.values()) {
            conn.disconnect();
        }
        activeConnections.clear();
    }

    public void startDiscovery() {
        if (!isBluetoothEnabled() || !hasLocationPermission()) return;

        try {
            discoveredDevices.clear();
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.e(TAG, "Discovery error", e);
        }
    }

    public void stopDiscovery() {
        if (bluetoothAdapter != null && hasLocationPermission()) {
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                Log.e(TAG, "Stop discovery error", e);
            }
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        if (device == null || activeConnections.containsKey(device.getAddress())) return;

        new Thread(() -> {
            try {
                if (!hasBluetoothPermission()) return;

                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MESH_UUID);
                stopDiscovery();
                socket.connect();

                handleNewConnection(socket);

            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
            }
        }).start();
    }

    private void handleNewConnection(BluetoothSocket socket) {
        try {
            String deviceAddress = socket.getRemoteDevice().getAddress();

            if (activeConnections.containsKey(deviceAddress)) {
                socket.close();
                return;
            }

            SimpleConnection connection = new SimpleConnection(socket);
            activeConnections.put(deviceAddress, connection);

        } catch (IOException e) {
            Log.e(TAG, "Error handling connection", e);
        }
    }

    public void broadcastMessage(Message message) {
        if (activeConnections.isEmpty()) {
            broadcastMessageFailed(message.getId(), "No connections");
            return;
        }

        boolean sentToAny = false;
        for (SimpleConnection connection : activeConnections.values()) {
            if (connection.isConnected() && connection.sendMessage(message)) {
                sentToAny = true;
            }
        }

        if (!sentToAny) {
            broadcastMessageFailed(message.getId(), "Failed to send");
        }
    }

    public List<String> getConnectedDevices() {
        List<String> connected = new ArrayList<>();
        for (SimpleConnection conn : activeConnections.values()) {
            if (conn.isConnected()) {
                connected.add(conn.getDeviceAddress());
            }
        }
        return connected;
    }

    public List<BluetoothDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }

    // Broadcast methods for UI updates
    private void broadcastMessageReceived(Message message, String senderAddress) {
        Intent intent = new Intent(ACTION_MESSAGE_RECEIVED);
        intent.putExtra(EXTRA_MESSAGE, gson.toJson(message));
        intent.putExtra(EXTRA_DEVICE_ADDRESS, senderAddress);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastMessageSent(String messageId, String recipientAddress) {
        Intent intent = new Intent(ACTION_MESSAGE_SENT);
        intent.putExtra(EXTRA_MESSAGE_ID, messageId);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, recipientAddress);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastMessageFailed(String messageId, String error) {
        Intent intent = new Intent(ACTION_MESSAGE_FAILED);
        intent.putExtra(EXTRA_MESSAGE_ID, messageId);
        intent.putExtra(EXTRA_ERROR, error);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastDeviceConnected(String deviceAddress, String deviceName) {
        Intent intent = new Intent(ACTION_DEVICE_CONNECTED);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress);
        intent.putExtra(EXTRA_DEVICE_NAME, deviceName);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastDeviceDisconnected(String deviceAddress) {
        Intent intent = new Intent(ACTION_DEVICE_DISCONNECTED);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastDiscoveredDevice(BluetoothDevice device) {
        Intent intent = new Intent(ACTION_DISCOVERY_DEVICE);
        intent.putExtra("device", device);
        broadcastManager.sendBroadcast(intent);
    }

    // Permission helpers
    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private String getDeviceName(BluetoothDevice device) {
        try {
            if (hasBluetoothPermission()) {
                String name = device.getName();
                return (name != null && !name.isEmpty()) ? name : "Emergency Device";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting name", e);
        }
        return "Emergency Device";
    }

    public void cleanup() {
        isRunning = false;
        try {
            context.unregisterReceiver(bluetoothReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Unregister error", e);
        }
        stopServer();
        stopDiscovery();
    }
}