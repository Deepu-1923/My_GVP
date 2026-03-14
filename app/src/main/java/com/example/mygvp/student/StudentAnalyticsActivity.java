package com.example.mygvp.student;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.mygvp.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudentAnalyticsActivity extends AppCompatActivity {

    private PieChart attendancePieChart;
    private LineChart resultsLineChart;
    private TextView tvAchievementCount, tvCurrentYear, tvBacklogs;
    private DatabaseReference studentRef;
    private String rollNo, branch, batch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_analytics);

        Toolbar toolbar = findViewById(R.id.toolbarAnalytics);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        attendancePieChart = findViewById(R.id.attendancePieChart);
        resultsLineChart = findViewById(R.id.resultsLineChart);
        tvAchievementCount = findViewById(R.id.tvAchievementCount);
        tvCurrentYear = findViewById(R.id.tvCurrentYear);
        tvBacklogs = findViewById(R.id.tvBacklogs);

        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        rollNo = prefs.getString("LOGGED_IN_ROLL_NO", "");
        branch = prefs.getString("LOGGED_IN_BRANCH", "");
        batch = prefs.getString("LOGGED_IN_BATCH", "");

        studentRef = FirebaseDatabase.getInstance().getReference("students")
                .child(branch).child(batch).child(rollNo);

        loadAnalyticsData();
    }

    private void loadAnalyticsData() {
        // 1. Load Attendance
        studentRef.child("attendance_percentage").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float present = 85f;
                if (snapshot.exists()) {
                    try {
                        present = Float.parseFloat(String.valueOf(snapshot.getValue()));
                    } catch (Exception e) {}
                }
                setupAttendanceChart(present, 100f - present);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Load Results for Line Chart
        studentRef.child("results").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Entry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                
                int index = 0;
                if (snapshot.exists()) {
                    // We iterate through years and semesters to build a timeline
                    for (DataSnapshot yearSnap : snapshot.getChildren()) {
                        String yearKey = yearSnap.getKey();
                        for (DataSnapshot semSnap : yearSnap.getChildren()) {
                            try {
                                Object cgpaObj = semSnap.child("cgpa").getValue();
                                if (cgpaObj != null) {
                                    float gpa = Float.parseFloat(String.valueOf(cgpaObj));
                                    entries.add(new Entry(index, gpa));
                                    labels.add(yearKey + "-" + semSnap.getKey());
                                    index++;
                                }
                            } catch (Exception e) {}
                        }
                    }
                }
                
                if (entries.isEmpty()) {
                    entries.add(new Entry(0, 0f));
                    labels.add("N/A");
                }
                setupResultsChart(entries, labels);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Stats logic
        tvCurrentYear.setText(batch);
        
        studentRef.child("achievements").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvAchievementCount.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        studentRef.child("backlogs").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvBacklogs.setText(String.valueOf(snapshot.getChildrenCount()));
                } else {
                    tvBacklogs.setText("0");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupAttendanceChart(float present, float absent) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(present, "Present"));
        entries.add(new PieEntry(absent, "Absent"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        
        // Green for Present, Red for Absent
        int greenColor = Color.parseColor("#4CAF50");
        int redColor = Color.parseColor("#F44336");
        
        dataSet.setColors(new int[]{greenColor, redColor});
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        attendancePieChart.setData(data);
        attendancePieChart.setHoleRadius(75f);
        attendancePieChart.setTransparentCircleRadius(80f);
        attendancePieChart.setCenterText(present + "%");
        attendancePieChart.setCenterTextColor(Color.BLACK);
        attendancePieChart.setCenterTextSize(22f);
        attendancePieChart.getDescription().setEnabled(false);
        attendancePieChart.getLegend().setEnabled(true);
        attendancePieChart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        attendancePieChart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        attendancePieChart.animateY(1200);
        attendancePieChart.invalidate();
    }

    private void setupResultsChart(List<Entry> entries, List<String> labels) {
        LineDataSet dataSet = new LineDataSet(entries, "GPA Timeline");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient_blue_light));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        resultsLineChart.setData(data);
        
        XAxis xAxis = resultsLineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setTextColor(Color.BLACK);

        YAxis leftAxis = resultsLineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(10f); // Constant 1-10 scale
        leftAxis.setLabelCount(11, true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F1F1F1"));

        resultsLineChart.getAxisRight().setEnabled(false);
        resultsLineChart.getDescription().setEnabled(false);
        resultsLineChart.animateX(1200);
        resultsLineChart.invalidate();
    }
}
