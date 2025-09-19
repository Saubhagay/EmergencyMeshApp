package com.emergencymesh.app;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencymesh.app.adapters.EmergencyContactAdapter;
import com.emergencymesh.app.models.EmergencyContact;
import com.emergencymesh.app.utils.ContactStorage;
import java.util.List;

import com.emergencymesh.app.R;


public class EmergencyContactsActivity extends AppCompatActivity {

    private RecyclerView rvContacts;
    private LinearLayout llEmptyState;
    private Button btnAddContact, btnBroadcastToAll, btnImportContacts;

    private EmergencyContactAdapter adapter;
    private ContactStorage contactStorage;
    private List<EmergencyContact> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        contactStorage = new ContactStorage(this);
        initViews();
        setupRecyclerView();
        loadContacts();
        setupClickListeners();
    }

    private void initViews() {
        rvContacts = findViewById(R.id.rvContacts);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnAddContact = findViewById(R.id.btnAddContact);
        btnBroadcastToAll = findViewById(R.id.btnBroadcastToAll);
        btnImportContacts = findViewById(R.id.btnImportContacts);
    }

    private void setupRecyclerView() {
        contactList = contactStorage.getAllContacts();
        adapter = new EmergencyContactAdapter(contactList, new EmergencyContactAdapter.ContactActionListener() {
            @Override
            public void onMessageClick(EmergencyContact contact) {
                sendMessageToContact(contact);
            }

            @Override
            public void onCallClick(EmergencyContact contact) {
                callContact(contact);
            }

            @Override
            public void onDeleteClick(EmergencyContact contact) {
                deleteContact(contact);
            }
        });

        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnAddContact.setOnClickListener(v -> showAddContactDialog());
        btnBroadcastToAll.setOnClickListener(v -> broadcastToAllContacts());
        btnImportContacts.setOnClickListener(v -> importContacts());
    }

    private void loadContacts() {
        contactList.clear();
        contactList.addAll(contactStorage.getAllContacts());
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (contactList.isEmpty()) {
            rvContacts.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
            btnBroadcastToAll.setEnabled(false);
        } else {
            rvContacts.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
            btnBroadcastToAll.setEnabled(true);
        }
    }

    private void showAddContactDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_contact);

        EditText etName = dialog.findViewById(R.id.etContactName);
        EditText etPhone = dialog.findViewById(R.id.etContactPhone);
        Spinner spinnerRelationship = dialog.findViewById(R.id.spinnerRelationship);
        CheckBox cbPrimary = dialog.findViewById(R.id.cbPrimaryContact);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        // Setup relationship spinner
        String[] relationships = {"Family", "Friend", "Colleague", "Doctor", "Other"};
        ArrayAdapter<String> relationshipAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, relationships);
        relationshipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRelationship.setAdapter(relationshipAdapter);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String relationship = spinnerRelationship.getSelectedItem().toString();
            boolean isPrimary = cbPrimary.isChecked();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            EmergencyContact contact = new EmergencyContact(name, phone, relationship, isPrimary);
            contactStorage.addContact(contact);
            loadContacts();
            dialog.dismiss();

            Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void sendMessageToContact(EmergencyContact contact) {
        Intent intent = new Intent(this, SendMessageActivity.class);
        intent.putExtra("recipient_phone", contact.getPhoneNumber());
        intent.putExtra("recipient_name", contact.getName());
        startActivity(intent);
    }

    private void callContact(EmergencyContact contact) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + contact.getPhoneNumber()));
        startActivity(callIntent);
    }

    private void deleteContact(EmergencyContact contact) {
        contactStorage.removeContact(contact);
        loadContacts();
        Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
    }

    private void broadcastToAllContacts() {
        if (contactList.isEmpty()) {
            Toast.makeText(this, "No contacts to broadcast to", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create broadcast message intent
        Intent intent = new Intent(this, SendMessageActivity.class);
        intent.putExtra("broadcast_mode", true);
        startActivity(intent);
    }

    private void importContacts() {
        Toast.makeText(this, "Contact import feature coming soon!", Toast.LENGTH_SHORT).show();
        // TODO: Implement contact import from phone contacts
    }
}
