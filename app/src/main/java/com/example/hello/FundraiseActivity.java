package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FundraiseActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FundraiseAdapter adapter;
    private List<Fundraise> fundraiseList;
    private String communityId;
    private DatabaseReference databaseRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fundraise);

        // Set up back button
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fundraiseList = new ArrayList<>();

        // Get current user ID before creating adapter
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        adapter = new FundraiseAdapter(this, fundraiseList, currentUserId, new FundraiseAdapter.OnFundraiseActionListener() {
            @Override
            public void onEdit(Fundraise fundraise) {
                Intent intent = new Intent(FundraiseActivity.this, EditFundraiseActivity.class);
                intent.putExtra("fundraiseId", fundraise.getId());
                intent.putExtra("communityId", communityId);
                startActivity(intent);
            }

            @Override
            public void onDelete(Fundraise fundraise) {
                databaseRef.child(fundraise.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> 
                        Toast.makeText(FundraiseActivity.this, 
                            "Fundraiser deleted successfully", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> 
                        Toast.makeText(FundraiseActivity.this, 
                            "Failed to delete fundraiser: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDonate(Fundraise fundraise) {
                // Show donation dialog or start donation activity
                showDonationDialog(fundraise);
            }
        });
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fabCreateFundraise = findViewById(R.id.fabCreateFundraise);
        fabCreateFundraise.setOnClickListener(v -> {
            Intent intent = new Intent(FundraiseActivity.this, CreateFundraiseActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });

        // Get community ID from intent
        communityId = getIntent().getStringExtra("communityId");
        if (communityId == null) {
            Toast.makeText(this, "Community ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference("Fundraisers").child(communityId);
        loadFundraisers();
    }

    private void loadFundraisers() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fundraiseList.clear();
                for (DataSnapshot fundraiseSnapshot : snapshot.getChildren()) {
                    Fundraise fundraise = fundraiseSnapshot.getValue(Fundraise.class);
                    if (fundraise != null) {
                        fundraise.setId(fundraiseSnapshot.getKey());
                        // Make sure creatorId is set when loading from Firebase
                        if (fundraise.getCreatorId() == null) {
                            fundraise.setCreatorId(fundraiseSnapshot.child("creatorId").getValue(String.class));
                        }
                        fundraiseList.add(fundraise);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FundraiseActivity.this, 
                    "Error loading fundraisers: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDonationDialog(Fundraise fundraise) {
        // Implement donation dialog or start donation activity
        // This could show payment options, amount input, etc.
    }
}
