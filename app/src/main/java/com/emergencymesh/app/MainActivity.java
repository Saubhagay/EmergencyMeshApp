package com.emergencymesh.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.emergencymesh.app.models.Message;
import com.emergencymesh.app.services.BluetoothMeshService;
import com.emergencymesh.app.services.GlobalMeshService;
import com.emergencymesh.app.utils.SharedPrefsHelper;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tvConnectionStatus, tvMeshInfo;
    private Button btnSendMessage, btnSendLocation, btnEmergencyContacts,
            btnNearbyDevices, btnBroadcastAlert, btnSlideSOS, btnMessageInbox;
    private LinearLayout navHome, navInbox, navMesh, navOn;
    private SharedPrefsHelper prefsHelper;
    private BluetoothMeshService meshService;
    private Gson gson;

    private BroadcastReceiver meshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case BluetoothMeshService.ACTION_MESSAGE_RECEIVED:
                    String msgJson = intent.getStringExtra(BluetoothMeshService.EXTRA_MESSAGE);
                    int hopCount = intent.getIntExtra(BluetoothMeshService.EXTRA_HOP_COUNT, 0);
                    if (msgJson != null) {
                        Message message = gson.fromJson(msgJson, Message.class);
                        String alertTitle = "MESSAGE RECEIVED";
                        if ("alert".equals(message.getMessageType())) {
                            alertTitle = "🚨 EMERGENCY ALERT";
                        } else if ("location".equals(message.getMessageType())) {
                            alertTitle = "📍 LOCATION SHARE";
                        }

                        String hopInfo = hopCount > 0 ? " (via " + hopCount + " hop" + (hopCount > 1 ? "s" : "") + ")" : "";
                        Toast.makeText(MainActivity.this,
                                alertTitle + hopInfo + "\nFrom: " + message.getSenderName(),
                                Toast.LENGTH_LONG).show();
                        updateConnectionStatus();
                    }
                    break;

                case BluetoothMeshService.ACTION_MESSAGE_SENT:
                    Toast.makeText(MainActivity.this,
                            "Message delivered successfully",
                            Toast.LENGTH_SHORT).show();
                    break;

                case BluetoothMeshService.ACTION_MESSAGE_FAILED:
                    String error = intent.getStringExtra(BluetoothMeshService.EXTRA_ERROR);
                    Toast.makeText(MainActivity.this,
                            "Send failed: " + error,
                            Toast.LENGTH_SHORT).show();
                    break;

                case BluetoothMeshService.ACTION_MESSAGE_FORWARDED:
                    int forwardedHops = intent.getIntExtra(BluetoothMeshService.EXTRA_HOP_COUNT, 0);
                    int deviceCount = intent.getIntExtra("device_count", 0);
                    Toast.makeText(MainActivity.this,
                            "📡 Message forwarded to " + deviceCount + " device(s) at hop " + forwardedHops,
                            Toast.LENGTH_SHORT).show();
                    break;

                case BluetoothMeshService.ACTION_DEVICE_CONNECTED:
                    String deviceName = intent.getStringExtra(BluetoothMeshService.EXTRA_DEVICE_NAME);
                    Toast.makeText(MainActivity.this,
                            "Connected: " + deviceName,
                            Toast.LENGTH_SHORT).show();
                    updateConnectionStatus();
                    break;

                case BluetoothMeshService.ACTION_DEVICE_DISCONNECTED:
                    Toast.makeText(MainActivity.this,
                            "Device disconnected",
                            Toast.LENGTH_SHORT).show();
                    updateConnectionStatus();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsHelper = new SharedPrefsHelper(this);
        gson = new Gson();

        if (!prefsHelper.isProfileComplete()) {
            startActivity(new Intent(this, ProfileSetupActivity.class));
        }

        initViews();
        setupClickListeners();
        checkPermissions(); // setupMeshService() is called inside after permissions granted
    }

    private void initViews() {
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvMeshInfo = findViewById(R.id.tvMeshInfo);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnSendLocation = findViewById(R.id.btnSendLocation);
        btnEmergencyContacts = findViewById(R.id.btnEmergencyContacts);
        btnNearbyDevices = findViewById(R.id.btnNearbyDevices);
        btnBroadcastAlert = findViewById(R.id.btnBroadcastAlert);
        btnSlideSOS = findViewById(R.id.btnSlideSOS);
        btnMessageInbox = findViewById(R.id.btnMessageInbox);
        navHome = findViewById(R.id.navHome);
        navInbox = findViewById(R.id.navInbox);
        navMesh = findViewById(R.id.navMesh);
        navOn = findViewById(R.id.navOn);

        tvConnectionStatus.setText("Initializing");
        tvConnectionStatus.setTextColor(getColor(android.R.color.holo_orange_light));
        tvMeshInfo.setText("Starting multi-hop emergency mesh...");
    }

    private void setupClickListeners() {
        btnSendMessage.setOnClickListener(v ->
                startActivity(new Intent(this, SendMessageActivity.class)));

        btnSendLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendMessageActivity.class);
            intent.putExtra("message_type", "location");
            startActivity(intent);
        });

        btnEmergencyContacts.setOnClickListener(v ->
                startActivity(new Intent(this, EmergencyContactsActivity.class)));

        btnNearbyDevices.setOnClickListener(v ->
                startActivity(new Intent(this, NearbyDevicesActivity.class)));

        btnBroadcastAlert.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendMessageActivity.class);
            intent.putExtra("message_type", "alert");
            intent.putExtra("broadcast_mode", true);
            startActivity(intent);
        });

        // SOS Slide Button — confirm then broadcast SOS alert
        if (btnSlideSOS != null) {
            btnSlideSOS.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("🚨 Send SOS Alert?")
                        .setMessage("This will broadcast an emergency SOS to all nearby mesh devices. Continue?")
                        .setPositiveButton("SEND SOS", (dialog, which) -> {
                            Intent intent = new Intent(this, SendMessageActivity.class);
                            intent.putExtra("message_type", "alert");
                            intent.putExtra("broadcast_mode", true);
                            intent.putExtra("sos_mode", true);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        btnMessageInbox.setOnClickListener(v ->
                startActivity(new Intent(this, MessageInboxActivity.class)));

        // Bottom navigation
        if (navHome != null) {
            navHome.setOnClickListener(v ->
                Toast.makeText(this, "You are on Home", Toast.LENGTH_SHORT).show());
        }

        if (navInbox != null) {
            navInbox.setOnClickListener(v ->
                    startActivity(new Intent(this, MessageInboxActivity.class)));
        }

        if (navMesh != null) {
            navMesh.setOnClickListener(v ->
                    startActivity(new Intent(this, NearbyDevicesActivity.class)));
        }

        if (navOn != null) {
            navOn.setOnClickListener(v -> {
                // Re-fetch service in case it started after click listeners were set up
                BluetoothMeshService svc = GlobalMeshService.getInstance(this).getMeshService();
                if (svc != null) {
                    List<String> devices = svc.getConnectedDevices();
                    Toast.makeText(this,
                            devices.isEmpty()
                                ? "Mesh ON — No devices connected yet"
                                : "Mesh ON — " + devices.size() + " device(s) connected",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Mesh service starting...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupMeshService() {
        meshService = GlobalMeshService.getInstance(this).getMeshService();

        if (meshService == null) {
            Log.w(TAG, "MeshService not ready yet");
            updateConnectionStatus();
            return;
        }

        if (meshService.isBluetoothEnabled()) {
            GlobalMeshService.getInstance(this).startService();
        }

        updateConnectionStatus();
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] newPermissions = {
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            };

            for (String permission : newPermissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(permission);
                }
            }
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            setupMeshService();
        }
    }

    private void updateConnectionStatus() {
        if (meshService == null) {
            tvConnectionStatus.setText("Offline");
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            tvMeshInfo.setText("Service not available");
            return;
        }

        List<String> connectedDevices = meshService.getConnectedDevices();
        int deviceCount = connectedDevices.size();
        int cacheSize = meshService.getMessageCacheSize();

        if (!meshService.isBluetoothEnabled()) {
            tvConnectionStatus.setText("Bluetooth Disabled");
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            tvMeshInfo.setText("Enable Bluetooth to start");
        } else if (deviceCount > 0) {
            tvConnectionStatus.setText("● Connected");
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            tvMeshInfo.setText(deviceCount + " device(s) connected • Multi-hop enabled • " +
                    cacheSize + " msgs cached");
        } else {
            tvConnectionStatus.setText("Ready");
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_orange_light));
            tvMeshInfo.setText("Server running • Multi-hop routing active");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                setupMeshService();
            } else {
                Toast.makeText(this, "Permissions required for emergency mesh", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register broadcast receiver with multi-hop actions
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothMeshService.ACTION_MESSAGE_RECEIVED);
        filter.addAction(BluetoothMeshService.ACTION_MESSAGE_SENT);
        filter.addAction(BluetoothMeshService.ACTION_MESSAGE_FAILED);
        filter.addAction(BluetoothMeshService.ACTION_MESSAGE_FORWARDED);
        filter.addAction(BluetoothMeshService.ACTION_DEVICE_CONNECTED);
        filter.addAction(BluetoothMeshService.ACTION_DEVICE_DISCONNECTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(meshReceiver, filter);

        updateConnectionStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(meshReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}