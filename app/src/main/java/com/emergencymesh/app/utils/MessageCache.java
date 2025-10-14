package com.emergencymesh.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages a cache of recently seen messages to prevent routing loops
 * and duplicate message processing in multi-hop routing
 */
public class MessageCache {
    private static final String TAG = "MessageCache";
    private static final String PREF_NAME = "MessageCache";
    private static final String KEY_SEEN_MESSAGES = "seen_messages";
    private static final long CACHE_DURATION = 60 * 60 * 1000; // 1 hour

    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private Map<String, Long> seenMessages; // messageId -> timestamp

    public MessageCache(Context context) {
        this.sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = sharedPrefs.edit();
        this.gson = new Gson();
        loadCache();
    }

    private void loadCache() {
        try {
            String json = sharedPrefs.getString(KEY_SEEN_MESSAGES, "");
            if (!json.isEmpty()) {
                Type type = new TypeToken<Map<String, Long>>(){}.getType();
                seenMessages = gson.fromJson(json, type);
                if (seenMessages == null) {
                    seenMessages = new HashMap<>();
                }
                cleanExpiredEntries();
            } else {
                seenMessages = new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cache", e);
            seenMessages = new HashMap<>();
        }
    }

    private void saveCache() {
        try {
            String json = gson.toJson(seenMessages);
            editor.putString(KEY_SEEN_MESSAGES, json);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving cache", e);
        }
    }

    /**
     * Check if a message has been seen before
     */
    public boolean hasSeenMessage(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }
        cleanExpiredEntries();
        return seenMessages.containsKey(messageId);
    }

    /**
     * Mark a message as seen
     */
    public void markMessageAsSeen(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return;
        }
        seenMessages.put(messageId, System.currentTimeMillis());
        saveCache();
        Log.d(TAG, "Message marked as seen: " + messageId);
    }

    /**
     * Remove expired entries from cache
     */
    private void cleanExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = seenMessages.entrySet().iterator();

        int removedCount = 0;
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (currentTime - entry.getValue() > CACHE_DURATION) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            saveCache();
            Log.d(TAG, "Cleaned " + removedCount + " expired entries from cache");
        }
    }

    /**
     * Clear all cached messages
     */
    public void clearCache() {
        seenMessages.clear();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Message cache cleared");
    }

    /**
     * Get the number of cached messages
     */
    public int getCacheSize() {
        cleanExpiredEntries();
        return seenMessages.size();
    }
}