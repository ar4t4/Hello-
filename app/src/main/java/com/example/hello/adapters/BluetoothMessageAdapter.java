package com.example.hello.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.R;
import com.example.hello.models.BluetoothMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BluetoothMessageAdapter extends RecyclerView.Adapter<BluetoothMessageAdapter.MessageViewHolder> {

    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;

    private final List<BluetoothMessage> messages;
    private final SimpleDateFormat timeFormat;

    public BluetoothMessageAdapter(List<BluetoothMessage> messages) {
        this.messages = messages;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isOutgoing() ? TYPE_OUTGOING : TYPE_INCOMING;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == TYPE_OUTGOING ? 
            R.layout.item_bluetooth_message_outgoing : 
            R.layout.item_bluetooth_message_incoming;
        
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        BluetoothMessage message = messages.get(position);
        holder.bind(message, timeFormat);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageContent;
        private final TextView messageTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContent = itemView.findViewById(R.id.messageContent);
            messageTime = itemView.findViewById(R.id.messageTime);
        }

        public void bind(BluetoothMessage message, SimpleDateFormat timeFormat) {
            messageContent.setText(message.getContent());
            messageTime.setText(timeFormat.format(new Date(message.getTimestamp())));
        }
    }
}
