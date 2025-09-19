package com.emergencymesh.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencymesh.app.R;
import com.emergencymesh.app.models.EmergencyContact;
import java.util.List;
import java.util.Random;

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    private List<EmergencyContact> contacts;
    private ContactActionListener listener;

    public interface ContactActionListener {
        void onMessageClick(EmergencyContact contact);
        void onCallClick(EmergencyContact contact);
        void onDeleteClick(EmergencyContact contact);
    }

    public EmergencyContactAdapter(List<EmergencyContact> contacts, ContactActionListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);

        // Set contact name and get initial
        holder.tvContactName.setText(contact.getName());
        String initial = contact.getName().substring(0, 1).toUpperCase();
        holder.tvContactInitial.setText(initial);

        // Set phone and relationship
        holder.tvContactPhone.setText(contact.getPhoneNumber());
        holder.tvContactRelationship.setText(contact.getRelationship());

        // Show primary badge if this is primary contact
        if (contact.isPrimary()) {
            holder.tvPrimaryBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvPrimaryBadge.setVisibility(View.GONE);
        }

        // Set random background color for profile circle
        int[] colors = {
                Color.parseColor("#FF5722"), Color.parseColor("#E91E63"),
                Color.parseColor("#9C27B0"), Color.parseColor("#673AB7"),
                Color.parseColor("#3F51B5"), Color.parseColor("#2196F3"),
                Color.parseColor("#00BCD4"), Color.parseColor("#009688"),
                Color.parseColor("#4CAF50"), Color.parseColor("#8BC34A")
        };
        int colorIndex = Math.abs(contact.getName().hashCode()) % colors.length;
        holder.tvContactInitial.setBackgroundColor(colors[colorIndex]);

        // Set click listeners
        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageClick(contact);
            }
        });

        holder.btnCall.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCallClick(contact);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvContactName, tvContactPhone, tvContactRelationship,
                tvContactInitial, tvPrimaryBadge;
        Button btnMessage, btnCall, btnDelete;

        public ContactViewHolder(View itemView) {
            super(itemView);
            tvContactName = itemView.findViewById(R.id.tvContactName);
            tvContactPhone = itemView.findViewById(R.id.tvContactPhone);
            tvContactRelationship = itemView.findViewById(R.id.tvContactRelationship);
            tvContactInitial = itemView.findViewById(R.id.tvContactInitial);
            tvPrimaryBadge = itemView.findViewById(R.id.tvPrimaryBadge);
            btnMessage = itemView.findViewById(R.id.btnMessage);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
