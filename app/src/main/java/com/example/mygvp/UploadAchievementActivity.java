package com.example.mygvp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.mygvp.student.Achievement;
import com.example.mygvp.student.AchievementAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UploadAchievementActivity extends AppCompatActivity {

    private Spinner spinnerType, spinnerDomain;
    private Button btnSelectFile, btnSubmit, btnCancel;
    private TextView tvFileStatus, tvFormTitle, tvEmptyMsg;
    private TextInputEditText etCourseName, etSearch;
    private TextInputLayout layoutSearch;
    private Uri selectedFileUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    private String loggedInUserId, loggedInUserName, userType;
    private RecyclerView rvAchievements;
    private LinearLayout layoutEmptyState;
    private ScrollView layoutUploadForm;
    private ExtendedFloatingActionButton fabAdd;
    private DatabaseReference dbRef;

    private AchievementAdapter adapter;
    private List<Achievement> allAchievements = new ArrayList<>();
    private List<Achievement> displayedAchievements = new ArrayList<>();
    private Achievement editingAchievement = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_achievement);

        // 1. Initialize Views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvAchievements = findViewById(R.id.rv_achievements);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyMsg = findViewById(R.id.tv_empty_msg);
        layoutUploadForm = findViewById(R.id.layout_upload_form);
        fabAdd = findViewById(R.id.fab_add_achievement);
        spinnerType = findViewById(R.id.spinner_type);
        spinnerDomain = findViewById(R.id.spinner_domain);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnSubmit = findViewById(R.id.btn_submit_achievement);
        btnCancel = findViewById(R.id.btn_cancel);
        tvFileStatus = findViewById(R.id.tv_file_status);
        tvFormTitle = findViewById(R.id.tv_form_title);
        etCourseName = findViewById(R.id.et_course_name);
        etSearch = findViewById(R.id.et_search);
        layoutSearch = findViewById(R.id.layout_search);

        // 2. Session Info
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        userType = prefs.getString("USER_TYPE", "STUDENT");
        if ("FACULTY".equals(userType)) {
            loggedInUserId = prefs.getString("LOGGED_IN_FACULTY_ID", "Unknown");
            loggedInUserName = prefs.getString("LOGGED_IN_FACULTY_NAME", "Faculty");
            layoutSearch.setHint("Search by Course or Domain");
        } else {
            loggedInUserId = prefs.getString("LOGGED_IN_ROLL_NO", "Unknown");
            loggedInUserName = prefs.getString("LOGGED_IN_NAME", "Student");
            layoutSearch.setHint("Search your achievements");
        }
        
        layoutSearch.setVisibility(View.VISIBLE);
        dbRef = FirebaseDatabase.getInstance().getReference("achievements");

        setupRecyclerView();
        setupSpinners();
        fetchAchievements();

        // 3. Listeners
        btnSelectFile.setOnClickListener(v -> openImagePicker());
        btnSubmit.setOnClickListener(v -> {
            if (editingAchievement != null && selectedFileUri == null) {
                saveToFirebase(editingAchievement.getFileUrl());
            } else {
                uploadToCloudinary();
            }
        });

        btnCancel.setOnClickListener(v -> showList());

        fabAdd.setOnClickListener(v -> {
            editingAchievement = null;
            tvFormTitle.setText("New Achievement");
            etCourseName.setText("");
            selectedFileUri = null;
            tvFileStatus.setText("No file selected");
            showForm();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAchievements(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        rvAchievements.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AchievementAdapter(this, displayedAchievements, true, loggedInUserId != null ? loggedInUserId : "Unknown");
        rvAchievements.setAdapter(adapter);
    }

    private void fetchAchievements() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allAchievements.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Achievement a = ds.getValue(Achievement.class);
                    if (a != null && a.getUploaderId() != null) {
                        if ("FACULTY".equals(userType) || a.getUploaderId().equals(loggedInUserId)) {
                            allAchievements.add(a);
                        }
                    }
                }
                filterAchievements(etSearch.getText().toString());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterAchievements(String query) {
        displayedAchievements.clear();
        String lowerQuery = query.toLowerCase().trim();

        if ("FACULTY".equals(userType) && lowerQuery.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (tvEmptyMsg != null) tvEmptyMsg.setText("Search for a course or domain to view certifications");
            rvAchievements.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
            return;
        }

        for (Achievement a : allAchievements) {
            boolean matches = lowerQuery.isEmpty() ||
                    (a.getCourseName() != null && a.getCourseName().toLowerCase().contains(lowerQuery)) ||
                    (a.getDomain() != null && a.getDomain().toLowerCase().contains(lowerQuery));
            
            if (matches) {
                displayedAchievements.add(a);
            }
        }

        if (displayedAchievements.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (tvEmptyMsg != null) {
                if (lowerQuery.isEmpty()) tvEmptyMsg.setText("No Achievements Yet");
                else tvEmptyMsg.setText("No results found for '" + lowerQuery + "'");
            }
            rvAchievements.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvAchievements.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void setupSpinners() {
        String[] types = {"Certification", "Hackathon", "Workshop", "Award", "Other"};
        String[] domains = {"Web Development", "App Development", "AI/ML", "Cyber Security", "Data Science", "Other", "Cloud Computing"};
        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
        spinnerDomain.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, domains));
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            tvFileStatus.setText("Image Selected");
            runTextRecognition(selectedFileUri);
        }
    }

    private void runTextRecognition(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Processing OCR...");

            recognizer.process(image)
                    .addOnSuccessListener(texts -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Save Achievement");
                        extractCourseName(texts);
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Save Achievement");
                        Toast.makeText(this, "OCR failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractCourseName(Text texts) {
        List<Text.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.isEmpty()) return;

        String bestMatch = "";
        float maxLineHeight = 0;

        for (Text.TextBlock block : blocks) {
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText().trim();
                if (line.getBoundingBox() != null) {
                    float height = line.getBoundingBox().height();
                    if (height > maxLineHeight && lineText.length() > 3 && lineText.length() < 60) {
                        if (!isLikelyNoise(lineText)) {
                            maxLineHeight = height;
                            bestMatch = lineText;
                        }
                    }
                }
            }
        }

        if (bestMatch.isEmpty() || isGenericLabel(bestMatch)) {
            String fullText = texts.getText();
            String[] lines = fullText.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String l = lines[i].toLowerCase();
                if (l.contains("successfully completed") || l.contains("course in") || l.contains("specializing in")) {
                    if (i + 1 < lines.length) {
                        bestMatch = lines[i+1].trim();
                        break;
                    }
                }
            }
        }

        if (bestMatch.isEmpty() && !blocks.isEmpty()) {
            bestMatch = blocks.get(0).getText().split("\n")[0];
        }

        etCourseName.setText(cleanCourseName(bestMatch));
    }

    private boolean isLikelyNoise(String text) {
        String t = text.toLowerCase();
        return t.contains("date") || t.contains("signature") || t.contains("id:") || 
               t.matches(".*\\d{2}/\\d{2}/\\d{4}.*") || t.length() < 4;
    }

    private boolean isGenericLabel(String text) {
        String t = text.toLowerCase();
        return t.equals("certificate") || t.equals("completion") || t.equals("achievement") || t.equals("appreciation");
    }

    private String cleanCourseName(String name) {
        return name.replaceAll("(?i)^(this is to certify that|presented to|for|of|the course)\\s+", "")
                   .replaceAll("(?i)\\s+(has successfully|is awarded)$", "")
                   .trim();
    }

    private void uploadToCloudinary() {
        if (selectedFileUri == null && editingAchievement == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading...");

        MediaManager.get().upload(selectedFileUri)
                .unsigned("mygvp_preset")
                .callback(new UploadCallback() {
                    @Override public void onSuccess(String requestId, Map resultData) {
                        saveToFirebase((String) resultData.get("secure_url"));
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Save Achievement");
                        Toast.makeText(UploadAchievementActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirebase(String url) {
        String id = editingAchievement != null ? editingAchievement.getId() : dbRef.push().getKey();
        if (id == null) return;

        String uploaderId = editingAchievement != null ? editingAchievement.getUploaderId() : (loggedInUserId != null ? loggedInUserId : "Unknown");
        String uploaderName = editingAchievement != null ? editingAchievement.getUploaderName() : (loggedInUserName != null ? loggedInUserName : "User");
        String uploaderType = editingAchievement != null ? editingAchievement.getUploaderType() : userType;
        String date = editingAchievement != null ? editingAchievement.getDate() : new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        Achievement a = new Achievement(
                id,
                uploaderId,
                uploaderName,
                uploaderType,
                spinnerType.getSelectedItem().toString(),
                spinnerDomain.getSelectedItem().toString(),
                url,
                date,
                etCourseName.getText().toString().trim()
        );

        dbRef.child(id).setValue(a).addOnSuccessListener(aVoid -> {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Save Achievement");
            Toast.makeText(this, "Achievement Saved!", Toast.LENGTH_SHORT).show();
            showList();
        });
    }

    public void editAchievement(Achievement a) {
        editingAchievement = a;
        tvFormTitle.setText("Edit Achievement");
        etCourseName.setText(a.getCourseName());
        
        ArrayAdapter<String> typeAdapter = (ArrayAdapter<String>) spinnerType.getAdapter();
        if (typeAdapter != null) {
            int pos = typeAdapter.getPosition(a.getType());
            if (pos >= 0) spinnerType.setSelection(pos);
        }

        ArrayAdapter<String> domainAdapter = (ArrayAdapter<String>) spinnerDomain.getAdapter();
        if (domainAdapter != null) {
            int pos = domainAdapter.getPosition(a.getDomain());
            if (pos >= 0) spinnerDomain.setSelection(pos);
        }
        
        selectedFileUri = null;
        tvFileStatus.setText("Certificate already uploaded (select new to change)");
        showForm();
    }

    private void showForm() {
        layoutUploadForm.setVisibility(View.VISIBLE);
        rvAchievements.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        fabAdd.hide();
    }

    private void showList() {
        layoutUploadForm.setVisibility(View.GONE);
        filterAchievements(etSearch.getText().toString());
        fabAdd.show();
    }
}
