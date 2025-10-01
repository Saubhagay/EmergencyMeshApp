package com.emergencymesh.app;

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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencymesh.app.adapters.DeviceListAdapter;
import com.emergencymesh.app.services.BluetoothMeshService;
import com.emergencymesh.app.services.GlobalMeshService;
import java.util.ArrayList;
import java.util.List;

public class NearbyDevicesActivity extends AppCompatActivity {

    private static final String TAG = "NearbyDevicesActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    private static final int REQUEST_DISCOVERABLE = 3;

    private BluetoothMeshService meshService;
    private DeviceListAdapter adapter;
    private List<BluetoothDevice> deviceList;

    private TextView tvBluetoothStatus, tvNoDevices, tvConnectionStatus, tvConnectedDevicesCount;
    private LinearLayout llScanningProgress;
    private RecyclerView rvDevices;
    private Button btnScan, btnMakeDiscoverable, btnRefresh, btnStartServer;

    private BluetoothAdapter bluetoothAdapter;
    private boolean isScanning = false;

    private BroadcastReceiver meshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case BluetoothMeshService.ACTION_DISCOVERY_DEVICE:
                    BluetoothDevice device = intent.getParcelableExtra("device");
                    if (device != null && !deviceList.contains(device)) {
                        deviceList.add(device);
                        adapter.notifyItemInserted(deviceList.size() - 1);
                        updateDeviceListVisibility();
                    }
                    break;

                case BluetoothMeshService.ACTION_DEVICE_CONNECTED:
                    String deviceName = intent.getStringExtra(BluetoothMeshService.EXTRA_DEVICE_NAME);
                    Toast.makeText(NearbyDevicesActivity.this,
                            "Connected: " + deviceName,
                            Toast.LENGTH_SHORT).show();
                    updateConnectionStatus();
                    break;

                case BluetoothMeshService.ACTION_DEVICE_DISCONNECTED:
                    Toast.makeText(NearbyDevicesActivity.this,
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
        setContentView(R.layout.activity_nearby_devices);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        meshService = GlobalMeshService.getInstance(this).getMeshService();

        initViews();
        setupRecyclerView();
        checkBluetoothPermissions();
        updateBluetoothStatus();
        updateConnectionStatus();
    }

    private void initViews() {
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        tvNoDevices = findViewById(R.id.tvNoDevices);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvConnectedDevicesCount = findViewById(R.id.tvConnectedDevicesCount);
        llScanningProgress = findViewById(R.id.llScanningProgress);
        rvDevices = findViewById(R.id.rvDevices);
        btnScan = findViewById(R.id.btnScan);
        btnMakeDiscoverable = findViewById(R.id.btnMakeDiscoverable);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnStartServer = findViewById(R.id.btnStartServer);

        btnScan.setOnClickListener(v -> startScanning());
        btnMakeDiscoverable.setOnClickListener(v -> makeDiscoverable());
        btnRefresh.setOnClickListener(v -> refreshDevices());
        btnStartServer.setOnClickListener(v -> toggleServer());
    }

    private void setupRecyclerView() {
        deviceList = new ArrayList<>();
        adapter = new DeviceListAdapter(deviceList, this::connectToDevice);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(adapter);
    }

    private void checkBluetoothPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private void updateBluetoothStatus() {
        if (bluetoothAdapter == null) {
            tvBluetoothStatus.setText("Not Supported");
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnScan.setEnabled(false);
            btnStartServer.setEnabled(false);
            return;
        }

        if (bluetoothAdapter.isEnabled()) {
            tvBluetoothStatus.setText("Enabled");
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnScan.setEnabled(true);
            btnStartServer.setEnabled(true);
        } else {
            tvBluetoothStatus.setText("Disabled");
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnScan.setEnabled(false);
            btnStartServer.setEnabled(false);

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void updateConnectionStatus() {
        if (meshService == null) return;

        List<String> connectedDevices = meshService.getConnectedDevices();
        int deviceCount = connectedDevices.size();

        if (tvConnectedDevicesCount != null) {
            tvConnectedDevicesCount.setText("Connected Devices: " + deviceCount);
        }

        if (tvConnectionStatus != null) {
            if (deviceCount > 0) {
                tvConnectionStatus.setText("Mesh Active");
                tvConnectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                tvConnectionStatus.setText("No Connections");
                tvConnectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light));
            }
        }
    }

    private void startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            updateBluetoothStatus();
            return;
        }

        deviceList.clear();
        adapter.notifyDataSetChanged();

        if (meshService != null) {
            meshService.startDiscovery();
            isScanning = true;
            llScanningProgress.setVisibility(View.VISIBLE);
            btnScan.setText("Scanning...");
            btnScan.setEnabled(false);
        }

        // Stop scanning after 30 seconds
        new android.os.Handler().postDelayed(() -> {
            if (isScanning) {
                stopScanning();
            }
        }, 30000);
    }

    private void stopScanning() {
        isScanning = false;
        if (meshService != null) {
            meshService.stopDiscovery();
        }
        llScanningProgress.setVisibility(View.GONE);
        btnScan.setText("Scan");
        btnScan.setEnabled(true);
        updateDeviceListVisibility();
    }

    private void makeDiscoverable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth Advertise permission required", Toast.LENGTH_LONG).show();
            return;
        }

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
    }

    private void refreshDevices() {
        startScanning();
    }

    private void toggleServer() {
        if (meshService == null) return;

        if (btnStartServer.getText().toString().equals("Start Server")) {
            meshService.startServer();
            btnStartServer.setText("Stop Server");
            Toast.makeText(this, "Emergency Mesh server started", Toast.LENGTH_SHORT).show();
        } else {
            meshService.stopServer();
            btnStartServer.setText("Start Server");
            Toast.makeText(this, "Emergency Mesh server stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        if (meshService != null) {
            String deviceName = getDeviceName(device);
            Toast.makeText(this, "Connecting to " + deviceName, Toast.LENGTH_SHORT).show();
            meshService.connectToDevice(device);
        }
    }

    private String getDeviceName(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return "Unknown Device";
        }

        String name = device.getName();
        return name != null ? name : "Unknown Device";
    }

    private void updateDeviceListVisibility() {
        if (deviceList.isEmpty()) {
            rvDevices.setVisibility(View.GONE);
            tvNoDevices.setVisibility(View.VISIBLE);
        } else {
            rvDevices.setVisibility(View.VISIBLE);
            tvNoDevices.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            updateBluetoothStatus();
        } else if (requestCode == REQUEST_DISCOVERABLE) {
            if (resultCode > 0) {
                Toast.makeText(this, "Device discoverable for " + resultCode + " seconds", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show();
            }
            updateBluetoothStatus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothMeshService.ACTION_DISCOVERY_DEVICE);
        filter.addAction(BluetoothMeshService.ACTION_DEVICE_CONNECTED);
        filter.addAction(BluetoothMeshService.ACTION_DEVICE_DISCONNECTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(meshReceiver, filter);

        updateConnectionStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop scanning if active
        if (isScanning) {
            stopScanning();
        }

        // Unregister broadcast receiver
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(meshReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't cleanup mesh service - it persists
    }
}