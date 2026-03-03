package com.example.mygvp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygvp.admin.AdminLoginActivity;
import com.example.mygvp.faculty.FacultyLoginActivity;
import com.example.mygvp.student.StudentLoginActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ExtendedFloatingActionButton fabLogin;
    private RecyclerView rvFaculty, rvCampus, rvEvents;
    private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private static final int AUTO_SCROLL_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        fabLogin = findViewById(R.id.fabLogin);
        rvFaculty = findViewById(R.id.rvFaculty);
        rvCampus = findViewById(R.id.rvCampus);
        rvEvents = findViewById(R.id.rvEvents);

        setupCampusGallery();
        setupEventsGallery();
        setupFacultyList();

        fabLogin.setOnClickListener(v -> showLoginBottomSheet());
        
        startAutoScroll();
    }

    private void setupCampusGallery() {
        List<GalleryItem> items = new ArrayList<>();
        items.add(new GalleryItem("Main Entrance", "clg_img"));
        items.add(new GalleryItem("Academic Block", "clg_img"));
        items.add(new GalleryItem("Central Library", "clg_img"));
        
        rvCampus.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCampus.setAdapter(new GalleryAdapter(items));
        new PagerSnapHelper().attachToRecyclerView(rvCampus);
    }

    private void setupEventsGallery() {
        List<GalleryItem> items = new ArrayList<>();
        items.add(new GalleryItem("Tech Fest 2024", "clg_img"));
        items.add(new GalleryItem("Sports Meet", "ic_sports"));
        items.add(new GalleryItem("Cultural Night", "clg_img"));
        
        rvEvents.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvEvents.setAdapter(new GalleryAdapter(items));
        new PagerSnapHelper().attachToRecyclerView(rvEvents);
    }

    private void setupFacultyList() {
        List<Faculty> facultyList = new ArrayList<>();
        facultyList.add(new Faculty("Prof. D. Saritha", "Professor", "sarithad@gvpcdpgc.edu.in", "saritha"));
        facultyList.add(new Faculty("Dr.G.R.S Murthy", "Professor & H.O.D (CSE)", "murthy.grs@gvpcdpgc.edu.in", "dr_grs_murthy"));
        facultyList.add(new Faculty("Dr. Bh. Padma", "Professor", "padmabh@gvpcdpgc.edu.in", "padma"));
        facultyList.add(new Faculty("Dr. D Chandravathi", "Associate Professor", "chandravathid@gvpcdpgc.edu.in", "smt_d_chandravati"));
        facultyList.add(new Faculty("Sri. G. Kalyan Chakravarthi", "Assistant Professor", "gkalyan@gvpcdpgc.edu.in", "kalyan_chakravarthy_1"));
        facultyList.add(new Faculty("Sri R.Kanaka Raju", "Assistant Professor", "rkanakaraju@gvpcdpgc.edu.in", "r_kanaka_raju"));
        facultyList.add(new Faculty("Smt L.Pratibha", "Assistant Professor", "pratibha@gvpcdpgc.edu.in", "ic_profile_placeholder"));
        facultyList.add(new Faculty("Mr. M. Anil", "Assistant Professor", "anilmeka@gvpcdpgc.edu.in", "anil"));
        facultyList.add(new Faculty("Mr. S. Arun Kumar", "Assistant Professor", "arunkumar@gvpcdpgc.edu.in", "arun_kumar"));
        facultyList.add(new Faculty("Sri T.Sri Krishna", "Assistant Professor", "srikrishna@gvpcdpgc.edu.in", "mr_t_sri_krishna"));
        facultyList.add(new Faculty("Ms.C Aparna", "Assistant Professor", "aparna@gvpcdpgc.edu.in", "c_aparna"));
        facultyList.add(new Faculty("Ms.Vaishna C Bhanu", "Assistant Professor", "vaishnacbhanu@gvpcdpgc.edu.in", "vaishnacbhanu"));
        facultyList.add(new Faculty("Mrs.M V G Sirisha", "Assistant Professor", "sirisha2025@gvpcdpgc.edu.in", "sirisha_photo"));
        facultyList.add(new Faculty("Mr.Bharat Kumar Jagana", "Assistant Professor", "aparna@gvpcdpgc.edu.in", "bharat_kumar"));
        facultyList.add(new Faculty("Mrs.N Sai Susmitha Naidu", "Assistant Professor", "sushmita.n@gvpcdpgc.edu.in", "sushmitha_naidu"));
        
        rvFaculty.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFaculty.setAdapter(new FacultyAdapter(facultyList));
        new PagerSnapHelper().attachToRecyclerView(rvFaculty);
    }

    private void startAutoScroll() {
        Runnable scrollRunnable = new Runnable() {
            @Override
            public void run() {
                autoScrollRecyclerView(rvCampus);
                autoScrollRecyclerView(rvEvents);
                autoScrollRecyclerView(rvFaculty);
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY);
            }
        };
        autoScrollHandler.postDelayed(scrollRunnable, AUTO_SCROLL_DELAY);
    }

    private void autoScrollRecyclerView(RecyclerView rv) {
        if (rv.getAdapter() == null || rv.getLayoutManager() == null) return;
        int currentPos = ((LinearLayoutManager) rv.getLayoutManager()).findFirstVisibleItemPosition();
        int nextPos = (currentPos + 1) % rv.getAdapter().getItemCount();
        rv.smoothScrollToPosition(nextPos);
    }

    private void showLoginBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_login, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        MaterialButton btnAdmin = bottomSheetView.findViewById(R.id.bsBtnAdmin);
        MaterialButton btnFaculty = bottomSheetView.findViewById(R.id.bsBtnFaculty);
        MaterialButton btnStudent = bottomSheetView.findViewById(R.id.bsBtnStudent);

        btnAdmin.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(MainActivity.this, AdminLoginActivity.class));
        });

        btnFaculty.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(MainActivity.this, FacultyLoginActivity.class));
        });

        btnStudent.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(MainActivity.this, StudentLoginActivity.class));
        });

        bottomSheetDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoScrollHandler.removeCallbacksAndMessages(null);
    }
}