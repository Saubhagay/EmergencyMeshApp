package com.emergencymesh.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.emergencymesh.app.models.Message;
import com.emergencymesh.app.services.BluetoothMeshService;
import com.emergencymesh.app.services.GlobalMeshService;
import com.emergencymesh.app.utils.SharedPrefsHelper;
import com.emergencymesh.app.utils.MessageStorage;
import java.util.List;
import java.util.UUID;

public class SendMessageActivity extends AppCompatActivity {

    private static final String TAG = "SendMessage";

    private EditText etRecipientPhone, etMessageContent;
    private RadioGroup rgMessageType;
    private RadioButton rbText, rbLocation, rbAlert;
    private Button btnSendMessage, btnTemplate1, btnTemplate2, btnTemplate3;
    private TextView tvConnectionStatus;

    private SharedPrefsHelper prefsHelper;
    private MessageStorage messageStorage;
    private BluetoothMeshService meshService;

    private boolean broadcastMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);

        prefsHelper = new SharedPrefsHelper(this);
        messageStorage = new MessageStorage(this);

        // USE GLOBAL SERVICE - DON'T CREATE NEW ONE
        meshService = GlobalMeshService.getInstance(this).getMeshService();
        // DON'T set listener - let MainActivity handle it

        broadcastMode = getIntent().getBooleanExtra("broadcast_mode", false);

        initViews();
        setupButtons();

        String messageType = getIntent().getStringExtra("message_type");
        if ("location".equals(messageType) && rbLocation != null) {
            rbLocation.setChecked(true);
            getCurrentLocation();
        } else if ("alert".equals(messageType) && rbAlert != null) {
            rbAlert.setChecked(true);
            etMessageContent.setText("🚨 EMERGENCY ALERT 🚨\n\nI need immediate help!\n\nFrom: " + prefsHelper.getName() + "\nPhone: " + prefsHelper.getPhone());
        }
    }

    private void initViews() {
        etRecipientPhone = findViewById(R.id.etRecipientPhone);
        etMessageContent = findViewById(R.id.etMessageContent);
        rgMessageType = findViewById(R.id.rgMessageType);
        rbText = findViewById(R.id.rbText);
        rbLocation = findViewById(R.id.rbLocation);
        rbAlert = findViewById(R.id.rbAlert);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnTemplate1 = findViewById(R.id.btnTemplate1);
        btnTemplate2 = findViewById(R.id.btnTemplate2);
        btnTemplate3 = findViewById(R.id.btnTemplate3);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);

        if (broadcastMode) {
            etRecipientPhone.setVisibility(View.GONE);
            findViewById(R.id.tvRecipientLabel).setVisibility(View.GONE);
        }
    }

    private void setupButtons() {
        btnTemplate1.setOnClickListener(v ->
                etMessageContent.setText("I am safe. This is " + prefsHelper.getName() + "."));

        btnTemplate2.setOnClickListener(v ->
                etMessageContent.setText("🆘 NEED HELP!\n\nFrom: " + prefsHelper.getName() + "\nPhone: " + prefsHelper.getPhone()));

        btnTemplate3.setOnClickListener(v ->
                etMessageContent.setText("🚨 DISASTER EMERGENCY\n\nStuck in disaster area. Need rescue.\n\nFrom: " + prefsHelper.getName() + "\nPhone: " + prefsHelper.getPhone()));

        rgMessageType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbLocation) {
                getCurrentLocation();
            }
        });

        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            etMessageContent.setText("Location unavailable");
            return;
        }

        etMessageContent.setText("Getting location...");

        Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnown == null) {
            lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (lastKnown != null) {
            setLocationMessage(lastKnown);
            return;
        }

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                setLocationMessage(location);
                locationManager.removeUpdates(this);
            }
        };

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        } else {
            etMessageContent.setText("Please enable GPS");
        }

        new Handler().postDelayed(() -> {
            locationManager.removeUpdates(listener);
            if (etMessageContent.getText().toString().contains("Getting")) {
                etMessageContent.setText("Timeout. Enable GPS and try again.");
            }
        }, 30000);
    }

    private void setLocationMessage(Location location) {
        String msg = String.format(
                "🚨 EMERGENCY LOCATION\n\n" +
                        "From: %s\n" +
                        "Phone: %s\n\n" +
                        "Coordinates: %.6f, %.6f\n" +
                        "Accuracy: %.0fm\n\n" +
                        "Google Maps:\nhttps://maps.google.com/?q=%.6f,%.6f",
                prefsHelper.getName(),
                prefsHelper.getPhone(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getLatitude(),
                location.getLongitude()
        );
        etMessageContent.setText(msg);
    }

    private void sendMessage() {
        String content = etMessageContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageType = "text";
        int checkedId = rgMessageType.getCheckedRadioButtonId();
        if (checkedId == R.id.rbLocation) messageType = "location";
        else if (checkedId == R.id.rbAlert) messageType = "alert";

        String recipient = broadcastMode ? "BROADCAST" : etRecipientPhone.getText().toString().trim();

        Message message = new Message(
                UUID.randomUUID().toString(),
                prefsHelper.getName(),
                prefsHelper.getPhone(),
                recipient,
                content,
                messageType
        );

        // ALWAYS STORE MESSAGE
        messageStorage.storeOutgoingMessage(message);

        // TRY TO SEND IMMEDIATELY
        if (meshService != null) {
            List<String> connections = meshService.getConnectedDevices();

            if (!connections.isEmpty()) {
                // CONNECTED - SEND NOW
                meshService.broadcastMessage(message);

                String msg = broadcastMode ?
                        "SENT to " + connections.size() + " device(s)!" :
                        "SENT successfully!";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            } else {
                // NOT CONNECTED - JUST STORED
                Toast.makeText(this, "Message saved. Go to Mesh Network to connect and send.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Service error. Message saved locally.", Toast.LENGTH_SHORT).show();
        }

        // Clear and finish
        etMessageContent.setText("");
        if (!broadcastMode) etRecipientPhone.setText("");
        rbText.setChecked(true);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        if (meshService == null || tvConnectionStatus == null) return;

        List<String> connections = meshService.getConnectedDevices();
        if (!connections.isEmpty()) {
            tvConnectionStatus.setText("✓ Connected to " + connections.size() + " device(s)");
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            tvConnectionStatus.setText("Not connected - Messages will be saved");
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}