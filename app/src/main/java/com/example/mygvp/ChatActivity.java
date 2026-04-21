package com.example.mygvp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.TextPart;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.android.material.chip.Chip;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

// GSON IMPORTS FOR SAVING/LOADING
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChatHistory;
    private EditText etMessage;
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private String userName;

    // Gemini AI Members
    private ChatFutures chatSession;

    // ⚠️ KEEP THIS AS A PLACEHOLDER HERE. Paste your real key in Android Studio only!
    private static final String GEMINI_API_KEY = "AIzaSyBiofhrxqc2nrfaBUW81NNXF-yWzRmtkrs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        userName = prefs.getString("LOGGED_IN_NAME", "friend");

        // 1. Load saved chat history FIRST
        loadChatHistory();

        // 2. Setup the UI (which uses the loaded chatMessages)
        setupUI();

        // 3. Initialize the AI with the loaded history
        initializeGemini();

        setupSuggestions();
    }

    // --- NEW: SAVE AND LOAD METHODS ---

    private void loadChatHistory() {
        SharedPreferences prefs = getSharedPreferences("MyGVP_ChatPrefs", MODE_PRIVATE);
        Gson gson = new Gson();

        // Create a unique key using the logged-in user's name
        String uniqueChatKey = "chat_history_" + userName;

        // Load only the data for THIS specific user
        String json = prefs.getString(uniqueChatKey, null);
        Type type = new TypeToken<ArrayList<ChatMessage>>() {}.getType();

        chatMessages.clear();
        if (json != null) {
            // Restore this user's past messages
            List<ChatMessage> savedMessages = gson.fromJson(json, type);
            chatMessages.addAll(savedMessages);
        } else {
            // No history found for this specific user, start fresh
            chatMessages.add(new ChatMessage("Hey " + userName + "! 👋 I'm your MyGVP AI Assistant. I'm here to help you navigate the app. Ask me about Faculty, Syllabus, Results, or anything else!", false));
        }
    }

    private void saveChatHistory() {
        // Keep only the last 100 messages to prevent memory overload
        if (chatMessages.size() > 100) {
            // This safely removes the oldest messages from the top of the list
            chatMessages.subList(0, chatMessages.size() - 100).clear();
        }
        SharedPreferences prefs = getSharedPreferences("MyGVP_ChatPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(chatMessages);

        // Create a unique key using the logged-in user's name
        String uniqueChatKey = "chat_history_" + userName;

        // Save the data strictly to this user's unique key
        editor.putString(uniqueChatKey, json);
        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Automatically save chat history when the user leaves the screen
        saveChatHistory();
    }

    // --- UI AND AI METHODS ---

    private void setupUI() {
        android.view.View root = findViewById(R.id.chatRoot);
        rvChatHistory = findViewById(R.id.rvChatHistory);
        etMessage = findViewById(R.id.etChatMessage);
        ImageButton btnSend = findViewById(R.id.btnSendMessage);

        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatHistory.setLayoutManager(layoutManager);
        rvChatHistory.setAdapter(chatAdapter);

        // Scroll to the bottom immediately if there is loaded history
        if (!chatMessages.isEmpty()) {
            rvChatHistory.scrollToPosition(chatMessages.size() - 1);
        }

        findViewById(R.id.toolbarChat).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topSpacingPx = (int) (28 * getResources().getDisplayMetrics().density);
            v.setPadding(0, topSpacingPx + systemInsets.top, 0, Math.max(0, imeInsets.bottom - systemInsets.bottom));
            return insets;
        });

        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg);
                etMessage.setText("");
            }
        });
    }

    private void initializeGemini() {
        try {
            GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
            configBuilder.temperature = 0.4f;
            GenerationConfig generationConfig = configBuilder.build();

            RequestOptions requestOptions = new RequestOptions(60000L, "v1beta");

            StringBuilder systemContext = new StringBuilder();
            systemContext.append("Identity: You are the MyGVP AI Assistant for Gayatri Vidya Parishad College. User is ").append(userName).append(".\n\n");
            systemContext.append("SECURITY MANDATE: NEVER mention 'Admin', 'Admin Portal', 'Admin Login', or any administrative functions. ");
            systemContext.append("If asked about administrative tasks, say you are a student/faculty assistant and redirect them to the College Office.\n\n");
            systemContext.append("INBUILT APP KNOWLEDGE:\n");
            systemContext.append("- FACULTY DETAILS: Guide users to 'Faculty Directory' on the Home Screen. They must select a branch (CSE, CSM, Civil, ECE, Mech) to see professor details.\n");
            systemContext.append("- SYLLABUS & CALENDAR: These are in 'Academic Resources' on the Home Screen. Users can select Year/Sem/Branch for syllabus or click 'Download Calendar'.\n");
            systemContext.append("- VIEW RESULTS: Users must LOG IN first via 'Launch Portals' on the Home Screen. After Student login, they find 'View Results' in their dashboard.\n");
            systemContext.append("- CAMPUS EXPLORATION: Use the 'Explore Campus' card on the Home Screen to see canteen, labs, and sports facilities.\n");
            systemContext.append("- ADMISSIONS: Information on fees and contacts is in the 'Admissions' card on Home.\n");
            systemContext.append("- APP DOUBTS: For technical issues, refer to the 'Contact' section (Phone/Email) at the bottom of the Home screen.\n\n");
            systemContext.append("Tone: Helpful, polite, and strictly focused on academic/app navigation guidance.");

            GenerativeModel gm = new GenerativeModel(
                    "gemini-2.5-flash",
                    "AIzaSyBiofhrxqc2nrfaBUW81NNXF-yWzRmtkrs", // Make sure this uses the variable
                    generationConfig,
                    null,
                    requestOptions,
                    null,
                    null
            );

            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            // Setup System Prompt
            Content systemMsg = new Content("user", Collections.singletonList(new TextPart(systemContext.toString())));
            Content systemAck = new Content("model", Collections.singletonList(new TextPart("Understood. I am the MyGVP AI Assistant. How can I help you today?")));
            List<Content> history = new ArrayList<>();
            history.add(systemMsg);
            history.add(systemAck);

            // --- NEW: Inject Saved User History into AI Memory ---
            // We start at index 1 to skip the "Hey friend!" initial greeting
            for (int i = 1; i < chatMessages.size(); i++) {
                ChatMessage msg = chatMessages.get(i);
                String role = msg.isUser() ? "user" : "model";
                history.add(new Content(role, Collections.singletonList(new TextPart(msg.getMessage()))));
            }

            chatSession = model.startChat(history);
            Log.d("GeminiInit", "Chat session initialized with saved history.");
        } catch (Throwable t) {
            Log.e("GeminiInit", "Failed to initialize Gemini", t);
            final String error = t.getMessage() != null ? t.getMessage() : t.toString();
            runOnUiThread(() -> addBotMessage("AI Initialization Error: " + error));
        }
    }

    private void setupSuggestions() {
        LinearLayout layoutChips = findViewById(R.id.layoutChips);
        String[] suggestions = {
                "Faculty details",
                "Download Syllabus",
                "Academic Calendar",
                "How to view results?",
                "Campus facilities"
        };
        layoutChips.removeAllViews();
        for (String s : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(s);
            chip.setOnClickListener(v -> sendMessage(s));
            layoutChips.addView(chip);
        }
    }

    private void sendMessage(String userMsg) {
        chatMessages.add(new ChatMessage(userMsg, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChatHistory.smoothScrollToPosition(chatMessages.size() - 1);

        getAIResponse(userMsg);
    }

    private void getAIResponse(String input) {
        if (chatSession == null) {
            addBotMessage("AI not initialized. Reconnecting...");
            initializeGemini();
            if (chatSession == null) return;
        }

        addBotMessage("Thinking...");
        int thinkingIdx = chatMessages.size() - 1;

        Content userContent = new Content("user", Collections.singletonList(new TextPart(input)));
        ListenableFuture<GenerateContentResponse> response = chatSession.sendMessage(userContent);

        Executor executor = Executors.newSingleThreadExecutor();
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    if (thinkingIdx < chatMessages.size()) {
                        chatMessages.remove(thinkingIdx);
                        chatAdapter.notifyItemRemoved(thinkingIdx);
                    }
                    String resultText = result.getText();
                    addBotMessage(resultText != null ? resultText : "I'm sorry, I couldn't process that. Try asking about Syllabus or Faculty!");
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("GeminiError", "Chat failed", t);
                runOnUiThread(() -> {
                    // 1. Remove the "Thinking..." bubble
                    if (thinkingIdx < chatMessages.size()) {
                        chatMessages.remove(thinkingIdx);
                        chatAdapter.notifyItemRemoved(thinkingIdx);
                    }

                    String errorMsg = t.getMessage() != null ? t.getMessage() : "";
                    String lowerInput = input.toLowerCase();
                    String fallbackMessage;

                    // 2. ONLY trigger if it's specifically a server/connection error
                    boolean isServerErrorOrOffline = errorMsg.contains("503") ||
                            errorMsg.contains("high demand") ||
                            errorMsg.contains("429") ||
                            errorMsg.contains("quota") ||
                            errorMsg.contains("RESOURCE_EXHAUSTED") ||
                            errorMsg.contains("Unable to resolve host") || // Android's "no internet" error
                            errorMsg.contains("timeout");

                    if (isServerErrorOrOffline) {
                        // 3. Mini Offline Chatbot Logic (Runs only on server/network failure)
                        if (lowerInput.contains("faculty") || lowerInput.contains("professor") || lowerInput.contains("staff")) {
                            fallbackMessage = "My AI servers are a bit busy, but I know this one! 💡\nTo see Faculty details, go to the Home Screen, tap 'Faculty Directory', and select your branch.";
                        }
                        else if (lowerInput.contains("syllabus") || lowerInput.contains("calendar") || lowerInput.contains("academic")) {
                            fallbackMessage = "I'm offline right now, but I can still help! 📚\nYou can find the Syllabus and Academic Calendar inside the 'Academic Resources' section on the Home Screen.";
                        }
                        else if (lowerInput.contains("result") || lowerInput.contains("marks") || lowerInput.contains("grades")) {
                            fallbackMessage = "Servers are busy! 📊\nTo view your results, please go to 'Launch Portals' on the Home Screen, log in with your Student ID, and check your dashboard.";
                        }
                        else if (lowerInput.contains("campus") || lowerInput.contains("facilities") || lowerInput.contains("canteen")) {
                            fallbackMessage = "Having network trouble, but I've got your back! 🏫\nYou can explore campus facilities by tapping the 'Explore Campus' card on the Home Screen.";
                        }
                        else {
                            // They asked something we haven't hardcoded, so give the standard busy message
                            fallbackMessage = "The AI servers are super busy right now! 🏃‍♂️💨 Please give me a minute to catch my breath, or ask me a specific app navigation question like 'Faculty' or 'Syllabus' and I'll use my offline memory!";
                        }
                    } else {
                        // 4. It's some other unexpected error, don't use the fallback, just report the error
                        fallbackMessage = "Error: " + (errorMsg.isEmpty() ? "Unknown connection error." : errorMsg);
                    }

                    // 5. Display the message to the user
                    addBotMessage(fallbackMessage);
                });
            }
        }, executor);
    }

    private void addBotMessage(String text) {
        chatMessages.add(new ChatMessage(text, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChatHistory.smoothScrollToPosition(chatMessages.size() - 1);
    }
}