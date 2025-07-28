package com.example.hello;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hello.adapters.JoinRequestAdapter;
import com.example.hello.models.JoinRequest;
import com.example.hello.repositories.JoinRequestRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class JoinRequestsActivity extends AppCompatActivity implements JoinRequestAdapter.OnRequestActionListener {
    
    private static final String TAG = "JoinRequestsActivity";
    private RecyclerView recyclerView;
    private JoinRequestAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialToolbar toolbar;
    
    private JoinRequestRepository repository;
    private String communityId;
    private ListenerRegistration requestListener;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_requests);
        
        // Get communityId from intent
        communityId = getIntent().getStringExtra("communityId");
        if (communityId == null) {
            Toast.makeText(this, "Community ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Log.d(TAG, "Activity created with communityId: " + communityId);
        
        // Initialize repository
        repository = new JoinRequestRepository();
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_join_requests);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.tv_empty_requests);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        
        // Set up toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Join Requests");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // Set up RecyclerView
        adapter = new JoinRequestAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        
        // Add dividers between items
        recyclerView.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL));
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadRequests);
        
        // Look up the specific user ID that's causing problems
        lookupSpecificUser("qTgC3NdvRtVB2pBE5Ryx71zyzvn2");
        
        // Log database structure to find user collections
        scanDatabaseStructure();
        
        // Add test item
        addTestItemToAdapter();
        
        // Load requests
        loadRequests();
        setupRequestsListener();
    }
    
    /**
     * Comprehensive lookup of a specific user ID across all possible locations
     */
    private void lookupSpecificUser(String userId) {
        Log.d(TAG, "LOOKUP: Starting comprehensive search for user ID: " + userId);
        
        // Firebase Realtime Database paths to check
        String[] realtimePaths = {
            "users/" + userId,
            "Users/" + userId,
            "userProfiles/" + userId,
            "members/" + userId,
            "profiles/" + userId,
            "Profiles/" + userId,
            "Communities/" + communityId + "/members/" + userId
        };
        
        // Check each Realtime Database path
        for (String path : realtimePaths) {
            FirebaseDatabase.getInstance().getReference(path)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Log.d(TAG, "LOOKUP: Found user data at Realtime DB path: " + path);
                        Log.d(TAG, "LOOKUP: Data: " + snapshot.getValue());
                        
                        // Log all children for detailed inspection
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            Log.d(TAG, "LOOKUP: Child key: " + child.getKey() + ", value: " + child.getValue());
                        }
                    } else {
                        Log.d(TAG, "LOOKUP: No data at Realtime DB path: " + path);
                    }
                })
                .addOnFailureListener(e -> 
                    Log.e(TAG, "LOOKUP: Error checking Realtime DB path " + path + ": " + e.getMessage()));
        }
        
        // Firestore collections to check
        String[] firestoreCollections = {
            "users", "Users", "userProfiles", "members", "profiles", "Profiles", "UserProfiles"
        };
        
        // Check each Firestore collection
        for (String collection : firestoreCollections) {
            FirebaseFirestore.getInstance().collection(collection)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Log.d(TAG, "LOOKUP: Found user in Firestore collection: " + collection);
                        Log.d(TAG, "LOOKUP: Document data: " + doc.getData());
                    } else {
                        Log.d(TAG, "LOOKUP: No document in Firestore collection: " + collection);
                    }
                })
                .addOnFailureListener(e -> 
                    Log.e(TAG, "LOOKUP: Error checking Firestore collection " + collection + ": " + e.getMessage()));
        }
        
        // Also check if there are any documents in any collection referencing this userId
        FirebaseFirestore.getInstance().collectionGroup("join_requests")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    Log.d(TAG, "LOOKUP: Found join requests for user: " + querySnapshot.size());
                    for (DocumentSnapshot doc : querySnapshot) {
                        Log.d(TAG, "LOOKUP: Join request ID: " + doc.getId());
                        Log.d(TAG, "LOOKUP: Join request data: " + doc.getData());
                    }
                } else {
                    Log.d(TAG, "LOOKUP: No join requests found for user");
                }
            })
            .addOnFailureListener(e -> 
                Log.e(TAG, "LOOKUP: Error searching join requests: " + e.getMessage()));
    }
    
    private void scanDatabaseStructure() {
        // Common collection names where user data might be stored
        String[] possibleCollections = {
            "users", "Users", "user_profiles", "UserProfiles", "members", "Members", "profiles", "Profiles"
        };
        
        for (String collection : possibleCollections) {
            FirebaseFirestore.getInstance()
                .collection(collection)
                .limit(3) // Just check a few docs to see schema
                .get()
                .addOnSuccessListener(docs -> {
                    if (!docs.isEmpty()) {
                        Log.d(TAG, "Found collection: " + collection + " with " + docs.size() + " documents");
                        // Check the first document's fields
                        DocumentSnapshot firstDoc = docs.getDocuments().get(0);
                        Log.d(TAG, "Sample document ID: " + firstDoc.getId());
                        Log.d(TAG, "Sample document fields: " + firstDoc.getData());
                    }
                })
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Error scanning collection " + collection + ": " + e.getMessage()));
        }
    }
    
    private void addTestItemToAdapter() {
        JoinRequest testRequest = new JoinRequest();
        testRequest.setId("NDUii9DRo3d0I7hYfWic"); // Use a real request ID if possible
        testRequest.setUserId("qTgC3NdvRtVB2pBE5Ryx71zyzvn2"); // Real user ID
        testRequest.setUserName("Unknown User");
        testRequest.setCommunityId(communityId);
        testRequest.setCommunityName("Test Community");
        testRequest.setTimestamp(System.currentTimeMillis());
        testRequest.setStatus("pending");
        
        // Log debug info about this specific join request and user ID
        Log.d(TAG, "DEBUG: Will look for user with ID: " + testRequest.getUserId());
        Log.d(TAG, "DEBUG: Join request ID: " + testRequest.getId());
        
        // Check if we have data for this user in Realtime Database
        FirebaseDatabase.getInstance().getReference("users")
            .child(testRequest.getUserId())
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    Log.d(TAG, "DEBUG: User data exists in Realtime DB");
                    Log.d(TAG, "DEBUG: User data: " + snapshot.getValue());
                } else {
                    Log.d(TAG, "DEBUG: User data NOT found in Realtime DB");
                }
            });
        
        // Also check Firestore
        FirebaseFirestore.getInstance().collection("users")
            .document(testRequest.getUserId())
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Log.d(TAG, "DEBUG: User data exists in Firestore");
                    Log.d(TAG, "DEBUG: User data: " + doc.getData());
                } else {
                    Log.d(TAG, "DEBUG: User data NOT found in Firestore");
                }
            });
        
        List<JoinRequest> testList = new ArrayList<>();
        testList.add(testRequest);
        adapter.setRequests(testList);
        
        Log.d(TAG, "Added test item to adapter: " + testRequest.getUserName());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        
        // First load any join requests that we know about
        if (getIntent().hasExtra("requestId")) {
            String specificRequestId = getIntent().getStringExtra("requestId");
            Log.d(TAG, "Looking for specific request ID: " + specificRequestId);
            
            // Load this specific request directly
            FirebaseFirestore.getInstance()
                .collection("join_requests")
                .document(specificRequestId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Log.d(TAG, "Found specific request: " + doc.getId());
                        Log.d(TAG, "Request data: " + doc.getData());
                    } else {
                        Log.d(TAG, "Specific request not found: " + specificRequestId);
                    }
                })
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Error finding specific request: " + e.getMessage()));
        }
        
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
                                if (userName == null || userName.trim().isEmpty() || "Unknown User".equals(userName)) {
                                    request.setUserName("Loading...");
                                    // We don't need to call fetchAndUpdateUserName anymore as the adapter will handle this
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
                    
                    // Sort requests by timestamp (newest first)
                    if (!matchingRequests.isEmpty()) {
                        matchingRequests.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    }
                    
                    adapter.setRequests(matchingRequests);
                    hideLoading();
                    
                    if (matchingRequests.isEmpty()) {
                        Log.d(TAG, "No requests found - showing empty state");
                        showEmpty();
                    } else {
                        Log.d(TAG, "Showing " + matchingRequests.size() + " requests");
                        hideEmpty();
                        
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
                    Toast.makeText(this, "Failed to load requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    
    private void fetchAndUpdateUserName(JoinRequest request, String requestId) {
        String userId = request.getUserId();
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Fetching user data for user: " + userId);
        
        // Show debug toast
        Toast.makeText(this, "Looking up user: " + userId, Toast.LENGTH_SHORT).show();
        
        // Comprehensive user data lookup that tries multiple collections and data sources
        findUserInMultipleCollections(userId, requestId, request);
    }
    
    private void findUserInMultipleCollections(String userId, String requestId, JoinRequest request) {
        // Try all these collections for user data
        String[] collectionsToCheck = {
            "users", "Users", "user_profiles", "UserProfiles", "members", "Members", "profiles", "Profiles"
        };
        
        // Check each collection in sequence
        checkNextCollection(collectionsToCheck, 0, userId, requestId, request);
    }
    
    private void checkNextCollection(String[] collections, int index, String userId, 
                                    String requestId, JoinRequest request) {
        // If we've checked all collections, try the Realtime Database
        if (index >= collections.length) {
            Log.d(TAG, "Tried all Firestore collections, checking Realtime Database");
            Toast.makeText(this, "Trying Firebase Realtime DB", Toast.LENGTH_SHORT).show();
            checkRealtimeDatabase(userId, requestId, request);
            return;
        }
        
        String collection = collections[index];
        Log.d(TAG, "Checking collection: " + collection + " for user: " + userId);
        
        // Try this collection
        FirebaseFirestore.getInstance()
            .collection(collection)
            .document(userId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Log.d(TAG, "Found user in collection: " + collection);
                    Toast.makeText(JoinRequestsActivity.this, 
                            "Found user in " + collection, Toast.LENGTH_SHORT).show();
                    
                    // Extract user info
                    extractUserInfo(doc, userId, requestId, request);
                } else {
                    // Try the next collection
                    checkNextCollection(collections, index + 1, userId, requestId, request);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking collection " + collection + ": " + e.getMessage());
                // Try the next collection
                checkNextCollection(collections, index + 1, userId, requestId, request);
            });
    }
    
    private void extractUserInfo(DocumentSnapshot doc, String userId, String requestId, JoinRequest request) {
        // Log all fields for debugging
        Log.d(TAG, "User document data: " + doc.getData());
        
        String userName = null;
        String photoUrl = null;
        
        // Try all these field combinations for the name
        String[][] nameFields = {
            {"firstName", "lastName"}, 
            {"first_name", "last_name"},
            {"name"}, 
            {"displayName"}, 
            {"display_name"},
            {"username"},
            {"full_name"},
            {"email"}
        };
        
        // Try each name field combination
        for (String[] fieldCombo : nameFields) {
            if (fieldCombo.length == 2) {
                // First name + last name fields
                String first = doc.getString(fieldCombo[0]);
                String last = doc.getString(fieldCombo[1]);
                
                if (first != null || last != null) {
                    userName = (first != null ? first : "") + 
                              ((first != null && last != null) ? " " : "") + 
                              (last != null ? last : "");
                    Log.d(TAG, "Using " + fieldCombo[0] + "+" + fieldCombo[1] + ": " + userName);
                    break;
                }
            } else {
                // Single field for name
                String name = doc.getString(fieldCombo[0]);
                if (name != null && !name.isEmpty()) {
                    userName = name;
                    Log.d(TAG, "Using " + fieldCombo[0] + ": " + userName);
                    break;
                }
            }
        }
        
        // Try all these field names for the photo
        String[] photoFields = {
            "imageUrl", "image_url", "photoUrl", "photo_url", "avatarUrl", "avatar_url", 
            "profileImage", "profile_image", "picture", "profilePicture", "profile_picture"
        };
        
        // Try each photo field
        for (String field : photoFields) {
            String url = doc.getString(field);
            if (url != null && !url.isEmpty()) {
                photoUrl = url;
                Log.d(TAG, "Using " + field + " for photo: " + photoUrl);
                break;
            }
        }
        
        // Fallback for name if still null
        if (userName == null || userName.isEmpty()) {
            // Try to get email and use part before @
            String email = doc.getString("email");
            if (email != null && !email.isEmpty() && email.contains("@")) {
                userName = email.split("@")[0];
                Log.d(TAG, "Using email-derived name: " + userName);
            } else {
                userName = "User " + userId.substring(0, Math.min(5, userId.length()));
                Log.d(TAG, "Using fallback name: " + userName);
            }
        }
        
        Toast.makeText(this, "Found name: " + userName, Toast.LENGTH_SHORT).show();
        
        // Update request with the user info we found
        updateRequestWithUserInfo(request, requestId, userName, photoUrl);
    }
    
    private void checkRealtimeDatabase(String userId, String requestId, JoinRequest request) {
        // Paths to check in Realtime Database
        String[] pathsToCheck = {
            "users/" + userId,
            "Users/" + userId,
            "userProfiles/" + userId,
            "profiles/" + userId,
            "members/" + userId
        };
        
        // Try the first path
        checkNextRealtimePath(pathsToCheck, 0, userId, requestId, request);
    }
    
    private void checkNextRealtimePath(String[] paths, int index, String userId, 
                                      String requestId, JoinRequest request) {
        if (index >= paths.length) {
            // We've tried all paths, use fallback
            Log.d(TAG, "No user data found anywhere, using fallback name");
            String fallbackName = "User " + userId.substring(0, Math.min(5, userId.length()));
            updateRequestWithUserInfo(request, requestId, fallbackName, null);
            return;
        }
        
        String path = paths[index];
        Log.d(TAG, "Checking Realtime Database path: " + path);
        
        FirebaseDatabase.getInstance().getReference(path)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    Log.d(TAG, "Found user data at path: " + path);
                    Log.d(TAG, "Data: " + snapshot.getValue());
                    
                    String name = extractRealtimeUserName(snapshot);
                    String photoUrl = extractRealtimePhotoUrl(snapshot);
                    
                    if (name != null) {
                        updateRequestWithUserInfo(request, requestId, name, photoUrl);
                    } else {
                        // Try next path
                        checkNextRealtimePath(paths, index + 1, userId, requestId, request);
                    }
                } else {
                    // Try next path
                    checkNextRealtimePath(paths, index + 1, userId, requestId, request);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking Realtime Database path " + path + ": " + e.getMessage());
                // Try next path
                checkNextRealtimePath(paths, index + 1, userId, requestId, request);
            });
    }
    
    private String extractRealtimeUserName(com.google.firebase.database.DataSnapshot snapshot) {
        // Try common field names for name in Realtime Database
        String[] nameFields = {"name", "displayName", "display_name", "fullName", "username", "firstName"};
        
        for (String field : nameFields) {
            if (snapshot.hasChild(field)) {
                String name = snapshot.child(field).getValue(String.class);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        }
        
        // Check if firstName + lastName exists
        if (snapshot.hasChild("firstName") && snapshot.hasChild("lastName")) {
            String firstName = snapshot.child("firstName").getValue(String.class);
            String lastName = snapshot.child("lastName").getValue(String.class);
            
            return (firstName != null ? firstName : "") + 
                   ((firstName != null && lastName != null) ? " " : "") + 
                   (lastName != null ? lastName : "");
        }
        
        // Check for email
        if (snapshot.hasChild("email")) {
            String email = snapshot.child("email").getValue(String.class);
            if (email != null && email.contains("@")) {
                return email.split("@")[0];
            }
        }
        
        return null;
    }
    
    private String extractRealtimePhotoUrl(com.google.firebase.database.DataSnapshot snapshot) {
        // Try common field names for photo URL in Realtime Database
        String[] photoFields = {
            "photoUrl", "profileImage", "avatarUrl", "imageUrl", "photo_url", "profile_image"
        };
        
        for (String field : photoFields) {
            if (snapshot.hasChild(field)) {
                String photoUrl = snapshot.child(field).getValue(String.class);
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    return photoUrl;
                }
            }
        }
        
        return null;
    }
    
    private void updateRequestWithUserInfo(JoinRequest request, String requestId, 
                                         String userName, String photoUrl) {
        Log.d(TAG, "Updating request with name: " + userName + ", photoUrl: " + photoUrl);
        
        // Update the request object in our adapter
        request.setUserName(userName);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            request.setUserImageUrl(photoUrl);
        }
        
        // Update the UI
        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            Log.d(TAG, "Adapter notified of data change");
            Toast.makeText(this, "Updated user: " + userName, Toast.LENGTH_SHORT).show();
        });
        
        // Update the Firestore document
        FirebaseFirestore.getInstance()
            .collection("join_requests")
            .document(requestId)
            .update("userName", userName,
                   "userImageUrl", photoUrl != null ? photoUrl : "")
            .addOnSuccessListener(aVoid -> 
                Log.d(TAG, "Updated username in Firestore for request: " + requestId))
            .addOnFailureListener(e -> 
                Log.e(TAG, "Failed to update username in Firestore: " + e.getMessage(), e));
    }
    
    @Override
    public void onAcceptRequest(JoinRequest request) {
        Log.d(TAG, "Accepting request: " + request.getId());
        
        // First update the request status to accepted
        repository.updateRequestStatus(request.getId(), "accepted")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Request status updated to accepted");
                    adapter.removeRequest(request);
                    Toast.makeText(this, "Request accepted", Toast.LENGTH_SHORT).show();
                    
                    // Add the user to the community members in Firebase Realtime Database
                    addUserToCommunity(request.getUserId(), request.getCommunityId());
                    
                    if (adapter.getItemCount() == 0) {
                        showEmpty();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept request: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to accept request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "User added to community", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add user to community: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to add user to community: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    @Override
    public void onDeclineRequest(JoinRequest request) {
        Log.d(TAG, "Declining request: " + request.getId());
        
        repository.updateRequestStatus(request.getId(), "rejected")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Request status updated to rejected");
                    adapter.removeRequest(request);
                    Toast.makeText(this, "Request declined", Toast.LENGTH_SHORT).show();
                    
                    if (adapter.getItemCount() == 0) {
                        showEmpty();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to decline request: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to decline request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
} 