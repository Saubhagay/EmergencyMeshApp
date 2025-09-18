package com.emergencymesh.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvConnectionStatus;
    private Button btnSendMessage, btnSendLocation, btnEmergencyContacts,
            btnNearbyDevices, btnBroadcastAlert, btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
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
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SendMessageActivity.class));
            }
        });

        btnEmergencyContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EmergencyContactsActivity.class));
            }
        });

        btnNearbyDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NearbyDevicesActivity.class));
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProfileSetupActivity.class));
            }
        });
    }
}