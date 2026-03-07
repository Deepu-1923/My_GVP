package com.example.mygvp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AdminUploadActivity extends AppCompatActivity {

    private RadioGroup rgType;
    private RadioButton rbSyllabus, rbCalendar;
    private LinearLayout llSyllabus;
    private TextInputLayout tilCalendarYear;
    private TextInputEditText etCalendarYear;
    private AutoCompleteTextView spinnerYear, spinnerSem, spinnerBranch;
    private Button btnSelect, btnUpload;
    private TextView tvStatus;
    
    private Uri selectedFileUri;
    private static final int PICK_FILE = 100;
    private DatabaseReference dbRef;

    // Added "All" options for smarter consolidated uploads
    private final String[] YEARS_OPTIONS = {"All Years", "1st Year", "2nd Year", "3rd Year", "4th Year"};
    private final String[] SEMS_OPTIONS = {"All Semesters", "Semester 1", "Semester 2"};
    
    // Base values for iteration
    private final String[] ACTUAL_YEARS = {"1st Year", "2nd Year", "3rd Year", "4th Year"};
    private final String[] ACTUAL_SEMS = {"Semester 1", "Semester 2"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_upload);

        dbRef = FirebaseDatabase.getInstance().getReference();

        rgType = findViewById(R.id.rgUploadType);
        llSyllabus = findViewById(R.id.llSyllabusFields);
        tilCalendarYear = findViewById(R.id.tilCalendarYear);
        etCalendarYear = findViewById(R.id.etCalendarYear);
        spinnerYear = findViewById(R.id.uploadSpinnerYear);
        spinnerSem = findViewById(R.id.uploadSpinnerSem);
        spinnerBranch = findViewById(R.id.uploadSpinnerBranch);
        btnSelect = findViewById(R.id.btnSelectUploadFile);
        btnUpload = findViewById(R.id.btnStartUpload);
        tvStatus = findViewById(R.id.tvUploadStatus);

        setupSpinners();

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSyllabus) {
                llSyllabus.setVisibility(View.VISIBLE);
                tilCalendarYear.setVisibility(View.GONE);
            } else {
                llSyllabus.setVisibility(View.GONE);
                tilCalendarYear.setVisibility(View.VISIBLE);
            }
        });

        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(intent, PICK_FILE);
        });

        btnUpload.setOnClickListener(v -> upload());
    }

    private void setupSpinners() {
        String[] branches = {"CIVIL", "CSE", "CSM", "ECE", "MECH"};

        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, YEARS_OPTIONS));
        spinnerSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, SEMS_OPTIONS));
        spinnerBranch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, branches));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            tvStatus.setText("PDF Selected: " + selectedFileUri.getLastPathSegment());
        }
    }

    private void upload() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Select a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpload.setEnabled(false);
        btnUpload.setText("Uploading...");

        // Using your new preset: sylbs_caldr
        MediaManager.get().upload(selectedFileUri)
                .unsigned("sylbs_caldr")
                .option("resource_type", "auto") 
                .callback(new UploadCallback() {
                    @Override public void onSuccess(String requestId, Map resultData) {
                        saveToFirebase((String) resultData.get("secure_url"));
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        btnUpload.setEnabled(true);
                        btnUpload.setText("Upload Failed");
                        Log.e("CloudinaryError", "Error: " + error.getDescription());
                        Toast.makeText(AdminUploadActivity.this, "Upload Error: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirebase(String url) {
        btnUpload.setText("Syncing Database...");
        
        if (rgType.getCheckedRadioButtonId() == R.id.rbSyllabus) {
            String selectedYear = spinnerYear.getText().toString();
            String selectedSem = spinnerSem.getText().toString();
            String branch = spinnerBranch.getText().toString();
            
            if(branch.isEmpty()) {
                Toast.makeText(this, "Select Branch", Toast.LENGTH_SHORT).show();
                btnUpload.setEnabled(true);
                return;
            }

            // SMART LOGIC: Handle "All" selections
            List<String> yearsToUpload = new ArrayList<>();
            if (selectedYear.equals("All Years")) {
                yearsToUpload.addAll(Arrays.asList(ACTUAL_YEARS));
            } else {
                yearsToUpload.add(selectedYear);
            }

            List<String> semsToUpload = new ArrayList<>();
            if (selectedSem.equals("All Semesters")) {
                semsToUpload.addAll(Arrays.asList(ACTUAL_SEMS));
            } else {
                semsToUpload.add(selectedSem);
            }

            // Sync all selected nodes to the SAME URL (Cloudinary URL is reused)
            for (String y : yearsToUpload) {
                if (y.isEmpty()) continue;
                for (String s : semsToUpload) {
                    if (s.isEmpty()) continue;
                    dbRef.child("syllabus").child(y).child(s).child(branch).setValue(url);
                }
            }
            finishUpload();
            
        } else {
            String calYear = etCalendarYear.getText().toString();
            if(calYear.isEmpty()) {
                Toast.makeText(this, "Enter calendar year", Toast.LENGTH_SHORT).show();
                btnUpload.setEnabled(true);
                return;
            }
            dbRef.child("academic_calendar").child(calYear).setValue(url)
                .addOnSuccessListener(aVoid -> finishUpload());
        }
    }

    private void finishUpload() {
        Toast.makeText(this, "Success! File Linked.", Toast.LENGTH_LONG).show();
        btnUpload.setEnabled(true);
        btnUpload.setText("Upload Another");
        selectedFileUri = null;
        tvStatus.setText("No file selected");
    }
}
