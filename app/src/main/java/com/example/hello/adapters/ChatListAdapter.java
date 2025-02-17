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
import com.example.hello.models.Chat;
import com.example.hello.models.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private Context context;
    private List<User> users;
    private String currentUserId;
    private String communityId;

    public ChatListAdapter(Context context, List<User> users, String currentUserId, String communityId) {
        this.context = context;
        this.users = users;
        this.currentUserId = currentUserId;
        this.communityId = communityId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_user, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        User user = users.get(position);
        
        if (user.isGroupChat()) {
            holder.userName.setText("Community Group Chat");
            holder.userImage.setImageResource(R.drawable.ic_group);
        } else {
            holder.userName.setText(user.getName());
            // You can load user image here if available
        }

        holder.itemView.setOnClickListener(v -> startChat(user));
    }

    private void startChat(User user) {
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
            chat.setParticipants(new ArrayList<>(List.of(currentUserId, user.getId())));
            chat.setGroup(false);
            chat.setCommunityId(communityId);
            
            chatsRef.child(chatId).setValue(chat);
        }

        // Start chat activity
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("isGroup", user.isGroupChat());
        intent.putExtra("otherUserName", user.getName());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView userImage;
        TextView userName;

        ChatViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.userImage);
            userName = itemView.findViewById(R.id.userName);
        }
    }
} 