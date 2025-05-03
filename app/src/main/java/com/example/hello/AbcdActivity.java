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

public class AbcdActivity extends AppCompatActivity implements MemberAdapter.OnKickMemberListener {
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
        
        // Pass the context (this) as the first parameter and set kick listener
        adapter = new MemberAdapter(this, memberList, this::onCallButtonClick, this);
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
    
    @Override
    public void onKickMember(Member member) {
        if (member == null || member.getUid() == null) {
            Toast.makeText(this, "Invalid member data", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Kicking member: " + member.getUid());
        
        // Remove the member from the community
        DatabaseReference membersRef = FirebaseDatabase.getInstance()
                .getReference("Communities")
                .child(communityId)
                .child("members")
                .child(member.getUid());
        
        membersRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully removed member from community");
                    
                    // Also check and remove from admins if they are an admin
                    FirebaseDatabase.getInstance()
                            .getReference("Communities")
                            .child(communityId)
                            .child("admins")
                            .child(member.getUid())
                            .removeValue();
                    
                    // Clear the saved community ID from the user's preferences if they're using the app
                    // This will force them back to the main screen next time they open the app
                    updateUserPreferences(member.getUid());
                    
                    // Remove from local list and update adapter
                    memberList.remove(member);
                    adapter.notifyDataSetChanged();
                    
                    // Show success message
                    String memberName = (member.getFirstName() != null ? member.getFirstName() : "Member");
                    Toast.makeText(this, memberName + " removed from community", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove member: " + e.getMessage());
                    Toast.makeText(this, "Failed to remove member: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Update user preferences to clear their saved community ID
     * @param userId The ID of the user being kicked
     */
    private void updateUserPreferences(String userId) {
        // Save a notification for the user to see when they open the app
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId);
        
        // Create a notification about being removed
        String communityName = getIntent().getStringExtra("communityName");
        if (communityName == null) {
            communityName = "this community"; // Fallback
        }
        
        // Create notification data
        long timestamp = System.currentTimeMillis();
        DatabaseReference newNotifRef = notificationsRef.push();
        
        newNotifRef.child("title").setValue("Removed from Community");
        newNotifRef.child("message").setValue("You have been removed from " + communityName + 
                ". You will need to send a new join request if you wish to rejoin.");
        newNotifRef.child("timestamp").setValue(timestamp);
        newNotifRef.child("read").setValue(false);
        newNotifRef.child("type").setValue("kicked");
        newNotifRef.child("communityId").setValue(communityId);
        
        Log.d(TAG, "Notification created for kicked user");
        
        // Also clear their saved community ID preference by setting a flag in their user data
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId);
        
        userRef.child("kickedFromCommunity").setValue(communityId);
    }
}
