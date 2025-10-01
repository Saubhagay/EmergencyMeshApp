package com.emergencymesh.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsHelper {
    private static final String PREF_NAME = "EmergencyMeshPrefs";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_EMERGENCY_CONTACT = "emergency_contact";
    private static final String KEY_BLOOD_GROUP = "blood_group";
    private static final String KEY_PROFILE_SETUP = "profile_setup_complete";

    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;
    private static final String KEY_USER_ROLE = "user_role"; // "emergency" or "saviour"


    public void setUserRole(String role) {
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public String getUserRole() {
        return sharedPrefs.getString(KEY_USER_ROLE, "emergency"); // Default to emergency
    }

    public SharedPrefsHelper(Context context) {
        sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();
    }


    public void saveProfile(String name, String phone, String emergencyContact, String bloodGroup) {
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_EMERGENCY_CONTACT, emergencyContact);
        editor.putString(KEY_BLOOD_GROUP, bloodGroup);
        editor.putBoolean(KEY_PROFILE_SETUP, true);
        editor.apply();
    }

    public String getName() {
        return sharedPrefs.getString(KEY_NAME, "");
    }

    public String getPhone() {
        return sharedPrefs.getString(KEY_PHONE, "");
    }

    public String getEmergencyContact() {
        return sharedPrefs.getString(KEY_EMERGENCY_CONTACT, "");
    }

    public String getBloodGroup() {
        return sharedPrefs.getString(KEY_BLOOD_GROUP, "");
    }

    public boolean isProfileComplete() {
        return sharedPrefs.getBoolean(KEY_PROFILE_SETUP, false);
    }
}
