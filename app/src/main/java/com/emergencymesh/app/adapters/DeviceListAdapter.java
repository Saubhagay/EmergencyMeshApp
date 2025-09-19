package com.emergencymesh.app.adapters;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.emergencymesh.app.R;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private List<BluetoothDevice> devices;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    public DeviceListAdapter(List<BluetoothDevice> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        Context context = holder.itemView.getContext();

        // Set device name
        String deviceName = "Unknown Device";
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            deviceName = device.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Emergency Device";
            }
        }
        holder.tvDeviceName.setText(deviceName);

        // Set device address
        holder.tvDeviceAddress.setText(device.getAddress());

        // Set connection status
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            holder.tvDeviceStatus.setText("Paired");
            holder.tvDeviceStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        } else {
            holder.tvDeviceStatus.setText("Available");
            holder.tvDeviceStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
        }

        // Set click listener
        holder.btnConnect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName, tvDeviceAddress, tvDeviceStatus;
        Button btnConnect;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }
    }
}
