package com.example.hello.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.R;

import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    private final List<BluetoothDevice> devices;
    private final OnDeviceClickListener listener;
    private final Context context;

    public BluetoothDeviceAdapter(List<BluetoothDevice> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
        this.context = null;
    }

    public BluetoothDeviceAdapter(List<BluetoothDevice> devices, Context context, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bluetooth_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView deviceName;
        private final TextView deviceAddress;
        private final TextView deviceStatus;
        private final ImageView deviceIcon;
        private final View itemView;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceAddress = itemView.findViewById(R.id.deviceAddress);
            deviceStatus = itemView.findViewById(R.id.deviceStatus);
            deviceIcon = itemView.findViewById(R.id.deviceIcon);
        }

        public void bind(BluetoothDevice device, OnDeviceClickListener listener) {
            // Set device name
            String name = null;
            if (ActivityCompat.checkSelfPermission(itemView.getContext(), android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                name = device.getName();
            }
            
            if (name != null && !name.isEmpty()) {
                deviceName.setText(name);
            } else {
                deviceName.setText("Unknown Device");
            }

            // Set device address
            deviceAddress.setText(device.getAddress());

            // Set device status
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                deviceStatus.setText("Paired");
                deviceStatus.setTextColor(itemView.getContext().getColor(R.color.success));
            } else {
                deviceStatus.setText("Available");
                deviceStatus.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            }

            // Set device icon based on device type
            setDeviceIcon(device);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });
        }

        private void setDeviceIcon(BluetoothDevice device) {
            // You can enhance this to show different icons based on device type
            deviceIcon.setImageResource(R.drawable.ic_bluetooth);
        }
    }
}
