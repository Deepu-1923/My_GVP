package com.example.mygvp.faculty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        facultyRef = FirebaseDatabase.getInstance()
                .getReference("faculty");

        btnLogin.setOnClickListener(v -> loginFaculty());
    }

    private void loginFaculty() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Enter email & password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        facultyRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot facSnap : snapshot.getChildren()) {

                            String dbEmail = facSnap.child("email")
                                    .getValue(String.class);
                            String dbPassword = facSnap.child("password")
                                    .getValue(String.class);

                            if (email.equals(dbEmail)
                                    && password.equals(dbPassword)) {

                                // ✅ IMPORTANT: Get facultyId (fac01)
                                String facultyId = facSnap.getKey();
                                String facultyName = facSnap.child("name").getValue(String.class);

                                // Save to SharedPreferences for Lost & Found and other modules
                                SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("LOGGED_IN_FACULTY_ID", facultyId);
                                editor.putString("LOGGED_IN_NAME", facultyName != null ? facultyName : "Faculty");
                                editor.putString("LOGGED_IN_ROLL_NO", facultyId); // Using ID as "Roll No" for consistency in Lost & Found
                                editor.putString("USER_TYPE", "FACULTY");
                                editor.apply();

                                // ✅ Pass facultyId to Dashboard
                                Intent intent = new Intent(
                                        FacultyLoginActivity.this,
                                        FacultyDashboardActivity.class
                                );

                                intent.putExtra("facultyId", facultyId);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }

                        Toast.makeText(
                                FacultyLoginActivity.this,
                                "Invalid Faculty Credentials",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(
                                FacultyLoginActivity.this,
                                "Database error",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}