package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class AbcdActivity extends AppCompatActivity {

    private ListView lvCommunityMembers;
    private DatabaseReference communityRef, usersRef;
    private ArrayList<HashMap<String, String>> membersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abcd);

        lvCommunityMembers = findViewById(R.id.lv_community_members);
        membersList = new ArrayList<>();

        String communityId = getIntent().getStringExtra("communityId");

        if (communityId == null) {
            Toast.makeText(this, "Community ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        communityRef = FirebaseDatabase.getInstance().getReference("Communities").child(communityId).child("members");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadCommunityMembers();
    }

    private void loadCommunityMembers() {
        communityRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<HashMap<String, String>> membersList = new ArrayList<>();
                for (DataSnapshot memberSnapshot : task.getResult().getChildren()) {
                    String userId = memberSnapshot.getKey();
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

                    userRef.get().addOnSuccessListener(userDetailsSnapshot -> {
                        HashMap<String, String> memberData = new HashMap<>();
                        memberData.put("name", userDetailsSnapshot.child("name").getValue(String.class));
                        memberData.put("bloodGroup", userDetailsSnapshot.child("bloodGroup").getValue(String.class));
                        memberData.put("college", userDetailsSnapshot.child("college").getValue(String.class));
                        memberData.put("home", userDetailsSnapshot.child("home").getValue(String.class));
                        memberData.put("district", userDetailsSnapshot.child("district").getValue(String.class));
                        memberData.put("university", userDetailsSnapshot.child("university").getValue(String.class));
                        memberData.put("school", userDetailsSnapshot.child("school").getValue(String.class));
                        memberData.put("email", userDetailsSnapshot.child("email").getValue(String.class));

                        membersList.add(memberData);

                        // Refresh ListView after adding data
                        MemberListAdapter adapter = new MemberListAdapter(this, membersList);
                        lvCommunityMembers.setAdapter(adapter);
                    });
                }
            } else {
                Toast.makeText(this, "Failed to load community members.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadUserDetails(String userId) {
        usersRef.child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                HashMap<String, String> userDetails = new HashMap<>();
                userDetails.put("name", snapshot.child("name").getValue(String.class));
                userDetails.put("userId", userId);
                userDetails.put("details", snapshot.getValue().toString()); // Full details
                membersList.add(userDetails);

                updateListView();
            }
        });
    }

    private void updateListView() {
        MemberListAdapter adapter = new MemberListAdapter(this, membersList);
        lvCommunityMembers.setAdapter(adapter);

        lvCommunityMembers.setOnItemClickListener((AdapterView<?> parent, android.view.View view, int position, long id) -> {
            HashMap<String, String> selectedMember = membersList.get(position);
            Intent intent = new Intent(this, UserDetailsActivity.class);
            intent.putExtra("userDetails", selectedMember.get("details"));
            startActivity(intent);
        });
    }
}
