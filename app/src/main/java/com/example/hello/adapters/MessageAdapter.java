package com.example.hello.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hello.R;
import com.example.hello.helpers.CloudinaryHelper;
import com.example.hello.models.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private Context context;
    private List<Message> messages;
    private String currentUserId;
    private boolean isGroupChat;

    public MessageAdapter(Context context, List<Message> messages, String currentUserId, boolean isGroupChat) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.isGroupChat = isGroupChat;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(context).inflate(
                isGroupChat ? R.layout.item_message_sent_group : R.layout.item_message_sent, 
                parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view, viewType, isGroupChat);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        
        // Set message content
        if (message.getContent() != null) {
            holder.messageText.setText(message.getContent());
        } else {
            holder.messageText.setText("");
        }
        
        // Set message time
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.timeText.setText(sdf.format(new Date(message.getTimestamp())));

        // Show sender profile image if available for received messages
        if (holder.profileImage != null) {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
            
            if (message.getSenderProfileImageUrl() != null && !message.getSenderProfileImageUrl().isEmpty()) {
                // Load profile image using Cloudinary
                CloudinaryHelper.loadImage(context, message.getSenderProfileImageUrl(), holder.profileImage);
            } else if (message.getSenderId() != null) {
                // Load profile image from Firebase if not cached
                FirebaseDatabase.getInstance().getReference("Users")
                    .child(message.getSenderId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists() && snapshot.hasChild("profileImageUrl")) {
                                String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    CloudinaryHelper.loadImage(context, imageUrl, holder.profileImage);
                                    // Cache the image URL for future use
                                    message.setSenderProfileImageUrl(imageUrl);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Just use default placeholder on error
                        }
                    });
            }
        }

        // Show sender name for received messages in group chat
        if (holder.senderName != null) {
            holder.senderName.setVisibility(View.GONE);
            
            if (message.getSenderName() != null && !message.getSenderName().isEmpty()) {
                holder.senderName.setText(message.getSenderName());
                holder.senderName.setVisibility(View.VISIBLE);
            } else if (message.getSenderId() != null) {
                // Load sender name if not cached
                FirebaseDatabase.getInstance().getReference("Users")
                    .child(message.getSenderId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String name = "User";
                                // Try to get firstName first, then fall back to name
                                if (snapshot.hasChild("firstName")) {
                                    String firstName = snapshot.child("firstName").getValue(String.class);
                                    if (firstName != null && !firstName.isEmpty()) {
                                        name = firstName;
                                    }
                                }
                                
                                holder.senderName.setText(name);
                                holder.senderName.setVisibility(View.VISIBLE);
                                // Cache the name for future use
                                message.setSenderName(name);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Use default name on error
                            holder.senderName.setText("User");
                            holder.senderName.setVisibility(View.VISIBLE);
                        }
                    });
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView senderName;
        ImageView profileImage;

        MessageViewHolder(View itemView, int viewType, boolean isGroupChat) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            
            if (viewType == VIEW_TYPE_RECEIVED) {
                profileImage = itemView.findViewById(R.id.profileImage);
                senderName = itemView.findViewById(R.id.senderName);
            } else if (isGroupChat) {
                senderName = itemView.findViewById(R.id.senderName);
            }
        }
    }
} 