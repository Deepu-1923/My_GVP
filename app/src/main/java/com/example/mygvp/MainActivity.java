package com.example.mygvp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

// IMPORT YOUR LOGIN ACTIVITIES (IMPORTANT)
import com.example.mygvp.admin.AdminLoginActivity;
import com.example.mygvp.faculty.FacultyLoginActivity;
import com.example.mygvp.student.StudentLoginActivity;

public class MainActivity extends AppCompatActivity {

    Button admin, faculty, student;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind buttons
        admin = findViewById(R.id.btnAdmin);
        faculty = findViewById(R.id.btnFaculty);
        student = findViewById(R.id.btnStudent);

        // Admin Login Page
        admin.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AdminLoginActivity.class)));

        // Faculty Login Page
        faculty.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FacultyLoginActivity.class)));

        // Student Login Page
        student.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, StudentLoginActivity.class)));
    }
}
