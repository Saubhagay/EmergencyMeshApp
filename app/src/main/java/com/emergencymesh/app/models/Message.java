package com.emergencymesh.app.models;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private String id;
    private String senderName;
    private String senderPhone;
    private String recipientPhone;
    private String content;
    private long timestamp;
    private boolean isDelivered;
    private String messageType; // "text", "location", "alert"

    // NEW: Multi-hop routing fields
    private int hopCount;           // Number of hops this message has traveled
    private int maxHops;            // Maximum allowed hops (default: 5)
    private List<String> routePath; // List of device addresses that forwarded this message
    private long ttl;               // Time-to-live in milliseconds (default: 1 hour)
    private String originDeviceId;  // Original sender's device ID

    public Message() {
        this.hopCount = 0;
        this.maxHops = 5; // Allow up to 5 hops
        this.routePath = new ArrayList<>();
        this.ttl = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour TTL
    }

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

        // Initialize routing fields
        this.hopCount = 0;
        this.maxHops = 5;
        this.routePath = new ArrayList<>();
        this.ttl = System.currentTimeMillis() + (60 * 60 * 1000);
    }

    // Existing getters and setters
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

    // NEW: Routing getters and setters
    public int getHopCount() { return hopCount; }
    public void setHopCount(int hopCount) { this.hopCount = hopCount; }

    public int getMaxHops() { return maxHops; }
    public void setMaxHops(int maxHops) { this.maxHops = maxHops; }

    public List<String> getRoutePath() {
        if (routePath == null) {
            routePath = new ArrayList<>();
        }
        return routePath;
    }
    public void setRoutePath(List<String> routePath) { this.routePath = routePath; }

    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }

    public String getOriginDeviceId() { return originDeviceId; }
    public void setOriginDeviceId(String originDeviceId) { this.originDeviceId = originDeviceId; }

    // NEW: Helper methods for routing
    public boolean canForward() {
        return hopCount < maxHops && System.currentTimeMillis() < ttl;
    }

    public void incrementHop(String deviceAddress) {
        this.hopCount++;
        if (this.routePath == null) {
            this.routePath = new ArrayList<>();
        }
        this.routePath.add(deviceAddress);
    }

    public boolean hasVisitedDevice(String deviceAddress) {
        if (routePath == null) {
            return false;
        }
        return routePath.contains(deviceAddress);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > ttl;
    }
}