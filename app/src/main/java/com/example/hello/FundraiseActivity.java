package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FundraiseActivity extends AppCompatActivity {

    private ListView fundraiseListView;
    private Button btnCreateFundraise;
    private List<Fundraiser> fundraiseList;
    private FundraiserAdapter adapter;
    private DatabaseReference fundraiseRef;
    private String communityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fundraise);

        // Get the community ID passed from the previous activity
        communityId = getIntent().getStringExtra("communityId");

        fundraiseListView = findViewById(R.id.listViewFundraisers);
        btnCreateFundraise = findViewById(R.id.btnCreateFundraise);

        fundraiseList = new ArrayList<>();
        adapter = new FundraiserAdapter(this, R.layout.fundraiser_item, fundraiseList);
        fundraiseListView.setAdapter(adapter);

        // Reference to the community-specific fundraisers in Firebase
        fundraiseRef = FirebaseDatabase.getInstance().getReference("Fundraisers").child(communityId);

        // Load the fundraisers from Firebase
        loadFundraisers();

        // Button to create a new fundraiser
        btnCreateFundraise.setOnClickListener(v -> {
            Intent intent = new Intent(FundraiseActivity.this, CreateFundraiseActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });
    }

    private void loadFundraisers() {
        fundraiseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fundraiseList.clear();
                for (DataSnapshot fundraiseSnapshot : snapshot.getChildren()) {
                    Fundraiser fundraiser = fundraiseSnapshot.getValue(Fundraiser.class);
                    if (fundraiser != null) {
                        fundraiser.setFundraiseId(fundraiseSnapshot.getKey());
                        fundraiseList.add(fundraiser);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FundraiseActivity.this, "Failed to load fundraisers.", Toast.LENGTH_SHORT).show();
                Log.e("FundraiseActivity", "Error: " + error.getMessage());
            }
        });
    }
}
