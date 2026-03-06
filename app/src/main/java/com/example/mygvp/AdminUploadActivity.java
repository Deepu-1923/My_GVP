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

    private final String[] YEARS = {"1st Year", "2nd Year", "3rd Year", "4th Year"};
    private final String[] SEMS = {"Semester 1", "Semester 2"};

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

        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, YEARS));
        spinnerSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, SEMS));
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

        // Reverted to correct preset name: mygvp_preset
        MediaManager.get().upload(selectedFileUri)
                .unsigned("mygvp_preset")
                .option("resource_type", "auto") 
                .callback(new UploadCallback() {
                    @Override public void onSuccess(String requestId, Map resultData) {
                        saveToFirebase((String) resultData.get("secure_url"));
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        btnUpload.setEnabled(true);
                        btnUpload.setText("Upload Failed");
                        
                        String errorMsg = error.getDescription();
                        Log.e("CloudinaryError", "Error: " + errorMsg + " Code: " + error.getCode());
                        
                        if (errorMsg.contains("401") || errorMsg.contains("API key")) {
                            Toast.makeText(AdminUploadActivity.this, "Cloudinary Auth Error: Ensure 'mygvp_preset' is set to UNSIGNED in Dashboard and Cloud Name is correct.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdminUploadActivity.this, "Cloudinary Error: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirebase(String url) {
        btnUpload.setText("Syncing Database...");
        
        if (rgType.getCheckedRadioButtonId() == R.id.rbSyllabus) {
            String year = spinnerYear.getText().toString();
            String sem = spinnerSem.getText().toString();
            String branch = spinnerBranch.getText().toString();
            
            if(branch.isEmpty()) {
                Toast.makeText(this, "Select Branch", Toast.LENGTH_SHORT).show();
                btnUpload.setEnabled(true);
                return;
            }

            if (branch.equals("CSE") || branch.equals("CSM") || branch.equals("ECE")) {
                for (String y : YEARS) {
                    for (String s : SEMS) {
                        dbRef.child("syllabus").child(y).child(s).child(branch).setValue(url);
                    }
                }
            } else if (branch.equals("CIVIL") || branch.equals("MECH")) {
                if (year.isEmpty()) {
                    Toast.makeText(this, "Select Year", Toast.LENGTH_SHORT).show();
                    btnUpload.setEnabled(true);
                    return;
                }
                for (String s : SEMS) {
                    dbRef.child("syllabus").child(year).child(s).child(branch).setValue(url);
                }
            } else {
                dbRef.child("syllabus").child(year).child(sem).child(branch).setValue(url);
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
        Toast.makeText(this, "Success! Syllabus Linked.", Toast.LENGTH_LONG).show();
        btnUpload.setEnabled(true);
        btnUpload.setText("Upload Another");
        selectedFileUri = null;
        tvStatus.setText("No file selected");
    }
}
