package com.emergencymesh.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.emergencymesh.app.models.Message;
import com.emergencymesh.app.utils.SharedPrefsHelper;
import com.emergencymesh.app.utils.MessageStorage;
import java.util.UUID;

public class SendMessageActivity extends AppCompatActivity {

    private EditText etRecipientPhone, etMessageContent;
    private RadioGroup rgMessageType;
    private RadioButton rbText, rbLocation, rbAlert;
    private Button btnSendMessage, btnTemplate1, btnTemplate2, btnTemplate3;
    private SharedPrefsHelper prefsHelper;
    private MessageStorage messageStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);

        prefsHelper = new SharedPrefsHelper(this);
        messageStorage = new MessageStorage(this);

        initViews();
        setupTemplateButtons();
        setupSendButton();
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
    }

    private void setupTemplateButtons() {
        btnTemplate1.setOnClickListener(v ->
                etMessageContent.setText("I am safe. Please don't worry."));

        btnTemplate2.setOnClickListener(v ->
                etMessageContent.setText("Need immediate help. Emergency situation."));

        btnTemplate3.setOnClickListener(v ->
                etMessageContent.setText("Stuck in disaster area. Send rescue team."));

        // Handle location message type
        rgMessageType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbLocation) {
                getCurrentLocationAndSetMessage();
            }
        });
    }

    private void setupSendButton() {
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void getCurrentLocationAndSetMessage() {
        // Check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                String locationText = String.format("Emergency Location: Latitude: %f, Longitude: %f\nGoogle Maps: https://maps.google.com/?q=%f,%f",
                        location.getLatitude(), location.getLongitude(),
                        location.getLatitude(), location.getLongitude());
                etMessageContent.setText(locationText);
            } else {
                etMessageContent.setText("Unable to get current location. Please enable GPS.");
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        String recipientPhone = etRecipientPhone.getText().toString().trim();
        String messageContent = etMessageContent.getText().toString().trim();

        if (recipientPhone.isEmpty() || messageContent.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get message type
        String messageType = "text";
        int checkedId = rgMessageType.getCheckedRadioButtonId();
        if (checkedId == R.id.rbLocation) {
            messageType = "location";
        } else if (checkedId == R.id.rbAlert) {
            messageType = "alert";
        }

        // Create message
        String messageId = UUID.randomUUID().toString();
        Message message = new Message(
                messageId,
                prefsHelper.getName(),
                prefsHelper.getPhone(),
                recipientPhone,
                messageContent,
                messageType
        );

        // Store message locally (DTN principle - store and forward)
        messageStorage.storeOutgoingMessage(message);

        // Show success message
        Toast.makeText(this, "Message queued for delivery!", Toast.LENGTH_LONG).show();

        // Clear fields
        etRecipientPhone.setText("");
        etMessageContent.setText("");
        rbText.setChecked(true);

        finish();
    }
}