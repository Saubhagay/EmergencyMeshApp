package com.emergencymesh.app.models;

public class EmergencyContact {
    private String name;
    private String phoneNumber;
    private String relationship;
    private boolean isPrimary;

    public EmergencyContact() {}

    public EmergencyContact(String name, String phoneNumber, String relationship, boolean isPrimary) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.isPrimary = isPrimary;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
}
