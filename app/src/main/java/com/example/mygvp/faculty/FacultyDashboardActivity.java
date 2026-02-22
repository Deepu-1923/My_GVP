package com.example.mygvp.faculty;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mygvp.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class FacultyDashboardActivity extends AppCompatActivity {

    private Button btnStartSession, btnEndSession;
    private TextView tvSessionPin;
    private EditText etSubjectName;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        btnStartSession = findViewById(R.id.btn_start_session);
        btnEndSession = findViewById(R.id.btn_end_session);
        tvSessionPin = findViewById(R.id.tv_session_pin);
        etSubjectName = findViewById(R.id.et_subject_name);

        // Pointing to the Active_Sessions node in Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("Active_Sessions");

        btnStartSession.setOnClickListener(v -> startAttendanceSession());
        btnEndSession.setOnClickListener(v -> endAttendanceSession());
    }

    private void startAttendanceSession() {
        String subject = etSubjectName.getText().toString().trim();
        if (subject.isEmpty()) {
            Toast.makeText(this, "Please enter a subject first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a random 4-digit PIN
        String sessionPin = String.format("%04d", new Random().nextInt(10000));
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Create the session data packet
        HashMap<String, Object> sessionData = new HashMap<>();
        sessionData.put("is_active", true);
        sessionData.put("date", currentDate);
        sessionData.put("session_pin", sessionPin);

        // Save to Firebase under Branch -> Subject
        databaseReference.child("CSE_Sem8").child(subject).setValue(sessionData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Session Live! Tell students the PIN.", Toast.LENGTH_LONG).show();

                    tvSessionPin.setText("PIN: " + sessionPin);
                    tvSessionPin.setVisibility(View.VISIBLE);
                    btnStartSession.setVisibility(View.GONE);
                    btnEndSession.setVisibility(View.VISIBLE);
                    etSubjectName.setEnabled(false); // Lock the input
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show());
    }

    private void endAttendanceSession() {
        String subject = etSubjectName.getText().toString().trim();

        // Update Firebase to close the session
        databaseReference.child("CSE_Sem8").child(subject).child("is_active").setValue(false)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Session Locked.", Toast.LENGTH_SHORT).show();

                    tvSessionPin.setVisibility(View.GONE);
                    btnStartSession.setVisibility(View.VISIBLE);
                    btnEndSession.setVisibility(View.GONE);
                    etSubjectName.setEnabled(true);
                    etSubjectName.setText(""); // clear for next time
                });
    }
}