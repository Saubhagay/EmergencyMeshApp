package com.emergencymesh.app.utils;

import android.content.Context;
import java.util.Collections;
import java.util.Comparator;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.emergencymesh.app.models.Message;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MessageStorage {
    private static final String PREF_NAME = "MessageStorage";
    private static final String KEY_OUTGOING_MESSAGES = "outgoing_messages";
    private static final String KEY_INCOMING_MESSAGES = "incoming_messages";

    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public MessageStorage(Context context) {
        sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();
        gson = new Gson();
    }

    public void storeOutgoingMessage(Message message) {
        List<Message> messages = getOutgoingMessages();
        messages.add(message);

        String json = gson.toJson(messages);
        editor.putString(KEY_OUTGOING_MESSAGES, json);
        editor.apply();
    }

    public void storeIncomingMessage(Message message) {
        List<Message> messages = getIncomingMessages();
        messages.add(message);

        String json = gson.toJson(messages);
        editor.putString(KEY_INCOMING_MESSAGES, json);
        editor.apply();
    }

    public List<Message> getOutgoingMessages() {
        String json = sharedPrefs.getString(KEY_OUTGOING_MESSAGES, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<Message>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public List<Message> getIncomingMessages() {
        String json = sharedPrefs.getString(KEY_INCOMING_MESSAGES, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<Message>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public List<Message> getAllMessages() {
        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(getOutgoingMessages());
        allMessages.addAll(getIncomingMessages());

        // Sort by timestamp using Collections.sort (compatible with API levels < 24)
        Collections.sort(allMessages, new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) {
                return Long.compare(m2.getTimestamp(), m1.getTimestamp());
            }
        });

        return allMessages;
    }


    public void markMessageAsDelivered(String messageId) {
        List<Message> messages = getOutgoingMessages();
        for (Message message : messages) {
            if (message.getId().equals(messageId)) {
                message.setDelivered(true);
                break;
            }
        }

        String json = gson.toJson(messages);
        editor.putString(KEY_OUTGOING_MESSAGES, json);
        editor.apply();
    }
}
