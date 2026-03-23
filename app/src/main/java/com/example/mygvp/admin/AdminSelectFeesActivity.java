package com.example.mygvp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;

public class AdminSelectFeesActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerBatch, spinnerStream, spinnerYear, spinnerSem;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_select_fees);

        initViews();
        setupSpinners();

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String batch = spinnerBatch.getText().toString();
            String stream = spinnerStream.getText().toString();
            String year = spinnerYear.getText().toString();
            String sem = spinnerSem.getText().toString();

            if (batch.isEmpty() || stream.isEmpty() || year.isEmpty() || sem.isEmpty()) {
                Toast.makeText(this, "Please select all details", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, AdminViewFeesActivity.class);
            intent.putExtra("BATCH", batch);
            intent.putExtra("STREAM", stream);
            intent.putExtra("YEAR", year);
            intent.putExtra("SEM", sem);
            startActivity(intent);
        });
    }

    private void initViews() {
        spinnerBatch = findViewById(R.id.spinnerBatch);
        spinnerStream = findViewById(R.id.spinnerStream);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSem = findViewById(R.id.spinnerSem);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupSpinners() {
        String[] batches = {"2021-2025", "2022-2026", "2023-2027", "2024-2028"};
        String[] streams = {"CSE", "CSM", "ECE", "MECH", "CIVIL"};
        String[] years = {"1", "2", "3", "4"};
        String[] sems = {"1", "2"};

        spinnerBatch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, batches));
        spinnerStream.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, streams));
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));
        spinnerSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sems));
    }
}
