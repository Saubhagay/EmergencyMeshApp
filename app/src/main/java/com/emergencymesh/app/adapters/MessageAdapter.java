package com.emergencymesh.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencymesh.app.R;
import com.emergencymesh.app.models.Message;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages;
    private SimpleDateFormat dateFormat;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
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

        // Set sender name and phone
        String senderInfo = message.getSenderName();
        if (message.getSenderPhone() != null && !message.getSenderPhone().isEmpty()) {
            senderInfo += " (" + message.getSenderPhone() + ")";
        }
        holder.tvSenderInfo.setText(senderInfo);

        // Set message content
        holder.tvMessageContent.setText(message.getContent());

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
            holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE"));
        } else if ("location".equals(messageType)) {
            holder.tvMessageType.setText("📍 LOCATION SHARE" + hopInfo);
            holder.tvMessageType.setTextColor(Color.parseColor("#FF9800"));
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0"));
        } else {
            holder.tvMessageType.setText("💬 MESSAGE" + hopInfo);
            holder.tvMessageType.setTextColor(Color.parseColor("#2196F3"));
            holder.itemView.setBackgroundColor(Color.parseColor("#E3F2FD"));
        }

        // Set delivery status
        if (message.isDelivered()) {
            holder.tvDeliveryStatus.setText("✓ Delivered");
            holder.tvDeliveryStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.tvDeliveryStatus.setText("⏳ Pending");
            holder.tvDeliveryStatus.setTextColor(Color.parseColor("#FF9800"));
        }

        // Show recipient info for outgoing messages
        if (message.getRecipientPhone() != null && !message.getRecipientPhone().isEmpty()) {
            holder.tvRecipientInfo.setText("To: " + message.getRecipientPhone());
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
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderInfo, tvMessageContent, tvTimestamp, tvMessageType,
                tvDeliveryStatus, tvRecipientInfo, tvRouteInfo;

        public MessageViewHolder(View itemView) {
            super(itemView);
            tvSenderInfo = itemView.findViewById(R.id.tvSenderInfo);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvMessageType = itemView.findViewById(R.id.tvMessageType);
            tvDeliveryStatus = itemView.findViewById(R.id.tvDeliveryStatus);
            tvRecipientInfo = itemView.findViewById(R.id.tvRecipientInfo);
            tvRouteInfo = itemView.findViewById(R.id.tvRouteInfo);
        }
    }
}