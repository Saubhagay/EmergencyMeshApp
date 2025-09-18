package com.emergencymesh.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.emergencymesh.app.utils.SharedPrefsHelper;

public class ProfileSetupActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmergencyContact;
    private Spinner spinnerBloodGroup;
    private Button btnSaveProfile;
    private SharedPrefsHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        prefsHelper = new SharedPrefsHelper(this);
        initViews();
        setupBloodGroupSpinner();
        loadExistingProfile();
        setupSaveButton();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmergencyContact = findViewById(R.id.etEmergencyContact);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void setupBloodGroupSpinner() {
        String[] bloodGroups = {"Select Blood Group", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);
    }

    private void loadExistingProfile() {
        etName.setText(prefsHelper.getName());
        etPhone.setText(prefsHelper.getPhone());
        etEmergencyContact.setText(prefsHelper.getEmergencyContact());

        String bloodGroup = prefsHelper.getBloodGroup();
        if (!bloodGroup.isEmpty()) {
            ArrayAdapter adapter = (ArrayAdapter) spinnerBloodGroup.getAdapter();
            int position = adapter.getPosition(bloodGroup);
            spinnerBloodGroup.setSelection(position);
        }
    }

    private void setupSaveButton() {
        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String emergencyContact = etEmergencyContact.getText().toString().trim();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();

        if (name.isEmpty() || phone.isEmpty() || emergencyContact.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bloodGroup.equals("Select Blood Group")) {
            bloodGroup = "";
        }

        prefsHelper.saveProfile(name, phone, emergencyContact, bloodGroup);
        Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
