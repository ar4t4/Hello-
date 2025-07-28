package com.example.hello.utils;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.hello.R;
import com.example.hello.models.JoinRequest;
import com.example.hello.models.User;
import com.example.hello.repositories.JoinRequestRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class JoinRequestDialog {
    
    private static final String TAG = "JoinRequestDialog";
    
    public interface JoinRequestCallback {
        void onRequestSent();
    }
    
    /**
     * Show a dialog to send a join request to a community
     * 
     * @param context The context
     * @param communityId The ID of the community to join
     * @param communityName The name of the community to join
     * @param callback Callback for when request is sent
     */
    public static void show(@NonNull Context context, 
                           @NonNull String communityId, 
                           @NonNull String communityName, 
                           JoinRequestCallback callback) {
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "You must be logged in to join communities", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Checking for existing requests for community: " + communityId);
        Log.d(TAG, "Current user: " + currentUser.getUid());
        
        // Check if user has already sent a request to this community
        FirebaseFirestore.getInstance()
                .collection("join_requests")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("communityId", communityId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " existing requests");
                    
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(context, "You already have a pending request to join this community", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Create and show the dialog
                    createDialog(context, communityId, communityName, currentUser, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing requests: " + e.getMessage(), e);
                    Toast.makeText(context, "Error checking request status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private static void createDialog(@NonNull Context context, 
                                    @NonNull String communityId, 
                                    @NonNull String communityName,
                                    @NonNull FirebaseUser currentUser,
                                    JoinRequestCallback callback) {
        
        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_join_request, null);
        dialog.setContentView(view);
        
        TextView titleText = view.findViewById(R.id.tv_dialog_title);
        TextView communityNameText = view.findViewById(R.id.tv_community_name);
        EditText messageInput = view.findViewById(R.id.et_join_message);
        Button cancelButton = view.findViewById(R.id.btn_cancel);
        Button sendButton = view.findViewById(R.id.btn_send_request);
        
        communityNameText.setText(communityName);
        
        // Set up click listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        sendButton.setOnClickListener(v -> {
            // Show loading state
            sendButton.setEnabled(false);
            sendButton.setText("Sending...");
            
            // Comprehensive user data lookup from multiple sources
            findUserDataFromMultipleSources(context, currentUser, communityId, communityName, dialog, callback);
        });
        
        // Show the dialog
        dialog.show();
    }
    
    private static void findUserDataFromMultipleSources(Context context, 
                                                        FirebaseUser currentUser,
                                                        String communityId,
                                                        String communityName,
                                                        Dialog dialog,
                                                        JoinRequestCallback callback) {
        // Debug message
        Log.d(TAG, "Looking for user data for: " + currentUser.getUid());
        Toast.makeText(context, "Retrieving your profile info...", Toast.LENGTH_SHORT).show();
        
        // Get a reference to the message input so we can access it later
        EditText messageInput = dialog.findViewById(R.id.et_join_message);
        String message = messageInput != null ? messageInput.getText().toString() : "";
        
        // First try Realtime Database which is the primary database for user profiles
        FirebaseDatabase.getInstance().getReference("users")
            .child(currentUser.getUid())
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
                            Log.d(TAG, "Using firstName+lastName: " + userName);
                        } else if (snapshot.hasChild("name")) {
                            userName = snapshot.child("name").getValue(String.class);
                            Log.d(TAG, "Using name field: " + userName);
                        } else if (snapshot.hasChild("displayName")) {
                            userName = snapshot.child("displayName").getValue(String.class);
                            Log.d(TAG, "Using displayName field: " + userName);
                        } else if (snapshot.hasChild("username")) {
                            userName = snapshot.child("username").getValue(String.class);
                            Log.d(TAG, "Using username field: " + userName);
                        }
                        
                        // Try common field names for photo - include more possible field names
                        if (snapshot.hasChild("profileImageUrl")) {
                            photoUrl = snapshot.child("profileImageUrl").getValue(String.class);
                            Log.d(TAG, "Using profileImageUrl: " + photoUrl);
                        } else if (snapshot.hasChild("photoUrl")) {
                            photoUrl = snapshot.child("photoUrl").getValue(String.class);
                            Log.d(TAG, "Using photoUrl: " + photoUrl);
                        } else if (snapshot.hasChild("profileImage")) {
                            photoUrl = snapshot.child("profileImage").getValue(String.class);
                            Log.d(TAG, "Using profileImage: " + photoUrl);
                        } else if (snapshot.hasChild("imageUrl")) {
                            photoUrl = snapshot.child("imageUrl").getValue(String.class);
                            Log.d(TAG, "Using imageUrl: " + photoUrl);
                        } else if (snapshot.hasChild("avatar")) {
                            photoUrl = snapshot.child("avatar").getValue(String.class);
                            Log.d(TAG, "Using avatar: " + photoUrl);
                        } else if (snapshot.hasChild("photo")) {
                            photoUrl = snapshot.child("photo").getValue(String.class);
                            Log.d(TAG, "Using photo: " + photoUrl);
                        }
                        
                        if (userName != null && !userName.isEmpty()) {
                            // Create and send the request with found data
                            createAndSendJoinRequest(context, currentUser, 
                                                communityId, communityName, 
                                                userName, photoUrl, message, dialog, callback);
                            return;
                        } else {
                            Log.w(TAG, "Found user data in Realtime DB but couldn't extract name");
                        }
                    } else {
                        Log.d(TAG, "No user data found in Realtime Database");
                    }
                    
                    // If we couldn't extract user data, try Firestore next
                    findUserInFirestore(context, currentUser, communityId, communityName, message, dialog, callback);
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Database error: " + error.getMessage());
                    // Continue to Firestore
                    findUserInFirestore(context, currentUser, communityId, communityName, message, dialog, callback);
                }
            });
    }
    
    private static void findUserInFirestore(Context context, 
                                           FirebaseUser currentUser,
                                           String communityId,
                                           String communityName,
                                           String message,
                                           Dialog dialog,
                                           JoinRequestCallback callback) {
        Log.d(TAG, "Looking for user in Firestore");
        
        // First try the "users" collection (lowercase)
        FirebaseFirestore.getInstance().collection("users")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    Log.d(TAG, "Found user in Firestore users collection");
                    
                    String userName = extractUserName(userDoc.getData());
                    String photoUrl = extractPhotoUrl(userDoc.getData());
                    
                    if (userName != null && !userName.isEmpty()) {
                        createAndSendJoinRequest(context, currentUser, 
                                            communityId, communityName, 
                                            userName, photoUrl, message, dialog, callback);
                        return;
                    } else {
                        Log.w(TAG, "Found user in Firestore but couldn't extract name");
                    }
                } else {
                    Log.d(TAG, "User not found in Firestore users collection");
                }
                
                // Try "Users" collection (uppercase)
                FirebaseFirestore.getInstance().collection("Users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Log.d(TAG, "Found user in Firestore Users collection");
                            
                            String userName = extractUserName(doc.getData());
                            String photoUrl = extractPhotoUrl(doc.getData());
                            
                            if (userName != null && !userName.isEmpty()) {
                                createAndSendJoinRequest(context, currentUser, 
                                                    communityId, communityName, 
                                                    userName, photoUrl, message, dialog, callback);
                                return;
                            } else {
                                Log.w(TAG, "Found user in Firestore Users but couldn't extract name");
                            }
                        } else {
                            Log.d(TAG, "User not found in Firestore Users collection");
                        }
                        
                        // Try wildcard search for user ID in any collection or fallback to Auth data
                        fallbackToAuthData(context, currentUser, 
                                        communityId, communityName, message, 
                                        dialog, callback);
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to Firebase Auth user data
                        fallbackToAuthData(context, currentUser, 
                                        communityId, communityName, message, 
                                        dialog, callback);
                    });
            })
            .addOnFailureListener(e -> {
                // Try "Users" collection (uppercase)
                FirebaseFirestore.getInstance().collection("Users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String userName = extractUserName(doc.getData());
                            String photoUrl = extractPhotoUrl(doc.getData());
                            
                            if (userName != null && !userName.isEmpty()) {
                                createAndSendJoinRequest(context, currentUser, 
                                                    communityId, communityName, 
                                                    userName, photoUrl, message, dialog, callback);
                            } else {
                                fallbackToAuthData(context, currentUser, 
                                                communityId, communityName, message, 
                                                dialog, callback);
                            }
                        } else {
                            fallbackToAuthData(context, currentUser, 
                                            communityId, communityName, message, 
                                            dialog, callback);
                        }
                    })
                    .addOnFailureListener(ex -> {
                        fallbackToAuthData(context, currentUser, 
                                        communityId, communityName, message, 
                                        dialog, callback);
                    });
            });
    }
    
    private static String extractUserName(Map<String, Object> userData) {
        if (userData == null) return null;
        
        // Log all fields for debugging
        for (Map.Entry<String, Object> entry : userData.entrySet()) {
            Log.d(TAG, "User field: " + entry.getKey() + " = " + entry.getValue());
        }
        
        // Try first name + last name
        if (userData.containsKey("firstName") && userData.containsKey("lastName")) {
            String firstName = (String) userData.get("firstName");
            String lastName = (String) userData.get("lastName");
            return (firstName != null ? firstName : "") + 
                   ((firstName != null && lastName != null && !lastName.isEmpty()) ? " " : "") + 
                   (lastName != null ? lastName : "");
        }
        
        // Try other common name fields
        String[] nameFields = {"name", "displayName", "display_name", "username", "fullName", "full_name"};
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
    
    private static String extractPhotoUrl(Map<String, Object> userData) {
        if (userData == null) return null;
        
        // Try common photo URL fields
        String[] photoFields = {"photoUrl", "photo_url", "profileImage", "profile_image", 
                              "imageUrl", "image_url", "avatarUrl", "avatar_url", "picture"};
        
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
    
    private static void fallbackToAuthData(Context context, 
                                         FirebaseUser currentUser,
                                         String communityId,
                                         String communityName,
                                         String message,
                                         Dialog dialog,
                                         JoinRequestCallback callback) {
        Log.d(TAG, "Falling back to Firebase Auth user data");
        
        String displayName = currentUser.getDisplayName();
        String photoUrl = currentUser.getPhotoUrl() != null ? 
                currentUser.getPhotoUrl().toString() : "";
        
        // Use email if no display name
        if (displayName == null || displayName.isEmpty()) {
            String email = currentUser.getEmail();
            if (email != null && !email.isEmpty() && email.contains("@")) {
                displayName = email.split("@")[0]; // Use part before @
                Log.d(TAG, "Using email username as fallback: " + displayName);
            } else {
                displayName = "User " + currentUser.getUid().substring(0, 5);
                Log.d(TAG, "Using UID prefix as fallback: " + displayName);
            }
        } else {
            Log.d(TAG, "Using Auth displayName: " + displayName);
        }
        
        createAndSendJoinRequest(context, currentUser, 
                              communityId, communityName, 
                              displayName, photoUrl, message, dialog, callback);
    }
    
    private static void createAndSendJoinRequest(Context context,
                                               FirebaseUser currentUser,
                                               String communityId,
                                               String communityName,
                                               String userName,
                                               String photoUrl,
                                               String message,
                                               Dialog dialog,
                                               JoinRequestCallback callback) {
        Log.d(TAG, "Creating join request with user name: " + userName);
        Toast.makeText(context, "Creating request as: " + userName, Toast.LENGTH_SHORT).show();
        
        // Create the join request
        JoinRequest request = new JoinRequest(
                currentUser.getUid(),
                userName,
                photoUrl != null ? photoUrl : "",
                communityId,
                communityName
        );
        
        // Debug info
        Map<String, Object> requestMap = request.toMap();
        Log.d(TAG, "Join request details:");
        for (Map.Entry<String, Object> entry : requestMap.entrySet()) {
            Log.d(TAG, "  " + entry.getKey() + ": " + entry.getValue());
        }
        
        // Save the user profile data in Firestore for future use
        saveUserProfile(currentUser, userName, photoUrl);
        
        // Send the join request
        new JoinRequestRepository().createJoinRequest(request)
                .addOnSuccessListener(documentReference -> {
                    String requestId = documentReference.getId();
                    Log.d(TAG, "Join request created successfully with ID: " + requestId);
                    
                    dialog.dismiss();
                    Toast.makeText(context, "Join request sent", Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.onRequestSent();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create join request: " + e.getMessage(), e);
                    Toast.makeText(context, "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Button sendButton = dialog.findViewById(R.id.btn_send_request);
                    if (sendButton != null) {
                        sendButton.setEnabled(true);
                        sendButton.setText("Send Request");
                    }
                });
    }
    
    private static void saveUserProfile(FirebaseUser currentUser, String userName, String photoUrl) {
        // Save user data to Firestore for future use
        User newUser = new User();
        newUser.setId(currentUser.getUid());
        newUser.setName(userName);
        newUser.setEmail(currentUser.getEmail());
        newUser.setImageUrl(photoUrl);
        
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .set(newUser)
                .addOnSuccessListener(aVoid -> 
                    Log.d(TAG, "Saved user profile data to Firestore"))
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Failed to save user profile: " + e.getMessage()));
    }
} 