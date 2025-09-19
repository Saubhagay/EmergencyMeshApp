package com.emergencymesh.app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emergencymesh.app.adapters.DeviceListAdapter;
import com.emergencymesh.app.services.BluetoothMeshService;

import java.util.ArrayList;
import java.util.List;

import com.emergencymesh.app.R;


public class NearbyDevicesActivity extends AppCompatActivity implements
        BluetoothMeshService.BluetoothDiscoveryListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    private static final int REQUEST_DISCOVERABLE = 3;

    private BluetoothMeshService meshService;
    private DeviceListAdapter adapter;
    private List<BluetoothDevice> deviceList;

    private TextView tvBluetoothStatus, tvNoDevices;
    private LinearLayout llScanningProgress;
    private RecyclerView rvDevices;
    private Button btnScan, btnMakeDiscoverable, btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_devices);

        initViews();
        setupRecyclerView();
        setupBluetoothService();
        checkBluetoothPermissions();
        updateBluetoothStatus();
    }

    private void initViews() {
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        tvNoDevices = findViewById(R.id.tvNoDevices);
        llScanningProgress = findViewById(R.id.llScanningProgress);
        rvDevices = findViewById(R.id.rvDevices);
        btnScan = findViewById(R.id.btnScan);
        btnMakeDiscoverable = findViewById(R.id.btnMakeDiscoverable);
        btnRefresh = findViewById(R.id.btnRefresh);

        btnScan.setOnClickListener(v -> startScanning());
        btnMakeDiscoverable.setOnClickListener(v -> makeDiscoverable());
        btnRefresh.setOnClickListener(v -> refreshDevices());
    }

    private void setupRecyclerView() {
        deviceList = new ArrayList<>();
        adapter = new DeviceListAdapter(deviceList, this::connectToDevice);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(adapter);
    }

    private void setupBluetoothService() {
        meshService = new BluetoothMeshService(this);
        meshService.setDiscoveryListener(this);
    }

    private void checkBluetoothPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {
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
        if (!meshService.isBluetoothSupported()) {
            tvBluetoothStatus.setText("Not Supported");
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnScan.setEnabled(false);
            return;
        }

        if (meshService.isBluetoothEnabled()) {
            tvBluetoothStatus.setText("● Enabled");
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnScan.setEnabled(true);
        } else {
            tvBluetoothStatus.setText("● Disabled");
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnScan.setEnabled(false);

            // Request to enable Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void startScanning() {
        if (!meshService.isBluetoothEnabled()) {
            updateBluetoothStatus();
            return;
        }

        deviceList.clear();
        adapter.notifyDataSetChanged();
        meshService.startDiscovery();
    }

    private void makeDiscoverable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                        != PackageManager.PERMISSION_GRANTED) {
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

    private void connectToDevice(BluetoothDevice device) {
        Toast.makeText(this, "Connecting to " + getDeviceName(device), Toast.LENGTH_SHORT).show();
        // TODO: Implement actual connection logic in Day 6
    }

    private String getDeviceName(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            return "Unknown Device";
        }

        String name = device.getName();
        return name != null ? name : "Unknown Device";
    }

    @Override
    public void onDeviceDiscovered(BluetoothDevice device) {
        runOnUiThread(() -> {
            deviceList.add(device);
            adapter.notifyItemInserted(deviceList.size() - 1);
            updateDeviceListVisibility();
        });
    }

    @Override
    public void onDiscoveryStarted() {
        runOnUiThread(() -> {
            llScanningProgress.setVisibility(View.VISIBLE);
            btnScan.setText("Scanning...");
            btnScan.setEnabled(false);
        });
    }

    @Override
    public void onDiscoveryFinished() {
        runOnUiThread(() -> {
            llScanningProgress.setVisibility(View.GONE);
            btnScan.setText("Scan");
            btnScan.setEnabled(true);
            updateDeviceListVisibility();
        });
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
                Toast.makeText(this, "Device is now discoverable for " + resultCode + " seconds",
                        Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, "Bluetooth permissions are required for mesh networking",
                        Toast.LENGTH_LONG).show();
            }
            updateBluetoothStatus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (meshService != null) {
            meshService.cleanup();
        }
    }
}
