package com.example.hello.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hello.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class CommunityRequestsButtonFragment extends Fragment {
    
    private String communityId;
    private Button viewRequestsButton;
    private TextView requestCountText;
    
    public static CommunityRequestsButtonFragment newInstance(String communityId) {
        CommunityRequestsButtonFragment fragment = new CommunityRequestsButtonFragment();
        Bundle args = new Bundle();
        args.putString("community_id", communityId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            communityId = getArguments().getString("community_id");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_requests_button, container, false);
        
        viewRequestsButton = view.findViewById(R.id.btn_view_requests);
        requestCountText = view.findViewById(R.id.tv_request_count);
        
        // Set up the button click listener
        viewRequestsButton.setOnClickListener(v -> {
            if (communityId != null) {
                // Navigate to the join requests fragment
                JoinRequestsFragment requestsFragment = JoinRequestsFragment.newInstance(communityId);
                
                // Use the fragment_container in activity_community_detail.xml
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, requestsFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateRequestCount();
    }
    
    private void updateRequestCount() {
        if (communityId == null) {
            return;
        }
        
        // Get the number of pending requests for this community
        FirebaseFirestore.getInstance()
                .collection("join_requests")
                .whereEqualTo("communityId", communityId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    
                    if (count > 0) {
                        requestCountText.setVisibility(View.VISIBLE);
                        requestCountText.setText(String.valueOf(count));
                        viewRequestsButton.setText("View Join Requests (" + count + ")");
                    } else {
                        requestCountText.setVisibility(View.GONE);
                        viewRequestsButton.setText("View Join Requests");
                    }
                });
    }
} 