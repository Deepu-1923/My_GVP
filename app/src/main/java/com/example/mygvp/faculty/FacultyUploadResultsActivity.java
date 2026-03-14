package com.example.mygvp.faculty;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class FacultyUploadResultsActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerBranch, spinnerYear, spinnerSemester;
    private TextInputEditText etBatch;
    private MaterialButton btnSelectFile, btnUpload;
    private TextView tvFileName;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_upload_results);

        spinnerBranch = findViewById(R.id.spinnerBranch);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        etBatch = findViewById(R.id.etBatch);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUpload = findViewById(R.id.btnUpload);
        tvFileName = findViewById(R.id.tvFileName);

        setupDropdowns();

        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        fileUri = result.getData().getData();
                        tvFileName.setText(fileUri.getLastPathSegment());
                    }
                }
        );

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            filePickerLauncher.launch(intent);
        });

        btnUpload.setOnClickListener(v -> processAndUpload());
    }

    private void setupDropdowns() {
        String[] branches = {"CSE", "ECE", "EEE", "MECH", "CIVIL"};
        spinnerBranch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, branches));

        String[] years = {"1", "2", "3", "4"};
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));

        String[] sems = {"1", "2"};
        spinnerSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sems));
    }

    private void processAndUpload() {
        String branch = spinnerBranch.getText().toString();
        String batch = etBatch.getText().toString().trim();
        String year = spinnerYear.getText().toString();
        String sem = spinnerSemester.getText().toString();

        if (branch.isEmpty() || batch.isEmpty() || year.isEmpty() || sem.isEmpty() || fileUri == null) {
            Toast.makeText(this, "Please fill all fields and select a file", Toast.LENGTH_SHORT).show();
            return;
        }

        // For now, providing a template logic. Actual Excel parsing requires Apache POI
        Toast.makeText(this, "Uploading results for " + branch + " " + batch, Toast.LENGTH_SHORT).show();
        
        // This is where you would iterate through Excel rows and upload:
        // DatabaseReference ref = FirebaseDatabase.getInstance().getReference("students")
        //    .child(branch).child(batch).child(rollNo).child("results").child(year).child(sem);
        
        // Example structure upload
        DatabaseReference testRef = FirebaseDatabase.getInstance().getReference("students")
                .child(branch).child(batch);
        
        Toast.makeText(this, "Processing Excel File...", Toast.LENGTH_LONG).show();
    }
}
