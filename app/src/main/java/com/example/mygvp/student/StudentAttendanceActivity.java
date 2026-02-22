package com.example.mygvp.student;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mygvp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentAttendanceActivity extends AppCompatActivity {

    private EditText etStudentSubject, etStudentPin;
    private Button btnSubmitAttendance;
    private String rollNo;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        etStudentSubject = findViewById(R.id.et_student_subject);
        etStudentPin = findViewById(R.id.et_student_pin);
        btnSubmitAttendance = findViewById(R.id.btn_submit_attendance);

        rollNo = getIntent().getStringExtra("rollNo");
        rootRef = FirebaseDatabase.getInstance().getReference();

        btnSubmitAttendance.setOnClickListener(v -> verifyAndMarkAttendance());
    }

    private void verifyAndMarkAttendance() {
        String subject = etStudentSubject.getText().toString().trim();
        String enteredPin = etStudentPin.getText().toString().trim();

        if (subject.isEmpty() || enteredPin.isEmpty()) {
            Toast.makeText(this, "Please enter both Subject and PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Check if the session is active and PIN matches
        DatabaseReference sessionRef = rootRef.child("Active_Sessions").child("CSE_Sem8").child(subject);

        sessionRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && Boolean.TRUE.equals(snapshot.child("is_active").getValue(Boolean.class))) {
                String actualPin = snapshot.child("session_pin").getValue(String.class);

                if (enteredPin.equals(actualPin)) {
                    // PIN matches! Mark attendance in the logs
                    markStudentPresent(subject);
                } else {
                    Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No active session found for this subject", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show());
    }

    private void markStudentPresent(String subject) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Save to Attendance_Logs -> CSE_Sem8 -> Date -> Subject -> RollNo = "Present"
        DatabaseReference logRef = rootRef.child("Attendance_Logs")
                .child("CSE_Sem8")
                .child(currentDate)
                .child(subject)
                .child(rollNo);

        logRef.setValue("Present").addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Attendance Marked Successfully!", Toast.LENGTH_LONG).show();
            finish(); // Close activity and return to dashboard
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to save record", Toast.LENGTH_SHORT).show());
    }
}