package com.emergencymesh.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.emergencymesh.app.models.Message;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessageStorage {
    private static final String TAG = "MessageStorage";
    private static final String PREF_NAME = "MessageStorage";
    private static final String KEY_OUTGOING_MESSAGES = "outgoing_messages";
    private static final String KEY_INCOMING_MESSAGES = "incoming_messages";

    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public MessageStorage(Context context) {
        try {
            sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            editor = sharedPrefs.edit();
            gson = new Gson();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MessageStorage", e);
        }
    }

    public void storeOutgoingMessage(Message message) {
        if (message == null) {
            Log.w(TAG, "Attempted to store null outgoing message");
            return;
        }

        try {
            List<Message> messages = getOutgoingMessages();
            messages.add(message);

            String json = gson.toJson(messages);
            editor.putString(KEY_OUTGOING_MESSAGES, json);
            editor.apply();

            Log.d(TAG, "Stored outgoing message: " + message.getMessageType());
        } catch (Exception e) {
            Log.e(TAG, "Error storing outgoing message", e);
        }
    }

    public void storeIncomingMessage(Message message) {
        if (message == null) {
            Log.w(TAG, "Attempted to store null incoming message");
            return;
        }

        try {
            List<Message> messages = getIncomingMessages();

            // Check for duplicates (based on message ID)
            for (Message existingMessage : messages) {
                if (existingMessage.getId() != null && existingMessage.getId().equals(message.getId())) {
                    Log.d(TAG, "Duplicate message ignored: " + message.getId());
                    return;
                }
            }

            messages.add(message);

            String json = gson.toJson(messages);
            editor.putString(KEY_INCOMING_MESSAGES, json);
            editor.apply();

            Log.d(TAG, "Stored incoming message from: " + message.getSenderName());
        } catch (Exception e) {
            Log.e(TAG, "Error storing incoming message", e);
        }
    }

    public List<Message> getOutgoingMessages() {
        try {
            String json = sharedPrefs.getString(KEY_OUTGOING_MESSAGES, "");
            if (json.isEmpty()) {
                return new ArrayList<>();
            }

            Type type = new TypeToken<List<Message>>(){}.getType();
            List<Message> messages = gson.fromJson(json, type);
            return messages != null ? messages : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error getting outgoing messages", e);
            return new ArrayList<>();
        }
    }

    public List<Message> getIncomingMessages() {
        try {
            String json = sharedPrefs.getString(KEY_INCOMING_MESSAGES, "");
            if (json.isEmpty()) {
                return new ArrayList<>();
            }

            Type type = new TypeToken<List<Message>>(){}.getType();
            List<Message> messages = gson.fromJson(json, type);
            return messages != null ? messages : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error getting incoming messages", e);
            return new ArrayList<>();
        }
    }

    public List<Message> getAllMessages() {
        try {
            List<Message> allMessages = new ArrayList<>();
            allMessages.addAll(getOutgoingMessages());
            allMessages.addAll(getIncomingMessages());

            // Sort by timestamp (newest first)
            Collections.sort(allMessages, new Comparator<Message>() {
                @Override
                public int compare(Message m1, Message m2) {
                    return Long.compare(m2.getTimestamp(), m1.getTimestamp());
                }
            });

            return allMessages;
        } catch (Exception e) {
            Log.e(TAG, "Error getting all messages", e);
            return new ArrayList<>();
        }
    }

    public List<Message> getMessagesByType(String messageType) {
        try {
            List<Message> allMessages = getAllMessages();
            List<Message> filteredMessages = new ArrayList<>();

            for (Message message : allMessages) {
                if (messageType.equals(message.getMessageType())) {
                    filteredMessages.add(message);
                }
            }

            return filteredMessages;
        } catch (Exception e) {
            Log.e(TAG, "Error filtering messages by type", e);
            return new ArrayList<>();
        }
    }

    public List<Message> getRecentMessages(int count) {
        try {
            List<Message> allMessages = getAllMessages();
            if (allMessages.size() <= count) {
                return allMessages;
            }
            return allMessages.subList(0, count);
        } catch (Exception e) {
            Log.e(TAG, "Error getting recent messages", e);
            return new ArrayList<>();
        }
    }

    public void markMessageAsDelivered(String messageId) {
        if (messageId == null) {
            Log.w(TAG, "Attempted to mark null message ID as delivered");
            return;
        }

        try {
            List<Message> messages = getOutgoingMessages();
            boolean updated = false;

            for (Message message : messages) {
                if (messageId.equals(message.getId())) {
                    message.setDelivered(true);
                    updated = true;
                    break;
                }
            }

            if (updated) {
                String json = gson.toJson(messages);
                editor.putString(KEY_OUTGOING_MESSAGES, json);
                editor.apply();
                Log.d(TAG, "Marked message as delivered: " + messageId);
            } else {
                Log.w(TAG, "Message not found for delivery confirmation: " + messageId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking message as delivered", e);
        }
    }

    public int getUnreadMessageCount() {
        try {
            // For simplicity, we'll consider all incoming messages as "unread"
            // You could add a separate "read" flag to the Message model if needed
            return getIncomingMessages().size();
        } catch (Exception e) {
            Log.e(TAG, "Error getting unread message count", e);
            return 0;
        }
    }

    public int getTotalMessageCount() {
        try {
            return getAllMessages().size();
        } catch (Exception e) {
            Log.e(TAG, "Error getting total message count", e);
            return 0;
        }
    }

    public void clearAllMessages() {
        try {
            editor.putString(KEY_OUTGOING_MESSAGES, "");
            editor.putString(KEY_INCOMING_MESSAGES, "");
            editor.apply();
            Log.d(TAG, "All messages cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing messages", e);
        }
    }

    public void clearOutgoingMessages() {
        try {
            editor.putString(KEY_OUTGOING_MESSAGES, "");
            editor.apply();
            Log.d(TAG, "Outgoing messages cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing outgoing messages", e);
        }
    }

    public void clearIncomingMessages() {
        try {
            editor.putString(KEY_INCOMING_MESSAGES, "");
            editor.apply();
            Log.d(TAG, "Incoming messages cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing incoming messages", e);
        }
    }

    public boolean hasMessages() {
        try {
            return getTotalMessageCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if has messages", e);
            return false;
        }
    }

    public Message getMessageById(String messageId) {
        if (messageId == null) {
            return null;
        }

        try {
            List<Message> allMessages = getAllMessages();
            for (Message message : allMessages) {
                if (messageId.equals(message.getId())) {
                    return message;
                }
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting message by ID", e);
            return null;
        }
    }

    public void deleteMessage(String messageId) {
        if (messageId == null) {
            Log.w(TAG, "Attempted to delete null message ID");
            return;
        }

        try {
            // Check and remove from outgoing messages
            List<Message> outgoingMessages = getOutgoingMessages();
            boolean removedFromOutgoing = outgoingMessages.removeIf(msg -> messageId.equals(msg.getId()));

            if (removedFromOutgoing) {
                String json = gson.toJson(outgoingMessages);
                editor.putString(KEY_OUTGOING_MESSAGES, json);
                editor.apply();
                Log.d(TAG, "Deleted outgoing message: " + messageId);
                return;
            }

            // Check and remove from incoming messages
            List<Message> incomingMessages = getIncomingMessages();
            boolean removedFromIncoming = incomingMessages.removeIf(msg -> messageId.equals(msg.getId()));

            if (removedFromIncoming) {
                String json = gson.toJson(incomingMessages);
                editor.putString(KEY_INCOMING_MESSAGES, json);
                editor.apply();
                Log.d(TAG, "Deleted incoming message: " + messageId);
            } else {
                Log.w(TAG, "Message not found for deletion: " + messageId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting message", e);
        }
    }
}