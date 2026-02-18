package com.emergencymesh.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencymesh.app.R;
import com.emergencymesh.app.SendMessageActivity;
import com.emergencymesh.app.models.Message;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages;
    private SimpleDateFormat dateFormat;
    private OnMessageActionListener listener;

    public interface OnMessageActionListener {
        void onDeleteMessage(Message message, int position);
        void onReplyMessage(Message message);
    }

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    public MessageAdapter(List<Message> messages, OnMessageActionListener listener) {
        this.messages = messages;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        this.listener = listener;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        Context context = holder.itemView.getContext();

        // Set sender name and phone — always white
        String senderName = message.getSenderName() != null ? message.getSenderName() : "Unknown";
        String senderPhone = message.getSenderPhone();
        String senderInfo = "From: " + senderName;
        if (senderPhone != null && !senderPhone.isEmpty()) {
            senderInfo += " (" + senderPhone + ")";
        }
        holder.tvSenderInfo.setText(senderInfo);
        holder.tvSenderInfo.setTextColor(Color.WHITE);

        // Set message content and make links clickable
        holder.tvMessageContent.setText(message.getContent());
        holder.tvMessageContent.setAutoLinkMask(Linkify.WEB_URLS);
        Linkify.addLinks(holder.tvMessageContent, Linkify.WEB_URLS);
        holder.tvMessageContent.setLinksClickable(true);

        // Set timestamp
        Date messageDate = new Date(message.getTimestamp());
        holder.tvTimestamp.setText(dateFormat.format(messageDate));

        // Set message type and styling with hop count
        String messageType = message.getMessageType();
        int hopCount = message.getHopCount();
        String hopInfo = hopCount > 0 ? " • " + hopCount + " hop" + (hopCount > 1 ? "s" : "") : "";

        if ("alert".equals(messageType)) {
            holder.tvMessageType.setText("🚨 EMERGENCY ALERT" + hopInfo);
            holder.tvMessageType.setTextColor(Color.parseColor("#F44336"));
            holder.itemView.setBackgroundColor(Color.parseColor("#1A0000"));
        } else if ("location".equals(messageType)) {
            holder.tvMessageType.setText("📍 LOCATION SHARE" + hopInfo);
            holder.tvMessageType.setTextColor(Color.parseColor("#FF9800"));
            holder.itemView.setBackgroundColor(Color.parseColor("#1A1000"));
        } else {
            holder.tvMessageType.setText("💬 MESSAGE" + hopInfo);
            holder.tvMessageType.setTextColor(Color.parseColor("#58A6FF"));
            holder.itemView.setBackgroundColor(Color.parseColor("#0D1117"));
        }

        // Set delivery status — incoming = RECEIVED, outgoing = PENDING/Delivered
        boolean isIncomingMsg = message.getRecipientPhone() == null || message.getRecipientPhone().isEmpty();
        if (isIncomingMsg) {
            // Receiver always sees RECEIVED
            holder.tvDeliveryStatus.setText("✓ RECEIVED");
            holder.tvDeliveryStatus.setTextColor(Color.parseColor("#3FB950"));
        } else if (message.isDelivered()) {
            holder.tvDeliveryStatus.setText("✓ Delivered");
            holder.tvDeliveryStatus.setTextColor(Color.parseColor("#3FB950"));
        } else {
            holder.tvDeliveryStatus.setText("⏳ PENDING");
            holder.tvDeliveryStatus.setTextColor(Color.parseColor("#FF9800"));
        }

        // Show recipient info for outgoing messages
        if (message.getRecipientPhone() != null && !message.getRecipientPhone().isEmpty()) {
            holder.tvRecipientInfo.setText("To: " + message.getRecipientPhone());
            holder.tvRecipientInfo.setTextColor(Color.parseColor("#FFFFFF"));
            holder.tvRecipientInfo.setVisibility(View.VISIBLE);
        } else {
            holder.tvRecipientInfo.setVisibility(View.GONE);
        }

        // Show route path if available (for debugging/info)
        if (message.getRoutePath() != null && !message.getRoutePath().isEmpty() && hopCount > 0) {
            holder.tvRouteInfo.setText("📡 Relayed through mesh network");
            holder.tvRouteInfo.setVisibility(View.VISIBLE);
        } else {
            holder.tvRouteInfo.setVisibility(View.GONE);
        }

        // Delete button
        holder.btnDeleteMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteMessage(message, holder.getAdapterPosition());
            }
        });

        // Reply button — only show for genuine incoming messages
        // Incoming = no recipientPhone OR recipientPhone is not BROADCAST
        String recipientPhone = message.getRecipientPhone();
        boolean isIncoming = (recipientPhone == null || recipientPhone.isEmpty())
                && message.getSenderPhone() != null && !message.getSenderPhone().isEmpty();
        if (isIncoming) {
            holder.btnReply.setVisibility(View.VISIBLE);
            holder.btnReply.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReplyMessage(message);
                } else {
                    Intent intent = new Intent(context, SendMessageActivity.class);
                    intent.putExtra("recipient_phone", message.getSenderPhone());
                    intent.putExtra("recipient_name", message.getSenderName() != null ? message.getSenderName() : "");
                    context.startActivity(intent);
                }
            });
        } else {
            holder.btnReply.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderInfo, tvMessageContent, tvTimestamp, tvMessageType,
                tvDeliveryStatus, tvRecipientInfo, tvRouteInfo;
        Button btnDeleteMessage, btnReply;

        public MessageViewHolder(View itemView) {
            super(itemView);
            tvSenderInfo = itemView.findViewById(R.id.tvSenderInfo);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvMessageType = itemView.findViewById(R.id.tvMessageType);
            tvDeliveryStatus = itemView.findViewById(R.id.tvDeliveryStatus);
            tvRecipientInfo = itemView.findViewById(R.id.tvRecipientInfo);
            tvRouteInfo = itemView.findViewById(R.id.tvRouteInfo);
            btnDeleteMessage = itemView.findViewById(R.id.btnDeleteMessage);
            btnReply = itemView.findViewById(R.id.btnReply);
        }
    }
}