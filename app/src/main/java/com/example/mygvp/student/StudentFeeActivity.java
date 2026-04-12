package com.example.mygvp.student;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
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
import java.util.Date;
import java.util.Locale;

public class StudentFeeActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerYear, spinnerSemester;
    private Button btnViewReceipt;
    private LinearLayout layoutReceipt;
    private TextView tvStudentInfo, tvPaymentDate, tvTransactionType, tvAmount, tvAmountInWords, tvDueAmount;

    private String rollNo, branch, batch, studentName;

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
        rollNo = prefs.getString("LOGGED_IN_ROLL_NO", "");
        branch = prefs.getString("LOGGED_IN_BRANCH", "");
        batch = prefs.getString("LOGGED_IN_BATCH", "");
        studentName = prefs.getString("LOGGED_IN_NAME", "");

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

        String[] years = {"1", "2", "3", "4"};
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));

        String[] semesters = {"1", "2"};
        spinnerSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, semesters));

        btnViewReceipt.setOnClickListener(v -> loadFeeReceipt());
    }

    private void loadFeeReceipt() {
        String year = spinnerYear.getText().toString().trim();
        String sem = spinnerSemester.getText().toString().trim();

        if (year.isEmpty() || sem.isEmpty()) {
            Toast.makeText(this, "Please select year and semester", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(branch)
                .child(batch)
                .child(rollNo);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    layoutReceipt.setVisibility(View.GONE);
                    Toast.makeText(StudentFeeActivity.this, "Student record not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // First try to find data in the feeHistory node for the selected year and semester
                DataSnapshot history = snapshot.child("feeHistory").child(year).child(sem);
                if (history.exists()) {
                    displayReceipt(history, year, sem);
                } else {
                    // Fallback to the main student record if it matches the selected year and semester
                    Object sYearObj = snapshot.child("year").getValue();
                    Object sSemObj = snapshot.child("semester").getValue();
                    String sYear = String.valueOf(sYearObj != null ? sYearObj : "");
                    String sSem = String.valueOf(sSemObj != null ? sSemObj : "");
                    
                    if (year.equals(sYear) && sem.equals(sSem)) {
                        displayReceipt(snapshot, year, sem);
                    } else {
                        layoutReceipt.setVisibility(View.GONE);
                        Toast.makeText(StudentFeeActivity.this, "No record found for Year " + year + " Sem " + sem, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
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

        if (paymentDate == null) paymentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        if (transType == null) transType = "Online Transfer";

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
                return Double.parseDouble((String) val);
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
