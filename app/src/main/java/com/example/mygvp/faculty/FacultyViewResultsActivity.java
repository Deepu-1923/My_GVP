package com.example.mygvp.faculty;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygvp.R;
import com.example.mygvp.student.SubjectAdapter;
import com.example.mygvp.student.SubjectModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FacultyViewResultsActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerYear, spinnerSemester;
    private TextInputEditText etRollNo, etBatch;
    private Button btnViewResult;
    private LinearLayout layoutResult;
    private TextView tvStudentInfo, tvCgpa;
    private RecyclerView rvSubjects;

    private String facultyBranch;
    private List<SubjectModel> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_view_results);

        // Get faculty branch from prefs
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        facultyBranch = prefs.getString("LOGGED_IN_FACULTY_BRANCH", "");

        etRollNo = findViewById(R.id.etRollNo);
        etBatch = findViewById(R.id.etBatch);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        btnViewResult = findViewById(R.id.btnViewResult);
        layoutResult = findViewById(R.id.layoutResult);
        tvStudentInfo = findViewById(R.id.tvStudentInfo);
        tvCgpa = findViewById(R.id.tvCgpa);
        rvSubjects = findViewById(R.id.rvSubjects);

        rvSubjects.setLayoutManager(new LinearLayoutManager(this));

        // Setup Year Dropdown
        String[] years = {"1", "2", "3", "4"};
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));

        // Setup Semester Dropdown
        String[] semesters = {"1", "2"};
        spinnerSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, semesters));

        btnViewResult.setOnClickListener(v -> searchStudentResult());
        
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void searchStudentResult() {
        String rollNo = etRollNo.getText().toString().trim().toUpperCase();
        String batch = etBatch.getText().toString().trim();
        String year = spinnerYear.getText().toString();
        String sem = spinnerSemester.getText().toString();

        if (rollNo.isEmpty() || batch.isEmpty() || year.isEmpty() || sem.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search for student name and details first to display in header
        DatabaseReference studentRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(facultyBranch)
                .child(batch)
                .child(rollNo);

        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                if (!studentSnapshot.exists()) {
                    layoutResult.setVisibility(View.GONE);
                    Toast.makeText(FacultyViewResultsActivity.this, "Student not found in branch " + facultyBranch, Toast.LENGTH_SHORT).show();
                    return;
                }

                String studentName = studentSnapshot.child("name").getValue() != null ? 
                        String.valueOf(studentSnapshot.child("name").getValue()) : "Unknown";

                DataSnapshot resultSnapshot = studentSnapshot.child("results").child(year).child(sem);

                if (!resultSnapshot.exists()) {
                    layoutResult.setVisibility(View.GONE);
                    Toast.makeText(FacultyViewResultsActivity.this, "Result not found for this semester", Toast.LENGTH_SHORT).show();
                    return;
                }

                subjectList.clear();
                for (DataSnapshot s : resultSnapshot.getChildren()) {
                    String key = s.getKey();
                    if (key == null || key.equals("cgpa") || key.equals("sgpa")) continue;

                    String credits = s.child("credits").getValue() != null ? String.valueOf(s.child("credits").getValue()) : "0";
                    String grade = "N/A";
                    if (s.child("grade").exists()) {
                        grade = String.valueOf(s.child("grade").getValue());
                    } else if (s.child("grades").exists()) {
                        grade = String.valueOf(s.child("grades").getValue());
                    }
                    String points = s.child("points").getValue() != null ? String.valueOf(s.child("points").getValue()) : "0";
                    
                    subjectList.add(new SubjectModel(key, credits, grade, points));
                }

                rvSubjects.setAdapter(new SubjectAdapter(subjectList));

                // Formatted Info with Bold Keys
                SpannableStringBuilder builder = new SpannableStringBuilder();
                appendBold(builder, "Name: ", studentName + "\n");
                appendBold(builder, "Roll Number: ", rollNo + "\n");
                appendBold(builder, "Semester: ", year + "-" + sem + "\n");
                appendBold(builder, "Branch: ", facultyBranch);

                tvStudentInfo.setText(builder);

                String cgpa = resultSnapshot.child("cgpa").getValue() != null ? String.valueOf(resultSnapshot.child("cgpa").getValue()) : "0.0";
                String sgpa = resultSnapshot.child("sgpa").getValue() != null ? String.valueOf(resultSnapshot.child("sgpa").getValue()) : "0.0";
                
                tvCgpa.setText(String.format("SGPA: %s     CGPA: %s", sgpa, cgpa));
                layoutResult.setVisibility(View.VISIBLE);
            }

            private void appendBold(SpannableStringBuilder builder, String key, String value) {
                int start = builder.length();
                builder.append(key);
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, start + key.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(value);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FacultyViewResultsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
