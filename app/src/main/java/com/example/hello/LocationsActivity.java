package com.example.hello;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

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

public class LocationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MemberAdapter memberAdapter;
    private List<Member> memberList;
    private DatabaseReference membersRef;
    private DatabaseReference usersRef;
    private Button stopSharingLocationButton;
    private String communityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locations);

        communityId = getIntent().getStringExtra("communityId");

        recyclerView = findViewById(R.id.recyclerViewMembers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        stopSharingLocationButton = findViewById(R.id.btnStopSharingLocation);

        memberList = new ArrayList<>();
        memberAdapter = new MemberAdapter(this, memberList);
        recyclerView.setAdapter(memberAdapter);

        membersRef = FirebaseDatabase.getInstance().getReference("Communities").child(communityId).child("members");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        stopSharingLocationButton.setOnClickListener(v -> {
            stopSharingLocation();
        });

        fetchMemberData();
    }

    private void fetchMemberData() {
        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberList.clear(); // Clear the list to avoid duplicates
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String userId = memberSnapshot.getKey(); // Get UID of the member
                    fetchUserDetails(userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to load member data", error.toException());
            }
        });
    }

    private void fetchUserDetails(String userId) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Member member = snapshot.getValue(Member.class);
                if (member != null) {
                    memberList.add(member);
                    memberAdapter.notifyDataSetChanged(); // Update the RecyclerView
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to load user data", error.toException());
            }
        });
    }

    private void stopSharingLocation() {
        // Logic to stop location sharing
    }
}
