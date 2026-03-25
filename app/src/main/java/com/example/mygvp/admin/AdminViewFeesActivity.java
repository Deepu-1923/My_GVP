package com.example.mygvp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AdminViewFeesActivity extends AppCompatActivity {

    private RecyclerView rvFeesList;
    private StudentFeeAdapter adapter;
    private List<StudentFee> allFees = new ArrayList<>();
    private List<StudentFee> filteredFees = new ArrayList<>();
    
    private AutoCompleteTextView spinnerBatch, spinnerStream, spinnerYear, spinnerSem;
    private ChipGroup chipGroupFees;
    private TextView tvEmptyState, tvCollectionPercentage, tvStatTotalStudents, tvStatPaid, tvStatDues;
    private ImageButton btnBack;
    private MaterialButton btnSubmit;
    private View scrollViewResults;

    private DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_view_fees);

        studentsRef = FirebaseDatabase.getInstance().getReference("students");

        initViews();
        setupSpinners();
        setupRecyclerView();
        setupFilters();

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> loadActualDataFromFirebase());

        // Check for pre-filled data (e.g. redirected from Manage Fees or Select Fees)
        String preBatch = getIntent().getStringExtra("BATCH");
        String preStream = getIntent().getStringExtra("STREAM");
        String preYear = getIntent().getStringExtra("YEAR");
        String preSem = getIntent().getStringExtra("SEM");

        if (preBatch != null && preStream != null) {
            spinnerBatch.setText(preBatch, false);
            spinnerStream.setText(preStream, false);
            if (preYear != null) spinnerYear.setText(preYear, false);
            if (preSem != null) spinnerSem.setText(preSem, false);
            loadActualDataFromFirebase();
        }
    }

    private void initViews() {
        rvFeesList = findViewById(R.id.rvFeesList);
        spinnerBatch = findViewById(R.id.spinnerBatch);
        spinnerStream = findViewById(R.id.spinnerStream);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSem = findViewById(R.id.spinnerSem);
        chipGroupFees = findViewById(R.id.chipGroupFees);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvCollectionPercentage = findViewById(R.id.tvCollectionPercentage);
        tvStatTotalStudents = findViewById(R.id.tvStatTotalStudents);
        tvStatPaid = findViewById(R.id.tvStatPaid);
        tvStatDues = findViewById(R.id.tvStatDues);
        
        ImageButton btnOcrSearch = findViewById(R.id.btnOcrSearch);
        if (btnOcrSearch != null) {
            btnOcrSearch.setVisibility(View.GONE);
        }

        btnBack = findViewById(R.id.btnBack);
        btnSubmit = findViewById(R.id.btnSubmit);
        scrollViewResults = findViewById(R.id.scrollViewResults);
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
        adapter = new StudentFeeAdapter(filteredFees);
        rvFeesList.setLayoutManager(new LinearLayoutManager(this));
        rvFeesList.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupFees.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
    }

    private void loadActualDataFromFirebase() {
        String batch = spinnerBatch.getText().toString().trim();
        String stream = spinnerStream.getText().toString().trim();
        String year = spinnerYear.getText().toString().trim();
        String sem = spinnerSem.getText().toString().trim();
        
        if (batch.isEmpty() || stream.isEmpty() || year.isEmpty() || sem.isEmpty()) {
            Toast.makeText(this, "Please select all configuration details", Toast.LENGTH_SHORT).show();
            return;
        }

        studentsRef.child(stream).child(batch).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFees.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot studentSnap : snapshot.getChildren()) {
                        String sYear = studentSnap.child("year").getValue(String.class);
                        String sSem = studentSnap.child("semester").getValue(String.class);

                        if (year.equals(sYear) && sem.equals(sSem)) {
                            String rollNo = studentSnap.getKey();
                            String name = studentSnap.child("name").getValue(String.class);
                            
                            Double totalFee = studentSnap.child("totalFee").getValue(Double.class);
                            if (totalFee == null) totalFee = 0.0;
                            
                            Double paidAmount = studentSnap.child("paidAmount").getValue(Double.class);
                            if (paidAmount == null) paidAmount = 0.0;
                            
                            Double dueAmount = studentSnap.child("dueAmount").getValue(Double.class);
                            if (dueAmount == null) dueAmount = Math.max(0, totalFee - paidAmount);

                            allFees.add(new StudentFee(rollNo, name, totalFee, paidAmount, dueAmount));
                        }
                    }
                    
                    Collections.sort(allFees, (f1, f2) -> f1.getRollNumber().compareTo(f2.getRollNumber()));
                    
                    if (allFees.isEmpty()) {
                        scrollViewResults.setVisibility(View.GONE);
                        Toast.makeText(AdminViewFeesActivity.this, "No records found for selected Year/Sem", Toast.LENGTH_SHORT).show();
                    } else {
                        scrollViewResults.setVisibility(View.VISIBLE);
                        updateStats();
                        applyFilters();
                    }
                } else {
                    scrollViewResults.setVisibility(View.GONE);
                    Toast.makeText(AdminViewFeesActivity.this, "No students found for this batch", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminViewFeesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        int total = allFees.size();
        int paidCount = 0;
        int duesCount = 0;
        double totalAmt = 0;
        double paidAmt = 0;

        for (StudentFee fee : allFees) {
            totalAmt += fee.getTotalFee();
            paidAmt += fee.getPaidAmount();
            if (fee.getDueAmount() <= 0) paidCount++;
            else duesCount++;
        }

        double percent = (totalAmt > 0) ? (paidAmt / totalAmt) * 100 : 0;
        tvCollectionPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", percent));
        tvStatTotalStudents.setText(String.valueOf(total));
        tvStatPaid.setText(String.valueOf(paidCount));
        tvStatDues.setText(String.valueOf(duesCount));
    }

    private void applyFilters() {
        filteredFees.clear();
        int checkedId = chipGroupFees.getCheckedChipId();

        for (StudentFee fee : allFees) {
            boolean matchesStatus = true;
            if (checkedId == R.id.chipPaid) {
                matchesStatus = (fee.getDueAmount() <= 0);
            } else if (checkedId == R.id.chipDues) {
                matchesStatus = (fee.getDueAmount() > 0);
            }

            if (matchesStatus) {
                filteredFees.add(fee);
            }
        }

        adapter.updateList(filteredFees);
        tvEmptyState.setVisibility(filteredFees.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
