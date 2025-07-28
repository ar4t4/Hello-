package com.example.hello.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hello.ChatActivity;
import com.example.hello.R;
import com.example.hello.helpers.CloudinaryHelper;
import com.example.hello.models.Chat;
import com.example.hello.models.User;
import de.hdodenhof.circleimageview.CircleImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private Context context;
    private List<User> users;
    private String currentUserId;
    private String communityId;
    private DatabaseReference messagesRef;
    private SimpleDateFormat timeFormat;
    private Map<String, ValueEventListener> messageListeners;

    public ChatListAdapter(Context context, List<User> users, String currentUserId, String communityId) {
        this.context = context;
        this.users = users;
        this.currentUserId = currentUserId;
        this.communityId = communityId;
        this.messagesRef = FirebaseDatabase.getInstance().getReference("Messages");
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        this.messageListeners = new HashMap<>();
    }
    
    @Override
    public void onViewRecycled(@NonNull ChatViewHolder holder) {
        super.onViewRecycled(holder);
        
        // Remove any active listeners when the view is recycled
        String chatId = (String) holder.itemView.getTag();
        if (chatId != null && messageListeners.containsKey(chatId)) {
            ValueEventListener listener = messageListeners.get(chatId);
            if (listener != null) {
                messagesRef.child(chatId).removeEventListener(listener);
                messageListeners.remove(chatId);
            }
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_user_modern, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        User user = users.get(position);
        
        if (user.isGroupChat()) {
            // Handle group chat
            holder.userName.setText("Community Group Chat");
            holder.userImage.setImageResource(R.drawable.ic_group);
            
            // Get last message for group chat
            String groupChatId = "group_" + communityId;
            loadLastMessage(groupChatId, holder);
            holder.itemView.setTag(groupChatId);
        } else {
            // Handle individual chat
            // Display full name for modern look
            String fullName = user.getName();
            holder.userName.setText(fullName != null && !fullName.isEmpty() ? fullName : "User");
            
            // Load user profile image
            if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                CloudinaryHelper.loadImage(context, user.getImageUrl(), holder.userImage);
            } else {
                holder.userImage.setImageResource(R.drawable.profile_placeholder);
            }
            
            // Generate chat ID for individual chat
            String chatId = currentUserId.compareTo(user.getId()) < 0 ? 
                    currentUserId + "_" + user.getId() : 
                    user.getId() + "_" + currentUserId;
            
            // Get last message for individual chat
            loadLastMessage(chatId, holder);
            holder.itemView.setTag(chatId);
        }

        holder.itemView.setOnClickListener(v -> startChat(user));
    }
    
    private void loadLastMessage(String chatId, ChatViewHolder holder) {
        // Remove any existing listener for this chat
        if (messageListeners.containsKey(chatId)) {
            messagesRef.child(chatId).removeEventListener(messageListeners.get(chatId));
        }
        
        // Set default text while loading
        holder.lastMessage.setText("No messages yet");
        holder.timeText.setText("");
        
        // Create and attach new real-time listener for this chat's last message
        Query lastMessageQuery = messagesRef.child(chatId).orderByChild("timestamp").limitToLast(1);
        ValueEventListener messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                        // Get message content
                        String content = messageSnapshot.child("content").getValue(String.class);
                        if (content != null) {
                            holder.lastMessage.setText(content);
                        } else {
                            holder.lastMessage.setText("");
                        }
                        
                        // Get message timestamp
                        Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                        if (timestamp != null) {
                            holder.timeText.setText(timeFormat.format(new Date(timestamp)));
                        } else {
                            holder.timeText.setText("");
                        }
                        
                        // Check if message is read (can be implemented later)
                        holder.unreadIndicator.setVisibility(View.GONE);
                    }
                } else {
                    holder.lastMessage.setText("No messages yet");
                    holder.timeText.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.lastMessage.setText("No messages yet");
                holder.timeText.setText("");
            }
        };
        
        // Store the listener and attach it
        messageListeners.put(chatId, messageListener);
        lastMessageQuery.addValueEventListener(messageListener);
    }

    private void startChat(User user) {
        if (user == null || user.getId() == null || communityId == null || currentUserId == null) {
            return; // Avoid crash if any required data is missing
        }
        
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("Chats");
        String chatId;
        
        if (user.isGroupChat()) {
            // Use community ID as group chat ID
            chatId = "group_" + communityId;
            
            // Create group chat if doesn't exist
            Chat groupChat = new Chat();
            groupChat.setId(chatId);
            groupChat.setName("Community Group Chat");
            groupChat.setGroup(true);
            groupChat.setCommunityId(communityId);
            
            chatsRef.child(chatId).setValue(groupChat);
        } else {
            // Generate chat ID for individual chat
            chatId = currentUserId.compareTo(user.getId()) < 0 ? 
                    currentUserId + "_" + user.getId() : 
                    user.getId() + "_" + currentUserId;
            
            // Create individual chat if doesn't exist
            Chat chat = new Chat();
            chat.setId(chatId);
            List<String> participants = new ArrayList<>();
            participants.add(currentUserId);
            participants.add(user.getId());
            chat.setParticipants(participants);
            chat.setGroup(false);
            chat.setCommunityId(communityId);
            
            chatsRef.child(chatId).setValue(chat);
        }

        // Start chat activity
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("isGroup", user.isGroupChat());
        intent.putExtra("otherUserName", user.getName()); 
        intent.putExtra("otherUserImageUrl", user.getImageUrl());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
    
    // Clean up listeners when adapter is detached
    public void cleanup() {
        for (Map.Entry<String, ValueEventListener> entry : messageListeners.entrySet()) {
            messagesRef.child(entry.getKey()).removeEventListener(entry.getValue());
        }
        messageListeners.clear();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userImage;
        TextView userName;
        TextView lastMessage;
        TextView timeText;
        View unreadIndicator;

        ChatViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.userImage);
            userName = itemView.findViewById(R.id.userName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            timeText = itemView.findViewById(R.id.timeText);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
} 