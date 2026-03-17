package com.example.mygvp.faculty;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FacultyUploadResultsActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerBranch, spinnerYear, spinnerSemester;
    private TextInputEditText etBatch;
    private MaterialButton btnSelectFile, btnUpload;
    private TextView tvFileName;
    private Uri fileUri;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

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
                        tvFileName.setText("Selected: " + fileUri.getLastPathSegment());
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
        String[] branches = {"CSE", "ECE", "CSM", "CIVIL", "MECH"};
        spinnerBranch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, branches));
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"1", "2", "3", "4"}));
        spinnerSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"1", "2"}));
    }

    private void processAndUpload() {
        final String branch = spinnerBranch.getText().toString();
        final String batch = etBatch.getText().toString().trim();
        final String year = spinnerYear.getText().toString();
        final String sem = spinnerSemester.getText().toString();

        if (branch.isEmpty() || batch.isEmpty() || year.isEmpty() || sem.isEmpty() || fileUri == null) {
            Toast.makeText(this, "Fill all details and select .xlsx file", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpload.setEnabled(false);
        btnUpload.setText("Processing...");

        DatabaseReference baseRef = FirebaseDatabase.getInstance().getReference("students")
                .child(branch).child(batch);

        baseRef.get().addOnSuccessListener(snapshot -> {
            executorService.execute(() -> {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(fileUri);
                    Workbook workbook = new XSSFWorkbook(inputStream);
                    Sheet sheet = workbook.getSheetAt(0);
                    DataFormatter dataFormatter = new DataFormatter();

                    Row headerRow = sheet.getRow(0);
                    int colCount = headerRow.getLastCellNum();

                    Map<String, Object> megaUpdateMap = new HashMap<>();
                    int studentCount = 0;

                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;

                        String rollNo = getCellValueSafe(row.getCell(0), dataFormatter).trim();
                        if (rollNo.isEmpty()) continue;

                        Map<String, Object> semDataMap = new HashMap<>();
                        double currentSgpa = 0.0;

                        for (int j = 1; j < colCount; j++) {
                            String header = getCellValueSafe(headerRow.getCell(j), dataFormatter).trim();
                            String value = getCellValueSafe(row.getCell(j), dataFormatter).trim();

                            if (header.toUpperCase().contains("SGPA")) {
                                currentSgpa = parseDoubleSafe(value);
                            } else if (value.startsWith("(") && value.endsWith(")")) {
                                String clean = value.substring(1, value.length() - 1);
                                String[] parts = clean.split(",");
                                if (parts.length == 3) {
                                    Map<String, String> subData = new HashMap<>();
                                    subData.put("credits", parts[0].trim());
                                    subData.put("grades", parts[1].trim()); // Match "grades"
                                    subData.put("points", parts[2].trim());
                                    semDataMap.put(header, subData);
                                }
                            }
                        }

                        // Calculate Year CGPA (Average of Sem 1 & Sem 2)
                        String otherSem = sem.equals("1") ? "2" : "1";
                        DataSnapshot otherSemSnap = snapshot.child(rollNo).child("results").child(year).child(otherSem).child("sgpa");
                        
                        double yearCgpa = currentSgpa;
                        if (otherSemSnap.exists() && otherSemSnap.getValue() != null) {
                            try {
                                double otherSgpa = Double.parseDouble(otherSemSnap.getValue().toString());
                                if (otherSgpa > 0) {
                                    yearCgpa = (currentSgpa + otherSgpa) / 2.0;
                                }
                            } catch (Exception ignored) {}
                        }

                        // Save strictly inside the semester folder
                        semDataMap.put("sgpa", Math.round(currentSgpa * 100.0) / 100.0);
                        semDataMap.put("cgpa", Math.round(yearCgpa * 100.0) / 100.0);

                        // CLEANUP: Remove any extra cgpa at the year level if it was created before
                        megaUpdateMap.put(rollNo + "/results/" + year + "/cgpa", null);

                        megaUpdateMap.put(rollNo + "/results/" + year + "/" + sem, semDataMap);
                        studentCount++;
                    }

                    final int finalStudentCount = studentCount;
                    baseRef.updateChildren(megaUpdateMap).addOnSuccessListener(aVoid -> {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(FacultyUploadResultsActivity.this, "Uploaded " + finalStudentCount + " students successfully!", Toast.LENGTH_LONG).show();
                            resetButton();
                        });
                    });

                    workbook.close();
                    inputStream.close();

                } catch (Exception e) {
                    handleError(e.getMessage());
                }
            });
        }).addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private double parseDoubleSafe(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.trim().replace(",", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void handleError(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
            resetButton();
        });
    }

    private void resetButton() {
        btnUpload.setEnabled(true);
        btnUpload.setText("Upload Results");
    }

    private String getCellValueSafe(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                switch (cell.getCachedFormulaResultType()) {
                    case NUMERIC:
                        double num = cell.getNumericCellValue();
                        return (num == (long) num) ? String.valueOf((long) num) : String.valueOf(num);
                    case STRING:
                        return cell.getStringCellValue();
                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanCellValue());
                    default:
                        return "";
                }
            } catch (Exception e) {
                return "";
            }
        }
        return formatter.formatCellValue(cell);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
