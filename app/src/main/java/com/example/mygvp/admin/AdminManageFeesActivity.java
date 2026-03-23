package com.example.mygvp.admin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AdminManageFeesActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerBatch, spinnerStream, spinnerYear, spinnerSem;
    private MaterialCardView cardUploadExcel;
    private TextView tvExcelFileName;
    private MaterialButton btnSubmit;
    private Uri excelUri;
    private static final int PICK_EXCEL_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_fees);

        initViews();
        setupSpinners();

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        cardUploadExcel.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Excel File"), PICK_EXCEL_REQUEST);
        });

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadExcelDataToFirebase();
            }
        });
    }

    private void initViews() {
        spinnerBatch = findViewById(R.id.spinnerBatch);
        spinnerStream = findViewById(R.id.spinnerStream);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSem = findViewById(R.id.spinnerSem);
        cardUploadExcel = findViewById(R.id.cardUploadExcel);
        tvExcelFileName = findViewById(R.id.tvExcelFileName);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupSpinners() {
        String[] batches = {"2021-2025", "2022-2026", "2023-2027", "2024-2028", "2025-2029", "2026-2030"};
        String[] streams = {"CSE", "CSM", "ECE", "MECH", "CIVIL"};
        String[] years = {"1", "2", "3", "4"};
        String[] sems = {"1", "2"};

        spinnerBatch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, batches));
        spinnerStream.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, streams));
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));
        spinnerSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sems));
    }

    private boolean validateInputs() {
        if (spinnerBatch.getText().toString().isEmpty() ||
            spinnerStream.getText().toString().isEmpty() ||
            spinnerYear.getText().toString().isEmpty() ||
            spinnerSem.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select all details", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (excelUri == null) {
            Toast.makeText(this, "Please upload an excel file", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_EXCEL_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            excelUri = data.getData();
            tvExcelFileName.setText("File Selected: " + excelUri.getLastPathSegment());
        }
    }

    private void uploadExcelDataToFirebase() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating student fees...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String batch = spinnerBatch.getText().toString().trim();
        String stream = spinnerStream.getText().toString().trim();
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students").child(stream).child(batch);

        try {
            InputStream inputStream = getContentResolver().openInputStream(excelUri);
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String rollNo = getCellValueAsString(row.getCell(0));
                String name = getCellValueAsString(row.getCell(1));
                double totalFee = getCellValueAsDouble(row.getCell(2));
                double paidAmount = getCellValueAsDouble(row.getCell(3));
                double dueAmount = totalFee - paidAmount;

                if (rollNo.isEmpty()) continue;

                // Create update map to preserve existing data (like email/password)
                Map<String, Object> studentUpdates = new HashMap<>();
                studentUpdates.put("name", name);
                studentUpdates.put("totalFee", totalFee);
                studentUpdates.put("paidAmount", paidAmount);
                studentUpdates.put("dueAmount", dueAmount);
                studentUpdates.put("rollNumber", rollNo);
                
                // Perform surgical update
                studentsRef.child(rollNo).updateChildren(studentUpdates);
            }

            workbook.close();
            progressDialog.dismiss();
            Toast.makeText(this, "Fees updated successfully. Login data preserved.", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(this, AdminViewFeesActivity.class);
            intent.putExtra("PRE_BATCH", batch);
            intent.putExtra("PRE_STREAM", stream);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        try {
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                return Double.parseDouble(cell.getStringCellValue());
            }
        } catch (Exception e) {
            return 0.0;
        }
        return 0.0;
    }
}
