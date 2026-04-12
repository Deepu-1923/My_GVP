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
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.android.material.chip.Chip;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
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

    // ⚠️ NOTE: The error 429 in logs indicates you have exceeded your free API quota.
    // Consider generating a new key at https://aistudio.google.com/ or waiting for the quota to reset.
    private static final String GEMINI_API_KEY = "AIzaSyBSfC_wSjKqfigRQXPCHDAZrznau0gB-QY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        userName = prefs.getString("LOGGED_IN_NAME", "friend");

        setupUI();
        initializeGemini();

        // Initial Greeting
        addBotMessage("Hey " + userName + "! 👋 I'm your MyGVP AI Assistant. I'm here to help you navigate the app. Ask me about Faculty, Syllabus, Results, or anything else!");
        setupSuggestions();
    }

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
            configBuilder.temperature = 0.4f; // Lower temperature for more factual guidance
            GenerationConfig generationConfig = configBuilder.build();

            RequestOptions requestOptions = new RequestOptions(60000L, "v1beta");

            // Define System Instructions properly using the official API
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

            Content systemInstruction = new Content.Builder()
                    .addText(systemContext.toString())
                    .build();

            // Using "gemini-1.5-flash" (2.5-flash does not exist)
            GenerativeModel gm = new GenerativeModel(
                    "gemini-1.5-flash",
                    GEMINI_API_KEY,
                    generationConfig,
                    null,               // safetySettings
                    requestOptions,     // requestOptions
                    null,               // tools
                    null,               // toolConfig
                    systemInstruction   // systemInstruction
            );

            GenerativeModelFutures model = GenerativeModelFutures.from(gm);
            chatSession = model.startChat(); 
            Log.d("GeminiInit", "Chat session initialized.");
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

        Content userContent = new Content.Builder().addText(input).build();
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
                    if (thinkingIdx < chatMessages.size()) {
                        chatMessages.remove(thinkingIdx);
                        chatAdapter.notifyItemRemoved(thinkingIdx);
                    }

                    String errorMsg = t.getMessage() != null ? t.getMessage() : "";

                    // Cleanly handle the Quota Exceeded error (429)
                    if (errorMsg.contains("429") || errorMsg.contains("RESOURCE_EXHAUSTED") || errorMsg.contains("quota")) {
                        addBotMessage("Whoops! I'm getting a little too many requests right now. Please wait a minute and try again. ⏳");
                    } else {
                        addBotMessage("Error: " + (errorMsg.isEmpty() ? "Unknown connection error." : errorMsg));
                    }
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
