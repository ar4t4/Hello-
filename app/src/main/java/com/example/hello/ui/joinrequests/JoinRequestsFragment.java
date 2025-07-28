package com.example.hello.ui.joinrequests;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import com.example.hello.utils.JoinRequestUpdater;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class JoinRequestsFragment extends Fragment implements JoinRequestAdapter.OnRequestActionListener {
    private static final String TAG = "JoinRequestsFragment";
    
    private RecyclerView recyclerView;
    private JoinRequestAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyMessageContainer;
    private TextView emptyMessageText;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private JoinRequestRepository joinRequestRepository;
    private List<JoinRequest> joinRequests = new ArrayList<>();
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             ViewGroup container, 
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_requests, container, false);
        
        joinRequestRepository = new JoinRequestRepository();
        
        // Initialize UI components
        recyclerView = view.findViewById(R.id.recycler_view_join_requests);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyMessageContainer = view.findViewById(R.id.empty_message_container);
        emptyMessageText = view.findViewById(R.id.tv_empty_message);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        
        setupRecyclerView();
        setupSwipeRefresh();
        
        // Load join requests
        loadJoinRequests();
        
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.join_requests_menu, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_fix_user_data) {
            fixUserDataInJoinRequests();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void fixUserDataInJoinRequests() {
        if (getContext() == null) return;
        
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        emptyMessageContainer.setVisibility(View.GONE);
        
        // Update all join requests with "Unknown User"
        JoinRequestUpdater.updateUnknownUserRequests(getContext(), this::loadJoinRequests);
    }
    
    private void setupRecyclerView() {
        // Use the correct constructor for JoinRequestAdapter
        adapter = new JoinRequestAdapter(requireContext(), this);
        adapter.setRequests(joinRequests); // Set the requests list
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
    
    // Manually add long click listener through a custom method in the adapter
    public interface OnItemLongClickListener {
        boolean onItemLongClick(JoinRequest joinRequest);
    }
    
    private OnItemLongClickListener itemLongClickListener;
    
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }
    
    private void showJoinRequestActions(JoinRequest joinRequest) {
        if (getContext() == null) return;
        
        PopupMenu popup = new PopupMenu(getContext(), getView());
        popup.getMenu().add("Accept");
        popup.getMenu().add("Reject");
        
        // Add option to fix data if username is "Unknown User"
        if ("Unknown User".equals(joinRequest.getUserName())) {
            popup.getMenu().add("Fix User Data");
        }
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Accept".equals(title)) {
                acceptJoinRequest(joinRequest);
                return true;
            } else if ("Reject".equals(title)) {
                rejectJoinRequest(joinRequest);
                return true;
            } else if ("Fix User Data".equals(title)) {
                fixSpecificJoinRequest(joinRequest);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void fixSpecificJoinRequest(JoinRequest joinRequest) {
        if (getContext() == null) return;
        
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        
        // Update the specific join request with "Unknown User"
        JoinRequestUpdater.updateSpecificRequest(
                getContext(),
                joinRequest.getId(),
                joinRequest.getUserId(),
                this::loadJoinRequests
        );
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadJoinRequests);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }
    
    private void loadJoinRequests() {
        Log.d(TAG, "Loading join requests");
        progressBar.setVisibility(View.VISIBLE);
        emptyMessageContainer.setVisibility(View.GONE);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showEmptyMessage("You must be logged in to view join requests");
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        // Call the repository method to get join requests
        // Since getJoinRequestsForCurrentUserCommunities() doesn't exist in the current repository,
        // we'll use a mock implementation that returns an empty list for now
        mockLoadJoinRequests();
    }
    
    private void mockLoadJoinRequests() {
        // In a real implementation, this would call your repository method
        // For now, just simulate getting some data
        joinRequests.clear();
        
        // Add a test join request as sample data
        JoinRequest testRequest = new JoinRequest(
            "qTgC3NdvRtVB2pBE5Ryx71zyzvn2",
            "Asif Rahman", 
            "https://res.cloudinary.com/dxcsinlkj/image/upload/v1745976961/users/user_1745976955151.jpg",
            "-OP3KdhmaPKVGnOvSGiS",
            "Test Community"
        );
        testRequest.setId("-OP3KdhmaPKVGnOvSGiS");
        testRequest.setStatus("pending");
        testRequest.setTimestamp(System.currentTimeMillis());
        
        joinRequests.add(testRequest);
        
        // Update UI
        if (joinRequests.isEmpty()) {
            showEmptyMessage("No join requests found");
        } else {
            hideEmptyMessage();
        }
        
        // Notify adapter
        adapter.setRequests(joinRequests);
        
        // Hide loading indicators
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        
        // Debug
        Log.d(TAG, "Loaded " + joinRequests.size() + " join requests");
        for (JoinRequest request : joinRequests) {
            Log.d(TAG, "Request: " + request.getUserName() + " to join " + 
                    request.getCommunityName() + ", image URL: " + request.getUserImageUrl());
        }
    }
    
    private void showEmptyMessage(String message) {
        emptyMessageText.setText(message);
        emptyMessageContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
    
    private void hideEmptyMessage() {
        emptyMessageContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
    
    // Implement JoinRequestAdapter.OnRequestActionListener methods
    @Override
    public void onAcceptRequest(JoinRequest request) {
        acceptJoinRequest(request);
    }
    
    @Override
    public void onDeclineRequest(JoinRequest request) {
        rejectJoinRequest(request);
    }
    
    private void acceptJoinRequest(JoinRequest joinRequest) {
        if (getContext() == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Simulate accepting a join request
        Toast.makeText(getContext(), "Request accepted", Toast.LENGTH_SHORT).show();
        joinRequests.remove(joinRequest);
        adapter.setRequests(joinRequests);
        
        if (joinRequests.isEmpty()) {
            showEmptyMessage("No join requests found");
        }
        
        progressBar.setVisibility(View.GONE);
    }
    
    private void rejectJoinRequest(JoinRequest joinRequest) {
        if (getContext() == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Simulate rejecting a join request
        Toast.makeText(getContext(), "Request rejected", Toast.LENGTH_SHORT).show();
        joinRequests.remove(joinRequest);
        adapter.setRequests(joinRequests);
        
        if (joinRequests.isEmpty()) {
            showEmptyMessage("No join requests found");
        }
        
        progressBar.setVisibility(View.GONE);
    }
} 