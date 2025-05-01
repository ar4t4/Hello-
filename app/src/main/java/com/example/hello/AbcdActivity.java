package com.example.hello;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
    private static final String TAG = "AbcdActivity";

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
        
        // First get the list of member IDs in this community
        loadCommunityMembers();
    }

    private void loadCommunityMembers() {
        Log.d(TAG, "Loading members for community: " + communityId);
        
        DatabaseReference membersRef = FirebaseDatabase.getInstance()
                .getReference("Communities")
                .child(communityId)
                .child("members");
                
        membersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> memberIds = new ArrayList<>();
                
                // Get all user IDs that are members of this community
                for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    Boolean isMember = memberSnapshot.getValue(Boolean.class);
                    
                    if (memberId != null && isMember != null && isMember) {
                        memberIds.add(memberId);
                        Log.d(TAG, "Found member ID: " + memberId);
                    }
                }
                
                if (memberIds.isEmpty()) {
                    Log.d(TAG, "No members found in community");
                    memberList.clear();
                    adapter.notifyDataSetChanged();
                } else {
                    // Load user details for these members
                    loadMemberDetails(memberIds);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading community members: " + databaseError.getMessage());
                Toast.makeText(AbcdActivity.this, 
                    "Error loading members: " + databaseError.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadMemberDetails(List<String> memberIds) {
        memberList.clear();
        
        for (String memberId : memberIds) {
            databaseRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                    if (userSnapshot.exists()) {
                        Member member = new Member();
                        member.setUid(userSnapshot.getKey()); // Set the UID
                        
                        // Check if we have firstName/lastName or name
                        if (userSnapshot.hasChild("firstName")) {
                            member.setFirstName(userSnapshot.child("firstName").getValue(String.class));
                        } else if (userSnapshot.hasChild("name")) {
                            // Handle old data format
                            String name = userSnapshot.child("name").getValue(String.class);
                            member.setName(name); // This will split the name into firstName and lastName
                        }
                        
                        if (userSnapshot.hasChild("lastName")) {
                            member.setLastName(userSnapshot.child("lastName").getValue(String.class));
                        }
                        
                        member.setCollege(userSnapshot.child("college").getValue(String.class));
                        member.setBloodGroup(userSnapshot.child("bloodGroup").getValue(String.class));
                        member.setPhone(userSnapshot.child("phone").getValue(String.class));
                        member.setEmail(userSnapshot.child("email").getValue(String.class));
                        member.setSchool(userSnapshot.child("school").getValue(String.class));
                        member.setHome(userSnapshot.child("home").getValue(String.class));
                        member.setDistrict(userSnapshot.child("district").getValue(String.class));
                        
                        // Load university field if available, otherwise it defaults to college
                        if (userSnapshot.hasChild("university")) {
                            member.setUniversity(userSnapshot.child("university").getValue(String.class));
                        } else {
                            member.setUniversity(userSnapshot.child("college").getValue(String.class));
                        }
                        
                        // Load profile image URL
                        member.setProfileImageUrl(userSnapshot.child("profileImageUrl").getValue(String.class));
                        
                        Boolean bloodDonate = userSnapshot.child("bloodDonate").getValue(Boolean.class);
                        member.setBloodDonate(bloodDonate != null && bloodDonate);
                        memberList.add(member);
                        
                        // Notify adapter for each new member
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading member details: " + error.getMessage());
                }
            });
        }
    }

    private void onCallButtonClick(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }
}
