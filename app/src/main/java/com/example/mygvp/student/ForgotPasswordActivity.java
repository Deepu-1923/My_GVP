package com.example.mygvp.student;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.R;
import com.example.mygvp.utils.EmailSender;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etCode, etNewPassword;
    private TextInputLayout layoutCode, layoutNewPass;
    private MaterialButton btnAction;
    private DatabaseReference dbRef;
    private String userKey, generatedCode;
    private DatabaseReference studentFullRef;

    // Pulled from keys.xml
    private String SENDER_EMAIL;
    private String SENDER_PASSWORD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        SENDER_EMAIL = getString(R.string.sender_email);
        SENDER_PASSWORD = getString(R.string.sender_password);

        etEmail = findViewById(R.id.etResetEmail);
        etCode = findViewById(R.id.etResetCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        layoutCode = findViewById(R.id.layoutCode);
        layoutNewPass = findViewById(R.id.layoutNewPass);
        btnAction = findViewById(R.id.btnResetAction);

        dbRef = FirebaseDatabase.getInstance().getReference("students");

        btnAction.setOnClickListener(v -> {
            if (btnAction.getText().toString().equals("Send Code")) {
                sendCode();
            } else {
                resetPassword();
            }
        });
    }

    private void sendCode() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etEmail.setError("Email required");
            return;
        }

        btnAction.setEnabled(false);
        btnAction.setText("Checking...");

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;
                for (DataSnapshot branchSnap : snapshot.getChildren()) {
                    for (DataSnapshot batchSnap : branchSnap.getChildren()) {
                        for (DataSnapshot ds : batchSnap.getChildren()) {
                            String dbEmail = ds.child("email").getValue(String.class);
                            if (dbEmail != null && email.equalsIgnoreCase(dbEmail.trim())) {
                                userKey = ds.getKey();
                                studentFullRef = ds.getRef();
                                generatedCode = String.valueOf(new Random().nextInt(899999) + 100000);

                                executeEmailTask(email, generatedCode);
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                    if (found) break;
                }
                
                if (!found) {
                    btnAction.setEnabled(true);
                    btnAction.setText("Send Code");
                    Toast.makeText(ForgotPasswordActivity.this, "This email is not registered!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnAction.setEnabled(true);
            }
        });
    }

    private void executeEmailTask(String recipientEmail, String code) {
        EmailSender sender = new EmailSender(SENDER_EMAIL, SENDER_PASSWORD);
        String subject = "MyGVP: Password Reset Code";
        String body = "Hello,\n\nYour 6-digit verification code for the MyGVP Student Portal is: " + code + 
                     "\n\nIf you did not request this, please ignore this email.";

        sender.sendEmail(recipientEmail, subject, body, new EmailSender.EmailListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email", Toast.LENGTH_LONG).show();
                layoutCode.setVisibility(View.VISIBLE);
                layoutNewPass.setVisibility(View.VISIBLE);
                btnAction.setEnabled(true);
                btnAction.setText("Reset Password");
                etEmail.setEnabled(false);
            }

            @Override
            public void onFailure(Exception e) {
                btnAction.setEnabled(true);
                btnAction.setText("Send Code");
                Log.e("ForgotPassword", "Mail fail", e);
                Toast.makeText(ForgotPasswordActivity.this, "Error sending email. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetPassword() {
        String inputCode = etCode.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (inputCode.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please enter OTP and new password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (inputCode.equals(generatedCode)) {
            if (studentFullRef != null) {
                studentFullRef.child("password").setValue(newPass).addOnSuccessListener(aVoid -> {
                    Toast.makeText(ForgotPasswordActivity.this, "Password reset successful!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        } else {
            Toast.makeText(this, "Invalid OTP!", Toast.LENGTH_SHORT).show();
        }
    }
}
