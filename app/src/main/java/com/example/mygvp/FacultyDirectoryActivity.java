package com.example.mygvp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FacultyDirectoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FacultyAdapter adapter;
    private List<Object> directoryList;
    private DatabaseReference dbRef;
    private String selectedBranch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_directory);

        // 1. Get the branch name passed from MainActivity (e.g., "CSE")
        selectedBranch = getIntent().getStringExtra("BRANCH_NAME");
        if (selectedBranch == null) {
            selectedBranch = "CSE";
        }

        // 2. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarFaculty);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(selectedBranch + " Faculty");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // 3. Setup RecyclerView
        recyclerView = findViewById(R.id.rvFacultyDirectory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        directoryList = new ArrayList<>();
        adapter = new FacultyAdapter(this, directoryList);
        recyclerView.setAdapter(adapter);

        // 4. Initialize Firebase reference
        // Note: Removed setLogLevel() as it causes crashes when called after initialization
        dbRef = FirebaseDatabase.getInstance("https://mygvp-718d4-default-rtdb.firebaseio.com")
                .getReference("faculty")
                .child(selectedBranch);

        // 5. Fetch Data
        fetchFacultyData();
    }

    private void fetchFacultyData() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                directoryList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot facSnapshot : snapshot.getChildren()) {
                        Faculty faculty = facSnapshot.getValue(Faculty.class);
                        if (faculty != null) {
                            directoryList.add(faculty);
                        }
                    }
                } else {
                    Toast.makeText(FacultyDirectoryActivity.this, "No faculty found for " + selectedBranch, Toast.LENGTH_SHORT).show();
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FacultyDirectoryActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
