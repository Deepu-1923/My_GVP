package com.example.mygvp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminManageFacultyActivity extends AppCompatActivity {

    private Spinner spinnerBranch;
    private MaterialButton btnViewDetails;
    private RecyclerView rvFaculty;
    private FloatingActionButton fabAddFaculty;
    private DatabaseReference dbRef;
    private FacultyAdapter adapter;
    private List<FacultyModel> facultyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_faculty);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        dbRef = FirebaseDatabase.getInstance().getReference("faculty");

        spinnerBranch = findViewById(R.id.spinnerBranch);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        rvFaculty = findViewById(R.id.rvFaculty);
        fabAddFaculty = findViewById(R.id.fabAddFaculty);

        setupSpinners();

        facultyList = new ArrayList<>();
        rvFaculty.setLayoutManager(new LinearLayoutManager(this));

        btnViewDetails.setOnClickListener(v -> fetchFaculty());

        fabAddFaculty.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEditFacultyActivity.class);
            intent.putExtra("IS_EDIT", false);
            intent.putExtra("BRANCH", spinnerBranch.getSelectedItem().toString());
            startActivity(intent);
        });
    }

    private void setupSpinners() {
        String[] branches = {"CSE", "ECE", "MECH", "CIVIL", "CSM"};
        spinnerBranch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, branches));
    }

    private void fetchFaculty() {
        String branch = spinnerBranch.getSelectedItem().toString();

        dbRef.child(branch).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                facultyList.clear();
                for (DataSnapshot facultySnap : snapshot.getChildren()) {
                    FacultyModel faculty = facultySnap.getValue(FacultyModel.class);
                    if (faculty != null) {
                        faculty.setFacultyId(facultySnap.getKey());
                        faculty.setBranch(branch);
                        facultyList.add(faculty);
                    }
                }
                adapter = new FacultyAdapter(AdminManageFacultyActivity.this, facultyList, (faculty) -> {
                    Intent intent = new Intent(AdminManageFacultyActivity.this, AdminEditFacultyActivity.class);
                    intent.putExtra("IS_EDIT", true);
                    intent.putExtra("FACULTY", faculty);
                    startActivity(intent);
                }, (faculty) -> {
                    deleteFaculty(faculty);
                });
                rvFaculty.setAdapter(adapter);
                if (facultyList.isEmpty()) {
                    Toast.makeText(AdminManageFacultyActivity.this, "No faculty found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminManageFacultyActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteFaculty(FacultyModel faculty) {
        dbRef.child(faculty.getBranch()).child(faculty.getFacultyId())
                .removeValue().addOnSuccessListener(aVoid -> Toast.makeText(this, "Faculty deleted", Toast.LENGTH_SHORT).show());
    }
}
