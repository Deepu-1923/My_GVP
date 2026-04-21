package com.example.mygvp.student;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.mygvp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentFeeActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerYear, spinnerSemester;
    private Button btnViewReceipt;
    private LinearLayout layoutReceipt;
    private TextView tvStudentInfo, tvPaymentDate, tvTransactionType, tvAmount, tvAmountInWords, tvDueAmount;

    private String rollNo, branch, batch, studentName;
    private DatabaseReference paymentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_fee);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        rollNo = prefs.getString("LOGGED_IN_ROLL_NO", "").trim();
        branch = prefs.getString("LOGGED_IN_BRANCH", "").trim();
        batch = prefs.getString("LOGGED_IN_BATCH", "").trim();
        studentName = prefs.getString("LOGGED_IN_NAME", "").trim();

        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        btnViewReceipt = findViewById(R.id.btnViewReceipt);
        layoutReceipt = findViewById(R.id.layoutReceipt);
        tvStudentInfo = findViewById(R.id.tvStudentInfo);
        tvPaymentDate = findViewById(R.id.tvPaymentDate);
        tvTransactionType = findViewById(R.id.tvTransactionType);
        tvAmount = findViewById(R.id.tvAmount);
        tvAmountInWords = findViewById(R.id.tvAmountInWords);
        tvDueAmount = findViewById(R.id.tvDueAmount);

        // Start at branch level to find the correct batch folder flexibly
        DatabaseReference branchRef = FirebaseDatabase.getInstance()
                .getReference("payments")
                .child(branch);

        branchRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot branchSnapshot) {
                if (!branchSnapshot.exists()) {
                    Toast.makeText(StudentFeeActivity.this, "No payment records found for " + branch, Toast.LENGTH_SHORT).show();
                    return;
                }

                DataSnapshot foundBatchSnap = null;
                // Try exact match first
                if (branchSnapshot.hasChild(batch)) {
                    foundBatchSnap = branchSnapshot.child(batch);
                } else {
                    // Try to find a folder that matches (e.g. 2025-29 matching 2025-2029)
                    String normInput = batch.replace("-20", "-");
                    for (DataSnapshot snap : branchSnapshot.getChildren()) {
                        String key = snap.getKey();
                        if (key != null && key.replace("-20", "-").equals(normInput)) {
                            foundBatchSnap = snap;
                            break;
                        }
                    }
                }

                if (foundBatchSnap == null) {
                    Toast.makeText(StudentFeeActivity.this, "No records found for " + branch + " " + batch, Toast.LENGTH_SHORT).show();
                    return;
                }

                // IMPORTANT: Update the class-level reference to the ACTUAL folder found
                paymentsRef = foundBatchSnap.getRef();
                
                List<String> years = new ArrayList<>();
                for (DataSnapshot yearSnap : foundBatchSnap.getChildren()) {
                    boolean studentFoundInYear = false;
                    for (DataSnapshot semSnap : yearSnap.getChildren()) {
                        if (checkIfStudentExists(semSnap, rollNo)) {
                            studentFoundInYear = true;
                            break;
                        }
                    }
                    if (studentFoundInYear) {
                        years.add(yearSnap.getKey());
                    }
                }
                
                if (!years.isEmpty()) {
                    ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(StudentFeeActivity.this,
                            android.R.layout.simple_list_item_1, years);
                    spinnerYear.setAdapter(yearAdapter);

                    String currentYear = spinnerYear.getText().toString();
                    if (currentYear.isEmpty() || !years.contains(currentYear)) {
                        spinnerYear.setText(years.get(0), false);
                        fetchSemesters(years.get(0));
                    }
                } else {
                    Toast.makeText(StudentFeeActivity.this, "No fee records found for " + rollNo, Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        spinnerYear.setOnItemClickListener((parent, view, position, id) -> {
            String selectedYear = (String) parent.getItemAtPosition(position);
            fetchSemesters(selectedYear);
        });

        btnViewReceipt.setOnClickListener(v -> loadFeeReceipt());
    }

    private boolean checkIfStudentExists(DataSnapshot semSnap, String roll) {
        if (semSnap.hasChild(roll)) return true;
        for (DataSnapshot student : semSnap.getChildren()) {
            if (roll.equals(student.getKey())) return true;
        }
        return false;
    }

    private void fetchSemesters(String selectedYear) {
        if (paymentsRef == null) return;
        
        paymentsRef.child(selectedYear).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> semesters = new ArrayList<>();
                for (DataSnapshot semSnap : snapshot.getChildren()) {
                    if (checkIfStudentExists(semSnap, rollNo)) {
                        semesters.add(semSnap.getKey());
                    }
                }
                
                if (!semesters.isEmpty()) {
                    ArrayAdapter<String> semAdapter = new ArrayAdapter<>(StudentFeeActivity.this,
                            android.R.layout.simple_list_item_1, semesters);
                    spinnerSemester.setAdapter(semAdapter);
                    
                    String currentSem = spinnerSemester.getText().toString();
                    if (currentSem.isEmpty() || !semesters.contains(currentSem)) {
                        spinnerSemester.setText(semesters.get(0), false);
                    }
                } else {
                    spinnerSemester.setText("", false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadFeeReceipt() {
        String year = spinnerYear.getText().toString().trim();
        String sem = spinnerSemester.getText().toString().trim();

        if (paymentsRef == null || year.isEmpty() || sem.isEmpty()) {
            Toast.makeText(this, "Please select year and semester", Toast.LENGTH_SHORT).show();
            return;
        }

        paymentsRef.child(year).child(sem).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot studentSnapshot = null;
                if (snapshot.hasChild(rollNo)) {
                    studentSnapshot = snapshot.child(rollNo);
                } else {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        if (rollNo.equals(s.getKey())) {
                            studentSnapshot = s;
                            break;
                        }
                    }
                }

                if (studentSnapshot == null || !studentSnapshot.exists()) {
                    layoutReceipt.setVisibility(View.GONE);
                    Toast.makeText(StudentFeeActivity.this, "No record found for Roll No " + rollNo, Toast.LENGTH_SHORT).show();
                    return;
                }
                displayReceipt(studentSnapshot, year, sem);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                 Toast.makeText(StudentFeeActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayReceipt(DataSnapshot data, String year, String sem) {
        Object paidVal = data.child("paidAmount").getValue();
        Object dueVal = data.child("dueAmount").getValue();
        String paymentDate = data.child("paymentDate").getValue(String.class);
        String transType = data.child("transactionType").getValue(String.class);

        double paid = getDoubleValue(paidVal);
        double due = getDoubleValue(dueVal);

        if (paymentDate == null || paymentDate.isEmpty()) paymentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        if (transType == null || transType.isEmpty()) transType = "Online Transfer";

        SpannableStringBuilder builder = new SpannableStringBuilder();
        appendBold(builder, "Name: ", studentName + "\n");
        appendBold(builder, "Roll Number: ", rollNo + "\n");
        appendBold(builder, "Semester: ", year + "-" + sem + "\n");
        appendBold(builder, "Branch: ", branch);
        tvStudentInfo.setText(builder);

        tvPaymentDate.setText(paymentDate);
        tvTransactionType.setText(transType);
        tvAmount.setText(String.format(Locale.getDefault(), "₹ %.2f", paid));
        tvAmountInWords.setText(convertToWords((long) paid) + " Only");
        tvDueAmount.setText(String.format(Locale.getDefault(), "Due Amount: ₹ %.2f", due));

        layoutReceipt.setVisibility(View.VISIBLE);
    }
    
    private double getDoubleValue(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Double) return (Double) val;
        if (val instanceof Long) return ((Long) val).doubleValue();
        if (val instanceof String) {
            try {
                return Double.parseDouble(((String) val).replaceAll("[^0-9.]", ""));
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private void appendBold(SpannableStringBuilder builder, String key, String value) {
        int start = builder.length();
        builder.append(key);
        builder.setSpan(new StyleSpan(Typeface.BOLD), start, start + key.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(value);
    }

    private String convertToWords(long n) {
        if (n == 0) return "Zero";
        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
                "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

        if (n < 20) return units[(int) n];
        if (n < 100) return tens[(int) (n / 10)] + ((n % 10 != 0) ? " " + units[(int) (n % 10)] : "");
        if (n < 1000) return units[(int) (n / 100)] + " Hundred" + ((n % 100 != 0) ? " and " + convertToWords(n % 100) : "");
        if (n < 100000) return convertToWords(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " + convertToWords(n % 1000) : "");
        if (n < 10000000) return convertToWords(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " + convertToWords(n % 100000) : "");
        return String.valueOf(n);
    }
}
