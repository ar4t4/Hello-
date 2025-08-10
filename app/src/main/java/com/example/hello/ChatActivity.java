package com.example.hello;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.adapters.MessageAdapter;
import com.example.hello.models.Message;
import com.example.hello.services.AIAssistantService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> messageList;
    private EditText messageInput;
    private String chatId;
    private String currentUserId;
    private DatabaseReference messagesRef;
    private String otherUserImageUrl;
    private AIAssistantService aiAssistant;
    private boolean isAIChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getStringExtra("chatId");
        boolean isGroup = getIntent().getBooleanExtra("isGroup", false);
        String otherUserName = getIntent().getStringExtra("otherUserName");
        otherUserImageUrl = getIntent().getStringExtra("otherUserImageUrl");
        isAIChat = getIntent().getBooleanExtra("isAIChat", false);

        if (chatId == null) {
            Toast.makeText(this, "Chat could not be loaded", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(chatId);
        
        // Initialize AI Assistant if this is an AI chat
        if (isAIChat) {
            aiAssistant = new AIAssistantService(this, chatId);
        }

        // Set up back button and user name
        ImageView backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }
        
        // Set user name in header
        TextView userNameView = findViewById(R.id.userName);
        if (userNameView != null) {
            userNameView.setText(isAIChat ? "Community Assistant" : 
                (isGroup ? "Community Group Chat" : (otherUserName != null ? otherUserName : "Chat")));
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        ImageButton sendButton = findViewById(R.id.btnSend);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(this, messageList, currentUserId, isGroup);
        recyclerView.setAdapter(adapter);

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Load messages
        loadMessages();
        
        // If this is an AI chat, send welcome message
        if (isAIChat && messageList.isEmpty()) {
            aiAssistant.processMessage("", chatId);
        }
    }

    private void loadMessages() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    try {
                        Message message = messageSnapshot.getValue(Message.class);
                        if (message != null) {
                            messageList.add(message);
                        }
                    } catch (DatabaseException e) {
                        Log.e("ChatActivity", "Error parsing message: " + e.getMessage());
                        // Try to manually parse the message to handle type mismatches
                        try {
                            Message message = parseMessageManually(messageSnapshot);
                            if (message != null) {
                                messageList.add(message);
                            }
                        } catch (Exception ex) {
                            Log.e("ChatActivity", "Failed to manually parse message: " + ex.getMessage());
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChatActivity.this, 
                    "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Toast.makeText(this, "Could not create message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user's name and profile image
        FirebaseDatabase.getInstance().getReference("Users")
            .child(currentUserId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(ChatActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String senderName = "User";
                    
                    // Get sender name using firstName (and possibly lastName)
                    if (snapshot.hasChild("firstName")) {
                        senderName = snapshot.child("firstName").getValue(String.class);
                        if (senderName == null) senderName = "User";
                    }
                    
                    // Get profile image URL
                    String profileImageUrl = null;
                    if (snapshot.hasChild("profileImageUrl")) {
                        profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    }
                    
                    Message message = new Message();
                    message.setId(messageId);
                    message.setSenderId(currentUserId);
                    message.setSenderName(senderName);
                    message.setContent(content);
                    message.setTimestamp(System.currentTimeMillis());
                    message.setChatId(chatId);
                    
                    // Set profile image URL if available
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        message.setSenderProfileImageUrl(profileImageUrl);
                    }

                    messagesRef.child(messageId).setValue(message)
                        .addOnSuccessListener(aVoid -> {
                            messageInput.setText("");
                            recyclerView.scrollToPosition(messageList.size() - 1);
                            
                            // If this is an AI chat, process the message with AI
                            if (isAIChat && aiAssistant != null) {
                                aiAssistant.processMessage(content, chatId);
                            }
                        })
                        .addOnFailureListener(e -> 
                            Toast.makeText(ChatActivity.this, 
                                "Failed to send message", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ChatActivity.this, 
                        "Error sending message", Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up AI Assistant resources if needed
        if (isAIChat && aiAssistant != null) {
            aiAssistant.shutdown();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    // Manual parsing method to handle type mismatches in Firebase data
    private Message parseMessageManually(DataSnapshot messageSnapshot) {
        try {
            Message message = new Message();
            
            // Parse basic string fields
            if (messageSnapshot.child("id").exists()) {
                message.setId(messageSnapshot.child("id").getValue(String.class));
            }
            if (messageSnapshot.child("senderId").exists()) {
                message.setSenderId(messageSnapshot.child("senderId").getValue(String.class));
            }
            if (messageSnapshot.child("senderName").exists()) {
                message.setSenderName(messageSnapshot.child("senderName").getValue(String.class));
            }
            if (messageSnapshot.child("content").exists()) {
                message.setContent(messageSnapshot.child("content").getValue(String.class));
            }
            if (messageSnapshot.child("chatId").exists()) {
                message.setChatId(messageSnapshot.child("chatId").getValue(String.class));
            }
            if (messageSnapshot.child("senderProfileImageUrl").exists()) {
                message.setSenderProfileImageUrl(messageSnapshot.child("senderProfileImageUrl").getValue(String.class));
            }
            
            // Parse timestamp (can be long or string)
            if (messageSnapshot.child("timestamp").exists()) {
                Object timestampObj = messageSnapshot.child("timestamp").getValue();
                if (timestampObj instanceof Long) {
                    message.setTimestamp((Long) timestampObj);
                } else if (timestampObj instanceof String) {
                    try {
                        message.setTimestamp(Long.parseLong((String) timestampObj));
                    } catch (NumberFormatException e) {
                        message.setTimestamp(System.currentTimeMillis());
                    }
                } else {
                    message.setTimestamp(System.currentTimeMillis());
                }
            }
            
            // Parse messageType (can be int or string)
            if (messageSnapshot.child("messageType").exists()) {
                Object messageTypeObj = messageSnapshot.child("messageType").getValue();
                if (messageTypeObj instanceof Integer) {
                    message.setMessageType((Integer) messageTypeObj);
                } else if (messageTypeObj instanceof String) {
                    try {
                        message.setMessageType(Integer.parseInt((String) messageTypeObj));
                    } catch (NumberFormatException e) {
                        message.setMessageType(0); // Default to text
                    }
                } else {
                    message.setMessageType(0); // Default to text
                }
            } else {
                message.setMessageType(0); // Default to text
            }
            
            return message;
        } catch (Exception e) {
            Log.e("ChatActivity", "Error in manual parsing: " + e.getMessage());
            return null;
        }
    }
} 