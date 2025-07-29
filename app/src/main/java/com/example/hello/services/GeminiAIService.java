package com.example.hello.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.hello.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class GeminiAIService {
    private static final String TAG = "GeminiAIService";
    
    // Get API key from BuildConfig (set in build.gradle.kts)
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/";
    
    private final Context context;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private GeminiApi geminiApi;
    private List<ChatMessage> conversationHistory;

    public interface GeminiResponseCallback {
        void onResponse(String response);
        void onError(String error);
    }

    // Retrofit interface for Gemini API
    private interface GeminiApi {
        @POST("models/gemini-1.5-flash:generateContent")
        Call<GeminiResponse> generateContent(
                @Query("key") String apiKey,
                @Body GeminiRequest request
        );
    }

    // Request classes for Gemini API
    private static class GeminiRequest {
        @SerializedName("contents")
        List<Content> contents;

        @SerializedName("generationConfig")
        GenerationConfig generationConfig;

        @SerializedName("safetySettings")
        List<SafetySetting> safetySettings;

        GeminiRequest(String text, List<ChatMessage> history) {
            this.contents = new ArrayList<>();
            
            // Add conversation history
            if (history != null && !history.isEmpty()) {
                for (ChatMessage msg : history) {
                    Content content = new Content();
                    content.role = msg.isUser ? "user" : "model";
                    content.parts = new ArrayList<>();
                    Part part = new Part();
                    part.text = msg.message;
                    content.parts.add(part);
                    this.contents.add(content);
                }
            }
            
            // Add current user message
            Content userContent = new Content();
            userContent.role = "user";
            userContent.parts = new ArrayList<>();
            Part userPart = new Part();
            userPart.text = text;
            userContent.parts.add(userPart);
            this.contents.add(userContent);

            // Set generation config
            this.generationConfig = new GenerationConfig();
            this.generationConfig.temperature = 0.7f;
            this.generationConfig.topK = 40;
            this.generationConfig.topP = 0.95f;
            this.generationConfig.maxOutputTokens = 1024;

            // Set safety settings
            this.safetySettings = new ArrayList<>();
            addSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE");
            addSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE");
            addSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE");
            addSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE");
        }

        private void addSafetySetting(String category, String threshold) {
            SafetySetting setting = new SafetySetting();
            setting.category = category;
            setting.threshold = threshold;
            this.safetySettings.add(setting);
        }
    }

    private static class Content {
        @SerializedName("role")
        String role;

        @SerializedName("parts")
        List<Part> parts;
    }

    private static class Part {
        @SerializedName("text")
        String text;
    }

    private static class GenerationConfig {
        @SerializedName("temperature")
        float temperature;

        @SerializedName("topK")
        int topK;

        @SerializedName("topP")
        float topP;

        @SerializedName("maxOutputTokens")
        int maxOutputTokens;
    }

    private static class SafetySetting {
        @SerializedName("category")
        String category;

        @SerializedName("threshold")
        String threshold;
    }

    // Response classes for Gemini API
    private static class GeminiResponse {
        @SerializedName("candidates")
        List<Candidate> candidates;

        @SerializedName("promptFeedback")
        PromptFeedback promptFeedback;
    }

    private static class Candidate {
        @SerializedName("content")
        Content content;

        @SerializedName("finishReason")
        String finishReason;

        @SerializedName("safetyRatings")
        List<SafetyRating> safetyRatings;
    }

    private static class PromptFeedback {
        @SerializedName("safetyRatings")
        List<SafetyRating> safetyRatings;
    }

    private static class SafetyRating {
        @SerializedName("category")
        String category;

        @SerializedName("probability")
        String probability;
    }

    // Chat message for conversation history
    private static class ChatMessage {
        String message;
        boolean isUser;
        long timestamp;

        ChatMessage(String message, boolean isUser) {
            this.message = message;
            this.isUser = isUser;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public GeminiAIService(Context context) {
        this.context = context;
        this.conversationHistory = new ArrayList<>();
        
        try {
            // Initialize Retrofit
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build();
            
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            
            geminiApi = retrofit.create(GeminiApi.class);
            
            // Add system message to set context
            addSystemContext();
            
            Log.d(TAG, "Gemini AI service initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Gemini AI service: " + e.getMessage(), e);
        }
    }

    private void addSystemContext() {
        // Add initial context message to guide the AI
        String systemContext = "You are a helpful Community Assistant for a blood donation and health community app. " +
                "You should provide accurate information about:\n" +
                "- Blood donation processes, eligibility, and preparation\n" +
                "- Basic health information and wellness tips\n" +
                "- Community events and organizing activities\n" +
                "- Emergency guidance and basic first aid\n" +
                "- Health awareness and education\n\n" +
                "Always be supportive, informative, and encourage community participation. " +
                "If you're unsure about medical advice, recommend consulting healthcare professionals. " +
                "Keep responses helpful, concise, and friendly.";
        
        conversationHistory.add(new ChatMessage(systemContext, false));
    }

    public void sendMessage(String userMessage, GeminiResponseCallback callback) {
        if (geminiApi == null) {
            callback.onError("Gemini AI service not initialized");
            return;
        }

        if (API_KEY == null || API_KEY.equals("YOUR_GEMINI_API_KEY_HERE") || API_KEY.trim().isEmpty()) {
            // Fallback to pattern-based responses if API key is not set
            String fallbackResponse = generateFallbackResponse(userMessage);
            mainHandler.post(() -> callback.onResponse(fallbackResponse));
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Add user message to conversation history
                conversationHistory.add(new ChatMessage(userMessage, true));
                
                // Keep conversation history manageable (last 10 exchanges)
                if (conversationHistory.size() > 20) {
                    // Keep system context and last 18 messages
                    List<ChatMessage> trimmed = new ArrayList<>();
                    trimmed.add(conversationHistory.get(0)); // System context
                    trimmed.addAll(conversationHistory.subList(conversationHistory.size() - 18, conversationHistory.size()));
                    conversationHistory = trimmed;
                }
                
                // Create the request
                GeminiRequest request = new GeminiRequest(userMessage, conversationHistory.subList(0, conversationHistory.size() - 1));
                
                // Make the API call
                Call<GeminiResponse> call = geminiApi.generateContent(API_KEY, request);
                
                Log.d(TAG, "Making API call to Gemini with API key: " + (API_KEY != null ? API_KEY.substring(0, Math.min(10, API_KEY.length())) + "..." : "null"));
                
                call.enqueue(new Callback<GeminiResponse>() {
                    @Override
                    public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            GeminiResponse geminiResponse = response.body();
                            
                            if (geminiResponse.candidates != null && !geminiResponse.candidates.isEmpty()) {
                                Candidate candidate = geminiResponse.candidates.get(0);
                                
                                if (candidate.content != null && 
                                    candidate.content.parts != null && 
                                    !candidate.content.parts.isEmpty()) {
                                    
                                    String responseText = candidate.content.parts.get(0).text;
                                    
                                    if (responseText != null && !responseText.trim().isEmpty()) {
                                        // Add AI response to conversation history
                                        conversationHistory.add(new ChatMessage(responseText, false));
                                        
                                        mainHandler.post(() -> callback.onResponse(responseText));
                                        return;
                                    }
                                }
                            }
                            
                            mainHandler.post(() -> callback.onError("Empty response from Gemini AI"));
                        } else {
                            Log.e(TAG, "API call failed with code: " + response.code());
                            final String errorMsg = "API Error: " + response.code();
                            if (response.errorBody() != null) {
                                try {
                                    final String errorBody = response.errorBody().string();
                                    Log.e(TAG, "Error response body: " + errorBody);
                                    mainHandler.post(() -> callback.onError(errorMsg + " - " + errorBody));
                                } catch (Exception e) {
                                    Log.e(TAG, "Error reading error body", e);
                                    mainHandler.post(() -> callback.onError(errorMsg));
                                }
                            } else {
                                mainHandler.post(() -> callback.onError(errorMsg));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<GeminiResponse> call, Throwable t) {
                        Log.e(TAG, "Network error calling Gemini API", t);
                        mainHandler.post(() -> callback.onError("Network error: " + t.getMessage()));
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message to Gemini AI: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    // Fallback response generator for when API key is not configured
    private String generateFallbackResponse(String userMessage) {
        String message = userMessage.toLowerCase().trim();
        
        // Greetings
        if (containsAny(message, "hello", "hi", "hey", "good morning", "good afternoon", "good evening")) {
            return "Hello! 👋 I'm your Community Assistant. I can help you with blood donation information, health questions, community events, and emergency guidance. How can I assist you today?";
        }
        
        // Blood donation eligibility
        if (containsAny(message, "blood", "donate", "donation") && 
            containsAny(message, "eligible", "eligibility", "can i", "requirements")) {
            
            return "To donate blood, you generally need to:\n\n" +
                   "• Be at least 17 years old (16 with parental consent)\n" +
                   "• Weigh at least 110 pounds\n" +
                   "• Be in good general health\n" +
                   "• Wait 56 days between whole blood donations\n\n" +
                   "Some medications and conditions may affect eligibility. For specific questions, please consult with blood donation center staff.";
        }
        
        // Blood donation process
        if (containsAny(message, "blood", "donation") && 
            containsAny(message, "process", "how", "steps", "procedure")) {
            
            return "The blood donation process involves:\n\n" +
                   "1. Registration and health screening\n" +
                   "2. Mini-physical (temperature, blood pressure, hemoglobin)\n" +
                   "3. Donation (8-10 minutes)\n" +
                   "4. Rest and refreshments (15 minutes)\n\n" +
                   "The entire process takes about an hour. Make sure to eat well, stay hydrated, and get good rest beforehand!";
        }
        
        // Health tips
        if (containsAny(message, "health", "healthy", "wellness", "tips")) {
            return "Here are some general wellness tips:\n\n" +
                   "• Stay hydrated - drink plenty of water\n" +
                   "• Eat a balanced diet with fruits and vegetables\n" +
                   "• Get regular exercise\n" +
                   "• Aim for 7-9 hours of sleep\n" +
                   "• Manage stress through relaxation techniques\n" +
                   "• Regular health check-ups are important\n\n" +
                   "Always consult healthcare professionals for specific medical advice!";
        }
        
        // Emergency guidance
        if (containsAny(message, "emergency", "first aid", "urgent", "help")) {
            return "For medical emergencies, always call emergency services immediately!\n\n" +
                   "Basic first aid reminders:\n" +
                   "• Check for responsiveness and breathing\n" +
                   "• Apply pressure to bleeding wounds\n" +
                   "• Don't move someone with potential spinal injury\n" +
                   "• Stay calm and call for professional help\n\n" +
                   "⚠️ This is not a substitute for proper first aid training or emergency services!";
        }
        
        // Community events
        if (containsAny(message, "event", "community", "organize", "volunteer")) {
            return "Great to hear you're interested in community involvement! 🌟\n\n" +
                   "Community events can include:\n" +
                   "• Blood donation drives\n" +
                   "• Health awareness campaigns\n" +
                   "• Volunteer training sessions\n" +
                   "• Educational workshops\n\n" +
                   "Check the app's events section for upcoming activities or contact local organizers to get involved!";
        }
        
        // Thank you
        if (containsAny(message, "thank", "thanks", "appreciate")) {
            return "You're very welcome! 😊 I'm here whenever you need help with blood donation, health information, or community activities. Feel free to ask anytime!";
        }
        
        // Default response
        return "I'm here to help with blood donation information, health questions, community events, and emergency guidance. Could you please tell me more about what you'd like to know?";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public void clearConversationHistory() {
        conversationHistory.clear();
        addSystemContext();
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
