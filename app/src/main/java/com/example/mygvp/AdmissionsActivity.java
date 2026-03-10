package com.example.mygvp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AdmissionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admissions);

        Toolbar toolbar = findViewById(R.id.toolbarAdmissions);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admissions");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        setupCourseData();
    }

    private void setupCourseData() {
        // Engineering Programmes
        setCourse(R.id.course1, "B.Tech. Civil Engineering", "₹ 92,400", "60 Seats");
        setCourse(R.id.course2, "B.Tech. Computer Science and Engineering", "₹ 92,400", "180 Seats");
        setCourse(R.id.course3, "B.Tech. Computer Science and Engineering (AI & ML)", "₹ 92,400", "120 Seats");
        setCourse(R.id.course4, "B.Tech. Electronics & Communication Engineering", "₹ 92,400", "120 Seats");
        setCourse(R.id.course5, "B.Tech. Mechanical Engineering", "₹ 92,400", "60 Seats");

        // PG Programmes
        setCourse(R.id.course6, "M.Tech. Structural Engineering", "₹ 75,700", "12 Seats");
        setCourse(R.id.course7, "M.Tech. Computer Science and Technology", "₹ 75,700", "12 Seats");
    }

    private void setCourse(int viewId, String name, String fee, String intake) {
        View view = findViewById(viewId);
        if (view != null) {
            ((TextView) view.findViewById(R.id.tvCourseName)).setText(name);
            ((TextView) view.findViewById(R.id.tvCourseFee)).setText(fee);
            ((TextView) view.findViewById(R.id.tvCourseIntake)).setText(intake);
        }
    }
}
