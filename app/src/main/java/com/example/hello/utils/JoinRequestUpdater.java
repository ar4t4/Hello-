package com.example.hello.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to update existing join requests that have "Unknown User" as userName
 */
public class JoinRequestUpdater {
    private static final String TAG = "JoinRequestUpdater";
    
    /**
     * Update all join requests with "Unknown User" as userName
     * 
     * @param context The context
     * @param callback Callback for when all requests are updated
     */
    public static void updateUnknownUserRequests(Context context, Runnable callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Find all join requests with "Unknown User" as userName
        db.collection("join_requests")
            .whereEqualTo("userName", "Unknown User")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "No join requests with Unknown User found");
                    if (callback != null) {
                        callback.run();
                    }
                    return;
                }
                
                Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " join requests with Unknown User");
                Toast.makeText(context, "Found " + queryDocumentSnapshots.size() + 
                        " requests with missing user data. Updating...", Toast.LENGTH_SHORT).show();
                
                // Create tasks list to track completion
                List<Task<?>> updateTasks = new ArrayList<>();
                
                // Process each join request
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    String userId = document.getString("userId");
                    if (userId == null || userId.isEmpty()) {
                        Log.e(TAG, "Join request has no userId: " + document.getId());
                        continue;
                    }
                    
                    // Start user data lookup process
                    updateTasks.add(findUserDataAndUpdateRequest(userId, document.getId()));
                }
                
                // When all tasks are complete
                if (!updateTasks.isEmpty()) {
                    Tasks.whenAll(updateTasks)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully updated all join requests");
                            Toast.makeText(context, "Successfully updated user data for join requests", 
                                    Toast.LENGTH_SHORT).show();
                            if (callback != null) {
                                callback.run();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating join requests: " + e.getMessage(), e);
                            Toast.makeText(context, "Error updating some join requests: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            if (callback != null) {
                                callback.run();
                            }
                        });
                } else {
                    if (callback != null) {
                        callback.run();
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error finding join requests: " + e.getMessage(), e);
                Toast.makeText(context, "Error finding join requests: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.run();
                }
            });
    }
    
    /**
     * Find user data for a specific user and update their join request
     * 
     * @param userId The user ID
     * @param requestId The join request ID
     * @return Task representing the update operation
     */
    private static Task<Void> findUserDataAndUpdateRequest(String userId, String requestId) {
        Log.d(TAG, "Finding user data for userId: " + userId + ", requestId: " + requestId);
        
        // Create a task source to resolve when the update is complete
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource = new com.google.android.gms.tasks.TaskCompletionSource<>();
        
        // First try Realtime Database
        FirebaseDatabase.getInstance().getReference("users")
            .child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Log.d(TAG, "Found user data in Realtime Database: " + snapshot.getValue());
                        
                        String userName = null;
                        String photoUrl = null;
                        
                        // Try common field names for name
                        if (snapshot.hasChild("firstName") && snapshot.hasChild("lastName")) {
                            String firstName = snapshot.child("firstName").getValue(String.class);
                            String lastName = snapshot.child("lastName").getValue(String.class);
                            userName = (firstName != null ? firstName : "") + 
                                       ((firstName != null && lastName != null && !lastName.isEmpty()) ? " " : "") + 
                                       (lastName != null ? lastName : "");
                        } else if (snapshot.hasChild("name")) {
                            userName = snapshot.child("name").getValue(String.class);
                        } else if (snapshot.hasChild("displayName")) {
                            userName = snapshot.child("displayName").getValue(String.class);
                        }
                        
                        // Try common field names for photo
                        if (snapshot.hasChild("profileImageUrl")) {
                            photoUrl = snapshot.child("profileImageUrl").getValue(String.class);
                        } else if (snapshot.hasChild("photoUrl")) {
                            photoUrl = snapshot.child("photoUrl").getValue(String.class);
                        } else if (snapshot.hasChild("profileImage")) {
                            photoUrl = snapshot.child("profileImage").getValue(String.class);
                        } else if (snapshot.hasChild("imageUrl")) {
                            photoUrl = snapshot.child("imageUrl").getValue(String.class);
                        }
                        
                        if (userName != null && !userName.isEmpty()) {
                            // Update join request
                            updateJoinRequest(requestId, userName, photoUrl, taskSource);
                            return;
                        }
                    }
                    
                    // If we didn't find or couldn't extract user data, try Firestore
                    findUserInFirestore(userId, requestId, taskSource);
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Database error: " + error.getMessage());
                    // Continue to Firestore
                    findUserInFirestore(userId, requestId, taskSource);
                }
            });
        
        return taskSource.getTask();
    }
    
    /**
     * Find user data in Firestore
     * 
     * @param userId The user ID
     * @param requestId The join request ID
     * @param taskSource Task source to resolve when complete
     */
    private static void findUserInFirestore(String userId, String requestId, 
                                           com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource) {
        Log.d(TAG, "Looking for user in Firestore for userId: " + userId);
        
        // First try "users" collection (lowercase)
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    Log.d(TAG, "Found user in Firestore users collection");
                    
                    String userName = extractUserName(userDoc.getData());
                    String photoUrl = extractPhotoUrl(userDoc.getData());
                    
                    if (userName != null && !userName.isEmpty()) {
                        updateJoinRequest(requestId, userName, photoUrl, taskSource);
                        return;
                    }
                }
                
                // Try "Users" collection (uppercase)
                FirebaseFirestore.getInstance().collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Log.d(TAG, "Found user in Firestore Users collection");
                            
                            String userName = extractUserName(doc.getData());
                            String photoUrl = extractPhotoUrl(doc.getData());
                            
                            if (userName != null && !userName.isEmpty()) {
                                updateJoinRequest(requestId, userName, photoUrl, taskSource);
                                return;
                            }
                        }
                        
                        // Try wildcard search for user ID in any collection
                        findUserInAllCollections(userId, requestId, taskSource);
                    })
                    .addOnFailureListener(e -> {
                        // Try wildcard search for user ID in any collection
                        findUserInAllCollections(userId, requestId, taskSource);
                    });
            })
            .addOnFailureListener(e -> {
                // Try "Users" collection (uppercase)
                FirebaseFirestore.getInstance().collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Log.d(TAG, "Found user in Firestore Users collection");
                            
                            String userName = extractUserName(doc.getData());
                            String photoUrl = extractPhotoUrl(doc.getData());
                            
                            if (userName != null && !userName.isEmpty()) {
                                updateJoinRequest(requestId, userName, photoUrl, taskSource);
                                return;
                            }
                        }
                        
                        // Try wildcard search for user ID in any collection
                        findUserInAllCollections(userId, requestId, taskSource);
                    })
                    .addOnFailureListener(ex -> {
                        // Try wildcard search for user ID in any collection
                        findUserInAllCollections(userId, requestId, taskSource);
                    });
            });
    }
    
    /**
     * Find user in all collections by searching for documents where ID matches
     * 
     * @param userId The user ID
     * @param requestId The join request ID
     * @param taskSource Task source to resolve when complete
     */
    private static void findUserInAllCollections(String userId, String requestId,
                                               com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource) {
        Log.d(TAG, "Searching all collections for userId: " + userId);
        
        // List of common collections that might contain user data
        String[] collections = {"users", "Users", "profiles", "Profiles", "accounts", "Accounts"};
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        
        // Search each collection for documents with this userId
        for (String collection : collections) {
            tasks.add(FirebaseFirestore.getInstance().collection(collection)
                .whereEqualTo("id", userId)
                .get());
            
            tasks.add(FirebaseFirestore.getInstance().collection(collection)
                .whereEqualTo("userId", userId)
                .get());
            
            tasks.add(FirebaseFirestore.getInstance().collection(collection)
                .whereEqualTo("uid", userId)
                .get());
        }
        
        // When all searches are complete
        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener(task -> {
                for (Task<QuerySnapshot> searchTask : tasks) {
                    if (searchTask.isSuccessful() && searchTask.getResult() != null && !searchTask.getResult().isEmpty()) {
                        DocumentSnapshot doc = searchTask.getResult().getDocuments().get(0);
                        
                        String userName = extractUserName(doc.getData());
                        String photoUrl = extractPhotoUrl(doc.getData());
                        
                        if (userName != null && !userName.isEmpty()) {
                            updateJoinRequest(requestId, userName, photoUrl, taskSource);
                            return;
                        }
                    }
                }
                
                // If we still haven't found the user, create a better fallback name
                updateJoinRequest(requestId, "User " + userId.substring(0, 5), null, taskSource);
            });
    }
    
    /**
     * Extract user name from user data
     * 
     * @param userData User data map
     * @return Extracted user name or null
     */
    private static String extractUserName(Map<String, Object> userData) {
        if (userData == null) return null;
        
        // Try first name + last name
        if (userData.containsKey("firstName") && userData.containsKey("lastName")) {
            String firstName = (String) userData.get("firstName");
            String lastName = (String) userData.get("lastName");
            return (firstName != null ? firstName : "") + 
                   ((firstName != null && lastName != null && !lastName.isEmpty()) ? " " : "") + 
                   (lastName != null ? lastName : "");
        }
        
        // Try other common name fields
        String[] nameFields = {"name", "displayName", "display_name", "username", 
                            "fullName", "full_name", "userName"};
        for (String field : nameFields) {
            if (userData.containsKey(field)) {
                String name = (String) userData.get(field);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        }
        
        // Try email as fallback
        if (userData.containsKey("email")) {
            String email = (String) userData.get("email");
            if (email != null && email.contains("@")) {
                return email.split("@")[0]; // Use part before @
            }
        }
        
        return null;
    }
    
    /**
     * Extract photo URL from user data
     * 
     * @param userData User data map
     * @return Extracted photo URL or null
     */
    private static String extractPhotoUrl(Map<String, Object> userData) {
        if (userData == null) return null;
        
        // Try common photo URL fields
        String[] photoFields = {"photoUrl", "photo_url", "profileImage", "profile_image", 
                              "imageUrl", "image_url", "avatarUrl", "avatar_url", "picture",
                              "profileImageUrl", "userImageUrl"};
        
        for (String field : photoFields) {
            if (userData.containsKey(field)) {
                String url = (String) userData.get(field);
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Update join request with proper user data
     * 
     * @param requestId The join request ID
     * @param userName The user name
     * @param photoUrl The photo URL
     * @param taskSource Task source to resolve when complete
     */
    private static void updateJoinRequest(String requestId, String userName, String photoUrl,
                                        com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource) {
        Log.d(TAG, "Updating join request " + requestId + " with userName: " + userName + 
                ", photoUrl: " + (photoUrl != null ? photoUrl : "null"));
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("userName", userName);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            updates.put("userImageUrl", photoUrl);
        }
        
        FirebaseFirestore.getInstance().collection("join_requests")
            .document(requestId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Join request updated successfully");
                taskSource.setResult(null);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating join request: " + e.getMessage(), e);
                taskSource.setException(e);
            });
    }
    
    /**
     * Update a specific join request using the userId provided
     * 
     * @param context The context
     * @param requestId The join request ID
     * @param userId The user ID
     * @param callback Callback for when the request is updated
     */
    public static void updateSpecificRequest(Context context, String requestId, String userId, Runnable callback) {
        Log.d(TAG, "Updating specific request: " + requestId + " for user: " + userId);
        Toast.makeText(context, "Updating user data for request...", Toast.LENGTH_SHORT).show();
        
        findUserDataAndUpdateRequest(userId, requestId)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Successfully updated join request");
                Toast.makeText(context, "Successfully updated user data", Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.run();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating join request: " + e.getMessage(), e);
                Toast.makeText(context, "Error updating user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.run();
                }
            });
    }
} 