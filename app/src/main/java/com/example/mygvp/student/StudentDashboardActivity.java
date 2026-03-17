package com.example.mygvp.student;

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
import androidx.cardview.widget.CardView;

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

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvName;
    private TextView tvResultStat, tvAttendanceStat;
    private ImageView imgProfile;
    private View btnMenu, btnBack;
    private View loadingOverlay;
    private ImageView ivBus;

    private CardView cardAttendance, cardFee, cardAchievement,
            cardResults, cardLostFound, cardAnalytics;

    private DatabaseReference studentRef;
    private String rollNo, branch, batch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // UI references
        tvName = findViewById(R.id.tvName);
        tvResultStat = findViewById(R.id.tvResultStat);
        tvAttendanceStat = findViewById(R.id.tvAttendanceStat);
        imgProfile = findViewById(R.id.imgProfile);
        btnMenu = findViewById(R.id.btnMenu);
        btnBack = findViewById(R.id.btnBack);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        ivBus = findViewById(R.id.ivBus);

        cardAttendance = findViewById(R.id.cardAttendance);
        cardFee = findViewById(R.id.cardFee);
        cardAchievement = findViewById(R.id.cardAchievement);
        cardResults = findViewById(R.id.cardResults);
        cardLostFound = findViewById(R.id.cardLostFound);
        cardAnalytics = findViewById(R.id.cardAnalytics);

        // Show loading immediately
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
            startBusAnimation();
        }

        // Pull stored credentials
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        rollNo = prefs.getString("LOGGED_IN_ROLL_NO", "");
        String studentName = prefs.getString("LOGGED_IN_NAME", "Student");
        branch = prefs.getString("LOGGED_IN_BRANCH", "");
        batch = prefs.getString("LOGGED_IN_BATCH", "");

        if (rollNo.isEmpty() || branch.isEmpty() || batch.isEmpty()) {
            Toast.makeText(this, "Session expired, please login again.", Toast.LENGTH_SHORT).show();
            logoutUser();
            return;
        }

        tvName.setText(studentName);
        studentRef = FirebaseDatabase.getInstance().getReference("students").child(branch).child(batch).child(rollNo);

        loadStudentProfile();
        loadRealTimeStats();
        setupDashboardClicks();

        // Hide overlay after 1.5 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
                        .start();
            }
        }, 1500);
    }

    private void startBusAnimation() {
        if (ivBus != null) {
            // Move bus from Left (-350) to Right (350)
            ObjectAnimator animator = ObjectAnimator.ofFloat(ivBus, "translationX", -350f, 350f);
            animator.setDuration(1500); // Updated to match display time
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setInterpolator(new LinearInterpolator());
            animator.start();
        }
    }

    private void loadStudentProfile() {
        String cloudName = "dlw4oisub";
        String transformations = "w_300,h_300,c_fill,q_auto,f_auto";
        String cloudinaryUrl = "https://res.cloudinary.com/" + cloudName + "/image/upload/" + transformations + "/" + rollNo;

        Glide.with(this)
                .load(cloudinaryUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(imgProfile);
    }

    private void loadRealTimeStats() {
        studentRef.child("attendance_percentage").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) tvAttendanceStat.setText(snapshot.getValue().toString() + "%");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        studentRef.child("results").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                for (DataSnapshot yearSnap : snapshot.getChildren()) {
                    for (DataSnapshot semSnap : yearSnap.getChildren()) {
                        Object cgpa = semSnap.child("cgpa").getValue();
                        if (cgpa != null) tvResultStat.setText(String.valueOf(cgpa));
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupDashboardClicks() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(StudentDashboardActivity.this, btnMenu);
                popupMenu.getMenu().add("Change Password");
                popupMenu.getMenu().add("Logout");
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Change Password")) showChangePasswordDialog();
                    else if (item.getTitle().equals("Logout")) logoutUser();
                    return true;
                });
                popupMenu.show();
            });
        }

        if (cardResults != null) {
            cardResults.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, StudentResultsActivity.class);
                intent.putExtra("rollNo", rollNo);
                startActivity(intent);
            });
        }

        if (cardAchievement != null) {
            cardAchievement.setOnClickListener(v -> {
                startActivity(new Intent(this, UploadAchievementActivity.class));
            });
        }

        if (cardLostFound != null) {
            cardLostFound.setOnClickListener(v -> {
                startActivity(new Intent(StudentDashboardActivity.this, LostAndFoundActivity.class));
            });
        }

        if (cardAnalytics != null) {
            cardAnalytics.setOnClickListener(v -> {
                startActivity(new Intent(StudentDashboardActivity.this, StudentAnalyticsActivity.class));
            });
        }

        if (cardAttendance != null) {
            cardAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, StudentAttendanceActivity.class);
                intent.putExtra("rollNo", rollNo);
                intent.putExtra("branch", branch);
                intent.putExtra("batch", batch);
                // For now, let's assume we want to see latest. 
                // In a real app, you might have a selector.
                intent.putExtra("year", "1"); 
                intent.putExtra("sem", "1");
                startActivity(intent);
            });
        }

        if (cardFee != null) cardFee.setOnClickListener(v -> Toast.makeText(this, "Fee Payments", Toast.LENGTH_SHORT).show());
    }

    private void logoutUser() {
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        TextInputEditText etOldPass = dialogView.findViewById(R.id.etOldPassword);
        TextInputEditText etNewPass = dialogView.findViewById(R.id.etNewPassword);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btnConfirmUpdate);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        btnUpdate.setOnClickListener(v -> {
            String oldP = etOldPass.getText().toString().trim();
            String newP = etNewPass.getText().toString().trim();
            if (oldP.isEmpty() || newP.isEmpty()) return;
            studentRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (oldP.equals(snapshot.getValue(String.class))) {
                        studentRef.child("password").setValue(newP).addOnSuccessListener(aVoid -> dialog.dismiss());
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        });
        dialog.show();
    }
}
