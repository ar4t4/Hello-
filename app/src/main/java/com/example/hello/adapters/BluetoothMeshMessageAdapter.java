package com.example.hello.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.R;
import com.example.hello.models.BluetoothMeshMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BluetoothMeshMessageAdapter - RecyclerView adapter for mesh network messages
 * 
 * This adapter displays chat messages in the mesh network with different
 * layouts for incoming and outgoing messages, plus system messages.
 */
public class BluetoothMeshMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;
    private static final int TYPE_SYSTEM = 3;

    private final List<BluetoothMeshMessage> messages;
    private final SimpleDateFormat timeFormat;

    public BluetoothMeshMessageAdapter(List<BluetoothMeshMessage> messages) {
        this.messages = messages;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        BluetoothMeshMessage message = messages.get(position);
        
        if (!message.isChatMessage()) {
            return TYPE_SYSTEM;
        }
        
        // For now, treat all messages as incoming since we don't have user identification
        // In a real implementation, you'd check if sourceNodeId matches current user
        return TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        switch (viewType) {
            case TYPE_OUTGOING:
                View outgoingView = inflater.inflate(R.layout.item_bluetooth_mesh_message_outgoing, parent, false);
                return new OutgoingMessageViewHolder(outgoingView);
                
            case TYPE_INCOMING:
                View incomingView = inflater.inflate(R.layout.item_bluetooth_mesh_message_incoming, parent, false);
                return new IncomingMessageViewHolder(incomingView);
                
            case TYPE_SYSTEM:
                View systemView = inflater.inflate(R.layout.item_bluetooth_mesh_message_system, parent, false);
                return new SystemMessageViewHolder(systemView);
                
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BluetoothMeshMessage message = messages.get(position);
        
        switch (holder.getItemViewType()) {
            case TYPE_OUTGOING:
                bindOutgoingMessage((OutgoingMessageViewHolder) holder, message);
                break;
                
            case TYPE_INCOMING:
                bindIncomingMessage((IncomingMessageViewHolder) holder, message);
                break;
                
            case TYPE_SYSTEM:
                bindSystemMessage((SystemMessageViewHolder) holder, message);
                break;
        }
    }

    private void bindOutgoingMessage(OutgoingMessageViewHolder holder, BluetoothMeshMessage message) {
        holder.messageContent.setText(message.getContent());
        holder.timestamp.setText(timeFormat.format(new Date(message.getTimestamp())));
        
        // Show hop count if message was forwarded
        if (message.getHopCount() > 0) {
            holder.hopCount.setVisibility(View.VISIBLE);
            holder.hopCount.setText(message.getHopCount() + " hop" + (message.getHopCount() > 1 ? "s" : ""));
        } else {
            holder.hopCount.setVisibility(View.GONE);
        }
    }

    private void bindIncomingMessage(IncomingMessageViewHolder holder, BluetoothMeshMessage message) {
        holder.messageContent.setText(message.getContent());
        holder.timestamp.setText(timeFormat.format(new Date(message.getTimestamp())));
        
        // Show sender name if available
        if (message.getSenderName() != null && !message.getSenderName().isEmpty()) {
            holder.senderName.setText(message.getSenderName());
            holder.senderName.setVisibility(View.VISIBLE);
        } else {
            holder.senderName.setText("Unknown Device");
            holder.senderName.setVisibility(View.VISIBLE);
        }
        
        // Show hop count if message was forwarded
        if (message.getHopCount() > 0) {
            holder.hopCount.setVisibility(View.VISIBLE);
            holder.hopCount.setText("via " + message.getHopCount() + " hop" + (message.getHopCount() > 1 ? "s" : ""));
        } else {
            holder.hopCount.setVisibility(View.GONE);
        }
    }

    private void bindSystemMessage(SystemMessageViewHolder holder, BluetoothMeshMessage message) {
        String systemText = "";
        
        switch (message.getType()) {
            case "DISCOVERY":
                systemText = "New device discovered in network";
                break;
            case "TOPOLOGY":
                systemText = "Network topology updated";
                break;
            case "HEARTBEAT":
                systemText = "Device heartbeat received";
                break;
            default:
                systemText = "System message: " + message.getType();
        }
        
        holder.systemMessage.setText(systemText);
        holder.timestamp.setText(timeFormat.format(new Date(message.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolder for outgoing messages
    static class OutgoingMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView messageContent;
        final TextView timestamp;
        final TextView hopCount;

        OutgoingMessageViewHolder(View itemView) {
            super(itemView);
            messageContent = itemView.findViewById(R.id.message_content);
            timestamp = itemView.findViewById(R.id.timestamp);
            hopCount = itemView.findViewById(R.id.hop_count);
        }
    }

    // ViewHolder for incoming messages
    static class IncomingMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView messageContent;
        final TextView timestamp;
        final TextView senderName;
        final TextView hopCount;

        IncomingMessageViewHolder(View itemView) {
            super(itemView);
            messageContent = itemView.findViewById(R.id.message_content);
            timestamp = itemView.findViewById(R.id.timestamp);
            senderName = itemView.findViewById(R.id.sender_name);
            hopCount = itemView.findViewById(R.id.hop_count);
        }
    }

    // ViewHolder for system messages
    static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView systemMessage;
        final TextView timestamp;

        SystemMessageViewHolder(View itemView) {
            super(itemView);
            systemMessage = itemView.findViewById(R.id.system_message);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}
