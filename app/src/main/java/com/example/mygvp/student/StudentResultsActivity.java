package com.example.mygvp.student;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentResultsActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerYear, spinnerSemester;
    private Button btnViewResult;
    private LinearLayout layoutResult;
    private TextView tvStudentInfo, tvCgpa, tvCollegeName;
    private RecyclerView rvSubjects;

    private String rollNo, branch, batch, year, sem, studentName;
    private List<SubjectModel> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_results);

        // Get user info from prefs
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        rollNo = prefs.getString("LOGGED_IN_ROLL_NO", "");
        branch = prefs.getString("LOGGED_IN_BRANCH", "");
        batch = prefs.getString("LOGGED_IN_BATCH", "");
        studentName = prefs.getString("LOGGED_IN_NAME", "");

        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        btnViewResult = findViewById(R.id.btnViewResult);
        layoutResult = findViewById(R.id.layoutResult);
        tvCollegeName = findViewById(R.id.tvCollegeName);
        tvStudentInfo = findViewById(R.id.tvStudentInfo);
        tvCgpa = findViewById(R.id.tvCgpa);
        rvSubjects = findViewById(R.id.rvSubjects);

        rvSubjects.setLayoutManager(new LinearLayoutManager(this));

        // Setup Year Dropdown
        String[] years = {"1", "2", "3", "4"};
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, years);
        spinnerYear.setAdapter(yearAdapter);

        // Setup Semester Dropdown
        String[] semesters = {"1", "2"};
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, semesters);
        spinnerSemester.setAdapter(semAdapter);

        btnViewResult.setOnClickListener(v -> loadResult());
        
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void loadResult() {
        year = spinnerYear.getText().toString();
        sem = spinnerSemester.getText().toString();

        if (year.isEmpty() || sem.isEmpty()) {
            Toast.makeText(this, "Please select year and semester", Toast.LENGTH_SHORT).show();
            return;
        }

        // Structure: students > branch > batch > rollNo > results > year > sem
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(branch)
                .child(batch)
                .child(rollNo)
                .child("results")
                .child(year)
                .child(sem);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    layoutResult.setVisibility(View.GONE);
                    Toast.makeText(StudentResultsActivity.this, "Result not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                subjectList.clear();

                for (DataSnapshot s : snapshot.getChildren()) {
                    String key = s.getKey();
                    if (key == null || key.equals("cgpa") || key.equals("sgpa")) continue;

                    String name = key;
                    String credits = s.child("credits").getValue() != null ? String.valueOf(s.child("credits").getValue()) : "0";
                    
                    // Trying to get "grade" or "grades" or "result"
                    String grade = "N/A";
                    if (s.child("grade").exists()) {
                        grade = String.valueOf(s.child("grade").getValue());
                    } else if (s.child("grades").exists()) {
                        grade = String.valueOf(s.child("grades").getValue());
                    }
                    
                    String points = s.child("points").getValue() != null ? String.valueOf(s.child("points").getValue()) : "0";
                    
                    subjectList.add(new SubjectModel(name, credits, grade, points));
                }

                // SORTING LOGIC: Subjects with more credits (3.0) first, Labs (1.5) last
                Collections.sort(subjectList, (s1, s2) -> {
                    try {
                        double c1 = Double.parseDouble(s1.credits);
                        double c2 = Double.parseDouble(s2.credits);
                        return Double.compare(c2, c1); // Descending order
                    } catch (Exception e) {
                        return 0;
                    }
                });

                rvSubjects.setAdapter(new SubjectAdapter(subjectList));

                // Formatted Info with Bold Keys
                SpannableStringBuilder builder = new SpannableStringBuilder();
                
                appendBold(builder, "Name: ", studentName + "\n");
                appendBold(builder, "Roll Number: ", rollNo + "\n");
                appendBold(builder, "Semester: ", year + "-" + sem + "\n");
                appendBold(builder, "Branch: ", branch);

                tvStudentInfo.setText(builder);

                String cgpa = snapshot.child("cgpa").getValue() != null ? String.valueOf(snapshot.child("cgpa").getValue()) : "0.0";
                String sgpa = snapshot.child("sgpa").getValue() != null ? String.valueOf(snapshot.child("sgpa").getValue()) : "0.0";
                
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
                Toast.makeText(StudentResultsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
