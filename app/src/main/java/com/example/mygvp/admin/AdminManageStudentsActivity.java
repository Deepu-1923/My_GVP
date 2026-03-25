package com.example.mygvp.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminManageStudentsActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerBranch, spinnerBatch, spinnerYear, spinnerSemester;
    private MaterialButton btnViewDetails, btnUploadExcel;
    private RecyclerView rvStudents;
    private FloatingActionButton fabAddStudent;
    private DatabaseReference dbRef;
    private StudentAdapter adapter;
    private List<StudentModel> studentList;

    private final ActivityResultLauncher<String> excelPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadExcelData(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_students);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        dbRef = FirebaseDatabase.getInstance().getReference("students");

        spinnerBranch = findViewById(R.id.spinnerBranch);
        spinnerBatch = findViewById(R.id.spinnerBatch);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        btnUploadExcel = findViewById(R.id.btnUploadExcel);
        rvStudents = findViewById(R.id.rvStudents);
        fabAddStudent = findViewById(R.id.fabAddStudent);

        setupSpinners();

        studentList = new ArrayList<>();
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        
        btnViewDetails.setOnClickListener(v -> fetchStudents());

        btnUploadExcel.setOnClickListener(v -> excelPickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        fabAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEditStudentActivity.class);
            intent.putExtra("IS_EDIT", false);
            intent.putExtra("BRANCH", spinnerBranch.getText().toString());
            intent.putExtra("BATCH", spinnerBatch.getText().toString());
            startActivity(intent);
        });
    }

    private void setupSpinners() {
        String[] branches = {"CSE", "ECE", "Mech", "Civil", "CSM"};
        String[] batches = {"2025-29"};
        String[] years = {"1", "2", "3", "4"};
        String[] semesters = {"1", "2"};

        spinnerBranch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, branches));
        spinnerBatch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, batches));
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));
        spinnerSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, semesters));
    }

    private void fetchStudents() {
        String branch = spinnerBranch.getText().toString();
        String batch = spinnerBatch.getText().toString();

        if (branch.isEmpty() || batch.isEmpty()) {
            Toast.makeText(this, "Please select Branch and Batch", Toast.LENGTH_SHORT).show();
            return;
        }

        dbRef.child(branch).child(batch).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    StudentModel student = studentSnap.getValue(StudentModel.class);
                    if (student != null) {
                        student.setRollNo(studentSnap.getKey());
                        student.setBranch(branch);
                        student.setBatch(batch);
                        studentList.add(student);
                    }
                }
                adapter = new StudentAdapter(AdminManageStudentsActivity.this, studentList, (student) -> {
                    Intent intent = new Intent(AdminManageStudentsActivity.this, AdminEditStudentActivity.class);
                    intent.putExtra("IS_EDIT", true);
                    intent.putExtra("STUDENT", student);
                    startActivity(intent);
                }, (student) -> {
                    deleteStudent(student);
                });
                rvStudents.setAdapter(adapter);
                if (studentList.isEmpty()) {
                    Toast.makeText(AdminManageStudentsActivity.this, "No students found for " + branch + " " + batch, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void uploadExcelData(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            
            String selectedBranch = spinnerBranch.getText().toString();
            String selectedBatch = spinnerBatch.getText().toString();

            if (selectedBranch.isEmpty() || selectedBatch.isEmpty()) {
                Toast.makeText(this, "Please select Branch and Batch first", Toast.LENGTH_SHORT).show();
                workbook.close();
                return;
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                String rollNo = formatter.formatCellValue(row.getCell(0)).trim();
                String name = formatter.formatCellValue(row.getCell(1)).trim();
                String email = formatter.formatCellValue(row.getCell(2)).trim();
                String password = formatter.formatCellValue(row.getCell(3)).trim();
                String branchFromExcel = formatter.formatCellValue(row.getCell(4)).trim();
                String batchFromExcel = formatter.formatCellValue(row.getCell(5)).trim();

                if (rollNo.isEmpty()) continue;

                String finalBranch = (!branchFromExcel.isEmpty()) ? branchFromExcel : selectedBranch;
                String finalBatch = (!batchFromExcel.isEmpty()) ? batchFromExcel : selectedBatch;
                
                finalBranch = formatBranchName(finalBranch);

                if (password.isEmpty()) password = "gvp@123";

                DatabaseReference studentRef = dbRef.child(finalBranch).child(finalBatch).child(rollNo);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", name);
                updates.put("email", email);
                updates.put("password", password);

                studentRef.updateChildren(updates);
            }
            Toast.makeText(this, "Excel data processed successfully", Toast.LENGTH_SHORT).show();
            workbook.close();
        } catch (Exception e) {
            Log.e("ExcelError", "Error: " + e.getMessage());
            Toast.makeText(this, "Failed to parse Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String formatBranchName(String branch) {
        if (branch == null || branch.isEmpty()) return "Unknown";
        if (branch.equalsIgnoreCase("civil")) return "Civil";
        if (branch.equalsIgnoreCase("mech")) return "Mech";
        return branch.toUpperCase();
    }

    private void deleteStudent(StudentModel student) {
        dbRef.child(student.getBranch()).child(student.getBatch()).child(student.getRollNo())
                .removeValue().addOnSuccessListener(aVoid -> Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show());
    }
}
