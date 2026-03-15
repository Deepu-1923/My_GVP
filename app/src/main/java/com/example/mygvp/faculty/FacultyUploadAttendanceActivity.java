package com.example.mygvp.faculty;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
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

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FacultyUploadAttendanceActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerBranch, spinnerYear, spinnerSemester;
    private TextInputEditText etBatch;
    private View btnSelectFile;
    private MaterialButton btnUpload;
    private TextView tvFileName;
    private ProgressBar progressBar;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_upload_attendance);

        spinnerBranch = findViewById(R.id.spinnerBranch);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        etBatch = findViewById(R.id.etBatch);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUpload = findViewById(R.id.btnUpload);
        tvFileName = findViewById(R.id.tvFileName);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

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

        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                Workbook workbook = new XSSFWorkbook(inputStream);
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                
                // Read headers to get month names from columns 4 to 9
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new Exception("Excel sheet is empty or invalid");
                
                String[] monthNamesFromSheet = new String[6];
                for (int i = 0; i < 6; i++) {
                    String name = formatter.formatCellValue(headerRow.getCell(4 + i), evaluator).trim();
                    if (name.isEmpty()) name = "Month " + (i + 1);
                    // Add index prefix (1_, 2_...) to maintain month order in Firebase alphabetical sorting
                    monthNamesFromSheet[i] = (i + 1) + "_" + name;
                }

                Iterator<Row> rowIterator = sheet.iterator();
                if (rowIterator.hasNext()) rowIterator.next(); // Skip header row

                DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                        .getReference("attendance")
                        .child(branch)
                        .child(batch)
                        .child(year)
                        .child(sem);

                Pattern digitPattern = Pattern.compile("\\d+");

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    
                    String rollNo = formatter.formatCellValue(row.getCell(0), evaluator).trim();
                    if (rollNo.isEmpty()) continue;

                    Map<String, Object> attendanceData = new HashMap<>();
                    attendanceData.put("name", formatter.formatCellValue(row.getCell(1), evaluator).trim());
                    
                    Map<String, Object> months = new HashMap<>();
                    int totalPresentSum = 0;
                    int totalDaysSum = 0;

                    for (int i = 0; i < 6; i++) {
                        Cell cell = row.getCell(4 + i);
                        int p = 0, t = 0;
                        
                        if (cell != null) {
                            String attendanceStr = formatter.formatCellValue(cell, evaluator).trim();
                            // Fallback if POI returns empty but cell has content
                            if (attendanceStr.isEmpty()) attendanceStr = cell.toString().trim();
                            
                            Matcher matcher = digitPattern.matcher(attendanceStr);
                            if (matcher.find()) {
                                p = Integer.parseInt(matcher.group());
                                if (matcher.find()) {
                                    t = Integer.parseInt(matcher.group());
                                }
                            }
                        }
                        
                        Map<String, Object> mData = new HashMap<>();
                        mData.put("present", p);
                        mData.put("total", t);
                        months.put(monthNamesFromSheet[i], mData);
                        
                        totalPresentSum += p;
                        totalDaysSum += t;
                    }
                    
                    attendanceData.put("months", months);
                    attendanceData.put("present_days", totalPresentSum);
                    attendanceData.put("total_days", totalDaysSum);
                    
                    double percentage = (totalDaysSum > 0) ? (totalPresentSum * 100.0 / totalDaysSum) : 0.0;
                    attendanceData.put("percentage", Math.round(percentage * 10.0) / 10.0);
                    attendanceData.put("holidays", 0);

                    attendanceRef.child(rollNo).setValue(attendanceData);
                    
                    // Update student node summary
                    FirebaseDatabase.getInstance().getReference("students")
                            .child(branch).child(batch).child(rollNo)
                            .child("attendance_percentage").setValue(Math.round(percentage * 10.0) / 10.0);
                }

                workbook.close();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(this, "Success! Re-upload complete.", Toast.LENGTH_SHORT).show();
                    finish();
                });

            } catch (Exception e) {
                Log.e("AttendanceUpload", "Processing failed", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
