package com.emergencymesh.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
    private View tvEmptyState;
    private TextView tvMessageCount;
    private Button btnClearAll, btnClearIncoming, btnClearOutgoing;
    private LinearLayout navHome, navInbox, navMesh, navOn;
    private MessageAdapter adapter;
    private MessageStorage messageStorage;
    private List<Message> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_inbox);

        try {
            messageStorage = new MessageStorage(this);
            initViews();
            setupRecyclerView();
            setupButtons();
            setupNavigation();
            loadMessages();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            e.printStackTrace();
            Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvMessageCount = findViewById(R.id.tvMessageCount);
        btnClearAll = findViewById(R.id.btnClearAll);
        btnClearIncoming = findViewById(R.id.btnClearIncoming);
        btnClearOutgoing = findViewById(R.id.btnClearOutgoing);
        navHome = findViewById(R.id.navHome);
        navInbox = findViewById(R.id.navInbox);
        navMesh = findViewById(R.id.navMesh);
        navOn = findViewById(R.id.navOn);
    }

    private void setupNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
        if (navInbox != null) {
            navInbox.setOnClickListener(v -> loadMessages());
        }
        if (navMesh != null) {
            navMesh.setOnClickListener(v -> {
                startActivity(new Intent(this, NearbyDevicesActivity.class));
                finish();
            });
        }
        if (navOn != null) {
            navOn.setOnClickListener(v ->
                Toast.makeText(this, "Mesh is active", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupRecyclerView() {
        try {
            messageList = messageStorage.getAllMessages();
            adapter = new MessageAdapter(messageList, new MessageAdapter.OnMessageActionListener() {
                @Override
                public void onDeleteMessage(Message message, int position) {
                    deleteMessage(message, position);
                }

                @Override
                public void onReplyMessage(Message message) {
                    // Open SendMessageActivity pre-filled with sender info
                    Intent intent = new Intent(MessageInboxActivity.this, SendMessageActivity.class);
                    if (message.getSenderPhone() != null && !message.getSenderPhone().isEmpty()) {
                        intent.putExtra("recipient_phone", message.getSenderPhone());
                        intent.putExtra("recipient_name", message.getSenderName());
                    }
                    startActivity(intent);
                }
            });

            if (rvMessages != null) {
                rvMessages.setLayoutManager(new LinearLayoutManager(this));
                rvMessages.setAdapter(adapter);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    private void setupButtons() {
        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> showClearAllDialog());
        }

        if (btnClearIncoming != null) {
            btnClearIncoming.setOnClickListener(v -> showClearIncomingDialog());
        }

        if (btnClearOutgoing != null) {
            btnClearOutgoing.setOnClickListener(v -> showClearOutgoingDialog());
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

    private void deleteMessage(Message message, int position) {
        if (message == null) {
            Toast.makeText(this, "Error deleting message", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        messageStorage.deleteMessage(message.getId());

                        if (messageList != null && position >= 0 && position < messageList.size()) {
                            messageList.remove(position);
                            if (adapter != null) {
                                adapter.notifyItemRemoved(position);
                            }
                        }

                        updateUI();
                        Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting message", e);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearAllDialog() {
        int totalMessages = messageStorage.getTotalMessageCount();

        if (totalMessages == 0) {
            Toast.makeText(this, "No messages to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear All Messages")
                .setMessage("Delete all " + totalMessages + " messages?")
                .setPositiveButton("YES", (dialog, which) -> {
                    messageStorage.clearAllMessages();
                    loadMessages();
                    Toast.makeText(this, "All messages cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearIncomingDialog() {
        List<Message> incomingMessages = messageStorage.getIncomingMessages();
        int incomingCount = incomingMessages != null ? incomingMessages.size() : 0;

        if (incomingCount == 0) {
            Toast.makeText(this, "No received messages to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear Received Messages")
                .setMessage("Delete all " + incomingCount + " received messages?")
                .setPositiveButton("YES", (dialog, which) -> {
                    messageStorage.clearIncomingMessages();
                    loadMessages();
                    Toast.makeText(this, "Received messages cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearOutgoingDialog() {
        List<Message> outgoingMessages = messageStorage.getOutgoingMessages();
        int outgoingCount = outgoingMessages != null ? outgoingMessages.size() : 0;

        if (outgoingCount == 0) {
            Toast.makeText(this, "No sent messages to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear Sent Messages")
                .setMessage("Delete all " + outgoingCount + " sent messages?")
                .setPositiveButton("YES", (dialog, which) -> {
                    messageStorage.clearOutgoingMessages();
                    loadMessages();
                    Toast.makeText(this, "Sent messages cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }
}