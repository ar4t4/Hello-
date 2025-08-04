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

/**
 * BluetoothMeshDeviceAdapter - Adapter for displaying discovered mesh-capable devices
 * 
 * This adapter displays Bluetooth devices that are compatible with mesh networking,
 * showing device information and mesh connectivity status.
 */
public class BluetoothMeshDeviceAdapter extends RecyclerView.Adapter<BluetoothMeshDeviceAdapter.MeshDeviceViewHolder> {

    public interface OnMeshDeviceClickListener {
        void onMeshDeviceClick(BluetoothDevice device);
    }

    private final List<BluetoothDevice> devices;
    private final OnMeshDeviceClickListener listener;
    private final Context context;

    public BluetoothMeshDeviceAdapter(List<BluetoothDevice> devices, OnMeshDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
        this.context = null;
    }

    public BluetoothMeshDeviceAdapter(List<BluetoothDevice> devices, Context context, OnMeshDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public MeshDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bluetooth_mesh_device, parent, false);
        return new MeshDeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MeshDeviceViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class MeshDeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView deviceName;
        private final TextView deviceAddress;
        private final TextView deviceStatus;
        private final TextView meshCapability;
        private final ImageView deviceIcon;
        private final ImageView meshIcon;
        private final View itemView;

        public MeshDeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            deviceName = itemView.findViewById(R.id.meshDeviceName);
            deviceAddress = itemView.findViewById(R.id.meshDeviceAddress);
            deviceStatus = itemView.findViewById(R.id.meshDeviceStatus);
            meshCapability = itemView.findViewById(R.id.meshCapability);
            deviceIcon = itemView.findViewById(R.id.meshDeviceIcon);
            meshIcon = itemView.findViewById(R.id.meshIcon);
        }

        public void bind(BluetoothDevice device, OnMeshDeviceClickListener listener) {
            // Set device name
            String name = null;
            if (ActivityCompat.checkSelfPermission(itemView.getContext(), android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                name = device.getName();
            }
            
            if (name != null && !name.isEmpty()) {
                deviceName.setText(name);
            } else {
                deviceName.setText("Unknown Mesh Device");
            }

            // Set device address
            deviceAddress.setText(device.getAddress());

            // Set device status based on bond state
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                deviceStatus.setText("Paired & Ready");
                deviceStatus.setTextColor(itemView.getContext().getColor(R.color.success));
                meshCapability.setText("Mesh Compatible");
                meshCapability.setTextColor(itemView.getContext().getColor(R.color.success));
            } else {
                deviceStatus.setText("Available");
                deviceStatus.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                meshCapability.setText("Auto-Discovered");
                meshCapability.setTextColor(itemView.getContext().getColor(R.color.primary));
            }

            // Set device icons
            setMeshDeviceIcon(device);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMeshDeviceClick(device);
                }
            });
        }

        private void setMeshDeviceIcon(BluetoothDevice device) {
            // Set device icon based on device type and mesh capability
            deviceIcon.setImageResource(R.drawable.ic_bluetooth);
            
            // Show mesh networking icon
            if (meshIcon != null) {
                meshIcon.setImageResource(R.drawable.ic_network);
                meshIcon.setVisibility(View.VISIBLE);
            }
        }
    }
}
