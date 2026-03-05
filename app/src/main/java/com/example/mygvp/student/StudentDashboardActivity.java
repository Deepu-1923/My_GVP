package com.example.mygvp.student;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
    private TextView tvResultStat;
    private ImageView imgProfile;
    private CardView btnMenu;

    private CardView cardAttendance, cardFee, cardAchievement,
            cardResults, cardLostFound, cardSports;

    private DatabaseReference studentRef;
    private String rollNo, branch, batch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // UI references
        tvName = findViewById(R.id.tvName);
        tvResultStat = findViewById(R.id.tvResultStat);
        imgProfile = findViewById(R.id.imgProfile);
        btnMenu = findViewById(R.id.btnMenu);

        cardAttendance = findViewById(R.id.cardAttendance);
        cardFee = findViewById(R.id.cardFee);
        cardAchievement = findViewById(R.id.cardAchievement);
        cardResults = findViewById(R.id.cardResults);
        cardLostFound = findViewById(R.id.cardLostFound);
        cardSports = findViewById(R.id.cardSports);

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

        // Set the name IMMEDIATELY from what we saved during login
        tvName.setText(studentName);

        // Correct Firebase path: students > CSE > 2025-29 > 5251411001
        studentRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(branch)
                .child(batch)
                .child(rollNo);

        loadStudentProfile();
        loadLatestCGPA();
        setupDashboardClicks();
    }

    // --- UPDATED CLOUDINARY LOGIC ---
    private void loadStudentProfile() {
        String cloudName = "dlw4oisub";

        // f_auto handles both JPG and PNG automatically!
        String transformations = "w_300,h_300,c_fill,q_auto,f_auto";

        // This looks for the Roll Number as the Public ID in the main Cloudinary area
        String cloudinaryUrl = "https://res.cloudinary.com/" + cloudName + "/image/upload/" + transformations + "/" + rollNo;

        Glide.with(this)
                .load(cloudinaryUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(imgProfile);
    }

    // --- FETCH LATEST CGPA LOGIC ---
    private void loadLatestCGPA() {
        studentRef.child("results").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvResultStat.setText("--");
                    return;
                }

                int maxYear = -1;
                DataSnapshot latestYearSnapshot = null;

                // Step 1: Find the highest year (1, 2, 3, or 4)
                for (DataSnapshot yearSnap : snapshot.getChildren()) {
                    try {
                        int year = Integer.parseInt(yearSnap.getKey());
                        if (year > maxYear) {
                            maxYear = year;
                            latestYearSnapshot = yearSnap;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore any non-numeric nodes if they exist
                    }
                }

                if (latestYearSnapshot != null) {
                    int maxSemester = -1;
                    DataSnapshot latestSemesterSnapshot = null;

                    // Step 2: Inside that highest year, find the highest semester (1 or 2)
                    for (DataSnapshot semSnap : latestYearSnapshot.getChildren()) {
                        try {
                            int sem = Integer.parseInt(semSnap.getKey());
                            if (sem > maxSemester) {
                                maxSemester = sem;
                                latestSemesterSnapshot = semSnap;
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }

                    // Step 3: Extract the CGPA from that latest semester node
                    if (latestSemesterSnapshot != null) {
                        Object cgpaObj = latestSemesterSnapshot.child("cgpa").getValue();
                        if (cgpaObj != null) {
                            tvResultStat.setText(String.valueOf(cgpaObj));
                        } else {
                            tvResultStat.setText("--");
                        }
                    } else {
                        tvResultStat.setText("--");
                    }
                } else {
                    tvResultStat.setText("--");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvResultStat.setText("--");
            }
        });
    }

    private void setupDashboardClicks() {
        // Pop-up menu for the Hamburger icon
        btnMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(StudentDashboardActivity.this, btnMenu);
            popupMenu.getMenu().add("Change Password");
            popupMenu.getMenu().add("Logout");

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Change Password")) {
                    showChangePasswordDialog();
                } else if (item.getTitle().equals("Logout")) {
                    logoutUser();
                }
                return true;
            });
            popupMenu.show();
        });

        cardResults.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboardActivity.this, StudentResultsActivity.class);
            intent.putExtra("rollNo", rollNo);
            startActivity(intent);
        });

        cardAchievement.setOnClickListener(v -> {
            startActivity(new Intent(this, UploadAchievementActivity.class));
        });

        cardLostFound.setOnClickListener(v -> {
            startActivity(new Intent(StudentDashboardActivity.this, LostAndFoundActivity.class));
        });

        cardAttendance.setOnClickListener(v -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show());
        cardFee.setOnClickListener(v -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show());
        cardSports.setOnClickListener(v -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show());
    }

    private void logoutUser() {
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Intent intent = new Intent(StudentDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
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

            studentRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (oldP.equals(snapshot.getValue(String.class))) {
                        studentRef.child("password").setValue(newP).addOnSuccessListener(aVoid -> {
                            Toast.makeText(StudentDashboardActivity.this, "Success", Toast.LENGTH_SHORT).show();
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
}