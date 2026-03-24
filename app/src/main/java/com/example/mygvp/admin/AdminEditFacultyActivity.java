package com.example.mygvp.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.Map;

public class AdminEditFacultyActivity extends AppCompatActivity {

    private ShapeableImageView ivProfile;
    private TextInputEditText etFacultyId, etName, etEmail, etSpec, etQual, etPassword;
    private MaterialButton btnChangeImage, btnSave;
    private TextView tvTitle;

    private boolean isEdit = false;
    private FacultyModel faculty;
    private String branch;
    private Uri imageUri;
    private static final int PICK_IMAGE = 102;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_faculty);

        dbRef = FirebaseDatabase.getInstance().getReference("faculty");

        ivProfile = findViewById(R.id.ivFacultyProfile);
        etFacultyId = findViewById(R.id.etFacultyId);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etSpec = findViewById(R.id.etSpecification);
        etQual = findViewById(R.id.etQualification);
        etPassword = findViewById(R.id.etPassword);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnSave = findViewById(R.id.btnSave);
        tvTitle = findViewById(R.id.tvTitle);

        isEdit = getIntent().getBooleanExtra("IS_EDIT", false);
        branch = getIntent().getStringExtra("BRANCH");

        if (isEdit) {
            faculty = (FacultyModel) getIntent().getSerializableExtra("FACULTY");
            branch = faculty.getBranch();
            populateFields();
            tvTitle.setText("Edit Faculty");
            etFacultyId.setEnabled(false);
        } else {
            tvTitle.setText("Add New Faculty");
        }

        btnChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void populateFields() {
        etFacultyId.setText(faculty.getFacultyId());
        etName.setText(faculty.getName());
        etEmail.setText(faculty.getEmail());
        etSpec.setText(faculty.getSpecification());
        etQual.setText(faculty.getQualification());
        etPassword.setText(faculty.getPassword());
        if (faculty.getImageUrl() != null && !faculty.getImageUrl().isEmpty()) {
            Glide.with(this).load(faculty.getImageUrl()).into(ivProfile);
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
        String fid = etFacultyId.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String spec = etSpec.getText().toString().trim();
        String qual = etQual.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (fid.isEmpty() || name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Required fields are empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageAndSave(fid, name, email, spec, qual, pass);
        } else {
            saveToFirebase(fid, name, email, spec, qual, pass, isEdit ? faculty.getImageUrl() : "");
        }
    }

    private void uploadImageAndSave(String fid, String name, String email, String spec, String qual, String pass) {
        btnSave.setEnabled(false);
        btnSave.setText("Uploading Image...");
        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override public void onSuccess(String requestId, Map resultData) {
                saveToFirebase(fid, name, email, spec, qual, pass, (String) resultData.get("secure_url"));
            }
            @Override public void onError(String requestId, ErrorInfo error) {
                btnSave.setEnabled(true);
                btnSave.setText("Save Faculty");
                Toast.makeText(AdminEditFacultyActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
            }
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveToFirebase(String fid, String name, String email, String spec, String qual, String pass, String imgUrl) {
        FacultyModel newFaculty = new FacultyModel();
        newFaculty.setName(name);
        newFaculty.setEmail(email);
        newFaculty.setSpecification(spec);
        newFaculty.setQualification(qual);
        newFaculty.setPassword(pass);
        newFaculty.setImageUrl(imgUrl);
        newFaculty.setBranch(branch);

        dbRef.child(branch).child(fid).setValue(newFaculty).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Faculty saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
