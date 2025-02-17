package com.example.hello;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AbcdActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MemberAdapter adapter;
    private List<Member> memberList;
    private String communityId;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abcd);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        memberList = new ArrayList<>();
        
        // Pass the context (this) as the first parameter
        adapter = new MemberAdapter(this, memberList, this::onCallButtonClick);
        recyclerView.setAdapter(adapter);

        // Get community ID from intent
        communityId = getIntent().getStringExtra("communityId");
        if (communityId == null) {
            Toast.makeText(this, "Community ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        loadMembers();
    }

    private void loadMembers() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Member member = new Member();
                    member.setUid(userSnapshot.getKey()); // Set the UID
                    member.setName(userSnapshot.child("name").getValue(String.class));
                    member.setCollege(userSnapshot.child("college").getValue(String.class));
                    member.setBloodGroup(userSnapshot.child("bloodGroup").getValue(String.class));
                    member.setPhone(userSnapshot.child("phone").getValue(String.class));
                    Boolean bloodDonate = userSnapshot.child("bloodDonate").getValue(Boolean.class);
                    member.setBloodDonate(bloodDonate != null && bloodDonate);
                    memberList.add(member);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AbcdActivity.this, 
                    "Error loading members: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCallButtonClick(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }
}
