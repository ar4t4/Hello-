package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.adapters.ActiveUserAdapter;
import com.example.hello.adapters.ChatListAdapter;
import com.example.hello.models.Chat;
import com.example.hello.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity implements ActiveUserAdapter.OnActiveUserClickListener {
    private RecyclerView chatRecyclerView;
    private RecyclerView activeUsersRecyclerView;
    private ChatListAdapter chatAdapter;
    private ActiveUserAdapter activeUserAdapter;
    private List<User> userList;
    private List<User> activeUsersList;
    private List<User> filteredUserList;
    private String currentUserId;
    private String communityId;
    private DatabaseReference usersRef;
    private DatabaseReference chatsRef;
    private DatabaseReference messagesRef;
    private EditText searchInput;
    private Map<String, Long> lastMessageTimeMap; // Store last message times
    private ChildEventListener messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        communityId = getIntent().getStringExtra("communityId");
        
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages");

        // Set up back button instead of toolbar
        ImageView backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        // Initialize views
        chatRecyclerView = findViewById(R.id.recyclerView);
        activeUsersRecyclerView = findViewById(R.id.activeUsersRecyclerView);
        searchInput = findViewById(R.id.searchInput);
        
        // Hide the New Chat FAB as requested
        View fabNewChat = findViewById(R.id.fabNewChat);
        if (fabNewChat != null) {
            fabNewChat.setVisibility(View.GONE);
        }

        // Initialize data structures
        userList = new ArrayList<>();
        activeUsersList = new ArrayList<>();
        filteredUserList = new ArrayList<>();
        lastMessageTimeMap = new HashMap<>();

        // Set up RecyclerViews
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatListAdapter(this, filteredUserList, currentUserId, communityId);
        chatRecyclerView.setAdapter(chatAdapter);
        
        LinearLayoutManager horizontalLayout = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        activeUsersRecyclerView.setLayoutManager(horizontalLayout);
        activeUserAdapter = new ActiveUserAdapter(this, activeUsersList, this);
        activeUsersRecyclerView.setAdapter(activeUserAdapter);

        // Set up search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString().toLowerCase().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up community chat card click
        findViewById(R.id.communityChatCard).setOnClickListener(v -> {
            startGroupChat();
        });
        
        // Listen for message updates to keep last messages current
        setupMessageListeners();

        // Load community members
        loadCommunityMembers();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up listeners
        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        
        // Clean up adapters
        if (chatAdapter != null) {
            chatAdapter.cleanup();
        }
    }
    
    private void setupMessageListeners() {
        // Listen for changes in all messages related to this community
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                updateChatLastMessage(snapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                updateChatLastMessage(snapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                // Not needed for our case
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                // Not needed for our case
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        };
        messagesRef.addChildEventListener(messagesListener);
    }
    
    private void updateChatLastMessage(DataSnapshot messageSnapshot) {
        String chatId = messageSnapshot.getKey();
        if (chatId == null) return;
        
        // Get the last message for this chat
        Query lastMessageQuery = messagesRef.child(chatId).orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                        Long timestamp = msgSnapshot.child("timestamp").getValue(Long.class);
                        if (timestamp != null) {
                            // Update the timestamp in our map
                            lastMessageTimeMap.put(chatId, timestamp);
                            
                            // Resort and refresh the list
                            sortAndUpdateChatList();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }
    
    private void sortAndUpdateChatList() {
        // Sort the user list based on last message times
        Collections.sort(userList, new Comparator<User>() {
            @Override
            public int compare(User u1, User u2) {
                String chatId1 = getChatId(u1.getId());
                String chatId2 = getChatId(u2.getId());
                
                Long time1 = lastMessageTimeMap.get(chatId1);
                Long time2 = lastMessageTimeMap.get(chatId2);
                
                // Users with no messages go to the bottom
                if (time1 == null && time2 == null) return 0;
                if (time1 == null) return 1;
                if (time2 == null) return -1;
                
                // Sort by most recent first
                return time2.compareTo(time1);
            }
        });
        
        // Re-filter and update the adapter
        filterUsers(searchInput.getText().toString().toLowerCase().trim());
    }
    
    private String getChatId(String userId) {
        if (userId == null) return "";
        
        if (userId.startsWith("group_")) {
            return userId; // It's already a group chat ID
        } else {
            // Generate individual chat ID based on user IDs
            return currentUserId.compareTo(userId) < 0 ? 
                    currentUserId + "_" + userId : 
                    userId + "_" + currentUserId;
        }
    }

    private void filterUsers(String query) {
        filteredUserList.clear();
        
        if (query.isEmpty()) {
            // Add only individual users (not group chat) to filtered list
            for (User user : userList) {
                if (!user.isGroupChat()) {
                    filteredUserList.add(user);
                }
            }
        } else {
            for (User user : userList) {
                if (!user.isGroupChat() && 
                    (user.getName().toLowerCase().contains(query) || 
                     (user.getEmail() != null && user.getEmail().toLowerCase().contains(query)))) {
                    filteredUserList.add(user);
                }
            }
        }
        
        chatAdapter.notifyDataSetChanged();
    }

    private void startGroupChat() {
        String chatId = "group_" + communityId;
        
        // Create group chat if doesn't exist
        Chat groupChat = new Chat();
        groupChat.setId(chatId);
        groupChat.setName("Community Group Chat");
        groupChat.setGroup(true);
        groupChat.setCommunityId(communityId);
        
        chatsRef.child(chatId).setValue(groupChat);
        
        // Check if face verification is required for this community
        FirebaseDatabase.getInstance().getReference("Communities")
            .child(communityId)
            .child("requiresFaceVerification")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean requiresVerification = true; // Default to true for security
                    
                    if (dataSnapshot.exists()) {
                        requiresVerification = dataSnapshot.getValue(Boolean.class);
                    }
                    
                    if (requiresVerification) {
                        // Start face verification activity first, which will then launch the chat
                        Intent intent = new Intent(ChatListActivity.this, FaceVerificationActivity.class);
                        intent.putExtra("chatId", chatId);
                        intent.putExtra("isGroup", true);
                        intent.putExtra("otherUserName", "Community Group Chat");
                        startActivity(intent);
                    } else {
                        // Skip face verification and go directly to chat
                        Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                        intent.putExtra("chatId", chatId);
                        intent.putExtra("isGroup", true);
                        intent.putExtra("otherUserName", "Community Group Chat");
                        startActivity(intent);
                    }
                }
                
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // If there's an error, default to requiring verification for security
                    Log.e("ChatListActivity", "Error checking face verification setting: " + databaseError.getMessage());
                    
                    // Start face verification as fallback
                    Intent intent = new Intent(ChatListActivity.this, FaceVerificationActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("isGroup", true);
                    intent.putExtra("otherUserName", "Community Group Chat");
                    startActivity(intent);
                }
            });
    }

    private void loadCommunityMembers() {
        DatabaseReference communityMembersRef = FirebaseDatabase.getInstance()
            .getReference("Communities")
            .child(communityId)
            .child("members");

        communityMembersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot membersSnapshot) {
                userList.clear();
                activeUsersList.clear();
                
                // Add a special "group chat" user to the main list
                // But we'll filter it out from the displayed list
                User groupChatUser = new User();
                groupChatUser.setId("group_" + communityId);
                groupChatUser.setName("Community Group Chat");
                groupChatUser.setIsGroupChat(true);
                userList.add(groupChatUser);

                // Load individual members
                for (DataSnapshot memberSnapshot : membersSnapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    if (memberId != null && !memberId.equals(currentUserId)) {
                        usersRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot userSnapshot) {
                                if (userSnapshot.exists()) {
                                    User user = new User();
                                    user.setId(memberId);
                                    
                                    // Handle firstName and lastName fields
                                    if (userSnapshot.hasChild("firstName")) {
                                        String firstName = userSnapshot.child("firstName").getValue(String.class);
                                        user.setFirstName(firstName);
                                        
                                        if (userSnapshot.hasChild("lastName")) {
                                            String lastName = userSnapshot.child("lastName").getValue(String.class);
                                            user.setLastName(lastName);
                                        }
                                    } else {
                                        // Default name if no firstName exists
                                        user.setFirstName("User");
                                    }
                                    
                                    // Set profile image URL if available
                                    if (userSnapshot.hasChild("profileImageUrl")) {
                                        String profileUrl = userSnapshot.child("profileImageUrl").getValue(String.class);
                                        if (profileUrl != null && !profileUrl.isEmpty()) {
                                            user.setImageUrl(profileUrl);
                                        }
                                    }
                                    
                                    // Check if user is sharing location or marked as active
                                    boolean isActive = false;
                                    if (userSnapshot.hasChild("sharingLocation")) {
                                        isActive = userSnapshot.child("sharingLocation").getValue(Boolean.class) == Boolean.TRUE;
                                    }
                                    
                                    userList.add(user);
                                    
                                    // Add to active users if they're sharing location
                                    if (isActive) {
                                        activeUsersList.add(user);
                                        activeUserAdapter.notifyDataSetChanged();
                                    }
                                    
                                    // Get last message time for sorting
                                    String chatId = getChatId(memberId);
                                    getLastMessageTime(chatId);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(ChatListActivity.this, 
                                    "Error loading users", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChatListActivity.this, 
                    "Error loading members", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void getLastMessageTime(String chatId) {
        if (chatId == null || chatId.isEmpty()) return;
        
        Query lastMessageQuery = messagesRef.child(chatId).orderByChild("timestamp").limitToLast(1);
        lastMessageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                        Long timestamp = msgSnapshot.child("timestamp").getValue(Long.class);
                        if (timestamp != null) {
                            lastMessageTimeMap.put(chatId, timestamp);
                        }
                    }
                    
                    // After getting last message time, sort the list
                    sortAndUpdateChatList();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }

    @Override
    public void onActiveUserClick(User user) {
        // Generate chat ID for individual chat
        String chatId = currentUserId.compareTo(user.getId()) < 0 ? 
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
        
        // Start chat activity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("isGroup", false);
        intent.putExtra("otherUserName", user.getName());
        intent.putExtra("otherUserImageUrl", user.getImageUrl());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 