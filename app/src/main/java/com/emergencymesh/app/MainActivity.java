package com.emergencymesh.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.emergencymesh.app.utils.SharedPrefsHelper;
import java.util.ArrayList;
import java.util.List;

import com.emergencymesh.app.R;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView tvConnectionStatus;
    private Button btnSendMessage, btnSendLocation, btnEmergencyContacts,
            btnNearbyDevices, btnBroadcastAlert, btnProfile;
    private SharedPrefsHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsHelper = new SharedPrefsHelper(this);

        // Check if profile is set up
        if (!prefsHelper.isProfileComplete()) {
            startActivity(new Intent(this, ProfileSetupActivity.class));
        }

        initViews();
        setupClickListeners();
        checkPermissions();
        updateConnectionStatus();
    }

    private void initViews() {
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnSendLocation = findViewById(R.id.btnSendLocation);
        btnEmergencyContacts = findViewById(R.id.btnEmergencyContacts);
        btnNearbyDevices = findViewById(R.id.btnNearbyDevices);
        btnBroadcastAlert = findViewById(R.id.btnBroadcastAlert);
        btnProfile = findViewById(R.id.btnProfile);
    }

    private void setupClickListeners() {
        btnSendMessage.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SendMessageActivity.class)));

        btnSendLocation.setOnClickListener(v -> sendLocationMessage());

        btnEmergencyContacts.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, EmergencyContactsActivity.class)));

        btnNearbyDevices.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, NearbyDevicesActivity.class)));

        btnBroadcastAlert.setOnClickListener(v -> broadcastEmergencyAlert());

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileSetupActivity.class)));
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void updateConnectionStatus() {
        // This will show mesh network status
        tvConnectionStatus.setText("● Mesh Mode");
        tvConnectionStatus.setTextColor(getColor(android.R.color.holo_orange_light));
    }

    private void sendLocationMessage() {
        Intent intent = new Intent(this, SendMessageActivity.class);
        intent.putExtra("message_type", "location");
        startActivity(intent);
    }

    private void broadcastEmergencyAlert() {
        Intent intent = new Intent(this, SendMessageActivity.class);
        intent.putExtra("message_type", "alert");
        intent.putExtra("broadcast_mode", true);
        startActivity(intent);
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

            if (!allGranted) {
                Toast.makeText(this, "Permissions are required for emergency mesh networking",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
    }
}