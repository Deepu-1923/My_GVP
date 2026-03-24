package com.example.mygvp.admin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.AdminUploadActivity;
import com.example.mygvp.MainActivity;
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

    private MaterialCardView cardStudentManagement, cardFacultyManagement, cardSystemNotices;
    private MaterialButton btnManageFees;
    private TextView tvTotalUsers;
    private ImageButton btnMenu;
    private DatabaseReference studentsRef, facultyRef, adminRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        cardStudentManagement = findViewById(R.id.cardStudentManagement);
        cardFacultyManagement = findViewById(R.id.cardFacultyManagement);
        cardSystemNotices = findViewById(R.id.cardSystemNotices);
        btnManageFees = findViewById(R.id.btnManageFees);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        btnMenu = findViewById(R.id.btnMenu);

        studentsRef = FirebaseDatabase.getInstance().getReference("students");
        facultyRef = FirebaseDatabase.getInstance().getReference("faculty");
        adminRef = FirebaseDatabase.getInstance().getReference("admin");

        setupRealTimeStats();

        btnMenu.setOnClickListener(v -> showCustomAdminMenu());

        cardStudentManagement.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminManageStudentsActivity.class));
        });

        cardFacultyManagement.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminManageFacultyActivity.class));
        });

        cardSystemNotices.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminUploadActivity.class));
        });

        if (btnManageFees != null) {
            btnManageFees.setOnClickListener(this::showFeesMenu);
        }
    }

    private void showFeesMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("View Fees Details");
        popup.getMenu().add("Manage Fees Details");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("View Fees Details")) {
                startActivity(new Intent(this, AdminViewFeesActivity.class));
            } else if (title.equals("Manage Fees Details")) {
                startActivity(new Intent(this, AdminManageFeesActivity.class));
            }
            return true;
        });
        popup.show();
    }

    private void showCustomAdminMenu() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_admin_menu);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout btnChangePass = dialog.findViewById(R.id.menuChangePassword);
        LinearLayout btnLogout = dialog.findViewById(R.id.menuLogout);

        btnChangePass.setOnClickListener(v -> {
            dialog.dismiss();
            showChangePasswordDialog();
        });

        btnLogout.setOnClickListener(v -> {
            dialog.dismiss();
            logoutAdmin();
        });

        dialog.show();
    }

    private void setupRealTimeStats() {
        ValueEventListener userCounterListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateTotalUserCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        studentsRef.addValueEventListener(userCounterListener);
        facultyRef.addValueEventListener(userCounterListener);
    }

    private void updateTotalUserCount() {
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                long studentCount = countUsersInNestedStructure(studentSnapshot);

                facultyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot facultySnapshot) {
                        long facultyCount = countUsersInNestedStructure(facultySnapshot);
                        long total = studentCount + facultyCount;

                        if (total >= 1000) {
                            tvTotalUsers.setText(String.format("%.1fk", total / 1000.0));
                        } else {
                            tvTotalUsers.setText(String.valueOf(total));
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private long countUsersInNestedStructure(DataSnapshot snapshot) {
        long count = 0;
        for (DataSnapshot level1 : snapshot.getChildren()) {
            for (DataSnapshot level2 : level1.getChildren()) {
                if (snapshot.getKey() != null && snapshot.getKey().equals("students")) {
                    count += level2.getChildrenCount();
                } else {
                    count++;
                }
            }
        }
        return count;
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
        Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
