package com.emergencymesh.app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencymesh.app.adapters.MessageAdapter;
import com.emergencymesh.app.models.Message;
import com.emergencymesh.app.utils.MessageStorage;
import java.util.List;

public class MessageInboxActivity extends AppCompatActivity {

    private static final String TAG = "MessageInboxActivity";

    private RecyclerView rvMessages;
    private TextView tvEmptyState, tvMessageCount;
    private MessageAdapter adapter;
    private MessageStorage messageStorage;
    private List<Message> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_message_inbox);

            messageStorage = new MessageStorage(this);

            initViews();
            setupRecyclerView();
            loadMessages();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        try {
            rvMessages = findViewById(R.id.rvMessages);
            tvEmptyState = findViewById(R.id.tvEmptyState);
            tvMessageCount = findViewById(R.id.tvMessageCount);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    private void setupRecyclerView() {
        try {
            messageList = messageStorage.getAllMessages();
            adapter = new MessageAdapter(messageList);

            if (rvMessages != null) {
                rvMessages.setLayoutManager(new LinearLayoutManager(this));
                rvMessages.setAdapter(adapter);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    private void loadMessages() {
        try {
            if (messageList != null && messageStorage != null) {
                messageList.clear();
                messageList.addAll(messageStorage.getAllMessages());

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                updateUI();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading messages", e);
            Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        try {
            int totalMessages = messageList != null ? messageList.size() : 0;

            if (tvMessageCount != null) {
                tvMessageCount.setText("Messages: " + totalMessages);
            }

            if (totalMessages == 0) {
                if (rvMessages != null) rvMessages.setVisibility(View.GONE);
                if (tvEmptyState != null) tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                if (rvMessages != null) rvMessages.setVisibility(View.VISIBLE);
                if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh messages when returning to this screen
        loadMessages();
    }
}