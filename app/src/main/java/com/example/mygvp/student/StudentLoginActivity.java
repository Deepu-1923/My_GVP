package com.example.mygvp.student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        // Bind Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Firebase Reference
        studentsRef = FirebaseDatabase.getInstance().getReference("students");

        // Login Action
        btnLogin.setOnClickListener(v -> loginStudent());
    }

    private void loginStudent() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isFound = false;

                // 1st Loop: Branches (e.g., CSE)
                for (DataSnapshot branchSnap : snapshot.getChildren()) {

                    // 2nd Loop: Batches (e.g., 2025-29)
                    for (DataSnapshot batchSnap : branchSnap.getChildren()) {

                        // 3rd Loop: Student IDs (e.g., 5251411001)
                        for (DataSnapshot studentSnap : batchSnap.getChildren()) {

                            String dbEmail = studentSnap.child("email").getValue(String.class);
                            String dbPassword = studentSnap.child("password").getValue(String.class);

                            // Using .trim() prevents accidental spaces in database from ruining the login
                            if (dbEmail != null && dbPassword != null &&
                                    email.equalsIgnoreCase(dbEmail.trim()) && password.equals(dbPassword.trim())) {

                                String rollNo = studentSnap.getKey();
                                String studentName = studentSnap.child("name").getValue(String.class);
                                String branch = studentSnap.child("branch").getValue(String.class);
                                String batch = studentSnap.child("batch").getValue(String.class);

                                // Format the name to Title Case so your dashboard doesn't just say "Student"
                                if (studentName == null || studentName.isEmpty()) {
                                    studentName = "Student";
                                } else {
                                    studentName = toTitleCase(studentName);
                                }

                                // Save Session in SharedPreferences
                                SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("LOGGED_IN_ROLL_NO", rollNo);
                                editor.putString("LOGGED_IN_NAME", studentName);
                                editor.putString("LOGGED_IN_BRANCH", branch);
                                editor.putString("LOGGED_IN_BATCH", batch);
                                editor.apply();

                                Toast.makeText(StudentLoginActivity.this, "Hello,\n" + studentName, Toast.LENGTH_SHORT).show();

                                // Navigate to Dashboard
                                Intent intent = new Intent(StudentLoginActivity.this, StudentDashboardActivity.class);
                                intent.putExtra("rollNo", rollNo);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);

                                isFound = true;
                                break; // Breaks student loop
                            }
                        }
                        if (isFound) break; // Breaks batch loop
                    }
                    if (isFound) break; // Breaks branch loop
                }

                if (!isFound) {
                    Toast.makeText(StudentLoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentLoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to convert "ABDUL SADIYA TASLIM" to "Abdul Sadiya Taslim"
    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else {
                c = Character.toLowerCase(c);
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }
}