package com.emergencymesh.app.models;

public class Message {
    private String id;
    private String senderName;
    private String senderPhone;
    private String recipientPhone;
    private String content;
    private long timestamp;
    private boolean isDelivered;
    private String messageType; // "text", "location", "alert"

    public Message() {}

    public Message(String id, String senderName, String senderPhone,
                   String recipientPhone, String content, String messageType) {
        this.id = id;
        this.senderName = senderName;
        this.senderPhone = senderPhone;
        this.recipientPhone = recipientPhone;
        this.content = content;
        this.messageType = messageType;
        this.timestamp = System.currentTimeMillis();
        this.isDelivered = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}