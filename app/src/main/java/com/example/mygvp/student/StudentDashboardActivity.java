package com.example.mygvp.student;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.R;

public class StudentDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        String rollNo = getIntent().getStringExtra("rollNo");

        TextView tv = findViewById(R.id.tvStudentInfo);
        tv.setText("Hello Student : " + rollNo);
    }
}
