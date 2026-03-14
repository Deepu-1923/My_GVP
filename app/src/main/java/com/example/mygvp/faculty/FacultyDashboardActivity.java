package com.example.mygvp.faculty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mygvp.LostAndFoundActivity;
import com.example.mygvp.MainActivity;
import com.example.mygvp.R;
import com.example.mygvp.UploadAchievementActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;

public class FacultyDashboardActivity extends AppCompatActivity {

    private TextView tvFacultyName, tvWelcome;
    private ImageView ivProfile;
    private DatabaseReference facultyRef;
    private MaterialButton btnReportLost, btnUploadResults, btnUploadAttendance, btnUploadAchievements;
    private View btnMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        tvFacultyName = findViewById(R.id.tvFacultyName);
        tvWelcome = findViewById(R.id.tvWelcomeTitle);
        ivProfile = findViewById(R.id.ivFacultyProfile);
        btnReportLost = findViewById(R.id.btnReportLost);
        btnUploadResults = findViewById(R.id.btnUploadResults);
        btnUploadAttendance = findViewById(R.id.btnUploadAttendance);
        btnUploadAchievements = findViewById(R.id.btnUploadAchievements);
        btnMenu = findViewById(R.id.btnMenu);

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

                    if (tvFacultyName != null) tvFacultyName.setText(name != null ? name : "Faculty");
                    if (tvWelcome != null) tvWelcome.setText("Welcome,");

                    if (imageUrl != null && !imageUrl.isEmpty() && ivProfile != null) {
                        Glide.with(FacultyDashboardActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(ivProfile);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        if (btnUploadResults != null) {
            btnUploadResults.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenu().add("View Student Results");
                popup.getMenu().add("Upload New Results");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("View Student Results")) {
                        startActivity(new Intent(this, FacultyViewResultsActivity.class));
                    } else {
                        startActivity(new Intent(this, FacultyUploadResultsActivity.class));
                    }
                    return true;
                });
                popup.show();
            });
        }

        if (btnUploadAttendance != null) {
            btnUploadAttendance.setOnClickListener(v -> 
                Toast.makeText(this, "Attendance module coming soon", Toast.LENGTH_SHORT).show());
        }

        if (btnUploadAchievements != null) {
            btnUploadAchievements.setOnClickListener(v -> 
                startActivity(new Intent(this, UploadAchievementActivity.class)));
        }

        if (btnReportLost != null) {
            btnReportLost.setOnClickListener(v -> {
                startActivity(new Intent(FacultyDashboardActivity.this, LostAndFoundActivity.class));
            });
        }

        if (btnMenu != null) {
            btnMenu.setOnClickListener(this::showPopupMenu);
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Change Password");
        popup.getMenu().add("Logout");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Logout")) {
                logout();
                return true;
            } else if (item.getTitle().equals("Change Password")) {
                Toast.makeText(this, "Change Password clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Intent intent = new Intent(FacultyDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
