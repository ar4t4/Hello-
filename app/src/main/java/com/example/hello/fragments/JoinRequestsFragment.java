package com.example.hello.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hello.R;
import com.example.hello.adapters.JoinRequestAdapter;
import com.example.hello.models.JoinRequest;
import com.example.hello.repositories.JoinRequestRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class JoinRequestsFragment extends Fragment implements JoinRequestAdapter.OnRequestActionListener {
    
    private RecyclerView recyclerView;
    private JoinRequestAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private JoinRequestRepository repository;
    private String communityId;
    private ListenerRegistration requestListener;
    private static final String TAG = "JoinRequestsFragment";
    
    public static JoinRequestsFragment newInstance(String communityId) {
        JoinRequestsFragment fragment = new JoinRequestsFragment();
        Bundle args = new Bundle();
        args.putString("community_id", communityId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new JoinRequestRepository();
        
        if (getArguments() != null) {
            communityId = getArguments().getString("community_id");
            Log.d(TAG, "Created with communityId: " + communityId);
        } else {
            Log.e(TAG, "ERROR: No communityId provided in arguments");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_requests, container, false);
        
        recyclerView = view.findViewById(R.id.recycler_join_requests);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.tv_empty_requests);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        
        // Set up RecyclerView
        adapter = new JoinRequestAdapter(requireContext(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        
        // Set a fixed height for better visibility
        ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
        params.height = 800; // Set a fixed height in pixels
        recyclerView.setLayoutParams(params);
        
        // Make sure the RecyclerView is visible
        recyclerView.setVisibility(View.VISIBLE);
        
        // Add a background color for better visibility
        recyclerView.setBackgroundColor(android.graphics.Color.WHITE);
        
        // Add dividers between items
        recyclerView.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(), androidx.recyclerview.widget.DividerItemDecoration.VERTICAL));
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadRequests);
        
        Log.d(TAG, "Fragment view created, RecyclerView initialized");
        
        // Debug the view hierarchy
        view.post(() -> {
            Log.d(TAG, "Fragment view width=" + view.getWidth() + ", height=" + view.getHeight());
            Log.d(TAG, "RecyclerView width=" + recyclerView.getWidth() + ", height=" + recyclerView.getHeight());
            Log.d(TAG, "RecyclerView has " + adapter.getItemCount() + " items");
        });
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated - Loading requests");
        
        // Add a test item to see if the adapter works properly
        addTestItemToAdapter();
        
        loadRequests();
        setupRequestsListener();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Refreshing requests");
        loadRequests();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestListener != null) {
            requestListener.remove();
        }
    }
    
    private void loadRequests() {
        if (communityId == null) {
            Log.e(TAG, "Cannot load requests: communityId is null");
            showEmpty();
            return;
        }
        
        Log.d(TAG, "Loading join requests for community: " + communityId);
        showLoading();
        
        // DEBUG: First check if there are ANY join requests in the collection
        FirebaseFirestore.getInstance()
                .collection("join_requests")
                .get()
                .addOnSuccessListener(allRequests -> {
                    Log.d(TAG, "Total requests in Firestore: " + allRequests.size());
                    
                    List<JoinRequest> matchingRequests = new ArrayList<>();
                    
                    for (DocumentSnapshot doc : allRequests) {
                        String docCommunityId = doc.getString("communityId");
                        String status = doc.getString("status");
                        
                        Log.d(TAG, "Request found: ID=" + doc.getId() + 
                              ", communityId=" + docCommunityId + 
                              ", status=" + status +
                              ", userId=" + doc.getString("userId") +
                              ", userName=" + doc.getString("userName"));
                        
                        // Filter manually while index is building
                        if (communityId.equals(docCommunityId) && "pending".equals(status)) {
                            try {
                                JoinRequest request = new JoinRequest();
                                request.setId(doc.getId());
                                request.setUserId(doc.getString("userId"));
                                String userName = doc.getString("userName");
                                // Check if username is empty or null
                                if (userName == null || userName.trim().isEmpty()) {
                                    request.setUserName("Loading..."); // Temporary name while we fetch the real one
                                    fetchAndUpdateUserName(request, doc.getId());
                                } else {
                                    request.setUserName(userName);
                                }
                                request.setUserImageUrl(doc.getString("userImageUrl"));
                                request.setCommunityId(doc.getString("communityId"));
                                request.setCommunityName(doc.getString("communityName"));
                                request.setTimestamp(doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0L);
                                request.setStatus(doc.getString("status"));
                                matchingRequests.add(request);
                                
                                Log.d(TAG, "Request added to list: " + doc.getId() + ", User: " + request.getUserName());
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing request: " + e.getMessage(), e);
                            }
                        }
                    }
                    
                    // Sort requests by timestamp (newest first) as the query would
                    if (!matchingRequests.isEmpty()) {
                        matchingRequests.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    }
                    
                    adapter.setRequests(matchingRequests);
                    hideLoading();
                    
                    if (matchingRequests.isEmpty()) {
                        Log.d(TAG, "No requests found - showing empty state");
                        showEmpty();
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        Log.d(TAG, "Showing " + matchingRequests.size() + " requests");
                        hideEmpty();
                        recyclerView.setVisibility(View.VISIBLE);
                        
                        // Debug each request in the adapter
                        for (int i = 0; i < adapter.getItemCount(); i++) {
                            JoinRequest req = adapter.getRequest(i);
                            Log.d(TAG, "Adapter item " + i + ": ID=" + req.getId() + 
                                  ", userName=" + req.getUserName());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load requests: " + e.getMessage(), e);
                    hideLoading();
                    showEmpty();
                    Toast.makeText(requireContext(), "Failed to load requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void setupRequestsListener() {
        if (communityId == null) {
            return;
        }
        
        Log.d(TAG, "Setting up real-time listener for join requests");
        
        // Listen for real-time updates to join requests
        requestListener = FirebaseFirestore.getInstance()
                .collection("join_requests")
                .whereEqualTo("communityId", communityId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listener error: " + error.getMessage(), error);
                        return;
                    }
                    
                    if (value != null) {
                        Log.d(TAG, "Real-time update received, count: " + value.size());
                        loadRequests();
                    }
                });
    }
    
    @Override
    public void onAcceptRequest(JoinRequest request) {
        Log.d(TAG, "Accepting request: " + request.getId());
        
        // First update the request status to accepted
        repository.updateRequestStatus(request.getId(), "accepted")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Request status updated to accepted");
                    adapter.removeRequest(request);
                    Toast.makeText(requireContext(), "Request accepted", Toast.LENGTH_SHORT).show();
                    
                    // Add the user to the community members in Firebase Realtime Database
                    addUserToCommunity(request.getUserId(), request.getCommunityId());
                    
                    if (adapter.getItemCount() == 0) {
                        showEmpty();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept request: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Failed to accept request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void addUserToCommunity(String userId, String communityId) {
        Log.d(TAG, "Adding user " + userId + " to community " + communityId);
        
        // Get reference to the community's members node in Realtime Database
        DatabaseReference membersRef = FirebaseDatabase.getInstance()
                .getReference("Communities")
                .child(communityId)
                .child("members");
        
        // Add the user to members list with value true
        membersRef.child(userId).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User successfully added to community");
                    Toast.makeText(requireContext(), "User added to community", Toast.LENGTH_SHORT).show();
                    
                    // You can also send a notification to the user here that their request was accepted
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add user to community: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Failed to add user to community: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    @Override
    public void onDeclineRequest(JoinRequest request) {
        Log.d(TAG, "Declining request: " + request.getId());
        
        repository.updateRequestStatus(request.getId(), "rejected")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Request status updated to rejected");
                    adapter.removeRequest(request);
                    Toast.makeText(requireContext(), "Request declined", Toast.LENGTH_SHORT).show();
                    
                    if (adapter.getItemCount() == 0) {
                        showEmpty();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to decline request: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Failed to decline request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }
    
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }
    
    private void showEmpty() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        Log.d(TAG, "Showing empty view, hiding recycler view");
    }
    
    private void hideEmpty() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        Log.d(TAG, "Hiding empty view, showing recycler view");
    }
    
    /**
     * Fetch the user's name from Firestore and update the request with it
     */
    private void fetchAndUpdateUserName(JoinRequest request, String requestId) {
        String userId = request.getUserId();
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Fetching user data for user: " + userId);
        
        // Try to get user data from Firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String userName = "Unknown User";
                    
                    if (userDoc.exists()) {
                        // First try to get firstName + lastName
                        if (userDoc.contains("firstName") && userDoc.contains("lastName")) {
                            String firstName = userDoc.getString("firstName");
                            String lastName = userDoc.getString("lastName");
                            userName = (firstName != null ? firstName : "") + 
                                       ((firstName != null && lastName != null) ? " " : "") + 
                                       (lastName != null ? lastName : "");
                        } 
                        // Then try name field
                        else if (userDoc.contains("name")) {
                            userName = userDoc.getString("name");
                        }
                    }
                    
                    // Use a fallback with user ID if name is still empty
                    if (userName == null || userName.trim().isEmpty()) {
                        userName = "User " + userId.substring(0, Math.min(5, userId.length()));
                    }
                    
                    Log.d(TAG, "Retrieved user name: " + userName + " for request: " + requestId);
                    
                    // Update the request object in our adapter
                    request.setUserName(userName);
                    adapter.notifyDataSetChanged();
                    
                    // Also update the Firestore document to fix it for the future
                    FirebaseFirestore.getInstance()
                            .collection("join_requests")
                            .document(requestId)
                            .update("userName", userName)
                            .addOnSuccessListener(aVoid -> 
                                Log.d(TAG, "Updated username in Firestore for request: " + requestId))
                            .addOnFailureListener(e -> 
                                Log.e(TAG, "Failed to update username in Firestore: " + e.getMessage(), e));
                })
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Failed to fetch user data: " + e.getMessage(), e));
    }
    
    private void addTestItemToAdapter() {
        JoinRequest testRequest = new JoinRequest();
        testRequest.setId("test-id");
        testRequest.setUserId("test-user-id");
        testRequest.setUserName("Test User (Always Visible)");
        testRequest.setCommunityId(communityId);
        testRequest.setCommunityName("Test Community");
        testRequest.setTimestamp(System.currentTimeMillis());
        testRequest.setStatus("pending");
        
        List<JoinRequest> testList = new ArrayList<>();
        testList.add(testRequest);
        adapter.setRequests(testList);
        
        Log.d(TAG, "Added test item to adapter: " + testRequest.getUserName());
    }
} 