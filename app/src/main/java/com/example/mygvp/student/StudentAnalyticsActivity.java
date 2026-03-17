package com.example.mygvp.student;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.mygvp.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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
import java.util.Arrays;
import java.util.List;

public class StudentAnalyticsActivity extends AppCompatActivity {

    private PieChart attendancePieChart;
    private BarChart resultsBarChart;
    private TextView tvAchievementCount, tvCurrentYear, tvBacklogs;
    private DatabaseReference studentRef;
    private DatabaseReference achievementsRef;
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
        resultsBarChart = findViewById(R.id.resultsBarChart);
        tvAchievementCount = findViewById(R.id.tvAchievementCount);
        tvCurrentYear = findViewById(R.id.tvCurrentYear);
        tvBacklogs = findViewById(R.id.tvBacklogs);

        SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
        rollNo = prefs.getString("LOGGED_IN_ROLL_NO", "");
        branch = prefs.getString("LOGGED_IN_BRANCH", "");
        batch = prefs.getString("LOGGED_IN_BATCH", "");

        studentRef = FirebaseDatabase.getInstance().getReference("students")
                .child(branch).child(batch).child(rollNo);
        
        achievementsRef = FirebaseDatabase.getInstance().getReference("achievements");

        showSampleData();
        loadAnalyticsData();
    }

    private void showSampleData() {
        setupAttendanceChart(85f, 15f);
        List<BarEntry> e1 = new ArrayList<>();
        List<BarEntry> e2 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            e1.add(new BarEntry(i, 0f));
            e2.add(new BarEntry(i, 0f));
        }
        setupResultsChart(e1, e2, Arrays.asList("Year 1", "Year 2", "Year 3", "Year 4"));
    }

    private void loadAnalyticsData() {
        // Attendance logic
        studentRef.child("attendance_percentage").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        float present = Float.parseFloat(String.valueOf(snapshot.getValue()));
                        setupAttendanceChart(present, 100f - present);
                    } catch (Exception ignored) {}
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Results logic (Side-by-side Bars)
        studentRef.child("results").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<BarEntry> entries1 = new ArrayList<>();
                    List<BarEntry> entries2 = new ArrayList<>();
                    int totalBacklogs = 0;
                    boolean hasAnyData = false;

                    for (int i = 1; i <= 4; i++) {
                        DataSnapshot yearSnap = snapshot.child(String.valueOf(i));
                        float s1 = 0, s2 = 0;
                        if (yearSnap.exists()) {
                            s1 = getGpa(yearSnap.child("1"));
                            s2 = getGpa(yearSnap.child("2"));
                            totalBacklogs += countBacklogs(yearSnap.child("1"));
                            totalBacklogs += countBacklogs(yearSnap.child("2"));
                            if (s1 > 0 || s2 > 0) hasAnyData = true;
                        }
                        entries1.add(new BarEntry(i - 1, s1));
                        entries2.add(new BarEntry(i - 1, s2));
                    }

                    tvBacklogs.setText(String.valueOf(totalBacklogs));
                    if (hasAnyData) {
                        setupResultsChart(entries1, entries2, Arrays.asList("Year 1", "Year 2", "Year 3", "Year 4"));
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        tvCurrentYear.setText(batch);
        achievementsRef.orderByChild("uploaderId").equalTo(rollNo).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvAchievementCount.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private int countBacklogs(DataSnapshot semSnap) {
        int count = 0;
        if (semSnap.exists()) {
            for (DataSnapshot sub : semSnap.getChildren()) {
                String key = sub.getKey();
                if (key == null || key.equals("sgpa") || key.equals("cgpa")) continue;
                
                Object gradeObj = sub.child("grades").getValue();
                if (gradeObj == null) gradeObj = sub.child("grade").getValue();
                
                if (gradeObj != null && "F".equalsIgnoreCase(String.valueOf(gradeObj))) {
                    count++;
                }
            }
        }
        return count;
    }

    private float getGpa(DataSnapshot semSnap) {
        if (!semSnap.exists()) return 0f;
        Object gpa = semSnap.child("sgpa").getValue();
        try { return gpa != null ? Float.parseFloat(String.valueOf(gpa)) : 0f; } catch (Exception e) { return 0f; }
    }

    private void setupAttendanceChart(float present, float absent) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(present, "Present")); 
        entries.add(new PieEntry(absent, "Absent"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#F44336"));
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        attendancePieChart.setData(data);
        attendancePieChart.setHoleRadius(75f);
        attendancePieChart.setCenterText(present + "%");
        attendancePieChart.setCenterTextSize(20f);
        attendancePieChart.getDescription().setEnabled(false);
        attendancePieChart.setDrawEntryLabels(false);
        
        Legend l = attendancePieChart.getLegend();
        l.setEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        attendancePieChart.invalidate();
    }

    private void setupResultsChart(List<BarEntry> entries1, List<BarEntry> entries2, List<String> labels) {
        BarDataSet set1 = new BarDataSet(entries1, "Sem 1");
        set1.setColor(Color.parseColor("#81D4FA")); // Light Blue
        set1.setDrawValues(false);

        BarDataSet set2 = new BarDataSet(entries2, "Sem 2");
        set2.setColor(Color.parseColor("#0288D1")); // Slightly Dark Blue
        set2.setDrawValues(false);

        BarData data = new BarData(set1, set2);
        
        float groupSpace = 0.08f;
        float barSpace = 0.02f;
        float barWidth = 0.44f;
        // (0.44 + 0.02) * 2 + 0.08 = 1.00 -> total width per group

        data.setBarWidth(barWidth);
        resultsBarChart.setData(data);
        resultsBarChart.groupBars(0f, groupSpace, barSpace);

        XAxis xAxis = resultsBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(data.getGroupWidth(groupSpace, barSpace) * labels.size());
        xAxis.setCenterAxisLabels(true);

        YAxis leftAxis = resultsBarChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(10f);
        leftAxis.setDrawGridLines(true);
        
        resultsBarChart.getAxisRight().setEnabled(false);
        resultsBarChart.getDescription().setEnabled(false);
        resultsBarChart.setTouchEnabled(false);
        
        Legend legend = resultsBarChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setYOffset(5f);

        resultsBarChart.invalidate();
    }
}
