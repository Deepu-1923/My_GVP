package com.example.mygvp.student;

import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygvp.R;
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class StudentResultsActivity extends AppCompatActivity {

    AutoCompleteTextView spinnerYear, spinnerSemester;
    Button btnViewResult, btnDownload;
    LinearLayout layoutResult;
    TextView tvStudentInfo, tvCgpa;
    RecyclerView rvSubjects;

    String rollNo, year, sem;
    DataSnapshot cachedSnapshot;
    List<SubjectModel> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_results);

        rollNo = getIntent().getStringExtra("rollNo");

        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        btnViewResult = findViewById(R.id.btnViewResult);
        btnDownload = findViewById(R.id.btnDownload);
        layoutResult = findViewById(R.id.layoutResult);
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
        btnDownload.setOnClickListener(v -> generatePdf());
    }

    private void loadResult() {
        year = spinnerYear.getText().toString();
        sem = spinnerSemester.getText().toString();

        if (year.isEmpty() || sem.isEmpty()) {
            Toast.makeText(this, "Please select year and semester", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(rollNo)
                .child("results")
                .child(year)
                .child(sem);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    layoutResult.setVisibility(View.GONE);
                    btnDownload.setVisibility(View.GONE);
                    Toast.makeText(StudentResultsActivity.this, "Result not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                cachedSnapshot = snapshot;
                subjectList.clear();

                DataSnapshot subjectsSnap = snapshot.child("subjects");
                for (DataSnapshot s : subjectsSnap.getChildren()) {
                    String name = s.getKey();
                    String credits = s.child("credits").getValue() != null ? s.child("credits").getValue().toString() : "0";
                    String grade = s.child("grade").getValue() != null ? s.child("grade").getValue().toString() : "N/A";
                    String points = s.child("points").getValue() != null ? s.child("points").getValue().toString() : "0";
                    
                    subjectList.add(new SubjectModel(name, credits, grade, points));
                }

                subjectList.sort((a, b) -> {
                    try {
                        return Integer.parseInt(b.points) - Integer.parseInt(a.points);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });

                rvSubjects.setAdapter(new SubjectAdapter(subjectList));

                tvStudentInfo.setText(
                        "GAYATRI VIDYA PARISHAD FOR DEGREE AND P.G COURSES(A)\n" +
                                "Register No: " + rollNo +
                                "\nBranch: CSE\nYear: " + year +
                                "\nSemester: " + sem +"\n"
                );

                String cgpa = snapshot.child("cgpa").getValue() != null ? snapshot.child("cgpa").getValue().toString() : "N/A";
                String sgpa = snapshot.child("sgpa").getValue() != null ? snapshot.child("sgpa").getValue().toString() : "N/A";
                
                tvCgpa.setText("CGPA: " + cgpa + "     SGPA: " + sgpa);

                layoutResult.setVisibility(View.VISIBLE);
                btnDownload.setVisibility(View.VISIBLE);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentResultsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generatePdf() {
        if (cachedSnapshot == null) return;

        PdfDocument pdf = new PdfDocument();
        PdfDocument.Page page = pdf.startPage(
                new PdfDocument.PageInfo.Builder(595, 842, 1).create());

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12);

        int y = 40;
        canvas.drawText("GAYATRI VIDYA PARISHAD - SEMESTER RESULT", 40, y, paint);
        y += 20;

        for (SubjectModel s : subjectList) {
            canvas.drawText(s.name + "  " + s.grade + "  " + s.points, 40, y, paint);
            y += 18;
        }

        pdf.finishPage(page);

        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "Result_" + rollNo + "_Y" + year + "_S" + sem + ".pdf");

            pdf.writeTo(new FileOutputStream(file));
            pdf.close();

            Toast.makeText(this, "PDF saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF generation failed", Toast.LENGTH_SHORT).show();
        }
    }
}
