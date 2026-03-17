package com.example.mygvp.faculty;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.mygvp.LostAndFoundActivity;
import com.example.mygvp.MainActivity;
import com.example.mygvp.R;
import com.example.mygvp.UploadAchievementActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FacultyDashboardActivity extends AppCompatActivity {

    private TextView tvFacultyName, tvStudentsManaged, tvAvgAttendance;
    private ImageView ivProfile;
    private DatabaseReference facultyRef;
    private MaterialButton btnReportLost, btnUploadResults, btnUploadAttendance, btnUploadAchievements;
    private View btnMenu, btnBack;
    private View loadingOverlay;
    private ImageView ivBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        tvFacultyName = findViewById(R.id.tvFacultyName);
        tvStudentsManaged = findViewById(R.id.tvStudentsManaged);
        tvAvgAttendance = findViewById(R.id.tvAvgAttendance);
        ivProfile = findViewById(R.id.ivFacultyProfile);
        btnReportLost = findViewById(R.id.btnReportLost);
        btnUploadResults = findViewById(R.id.btnUploadResults);
        btnUploadAttendance = findViewById(R.id.btnUploadAttendance);
        btnUploadAchievements = findViewById(R.id.btnUploadAchievements);
        btnMenu = findViewById(R.id.btnMenu);
        btnBack = findViewById(R.id.btnBack);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        ivBus = findViewById(R.id.ivBus);

        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
            startBusAnimation();
        }

        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        String facultyId = prefs.getString("LOGGED_IN_FACULTY_ID", null);
        String branch = prefs.getString("LOGGED_IN_FACULTY_BRANCH", null);

        if (facultyId == null || branch == null) {
            Toast.makeText(this, "Session expired, please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        facultyRef = FirebaseDatabase.getInstance().getReference("faculty").child(branch).child(facultyId);

        loadProfileAndStats();
        setupClicks();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.animate().alpha(0f).setDuration(400).withEndAction(() -> loadingOverlay.setVisibility(View.GONE)).start();
            }
        }, 1500);
    }

    private void loadProfileAndStats() {
        // Real-time listener for name and profile image
        facultyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                    
                    if (tvFacultyName != null) tvFacultyName.setText(name != null ? name : "Faculty");
                    
                    if (imageUrl != null && !imageUrl.isEmpty() && ivProfile != null) {
                        Glide.with(FacultyDashboardActivity.this)
                                .load(imageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(ivProfile);
                    }

                    // Real-time stats
                    Object managed = snapshot.child("students_managed").getValue();
                    Object attendance = snapshot.child("avg_attendance").getValue();
                    
                    if (tvStudentsManaged != null) tvStudentsManaged.setText(managed != null ? managed.toString() : "0");
                    if (tvAvgAttendance != null) tvAvgAttendance.setText(attendance != null ? attendance.toString() + "%" : "0%");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startBusAnimation() {
        if (ivBus != null) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(ivBus, "translationX", -400f, 400f);
            animator.setDuration(1500);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setInterpolator(new LinearInterpolator());
            animator.start();
        }
    }

    private void setupClicks() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

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
                startActivity(new Intent(this, FacultyViewAttendanceActivity.class)));
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
            btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenu().add("Change Password");
                popup.getMenu().add("Logout");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Logout")) {
                        logout();
                    } else if (item.getTitle().equals("Change Password")) {
                        showChangePasswordDialog();
                    }
                    return true;
                });
                popup.show();
            });
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        TextInputEditText etOldPass = dialogView.findViewById(R.id.etOldPassword);
        TextInputEditText etNewPass = dialogView.findViewById(R.id.etNewPassword);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btnConfirmUpdate);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnUpdate.setOnClickListener(v -> {
            String oldP = etOldPass.getText().toString().trim();
            String newP = etNewPass.getText().toString().trim();

            if (oldP.isEmpty() || newP.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            facultyRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (oldP.equals(snapshot.getValue(String.class))) {
                        facultyRef.child("password").setValue(newP).addOnSuccessListener(aVoid -> {
                            Toast.makeText(FacultyDashboardActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    } else {
                        etOldPass.setError("Wrong password");
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        });
        dialog.show();
    }

    private void logout() {
        getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
