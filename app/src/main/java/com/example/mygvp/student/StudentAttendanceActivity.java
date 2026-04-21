package com.example.mygvp.student;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygvp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "StudentAttendance";
    private TextView tvPercentage, tvTotalDays, tvPresentDays, tvAbsentDays;
    private AutoCompleteTextView spinnerYear, spinnerSemester;
    private RecyclerView rvMonthlyAttendance;
    private MonthlyAttendanceAdapter adapter;
    private List<MonthlyAttendance> attendanceList;
    private String rollNo, branch, batch;
    private DatabaseReference baseAttendanceRef;
    private ValueEventListener attendanceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvPercentage = findViewById(R.id.tvPercentage);
        tvTotalDays = findViewById(R.id.tvTotalDays);
        tvPresentDays = findViewById(R.id.tvPresentDays);
        tvAbsentDays = findViewById(R.id.tvAbsentDays);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        rvMonthlyAttendance = findViewById(R.id.rvMonthlyAttendance);

        rollNo = getIntent().getStringExtra("rollNo");
        branch = getIntent().getStringExtra("branch");
        batch = getIntent().getStringExtra("batch");

        attendanceList = new ArrayList<>();
        adapter = new MonthlyAttendanceAdapter(attendanceList);
        rvMonthlyAttendance.setLayoutManager(new LinearLayoutManager(this));
        rvMonthlyAttendance.setAdapter(adapter);

        baseAttendanceRef = FirebaseDatabase.getInstance().getReference("attendance")
                .child(branch.trim())
                .child(batch.trim());

        setupDynamicDropdowns();
    }

    private void setupDynamicDropdowns() {
        if (rollNo == null || branch == null || batch == null) {
            Toast.makeText(this, "Missing student information", Toast.LENGTH_SHORT).show();
            return;
        }

        baseAttendanceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                List<String> years = new ArrayList<>();
                for (DataSnapshot yearSnap : snapshot.getChildren()) {
                    // Check if this student has any records in this year
                    boolean found = false;
                    for (DataSnapshot semSnap : yearSnap.getChildren()) {
                        if (semSnap.hasChild(rollNo)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        years.add(yearSnap.getKey());
                    }
                }

                if (!years.isEmpty()) {
                    ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(StudentAttendanceActivity.this,
                            android.R.layout.simple_list_item_1, years);
                    spinnerYear.setAdapter(yearAdapter);
                    
                    // If current text is empty or not in new list, pick first
                    String currentYear = spinnerYear.getText().toString();
                    if (currentYear.isEmpty() || !years.contains(currentYear)) {
                        spinnerYear.setText(years.get(0), false);
                        fetchSemesters(years.get(0));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        spinnerYear.setOnItemClickListener((parent, view, position, id) -> {
            String selectedYear = parent.getItemAtPosition(position).toString();
            fetchSemesters(selectedYear);
        });

        spinnerSemester.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSem = parent.getItemAtPosition(position).toString();
            fetchAttendanceData(spinnerYear.getText().toString(), selectedSem);
        });
    }

    private void fetchSemesters(String selectedYear) {
        baseAttendanceRef.child(selectedYear).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> semesters = new ArrayList<>();
                for (DataSnapshot semSnap : snapshot.getChildren()) {
                    if (semSnap.hasChild(rollNo)) {
                        semesters.add(semSnap.getKey());
                    }
                }
                
                if (!semesters.isEmpty()) {
                    ArrayAdapter<String> semAdapter = new ArrayAdapter<>(StudentAttendanceActivity.this,
                            android.R.layout.simple_list_item_1, semesters);
                    spinnerSemester.setAdapter(semAdapter);
                    
                    String currentSem = spinnerSemester.getText().toString();
                    if (currentSem.isEmpty() || !semesters.contains(currentSem)) {
                        spinnerSemester.setText(semesters.get(0), false);
                        fetchAttendanceData(selectedYear, semesters.get(0));
                    } else {
                        fetchAttendanceData(selectedYear, currentSem);
                    }
                } else {
                    spinnerSemester.setText("", false);
                    resetUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchAttendanceData(String year, String sem) {
        if (rollNo == null || branch == null || batch == null || year.isEmpty() || sem.isEmpty()) {
            return;
        }

        DatabaseReference ref = baseAttendanceRef.child(year).child(sem).child(rollNo);

        attendanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                attendanceList.clear();
                if (!snapshot.exists()) {
                    resetUI();
                    return;
                }

                double percentage = getDoubleValue(snapshot.child("percentage"));
                long total = getLongValue(snapshot.child("total_days"));
                long present = getLongValue(snapshot.child("present_days"));
                long absent = total - present;

                tvPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", percentage));
                tvTotalDays.setText(String.valueOf(total));
                tvPresentDays.setText(String.valueOf(present));
                tvAbsentDays.setText(String.valueOf(Math.max(0, absent)));

                DataSnapshot monthsSnap = snapshot.child("months");
                for (DataSnapshot m : monthsSnap.getChildren()) {
                    String monthName = m.getKey();
                    int p = (int) getLongValue(m.child("present"));
                    int t = (int) getLongValue(m.child("total"));
                    attendanceList.add(new MonthlyAttendance(monthName, p, t));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentAttendanceActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        ref.addValueEventListener(attendanceListener);
    }

    private double getDoubleValue(DataSnapshot snapshot) {
        Object val = snapshot.getValue();
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0.0; }
    }

    private long getLongValue(DataSnapshot snapshot) {
        Object val = snapshot.getValue();
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return 0; }
    }

    private void resetUI() {
        tvPercentage.setText("0%");
        tvTotalDays.setText("0");
        tvPresentDays.setText("0");
        tvAbsentDays.setText("0");
        attendanceList.clear();
        adapter.notifyDataSetChanged();
    }
}
