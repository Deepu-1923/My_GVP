package com.example.mygvp.student;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.R;
import com.google.firebase.database.*;

public class StudentLoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ LOAD LOGIN LAYOUT (IMPORTANT)
        setContentView(R.layout.activity_student_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        studentsRef = FirebaseDatabase
                .getInstance()
                .getReference("students");

        btnLogin.setOnClickListener(v -> loginStudent());
    }

    private void loginStudent() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot studentSnap : snapshot.getChildren()) {

                    String dbEmail = studentSnap.child("email").getValue(String.class);
                    String dbPassword = studentSnap.child("password").getValue(String.class);

                    if (email.equals(dbEmail) && password.equals(dbPassword)) {

                        String rollNo = studentSnap.getKey();

                        Intent intent = new Intent(
                                StudentLoginActivity.this,
                                StudentDashboardActivity.class
                        );
                        intent.putExtra("rollNo", rollNo);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }

                Toast.makeText(StudentLoginActivity.this,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentLoginActivity.this,
                        "Database error",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
