package com.example.mygvp.faculty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygvp.R;
import com.example.mygvp.student.MonthlyAttendance;
import com.example.mygvp.student.MonthlyAttendanceAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FacultyViewAttendanceActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerBranch, spinnerYear, spinnerSemester;
    private TextInputEditText etRollNo, etBatch;
    private MaterialButton btnViewAttendance;
    private ExtendedFloatingActionButton fabUpload;
    private LinearLayout layoutResult;
    private TextView tvStudentName, tvOverallPercentage, tvTotalDays, tvPresentDays, tvAbsentDays;
    private RecyclerView rvMonthlyAttendance;

    private List<MonthlyAttendance> attendanceList = new ArrayList<>();
    private MonthlyAttendanceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_view_attendance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        spinnerBranch = findViewById(R.id.spinnerBranch);
        etRollNo = findViewById(R.id.etRollNo);
        etBatch = findViewById(R.id.etBatch);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        fabUpload = findViewById(R.id.fabUpload);
        layoutResult = findViewById(R.id.layoutResult);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        tvTotalDays = findViewById(R.id.tvTotalDays);
        tvPresentDays = findViewById(R.id.tvPresentDays);
        tvAbsentDays = findViewById(R.id.tvAbsentDays);
        rvMonthlyAttendance = findViewById(R.id.rvMonthlyAttendance);

        setupDropdowns();

        adapter = new MonthlyAttendanceAdapter(attendanceList);
        rvMonthlyAttendance.setLayoutManager(new LinearLayoutManager(this));
        rvMonthlyAttendance.setAdapter(adapter);

        btnViewAttendance.setOnClickListener(v -> searchAttendance());
        
        if (fabUpload != null) {
            fabUpload.setOnClickListener(v -> {
                startActivity(new Intent(this, FacultyUploadAttendanceActivity.class));
            });
        }
    }

    private void setupDropdowns() {
        String[] branches = {"CSE", "ECE", "EEE", "MECH", "CIVIL"};
        spinnerBranch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, branches));

        // Pre-select faculty's own branch
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        String myBranch = prefs.getString("LOGGED_IN_FACULTY_BRANCH", "");
        if (!myBranch.isEmpty()) {
            spinnerBranch.setText(myBranch, false);
        }

        String[] years = {"1", "2", "3", "4"};
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));

        String[] semesters = {"1", "2"};
        spinnerSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, semesters));
    }

    private void searchAttendance() {
        String branch = spinnerBranch.getText().toString();
        String rollNo = etRollNo.getText().toString().trim().toUpperCase();
        String batch = etBatch.getText().toString().trim();
        String year = spinnerYear.getText().toString();
        String sem = spinnerSemester.getText().toString();

        if (branch.isEmpty() || rollNo.isEmpty() || batch.isEmpty() || year.isEmpty() || sem.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("attendance")
                .child(branch).child(batch).child(year).child(sem).child(rollNo);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                attendanceList.clear();
                if (!snapshot.exists()) {
                    layoutResult.setVisibility(View.GONE);
                    Toast.makeText(FacultyViewAttendanceActivity.this, "No attendance data found", Toast.LENGTH_SHORT).show();
                    return;
                }

                tvStudentName.setText(snapshot.child("name").getValue(String.class));

                double percentage = getDoubleValue(snapshot.child("percentage"));
                tvOverallPercentage.setText(String.format(Locale.getDefault(), "Overall: %.1f%%", percentage));

                long total = getLongValue(snapshot.child("total_days"));
                long present = getLongValue(snapshot.child("present_days"));
                long absent = total - present;

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
                layoutResult.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FacultyViewAttendanceActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
}
