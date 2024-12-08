package com.example.hello;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class AbcdActivity extends AppCompatActivity {

    private ListView lvCommunityMembers;
    private DatabaseReference communityRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abcd);

        lvCommunityMembers = findViewById(R.id.lv_community_members);

        String communityId = getIntent().getStringExtra("communityId");

        if (communityId == null) {
            Toast.makeText(this, "Community ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        communityRef = FirebaseDatabase.getInstance().getReference("Communities").child(communityId).child("members");

        loadCommunityMembers();
    }

    private void loadCommunityMembers() {
        communityRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<String> members = new ArrayList<>();
                for (DataSnapshot memberSnapshot : task.getResult().getChildren()) {
                    members.add(memberSnapshot.getKey()); // Assuming user ID is stored
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, members);
                lvCommunityMembers.setAdapter(adapter);
            } else {
                Toast.makeText(this, "Failed to load community members.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
