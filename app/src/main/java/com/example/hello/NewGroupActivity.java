package com.example.hello;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.adapters.UserSelectionAdapter;
import com.example.hello.models.Chat;
import com.example.hello.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class NewGroupActivity extends AppCompatActivity {
    private EditText groupNameInput;
    private RecyclerView recyclerView;
    private UserSelectionAdapter adapter;
    private List<User> selectedUsers;
    private String currentUserId;
    private String communityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_group);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        communityId = getIntent().getStringExtra("communityId");

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create Group");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        groupNameInput = findViewById(R.id.groupNameInput);
        recyclerView = findViewById(R.id.recyclerView);
        Button createButton = findViewById(R.id.btnCreateGroup);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        selectedUsers = new ArrayList<>();
        adapter = new UserSelectionAdapter(this, selectedUsers);
        recyclerView.setAdapter(adapter);

        // Set up create button
        createButton.setOnClickListener(v -> createGroup());
    }

    private void createGroup() {
        String groupName = groupNameInput.getText().toString().trim();
        if (groupName.isEmpty()) {
            groupNameInput.setError("Group name required");
            return;
        }

        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "Select at least one member", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("Chats");
        String chatId = chatsRef.push().getKey();

        if (chatId != null) {
            List<String> participants = new ArrayList<>();
            participants.add(currentUserId);
            for (User user : selectedUsers) {
                participants.add(user.getId());
            }

            Chat chat = new Chat();
            chat.setId(chatId);
            chat.setName(groupName);
            chat.setParticipants(participants);
            chat.setGroup(true);
            chat.setCommunityId(communityId);
            chat.setLastMessageTime(System.currentTimeMillis());

            chatsRef.child(chatId).setValue(chat)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Group created", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to create group: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 