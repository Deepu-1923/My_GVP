package com.example.mygvp.faculty;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.R;
import com.google.firebase.database.*;

public class FacultyLoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    DatabaseReference facultyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        facultyRef = FirebaseDatabase.getInstance().getReference("faculty");

        btnLogin.setOnClickListener(v -> loginFaculty());
    }

    private void loginFaculty() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
            return;
        }

        facultyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot facSnap : snapshot.getChildren()) {
                    String dbEmail = facSnap.child("email").getValue(String.class);
                    String dbPassword = facSnap.child("password").getValue(String.class);

                    if (email.equals(dbEmail) && password.equals(dbPassword)) {
                        startActivity(new Intent(FacultyLoginActivity.this,
                                FacultyDashboardActivity.class));
                        finish();
                        return;
                    }
                }
                Toast.makeText(FacultyLoginActivity.this,
                        "Invalid Faculty Credentials", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FacultyLoginActivity.this,
                        "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
