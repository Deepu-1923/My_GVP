package com.example.mygvp.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class AdminEditStudentActivity extends AppCompatActivity {

    private ShapeableImageView ivProfile;
    private TextInputEditText etRollNo, etName, etEmail, etPassword;
    private MaterialButton btnChangeImage, btnSave;
    private TextView tvTitle;
    
    private boolean isEdit = false;
    private StudentModel student;
    private String branch, batch;
    private Uri imageUri;
    private static final int PICK_IMAGE = 101;
    private DatabaseReference dbRef, metadataRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_student);

        dbRef = FirebaseDatabase.getInstance().getReference("students");
        metadataRef = FirebaseDatabase.getInstance().getReference("metadata");

        ivProfile = findViewById(R.id.ivStudentProfile);
        etRollNo = findViewById(R.id.etRollNo);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnSave = findViewById(R.id.btnSave);
        tvTitle = findViewById(R.id.tvTitle);

        isEdit = getIntent().getBooleanExtra("IS_EDIT", false);
        
        if (isEdit) {
            student = (StudentModel) getIntent().getSerializableExtra("STUDENT");
            branch = student.getBranch();
            batch = student.getBatch();
            populateFields();
            tvTitle.setText("Edit Student Details");
            etRollNo.setEnabled(false);
        } else {
            branch = getIntent().getStringExtra("BRANCH");
            batch = getIntent().getStringExtra("BATCH");
            tvTitle.setText("Add New Student");
        }

        btnChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void populateFields() {
        etRollNo.setText(student.getRollNo());
        etName.setText(student.getName());
        etEmail.setText(student.getEmail());
        etPassword.setText(student.getPassword());
        if (student.getImageUrl() != null && !student.getImageUrl().isEmpty()) {
            Glide.with(this).load(student.getImageUrl()).into(ivProfile);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            ivProfile.setImageURI(imageUri);
        }
    }

    private void validateAndSave() {
        String roll = etRollNo.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (roll.isEmpty() || name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageAndSave(roll, name, email, pass);
        } else {
            saveToFirebase(roll, name, email, pass, isEdit ? student.getImageUrl() : "");
        }
    }

    private void uploadImageAndSave(String roll, String name, String email, String pass) {
        btnSave.setEnabled(false);
        btnSave.setText("Uploading Image...");

        String publicId = "students/" + branch + "/" + batch + "/" + roll;

        MediaManager.get().upload(imageUri)
                .option("public_id", publicId)
                .option("overwrite", true)
                .callback(new UploadCallback() {
            @Override public void onSuccess(String requestId, Map resultData) {
                saveToFirebase(roll, name, email, pass, (String) resultData.get("secure_url"));
            }
            @Override public void onError(String requestId, ErrorInfo error) {
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
                Toast.makeText(AdminEditStudentActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveToFirebase(String roll, String name, String email, String pass, String imgUrl) {
        StudentModel updatedStudent = new StudentModel();
        updatedStudent.setName(name);
        updatedStudent.setEmail(email);
        updatedStudent.setPassword(pass);
        updatedStudent.setImageUrl(imgUrl);

        dbRef.child(branch).child(batch).child(roll).setValue(updatedStudent).addOnSuccessListener(aVoid -> {
            if (!isEdit) {
                metadataRef.child("student_count").setValue(ServerValue.increment(1));
            }
            Toast.makeText(this, "Student saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
