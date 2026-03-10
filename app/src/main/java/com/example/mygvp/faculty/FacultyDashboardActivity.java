package com.example.mygvp.faculty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.mygvp.LostAndFoundActivity;
import com.example.mygvp.MainActivity;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;

public class FacultyDashboardActivity extends AppCompatActivity {

    private TextView tvFacultyName, tvWelcome;
    private ImageView ivProfile;
    private DatabaseReference facultyRef;
    private CardView btnLogout;
    private MaterialButton btnReportLost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        tvFacultyName = findViewById(R.id.tvFacultyName);
        tvWelcome = findViewById(R.id.tvWelcomeTitle);
        ivProfile = findViewById(R.id.ivFacultyProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnReportLost = findViewById(R.id.btnReportLost);

        // Get data from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        String facultyId = prefs.getString("LOGGED_IN_FACULTY_ID", null);
        String branch = prefs.getString("LOGGED_IN_FACULTY_BRANCH", null);

        if (facultyId == null || branch == null) {
            Toast.makeText(this, "Session expired, please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Reference the EXACT faculty node using branch and facultyId
        facultyRef = FirebaseDatabase.getInstance()
                .getReference("faculty")
                .child(branch)
                .child(facultyId);

        // Fetch data once to populate dashboard
        facultyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String imageUrl = snapshot.child("imageUrl").getValue(String.class);

                    tvFacultyName.setText(name != null ? name : "Faculty");
                    tvWelcome.setText("Welcome,");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(FacultyDashboardActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(ivProfile);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        if (btnReportLost != null) {
            btnReportLost.setOnClickListener(v -> {
                startActivity(new Intent(FacultyDashboardActivity.this, LostAndFoundActivity.class));
            });
        }

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(FacultyDashboardActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
