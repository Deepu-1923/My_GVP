package com.example.mygvp.faculty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.mygvp.MainActivity;
import com.example.mygvp.R;
import com.google.firebase.database.*;

public class FacultyDashboardActivity extends AppCompatActivity {

    private TextView tvFacultyName;
    private DatabaseReference facultyRef;
    private CardView btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        tvFacultyName = findViewById(R.id.tvFacultyName);
        btnLogout = findViewById(R.id.btnLogout);

        String facultyId = getIntent().getStringExtra("facultyId");

        if (facultyId == null) {
            SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
            facultyId = prefs.getString("LOGGED_IN_FACULTY_ID", null);
            if (facultyId == null) {
                finish();
                return;
            }
        }

        facultyRef = FirebaseDatabase.getInstance()
                .getReference("faculty")
                .child(facultyId);

        facultyRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvFacultyName.setText(snapshot.getValue(String.class));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(FacultyDashboardActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
