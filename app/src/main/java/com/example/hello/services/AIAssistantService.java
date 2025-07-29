package com.example.hello.services;

import android.content.Context;
import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.hello.models.Message;

public class AIAssistantService {
    private static final String TAG = "AIAssistantService";
    private static final String ASSISTANT_ID = "ai_assistant";
    private static final String ASSISTANT_NAME = "Community Assistant";
    private final DatabaseReference messagesRef;
    private final Context context;
    private GeminiAIService geminiAIService;
    private boolean isProcessingMessage = false;

    public AIAssistantService(Context context, String chatId) {
        this.context = context;
        this.messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(chatId);
        try {
            this.geminiAIService = new GeminiAIService(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GeminiAIService: " + e.getMessage(), e);
            // We'll handle this in processMessage
        }
    }

    public void processMessage(String userMessage, String chatId) {
        // If empty message (first message in chat), send welcome message
        if (userMessage == null || userMessage.isEmpty()) {
            sendWelcomeMessage(chatId);
            return;
        }
        
        // Prevent multiple simultaneous requests
        if (isProcessingMessage) {
            Log.d(TAG, "Already processing a message, ignoring new request");
            return;
        }
        
        isProcessingMessage = true;
        
        // Show typing indicator (optional)
        sendTypingIndicator(chatId, true);
        
        // Check if Gemini AI service is available
        if (geminiAIService == null) {
            Log.e(TAG, "GeminiAIService is null, using fallback response");
            sendTypingIndicator(chatId, false);
            sendAIResponse("I'm having trouble connecting to my brain right now. Please try again later.", chatId);
            isProcessingMessage = false;
            return;
        }
        
        // Process with Gemini AI
        try {
            geminiAIService.sendMessage(userMessage, new GeminiAIService.GeminiResponseCallback() {
                @Override
                public void onResponse(String response) {
                    // Remove typing indicator
                    sendTypingIndicator(chatId, false);
                    
                    // Send the AI response
                    sendAIResponse(response, chatId);
                    isProcessingMessage = false;
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Gemini AI error: " + error);
                    
                    // Remove typing indicator
                    sendTypingIndicator(chatId, false);
                    
                    // Send fallback response
                    sendAIResponse(getFallbackResponse(), chatId);
                    isProcessingMessage = false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending message to Gemini AI: " + e.getMessage(), e);
            sendTypingIndicator(chatId, false);
            sendAIResponse(getFallbackResponse(), chatId);
            isProcessingMessage = false;
        }
    }
    
    private void sendWelcomeMessage(String chatId) {
        String welcomeMessage = "👋 Hi! I'm your Community Assistant powered by Gemini AI. I can help you with:\n\n" +
                "• Blood donation information and eligibility\n" +
                "• Health-related questions and wellness tips\n" +
                "• Community events and volunteer opportunities\n" +
                "• Emergency guidance and basic first aid\n" +
                "• General health awareness and education\n\n" +
                "How can I assist you today? Feel free to ask me anything about health, blood donation, or community activities!";
        
        sendAIResponse(welcomeMessage, chatId);
    }
    
    private void sendTypingIndicator(String chatId, boolean isTyping) {
        // Optional: Implement typing indicator in your UI
        // This could update a field in Firebase to show the assistant is typing
    }
    
    private String getFallbackResponse() {
        String[] fallbacks = {
            "I'm sorry, I'm having trouble understanding. Could you rephrase that?",
            "I didn't quite catch that. Could you try asking in a different way?",
            "I'm still learning! Could you provide more details about your question?",
            "I apologize, but I'm not sure how to help with that. Could you try asking something related to blood donation, health information, or community events?"
        };
        
        return fallbacks[(int) (Math.random() * fallbacks.length)];
    }

    private void sendAIResponse(String response, String chatId) {
        try {
            String messageId = messagesRef.push().getKey();
            if (messageId == null) {
                Log.e(TAG, "Failed to generate message ID");
                return;
            }

            Message message = new Message();
            message.setId(messageId);
            message.setSenderId(ASSISTANT_ID);
            message.setSenderName(ASSISTANT_NAME);
            message.setContent(response);
            message.setTimestamp(System.currentTimeMillis());
            message.setChatId(chatId);
            message.setMessageType(0); // Text message

            messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "AI message sent successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send AI message: " + e.getMessage(), e));
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending AI response: " + e.getMessage(), e);
        }
    }
    
    public void shutdown() {
        if (geminiAIService != null) {
            try {
                geminiAIService.shutdown();
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down GeminiAIService: " + e.getMessage(), e);
            }
        }
    }
} 