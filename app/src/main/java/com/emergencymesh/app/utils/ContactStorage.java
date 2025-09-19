package com.emergencymesh.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.emergencymesh.app.models.EmergencyContact;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ContactStorage {
    private static final String PREF_NAME = "ContactStorage";
    private static final String KEY_CONTACTS = "emergency_contacts";

    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public ContactStorage(Context context) {
        sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();
        gson = new Gson();
    }

    public void addContact(EmergencyContact contact) {
        List<EmergencyContact> contacts = getAllContacts();

        // If this is set as primary, remove primary flag from others
        if (contact.isPrimary()) {
            for (EmergencyContact existingContact : contacts) {
                existingContact.setPrimary(false);
            }
        }

        contacts.add(contact);
        saveContacts(contacts);
    }

    public void removeContact(EmergencyContact contactToRemove) {
        List<EmergencyContact> contacts = getAllContacts();

        // Manually remove matching contact
        for (int i = 0; i < contacts.size(); i++) {
            EmergencyContact contact = contacts.get(i);
            if (contact.getName().equals(contactToRemove.getName()) &&
                    contact.getPhoneNumber().equals(contactToRemove.getPhoneNumber())) {
                contacts.remove(i);
                break;  // Exit after removing the first match
            }
        }
        saveContacts(contacts);
    }


    public List<EmergencyContact> getAllContacts() {
        String json = sharedPrefs.getString(KEY_CONTACTS, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<EmergencyContact>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public EmergencyContact getPrimaryContact() {
        List<EmergencyContact> contacts = getAllContacts();
        for (EmergencyContact contact : contacts) {
            if (contact.isPrimary()) {
                return contact;
            }
        }
        return contacts.isEmpty() ? null : contacts.get(0);
    }

    private void saveContacts(List<EmergencyContact> contacts) {
        String json = gson.toJson(contacts);
        editor.putString(KEY_CONTACTS, json);
        editor.apply();
    }
}
