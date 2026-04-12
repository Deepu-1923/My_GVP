package com.example.mygvp.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.AdminUploadActivity;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardActivity extends AppCompatActivity {

    private MaterialCardView cardStudentManagement, cardFacultyManagement, cardSystemNotices, cardFees;
    private TextView tvTotalUsers;
    private ImageButton btnMenu, btnBack;
    private DatabaseReference adminRef, metadataRef, studentsRef, facultyRef;
    private ValueEventListener metadataListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        cardStudentManagement = findViewById(R.id.cardStudentManagement);
        cardFacultyManagement = findViewById(R.id.cardFacultyManagement);
        cardSystemNotices = findViewById(R.id.cardSystemNotices);
        cardFees = findViewById(R.id.cardFees);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        btnMenu = findViewById(R.id.btnMenu);
        btnBack = findViewById(R.id.btnBack);

        adminRef = FirebaseDatabase.getInstance().getReference("admin");
        metadataRef = FirebaseDatabase.getInstance().getReference("metadata");
        studentsRef = FirebaseDatabase.getInstance().getReference("students");
        facultyRef = FirebaseDatabase.getInstance().getReference("faculty");

        // Temporary: Initialize counts if they don't exist
        initializeMetadataIfMissing();

        setupRealTimeStats();

        btnBack.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(v -> showCustomAdminMenu(v));

        cardStudentManagement.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminManageStudentsActivity.class));
        });

        cardFacultyManagement.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminManageFacultyActivity.class));
        });

        cardSystemNotices.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminUploadActivity.class));
        });

        cardFees.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminManageFeesActivity.class));
        });
    }

    private void initializeMetadataIfMissing() {
        metadataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !snapshot.hasChild("student_count")) {
                    // One-time manual count to set up the new system
                    countAndInitialize();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void countAndInitialize() {
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                long sCount = 0;
                for (DataSnapshot branch : studentSnapshot.getChildren()) {
                    for (DataSnapshot batch : branch.getChildren()) {
                        sCount += batch.getChildrenCount();
                    }
                }
                final long finalSCount = sCount;
                facultyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot facultySnapshot) {
                        long fCount = 0;
                        for (DataSnapshot branch : facultySnapshot.getChildren()) {
                            fCount += branch.getChildrenCount();
                        }
                        metadataRef.child("student_count").setValue(finalSCount);
                        metadataRef.child("faculty_count").setValue(fCount);
                        Toast.makeText(AdminDashboardActivity.this, "Database Stats Initialized!", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showCustomAdminMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add(0, 1, 0, "Change Password");
        popupMenu.getMenu().add(0, 2, 1, "Logout");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showChangePasswordDialog();
                return true;
            } else if (item.getItemId() == 2) {
                logoutAdmin();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void setupRealTimeStats() {
        metadataListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long studentCount = 0;
                long facultyCount = 0;
                
                if (snapshot.hasChild("student_count")) {
                    studentCount = snapshot.child("student_count").getValue(Long.class);
                }
                if (snapshot.hasChild("faculty_count")) {
                    facultyCount = snapshot.child("faculty_count").getValue(Long.class);
                }
                
                long total = studentCount + facultyCount;
                if (total >= 1000) {
                    tvTotalUsers.setText(String.format("%.1fk", total / 1000.0));
                } else {
                    tvTotalUsers.setText(String.valueOf(total));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        metadataRef.addValueEventListener(metadataListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (metadataRef != null && metadataListener != null) {
            metadataRef.removeEventListener(metadataListener);
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
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnUpdate.setOnClickListener(v -> {
            String oldP = etOldPass.getText().toString().trim();
            String newP = etNewPass.getText().toString().trim();

            if (oldP.isEmpty() || newP.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            adminRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (oldP.equals(snapshot.getValue(String.class))) {
                        adminRef.child("password").setValue(newP).addOnSuccessListener(aVoid -> {
                            Toast.makeText(AdminDashboardActivity.this, "Password updated!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    } else {
                        Toast.makeText(AdminDashboardActivity.this, "Incorrect old password", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        });
        dialog.show();
    }

    private void logoutAdmin() {
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Intent intent = new Intent(AdminDashboardActivity.this, com.example.mygvp.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
