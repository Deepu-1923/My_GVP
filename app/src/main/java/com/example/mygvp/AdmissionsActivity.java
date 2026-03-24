package com.example.mygvp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AdmissionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admissions);

        Toolbar toolbar = findViewById(R.id.toolbarAdmissions);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admissions");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.rvAdmissions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Course> courses = new ArrayList<>();
        // Engineering Programmes
        courses.add(new Course("B.Tech. Civil Engineering", "₹ 92,400", "60 Seats"));
        courses.add(new Course("B.Tech. Computer Science and Engineering", "₹ 92,400", "180 Seats"));
        courses.add(new Course("B.Tech. Computer Science and Engineering (AI & ML)", "₹ 92,400", "120 Seats"));
        courses.add(new Course("B.Tech. Electronics & Communication Engineering", "₹ 92,400", "120 Seats"));
        courses.add(new Course("B.Tech. Mechanical Engineering", "₹ 92,400", "60 Seats"));

        // PG Programmes
        courses.add(new Course("M.Tech. Structural Engineering", "₹ 75,700", "12 Seats"));
        courses.add(new Course("M.Tech. Computer Science and Technology", "₹ 75,700", "12 Seats"));

        CourseAdapter adapter = new CourseAdapter(courses);
        recyclerView.setAdapter(adapter);
    }
}
