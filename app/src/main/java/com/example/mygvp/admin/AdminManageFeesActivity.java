package com.example.mygvp.admin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminManageFeesActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerBatch, spinnerStream, spinnerYear, spinnerSem;
    private MaterialButton btnViewFees, btnUploadExcel;
    private TextView tvExcelFileName;
    private RecyclerView rvFeesList;
    private StudentFeeAdapter adapter;
    private List<StudentFee> feeList = new ArrayList<>();
    
    private Uri excelUri;
    private static final int PICK_EXCEL_REQUEST = 1;
    private DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_fees);

        studentsRef = FirebaseDatabase.getInstance().getReference("students");

        initViews();
        setupSpinners();
        setupRecyclerView();

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        btnUploadExcel.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Excel File"), PICK_EXCEL_REQUEST);
        });

        btnViewFees.setOnClickListener(v -> loadFeesData());
    }

    private void initViews() {
        spinnerBatch = findViewById(R.id.spinnerBatch);
        spinnerStream = findViewById(R.id.spinnerStream);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSem = findViewById(R.id.spinnerSem);
        btnViewFees = findViewById(R.id.btnViewFees);
        btnUploadExcel = findViewById(R.id.btnUploadExcel);
        tvExcelFileName = findViewById(R.id.tvExcelFileName);
        rvFeesList = findViewById(R.id.rvFeesList);
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

    private void setupRecyclerView() {
        adapter = new StudentFeeAdapter(feeList);
        rvFeesList.setLayoutManager(new LinearLayoutManager(this));
        rvFeesList.setAdapter(adapter);
    }

    private void loadFeesData() {
        String batch = spinnerBatch.getText().toString();
        String stream = spinnerStream.getText().toString();

        if (batch.isEmpty() || stream.isEmpty()) {
            Toast.makeText(this, "Please select Batch and Stream", Toast.LENGTH_SHORT).show();
            return;
        }

        studentsRef.child(stream).child(batch).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                feeList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot studentSnap : snapshot.getChildren()) {
                        String rollNo = studentSnap.getKey();
                        String name = studentSnap.child("name").getValue(String.class);
                        Double totalFee = studentSnap.child("totalFee").getValue(Double.class);
                        Double paidAmount = studentSnap.child("paidAmount").getValue(Double.class);
                        Double dueAmount = studentSnap.child("dueAmount").getValue(Double.class);

                        if (totalFee == null) totalFee = 0.0;
                        if (paidAmount == null) paidAmount = 0.0;
                        if (dueAmount == null) dueAmount = totalFee - paidAmount;

                        feeList.add(new StudentFee(rollNo, name, totalFee, paidAmount, dueAmount));
                    }
                    Collections.sort(feeList, (f1, f2) -> f1.getRollNumber().compareTo(f2.getRollNumber()));
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.notifyDataSetChanged();
                    Toast.makeText(AdminManageFeesActivity.this, "No records found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminManageFeesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_EXCEL_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            excelUri = data.getData();
            tvExcelFileName.setVisibility(View.VISIBLE);
            tvExcelFileName.setText("Selected: " + excelUri.getLastPathSegment());
            uploadExcelDataToFirebase();
        }
    }

    private void uploadExcelDataToFirebase() {
        String batch = spinnerBatch.getText().toString();
        String stream = spinnerStream.getText().toString();

        if (batch.isEmpty() || stream.isEmpty()) {
            Toast.makeText(this, "Please select Batch and Stream first", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading and updating fees...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        DatabaseReference targetRef = studentsRef.child(stream).child(batch);

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

                Map<String, Object> studentUpdates = new HashMap<>();
                studentUpdates.put("name", name);
                studentUpdates.put("totalFee", totalFee);
                studentUpdates.put("paidAmount", paidAmount);
                studentUpdates.put("dueAmount", dueAmount);
                studentUpdates.put("rollNumber", rollNo);
                
                targetRef.child(rollNo).updateChildren(studentUpdates);
            }

            workbook.close();
            progressDialog.dismiss();
            Toast.makeText(this, "Fees updated successfully!", Toast.LENGTH_SHORT).show();
            loadFeesData();

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Upload Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
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
        } catch (Exception e) {}
        return 0.0;
    }
}
