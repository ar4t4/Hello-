package com.example.hello.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class DialogflowService {
    private static final String TAG = DialogflowService.class.getSimpleName();
    private static final String PROJECT_ID = "blooddonationsupport-gbba";
    private static final String DIALOGFLOW_API_URL = "https://dialogflow.googleapis.com/v2/";
    
    private final Context context;
    private final String sessionId;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DialogflowApi dialogflowApi;
    private String accessToken;

    public interface DialogflowResponseCallback {
        void onResponse(String response);
        void onError(String error);
    }

    // Retrofit interface for Dialogflow API
    private interface DialogflowApi {
        @POST("projects/{projectId}/agent/sessions/{sessionId}:detectIntent")
        Call<DetectIntentResponse> detectIntent(
                @Path("projectId") String projectId,
                @Path("sessionId") String sessionId,
                @Header("Authorization") String authorization,
                @Body DetectIntentRequest request
        );
    }

    // Request and response classes for Dialogflow API
    private static class DetectIntentRequest {
        QueryInput queryInput;

        DetectIntentRequest(String text) {
            this.queryInput = new QueryInput(text);
        }
    }

    private static class QueryInput {
        TextInput text;

        QueryInput(String text) {
            this.text = new TextInput(text);
        }
    }

    private static class TextInput {
        String text;
        String languageCode;

        TextInput(String text) {
            this.text = text;
            this.languageCode = "en-US";
        }
    }

    private static class DetectIntentResponse {
        QueryResult queryResult;
    }

    private static class QueryResult {
        String fulfillmentText;
    }

    public DialogflowService(Context context) {
        this.context = context;
        this.sessionId = UUID.randomUUID().toString();
        
        try {
            // Initialize Retrofit
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();
                        
                        // Add common headers if needed
                        builder.header("Content-Type", "application/json");
                        
                        return chain.proceed(builder.build());
                    })
                    .build();
            
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(DIALOGFLOW_API_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            
            dialogflowApi = retrofit.create(DialogflowApi.class);
            
            // Load credentials
            loadCredentials();
            
            Log.d(TAG, "Dialogflow service initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Dialogflow service: " + e.getMessage(), e);
        }
    }

    private void loadCredentials() {
        executorService.execute(() -> {
            try {
                // For simplicity, we'll use a simpler approach for authentication
                // In a production app, you should use a more secure method
                InputStream credentialsStream = context.getAssets().open("dialogflow_credentials.json");
                byte[] buffer = new byte[credentialsStream.available()];
                credentialsStream.read(buffer);
                credentialsStream.close();
                
                String json = new String(buffer, "UTF-8");
                JsonObject credentials = new Gson().fromJson(json, JsonObject.class);
                
                // Extract private key and other info
                String privateKey = credentials.get("private_key").getAsString();
                String clientEmail = credentials.get("client_email").getAsString();
                
                // In a real app, you would use the private key to generate a JWT token
                // and then exchange it for an access token
                // For simplicity, we'll use a mock token
                accessToken = "Bearer mock_token";
                
                Log.d(TAG, "Credentials loaded successfully");
            } catch (IOException e) {
                Log.e(TAG, "Error loading credentials: " + e.getMessage(), e);
            }
        });
    }

    public void sendMessage(String message, DialogflowResponseCallback callback) {
        if (dialogflowApi == null) {
            callback.onError("Dialogflow API not initialized");
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Create the request
                DetectIntentRequest request = new DetectIntentRequest(message);
                
                // Make the API call
                Call<DetectIntentResponse> call = dialogflowApi.detectIntent(
                        PROJECT_ID, 
                        sessionId, 
                        accessToken, 
                        request
                );
                
                // For now, let's use a simpler approach with pattern matching
                // This is a fallback while we resolve the API integration
                String response = generateFallbackResponse(message);
                mainHandler.post(() -> callback.onResponse(response));
                
                // In a real implementation, you would uncomment this code:
                /*
                call.enqueue(new Callback<DetectIntentResponse>() {
                    @Override
                    public void onResponse(Call<DetectIntentResponse> call, Response<DetectIntentResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String fulfillmentText = response.body().queryResult.fulfillmentText;
                            if (fulfillmentText != null && !fulfillmentText.isEmpty()) {
                                mainHandler.post(() -> callback.onResponse(fulfillmentText));
                            } else {
                                mainHandler.post(() -> callback.onError("Empty response from Dialogflow"));
                            }
                        } else {
                            mainHandler.post(() -> callback.onError("Error: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<DetectIntentResponse> call, Throwable t) {
                        mainHandler.post(() -> callback.onError("Network error: " + t.getMessage()));
                    }
                });
                */
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message to Dialogflow: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }
    
    // Fallback response generator with comprehensive pattern matching
    private String generateFallbackResponse(String userMessage) {
        // Convert to lowercase for easier matching
        String message = userMessage.toLowerCase().trim();
        
        // 1. GREETINGS INTENT
        if (containsAny(message, "hello", "hi", "hey", "good morning", "good afternoon", "good evening", "what's up", "howdy")) {
            return getRandomResponse(
                "Hello! 👋 I'm your Community Assistant. How can I help you today?",
                "Hi there! I'm here to help with blood donation information, health questions, event organization, or emergency guidance. What do you need?",
                "Good day! How can I assist you with your community health needs?",
                "Hello! I'm ready to answer your questions about blood donation, health, events, or emergencies. What would you like to know?"
            );
        }
        
        // 2. HOW ARE YOU INTENT
        if (containsAny(message, "how are you", "how's it going", "how are things", "how do you do", "how's your day")) {
            return getRandomResponse(
                "I'm doing well, thanks for asking! I'm ready to help with any questions you might have.",
                "I'm great! How can I assist you today with blood donation or community health information?",
                "I'm excellent! Ready to help with whatever you need. What questions do you have today?"
            );
        }
        
        // 3. INTRODUCTION/IDENTITY INTENT
        if (containsAny(message, "who are you", "what are you", "what's your name", "what do you do", "tell me about yourself", "what can you do")) {
            return "I'm the Community Assistant, designed to provide information about blood donation, health topics, community events, and emergency guidance. I can answer questions about blood donation eligibility, health tips, organizing community events, and basic emergency procedures.";
        }
        
        // 4. BLOOD DONATION ELIGIBILITY INTENT
        if ((containsAny(message, "blood", "donate", "donation") && 
             containsAny(message, "eligible", "eligibility", "can i", "requirements", "qualify", "allowed")) ||
            containsAny(message, "who can donate blood", "am i eligible to donate")) {
            
            return "To donate blood, you must generally:\n\n" +
                   "• Be at least 17 years old (16 with parental consent in some states)\n" +
                   "• Weigh at least 110 pounds (50 kg)\n" +
                   "• Be in good general health\n" +
                   "• Have not donated whole blood in the last 56 days\n\n" +
                   "Some medications and medical conditions may affect eligibility. Would you like to know about specific conditions that might affect your ability to donate?";
        }
        
        // 5. BLOOD DONATION PROCESS INTENT
        if ((containsAny(message, "blood", "donate", "donation") && 
             containsAny(message, "process", "procedure", "how to", "what happens", "steps", "how does"))) {
            
            return "The blood donation process has 4 simple steps:\n\n" +
                   "1. Registration: You'll complete a donor registration form and show ID\n" +
                   "2. Mini-physical: A healthcare professional will check your temperature, blood pressure, pulse and hemoglobin levels\n" +
                   "3. Donation: The actual donation takes only 8-10 minutes, where about a pint of blood is collected\n" +
                   "4. Refreshments: You'll spend 15 minutes enjoying snacks and drinks while making sure you feel well before leaving\n\n" +
                   "The entire process takes about one hour. Would you like tips on preparing for donation?";
        }
        
        // 6. BLOOD DONATION PREPARATION INTENT
        if ((containsAny(message, "blood", "donate", "donation") && 
             containsAny(message, "prepare", "preparation", "ready", "before", "tips", "advice"))) {
            
            return "To prepare for blood donation:\n\n" +
                   "• Get a good night's sleep\n" +
                   "• Eat a healthy meal before donating\n" +
                   "• Drink extra water (16 oz) before donation\n" +
                   "• Avoid fatty foods before donating\n" +
                   "• Bring ID and list of medications you're taking\n" +
                   "• Wear a shirt with sleeves that can be rolled up\n\n" +
                   "After donation, drink extra fluids and avoid strenuous activity for 24 hours.";
        }
        
        // 7. BLOOD TYPE COMPATIBILITY INTENT
        if (containsAny(message, "blood type", "blood types", "compatible", "compatibility", "universal donor", "universal recipient")) {
            
            return "Here's a guide to blood type compatibility:\n\n" +
                   "• Type O- can donate to all blood types (universal donor)\n" +
                   "• Type O+ can donate to O+, A+, B+, AB+\n" +
                   "• Type A- can donate to A-, A+, AB-, AB+\n" +
                   "• Type A+ can donate to A+, AB+\n" +
                   "• Type B- can donate to B-, B+, AB-, AB+\n" +
                   "• Type B+ can donate to B+, AB+\n" +
                   "• Type AB- can donate to AB-, AB+\n" +
                   "• Type AB+ can only donate to AB+ (but is the universal recipient)";
        }
        
        // 8. HEALTH INFORMATION GENERAL INTENT
        if (containsAny(message, "health", "healthy", "wellness") && 
            !containsAny(message, "emergency", "urgent", "help", "911")) {
            
            return "I can provide general health information, but please remember I'm not a medical professional. For specific medical advice, please consult your healthcare provider.\n\n" +
                   "I can help with information about:\n" +
                   "• Basic nutrition and hydration\n" +
                   "• Exercise recommendations\n" +
                   "• Sleep guidelines\n" +
                   "• Stress management\n" +
                   "• General preventive health measures\n\n" +
                   "What specific health information are you looking for?";
        }
        
        // 9. HYDRATION INTENT
        if (containsAny(message, "water", "hydration", "hydrate", "dehydrated", "thirsty", "drink") &&
            containsAny(message, "how much", "daily", "should i", "recommended")) {
            
            return "For proper hydration, most health authorities recommend:\n\n" +
                   "• About 3.7 liters (125 oz) of fluids daily for men\n" +
                   "• About 2.7 liters (91 oz) of fluids daily for women\n\n" +
                   "This includes water and other beverages. A simple guideline is to drink enough so your urine is pale yellow. Needs increase with exercise, hot weather, or illness.";
        }
        
        // 10. EVENT ORGANIZATION INTENT
        if (containsAny(message, "event", "organize", "planning", "plan", "host", "arrange") &&
            containsAny(message, "how to", "tips", "steps", "guide", "advice")) {
            
            return "To organize a successful community event:\n\n" +
                   "1. Define clear goals and target audience\n" +
                   "2. Form a planning committee and assign roles\n" +
                   "3. Set a date, time, and find an appropriate venue\n" +
                   "4. Create a detailed budget and timeline\n" +
                   "5. Arrange necessary permits, equipment, and supplies\n" +
                   "6. Develop a marketing and promotion strategy\n" +
                   "7. Recruit and train volunteers\n" +
                   "8. Plan for safety and emergencies\n\n" +
                   "Would you like specific information about organizing a blood drive or health fair?";
        }
        
        // 11. BLOOD DRIVE ORGANIZATION INTENT
        if (containsAny(message, "blood drive", "donation drive", "donor drive") &&
            containsAny(message, "organize", "planning", "plan", "host", "arrange", "run")) {
            
            return "To organize a blood drive:\n\n" +
                   "1. Contact your local blood center 8-12 weeks in advance\n" +
                   "2. Secure a suitable location with adequate space\n" +
                   "3. Set a date and time (typically 4-6 hours)\n" +
                   "4. Recruit a coordinator and volunteers\n" +
                   "5. Promote the drive through multiple channels\n" +
                   "6. Encourage pre-registration of donors\n" +
                   "7. Arrange refreshments for donors\n" +
                   "8. Follow up with thank-you messages\n\n" +
                   "The blood center will typically provide equipment, staff, and promotional materials.";
        }
        
        // 12. EMERGENCY GUIDANCE INTENT
        if (containsAny(message, "emergency", "urgent", "critical", "life-threatening", "911", "ambulance")) {
            
            return "🚨 For any serious medical emergency:\n\n" +
                   "1. Call emergency services (911) immediately\n" +
                   "2. Stay calm and speak clearly to the dispatcher\n" +
                   "3. Provide exact location and nature of the emergency\n" +
                   "4. Follow the dispatcher's instructions\n" +
                   "5. If safe, stay with the person until help arrives\n\n" +
                   "Remember, prompt professional medical care is crucial in emergencies.";
        }
        
        // 13. CPR GUIDANCE INTENT
        if (containsAny(message, "cpr", "cardiopulmonary resuscitation", "chest compression", "rescue breathing")) {
            
            return "Basic CPR steps (for adults):\n\n" +
                   "1. Check if the person is responsive and not breathing normally\n" +
                   "2. Call 911 immediately or have someone else call\n" +
                   "3. Place the person on their back on a firm surface\n" +
                   "4. Place your hands on the center of their chest\n" +
                   "5. Push hard and fast (100-120 compressions per minute)\n" +
                   "6. Continue until help arrives\n\n" +
                   "⚠️ Note: This is basic information only. Please get proper CPR training.";
        }
        
        // 14. STROKE GUIDANCE INTENT
        if (containsAny(message, "stroke", "brain attack", "cerebrovascular")) {
            
            return "Remember FAST for stroke symptoms:\n\n" +
                   "• F: Face drooping on one side\n" +
                   "• A: Arm weakness or numbness\n" +
                   "• S: Speech difficulty or slurred speech\n" +
                   "• T: Time to call emergency services (911)\n\n" +
                   "Other symptoms may include sudden: severe headache, trouble walking, dizziness, or vision problems. Immediate medical attention is critical for stroke.";
        }
        
        // 15. THANK YOU INTENT
        if (containsAny(message, "thank you", "thanks", "thank", "appreciate", "helpful", "great info")) {
            
            return getRandomResponse(
                "You're welcome! Is there anything else I can help you with?",
                "Happy to help! Feel free to ask if you have any other questions.",
                "Glad I could assist. Have a great day!",
                "You're welcome! I'm here whenever you need information about blood donation or community health."
            );
        }
        
        // 16. GOODBYE INTENT
        if (containsAny(message, "bye", "goodbye", "see you", "talk to you later", "that's all", "i'm done")) {
            
            return getRandomResponse(
                "Goodbye! Feel free to chat again if you need assistance.",
                "Take care! I'm here if you need more information later.",
                "Bye for now! Have a wonderful day.",
                "Until next time! Feel free to return whenever you have questions."
            );
        }
        
        // DEFAULT RESPONSE if no patterns match
        return "I'm your community assistant! I can help you with:\n\n" +
               "• Blood donation information\n" +
               "• Health-related queries\n" +
               "• Event organization\n" +
               "• Emergency guidance\n\n" +
               "What would you like to know?";
    }
    
    // Helper method to check if a string contains any of the given keywords
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    // Helper method to get a random response from a list of options
    private String getRandomResponse(String... responses) {
        int index = (int) (Math.random() * responses.length);
        return responses[index];
    }

    public void shutdown() {
        executorService.shutdown();
    }
} 