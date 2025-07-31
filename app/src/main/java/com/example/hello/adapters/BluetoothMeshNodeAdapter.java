package com.example.hello.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.R;
import com.example.hello.models.BluetoothMeshNode;

import java.util.List;

/**
 * BluetoothMeshNodeAdapter - RecyclerView adapter for network nodes
 * 
 * This adapter displays the nodes in the mesh network with their
 * connection status, hop distance, and other network information.
 */
public class BluetoothMeshNodeAdapter extends RecyclerView.Adapter<BluetoothMeshNodeAdapter.NodeViewHolder> {
    
    public interface OnNodeClickListener {
        void onNodeClick(BluetoothMeshNode node);
    }
    
    private final List<BluetoothMeshNode> nodes;
    private final OnNodeClickListener listener;

    public BluetoothMeshNodeAdapter(List<BluetoothMeshNode> nodes, OnNodeClickListener listener) {
        this.nodes = nodes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bluetooth_mesh_node, parent, false);
        return new NodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NodeViewHolder holder, int position) {
        BluetoothMeshNode node = nodes.get(position);
        
        // Set device name
        holder.deviceName.setText(node.getDisplayName());
        
        // Set connection info
        holder.connectionInfo.setText(node.getConnectionInfo());
        
        // Set status icon and color based on node status
        switch (node.getStatus()) {
            case ONLINE:
                holder.statusIcon.setImageResource(R.drawable.ic_circle);
                holder.statusIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
                break;
            case OFFLINE:
                holder.statusIcon.setImageResource(R.drawable.ic_circle);
                holder.statusIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.gray));
                break;
            case CONNECTING:
                holder.statusIcon.setImageResource(R.drawable.ic_refresh);
                holder.statusIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.orange));
                break;
            case UNREACHABLE:
                holder.statusIcon.setImageResource(R.drawable.ic_error);
                holder.statusIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
                break;
        }
        
        // Set hop count indicator
        if (node.isDirectlyConnected()) {
            holder.hopIndicator.setText("Direct");
            holder.hopIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
        } else if (node.getHopDistance() == Integer.MAX_VALUE) {
            holder.hopIndicator.setText("∞");
            holder.hopIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
        } else {
            holder.hopIndicator.setText(String.valueOf(node.getHopDistance()));
            if (node.getHopDistance() <= 2) {
                holder.hopIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
            } else if (node.getHopDistance() <= 4) {
                holder.hopIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.orange));
            } else {
                holder.hopIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
            }
        }
        
        // Set connected nodes count
        int connectedCount = node.getConnectedNodes().size();
        if (connectedCount > 0) {
            holder.connectedCount.setVisibility(View.VISIBLE);
            holder.connectedCount.setText(connectedCount + " connected");
        } else {
            holder.connectedCount.setVisibility(View.GONE);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNodeClick(node);
            }
        });
    }

    @Override
    public int getItemCount() {
        return nodes.size();
    }

    static class NodeViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceName;
        final TextView connectionInfo;
        final TextView connectedCount;
        final TextView hopIndicator;
        final ImageView statusIcon;

        NodeViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            connectionInfo = itemView.findViewById(R.id.connection_info);
            connectedCount = itemView.findViewById(R.id.connected_count);
            hopIndicator = itemView.findViewById(R.id.hop_indicator);
            statusIcon = itemView.findViewById(R.id.status_icon);
        }
    }
}
