package com.example.hello;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.adapters.ChatListAdapter;
import com.example.hello.models.Chat;
import com.example.hello.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<User> userList;
    private String currentUserId;
    private String communityId;
    private DatabaseReference usersRef;
    private DatabaseReference chatsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        communityId = getIntent().getStringExtra("communityId");
        
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Community Chats");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        adapter = new ChatListAdapter(this, userList, currentUserId, communityId);
        recyclerView.setAdapter(adapter);

        // Load community members
        loadCommunityMembers();
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
                
                // Add a special "group chat" user at the top
                User groupChatUser = new User();
                groupChatUser.setId("group_" + communityId);
                groupChatUser.setName("Community Group Chat");
                groupChatUser.setIsGroupChat(true);
                userList.add(groupChatUser);

                // Load individual members
                for (DataSnapshot memberSnapshot : membersSnapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    if (!memberId.equals(currentUserId)) {
                        usersRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot userSnapshot) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
                                    user.setId(memberId);
                                    userList.add(user);
                                    adapter.notifyDataSetChanged();
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 