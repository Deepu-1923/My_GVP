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

import java.util.Objects;

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
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> loginStudent());
        }
    }

    private void loginStudent() {
        if (etEmail == null || etPassword == null) return;

        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isFound = false;

                for (DataSnapshot branchSnap : snapshot.getChildren()) {
                    String branchKey = branchSnap.getKey();
                    for (DataSnapshot batchSnap : branchSnap.getChildren()) {
                        String batchKey = batchSnap.getKey();
                        for (DataSnapshot studentSnap : batchSnap.getChildren()) {

                            String dbEmail = studentSnap.child("email").getValue(String.class);
                            String dbPassword = studentSnap.child("password").getValue(String.class);

                            if (dbEmail != null && dbPassword != null &&
                                    email.equalsIgnoreCase(dbEmail.trim()) && password.equals(dbPassword.trim())) {

                                String rollNo = studentSnap.getKey();
                                String studentName = studentSnap.child("name").getValue(String.class);

                                if (studentName == null || studentName.isEmpty()) {
                                    studentName = "Student";
                                } else {
                                    studentName = toTitleCase(studentName);
                                }

                                SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("LOGGED_IN_ROLL_NO", rollNo);
                                editor.putString("LOGGED_IN_NAME", studentName);
                                editor.putString("LOGGED_IN_BRANCH", branchKey);
                                editor.putString("LOGGED_IN_BATCH", batchKey);
                                editor.putString("USER_TYPE", "STUDENT");
                                editor.apply();

                                Toast.makeText(StudentLoginActivity.this, "Hello,\n" + studentName, Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(StudentLoginActivity.this, StudentDashboardActivity.class);
                                intent.putExtra("rollNo", rollNo);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);

                                isFound = true;
                                break;
                            }
                        }
                        if (isFound) break;
                    }
                    if (isFound) break;
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
