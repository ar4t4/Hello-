package com.example.hello.repositories;

import com.example.hello.models.JoinRequest;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class JoinRequestRepository {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String COLLECTION_REQUESTS = "join_requests";
    
    public JoinRequestRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    public Task<DocumentReference> createJoinRequest(JoinRequest request) {
        // Add the request to Firestore and return the task
        return db.collection(COLLECTION_REQUESTS)
                .add(request.toMap())
                .addOnSuccessListener(documentReference -> {
                    // Update the request with the generated ID
                    String requestId = documentReference.getId();
                    documentReference.update("id", requestId);
                });
    }
    
    public Task<QuerySnapshot> getPendingRequestsForCommunity(String communityId) {
        // Get all pending requests for a specific community
        return db.collection(COLLECTION_REQUESTS)
                .whereEqualTo("communityId", communityId)
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }
    
    public Task<QuerySnapshot> getUserRequests(String userId) {
        // Get all requests made by a specific user
        return db.collection(COLLECTION_REQUESTS)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }
    
    public Task<Void> updateRequestStatus(String requestId, String newStatus) {
        // Update the status of a request (accept or reject)
        return db.collection(COLLECTION_REQUESTS)
                .document(requestId)
                .update("status", newStatus);
    }
    
    public Task<Void> deleteRequest(String requestId) {
        // Delete a request
        return db.collection(COLLECTION_REQUESTS)
                .document(requestId)
                .delete();
    }
} 