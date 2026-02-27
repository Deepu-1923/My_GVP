package com.example.mygvp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UploadAchievementActivity extends AppCompatActivity {

    private Spinner spinnerType, spinnerDomain;
    private Button btnSelectFile, btnSubmit;
    private TextView tvFileStatus;
    private Uri selectedFileUri;
    private static final int PICK_FILE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_achievement);

        // Initialize Views
        spinnerType = findViewById(R.id.spinner_type);
        spinnerDomain = findViewById(R.id.spinner_domain);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnSubmit = findViewById(R.id.btn_submit_achievement);
        tvFileStatus = findViewById(R.id.tv_file_status);

        // 1. Setup Spinners
        setupSpinners();

        // 2. File Picker Click
        btnSelectFile.setOnClickListener(v -> openFilePicker());

        // 3. Submit Click
        btnSubmit.setOnClickListener(v -> uploadData());
    }

    private void setupSpinners() {
        String[] types = {"Hackathon", "Certification Course", "Course Completion", "Workshop", "Other"};
        String[] domains = {"Data Science", "Cloud Computing", "AI/ML", "Web Development", "App Development", "Cyber Security"};

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(typeAdapter);

        ArrayAdapter<String> domainAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, domains);
        spinnerDomain.setAdapter(domainAdapter);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // This allows both Images and PDFs
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Select Document"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                tvFileStatus.setText("File Selected: " + selectedFileUri.getLastPathSegment());
            }
        }
    }

    private void uploadData() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = spinnerType.getSelectedItem().toString();
        String domain = spinnerDomain.getSelectedItem().toString();
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        // TODO: Next step - integrate Cloudinary upload here
        Toast.makeText(this, "Uploading " + type + " in " + domain + " on " + date, Toast.LENGTH_LONG).show();
    }
}
