package com.example.mygvp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.mygvp.student.Achievement;
import com.example.mygvp.student.AchievementAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UploadAchievementActivity extends AppCompatActivity {

    private Spinner spinnerType, spinnerDomain;
    private Button btnSelectFile, btnSubmit;
    private TextView tvFileStatus;
    private Uri selectedFileUri;
    private static final int PICK_FILE_REQUEST = 1;
    private String loggedInRollNo;

    private RecyclerView rvAchievements;
    private LinearLayout layoutEmptyState;
    private ScrollView layoutUploadForm;
    private ExtendedFloatingActionButton fabAdd;
    private DatabaseReference dbRef;
    
    private AchievementAdapter adapter;
    private List<Achievement> achievementList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_achievement);

        // 1. Initialize Views
        rvAchievements = findViewById(R.id.rv_achievements);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutUploadForm = findViewById(R.id.layout_upload_form);
        fabAdd = findViewById(R.id.fab_add_achievement); // Fixed cast
        spinnerType = findViewById(R.id.spinner_type);
        spinnerDomain = findViewById(R.id.spinner_domain);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnSubmit = findViewById(R.id.btn_submit_achievement);
        tvFileStatus = findViewById(R.id.tv_file_status);

        // 2. Data/Session
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        loggedInRollNo = prefs.getString("LOGGED_IN_ROLL_NO", "Unknown");
        dbRef = FirebaseDatabase.getInstance().getReference("achievements");

        setupRecyclerView();
        setupSpinners();
        fetchAchievements();

        // 3. Button Listeners
        btnSelectFile.setOnClickListener(v -> openFilePicker());
        btnSubmit.setOnClickListener(v -> uploadToCloudinary());

        fabAdd.setOnClickListener(v -> {
            layoutEmptyState.setVisibility(View.GONE);
            rvAchievements.setVisibility(View.GONE);
            layoutUploadForm.setVisibility(View.VISIBLE);
            fabAdd.hide();
        });
    }

    private void setupRecyclerView() {
        rvAchievements.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AchievementAdapter(this, achievementList);
        rvAchievements.setAdapter(adapter);
    }

    private void fetchAchievements() {
        dbRef.orderByChild("rollNo").equalTo(loggedInRollNo)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        achievementList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            try {
                                Achievement achievement = dataSnapshot.getValue(Achievement.class);
                                if (achievement != null) {
                                    achievementList.add(achievement);
                                }
                            } catch (Exception e) {
                                // Skip malformed data
                            }
                        }
                        
                        if (achievementList.isEmpty()) {
                            layoutEmptyState.setVisibility(View.VISIBLE);
                            rvAchievements.setVisibility(View.GONE);
                        } else {
                            layoutEmptyState.setVisibility(View.GONE);
                            rvAchievements.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                        layoutUploadForm.setVisibility(View.GONE);
                        fabAdd.show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupSpinners() {
        String[] types = {"Hackathon", "Certification Course", "Workshop", "Other"};
        String[] domains = {"AI/ML", "Web Dev", "App Dev", "Cyber Security"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(typeAdapter);
        ArrayAdapter<String> domainAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, domains);
        spinnerDomain.setAdapter(domainAdapter);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select Document"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            tvFileStatus.setText("File Selected");
        }
    }

    private void uploadToCloudinary() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
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
                        btnSubmit.setText("Upload Now");
                        Toast.makeText(UploadAchievementActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirebase(String url) {
        String id = dbRef.push().getKey();
        if (id == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("rollNo", loggedInRollNo);
        data.put("type", spinnerType.getSelectedItem().toString());
        data.put("domain", spinnerDomain.getSelectedItem().toString());
        data.put("fileUrl", url);
        data.put("date", new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));

        dbRef.child(id).setValue(data).addOnSuccessListener(aVoid -> {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Upload Now");
            Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
            selectedFileUri = null;
            tvFileStatus.setText("No file selected");
        });
    }
}
