package com.example.mygvp.student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.MainActivity;
import com.example.mygvp.R;
import com.example.mygvp.utils.EmailSender;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.Random;

public class StudentLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private DatabaseReference studentsRef;
    private String generatedOtp;
    
    // Developer credentials (set once by you in keys.xml)
    private String SENDER_EMAIL;
    private String SENDER_PASSWORD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        // Fetching the 'Postman' credentials you set in keys.xml
        SENDER_EMAIL = getString(R.string.sender_email);
        SENDER_PASSWORD = getString(R.string.sender_password);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        studentsRef = FirebaseDatabase.getInstance().getReference("students");

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> loginStudent());
        }
    }

    private void loginStudent() {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Verifying...");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                boolean isFound = false;
                for (DataSnapshot branchSnap : snapshot.getChildren()) {
                    for (DataSnapshot batchSnap : branchSnap.getChildren()) {
                        for (DataSnapshot studentSnap : batchSnap.getChildren()) {
                            String dbEmail = studentSnap.child("email").getValue(String.class);
                            String dbPassword = studentSnap.child("password").getValue(String.class);

                            if (dbEmail != null && dbPassword != null &&
                                    email.equalsIgnoreCase(dbEmail.trim()) && password.equals(dbPassword.trim())) {

                                isFound = true;
                                handleSuccessfulLogin(studentSnap, branchSnap.getKey(), batchSnap.getKey(), password);
                                break;
                            }
                        }
                        if (isFound) break;
                    }
                    if (isFound) break;
                }
                if (!isFound) Toast.makeText(StudentLoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
            }
        });
    }

    private void handleSuccessfulLogin(DataSnapshot studentSnap, String branch, String batch, String typedPassword) {
        String email = studentSnap.child("email").getValue(String.class);

        // If first time login (using default password)
        if ("gvp@123".equals(typedPassword)) {
            sendOtpAndShowBottomSheet(email, studentSnap, branch, batch);
        } else {
            proceedToHome(studentSnap, branch, batch);
        }
    }

    private void sendOtpAndShowBottomSheet(String targetEmail, DataSnapshot studentSnap, String branch, String batch) {
        generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setMessage("Sending 6-digit passcode to " + targetEmail + "...")
                .setCancelable(false)
                .show();

        EmailSender sender = new EmailSender(SENDER_EMAIL, SENDER_PASSWORD);
        String subject = "MyGVP Account Passcode";
        String body = "Hello " + studentSnap.child("name").getValue(String.class) + ",\n\n" +
                "Your 6-digit passcode for account activation is: " + generatedOtp + "\n\n" +
                "Enter this code in the app to secure your account.\n\n" +
                "Regards,\nMyGVP Team";
        
        sender.sendEmail(targetEmail, subject, body, new EmailSender.EmailListener() {
            @Override
            public void onSuccess() {
                loadingDialog.dismiss();
                showOtpDialog(targetEmail, studentSnap);
            }

            @Override
            public void onFailure(Exception e) {
                loadingDialog.dismiss();
                Log.e("StudentLogin", "Failed to send OTP", e);
                Toast.makeText(StudentLoginActivity.this, "Failed to send email. Ensure the developer has configured the App Password correctly.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showOtpDialog(String targetEmail, DataSnapshot studentSnap) {
        BottomSheetDialog otpDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_otp_verify, null);
        otpDialog.setContentView(view);

        TextView tvMsg = view.findViewById(R.id.tvOtpMessage);
        tvMsg.setText("Enter the 6-digit code sent to " + targetEmail);
        
        TextInputEditText etOtp = view.findViewById(R.id.etOtpCode);
        MaterialButton btnVerify = view.findViewById(R.id.btnVerifyOtp);

        btnVerify.setOnClickListener(v -> {
            String enteredOtp = etOtp.getText().toString().trim();
            if (generatedOtp.equals(enteredOtp)) {
                otpDialog.dismiss();
                showChangePasswordDialog(studentSnap);
            } else {
                Toast.makeText(this, "Invalid code. Please check your email.", Toast.LENGTH_SHORT).show();
            }
        });

        otpDialog.show();
    }

    private void showChangePasswordDialog(DataSnapshot studentSnap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Your New Password");
        builder.setCancelable(false);
        View dView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dView);
        
        TextInputEditText etNew = dView.findViewById(R.id.etNewPassword);
        dView.findViewById(R.id.etOldPassword).setVisibility(View.GONE); 
        MaterialButton btnUpdate = dView.findViewById(R.id.btnConfirmUpdate);
        
        AlertDialog dialog = builder.create();
        btnUpdate.setOnClickListener(v -> {
            String newP = etNew.getText().toString().trim();
            if (newP.length() < 6 || "gvp@123".equals(newP)) {
                Toast.makeText(this, "Please choose a password with at least 6 characters.", Toast.LENGTH_SHORT).show();
                return;
            }
            studentSnap.getRef().child("password").setValue(newP).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Account secured! Please login with your new password.", Toast.LENGTH_LONG).show();
                dialog.dismiss();
                etPassword.setText("");
            });
        });
        dialog.show();
    }

    private void proceedToHome(DataSnapshot studentSnap, String branch, String batch) {
        String rollNo = studentSnap.getKey();
        String name = toTitleCase(studentSnap.child("name").getValue(String.class));
        
        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("LOGGED_IN_ROLL_NO", rollNo);
        editor.putString("LOGGED_IN_NAME", name);
        editor.putString("LOGGED_IN_BRANCH", branch);
        editor.putString("LOGGED_IN_BATCH", batch);
        editor.putString("USER_TYPE", "STUDENT");
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return "Student";
        StringBuilder sb = new StringBuilder();
        boolean next = true;
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) next = true;
            else if (next) { c = Character.toUpperCase(c); next = false; }
            else c = Character.toLowerCase(c);
            sb.append(c);
        }
        return sb.toString();
    }
}
