package com.example.mygvp.student;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvName;
    private ImageView imgProfile;
    private FloatingActionButton imgSettings;
    private CardView btnLogout;

    private CardView cardAttendance, cardFee, cardAchievement,
            cardResults, cardLostFound, cardSports;

    private DatabaseReference studentRef;
    private String rollNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // UI references
        tvName = findViewById(R.id.tvName);
        imgProfile = findViewById(R.id.imgProfile);
        imgSettings = findViewById(R.id.imgSettings);
        btnLogout = findViewById(R.id.btnLogout);

        cardAttendance = findViewById(R.id.cardAttendance);
        cardFee = findViewById(R.id.cardFee);
        cardAchievement = findViewById(R.id.cardAchievement);
        cardResults = findViewById(R.id.cardResults);
        cardLostFound = findViewById(R.id.cardLostFound);
        cardSports = findViewById(R.id.cardSports);

        // Get roll number from Intent
        rollNo = getIntent().getStringExtra("rollNo");

        if (rollNo == null || rollNo.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
            rollNo = prefs.getString("LOGGED_IN_ROLL_NO", null);
            if (rollNo == null) {
                finish();
                return;
            }
        }

        // Firebase reference to specific student
        studentRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(rollNo);

        loadStudentProfile();
        setupDashboardClicks();
    }

    private void loadStudentProfile() {
        studentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String name = snapshot.child("name").getValue(String.class);
                String imageUrl = snapshot.child("imageUrl").getValue(String.class);

                tvName.setText(name != null ? name : "Student");

                // Loading Profile Image with Glide
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(StudentDashboardActivity.this)
                            .load(imageUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(imgProfile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupDashboardClicks() {
        imgSettings.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> logoutUser());

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
