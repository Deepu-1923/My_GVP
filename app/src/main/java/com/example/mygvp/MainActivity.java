package com.example.mygvp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private static final int AUTO_SCROLL_DELAY = 3000;
    private DatabaseReference dbRef;

    private RecyclerView rvCampus, rvFaculty, rvAdmin, rvSyllabus, rvContactInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);
        dbRef = FirebaseDatabase.getInstance().getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        initViews();
        setupMiniCards();
        setupContactInfo();
        
        findViewById(R.id.btnProfile).setOnClickListener(v -> showLoginBottomSheet());
        findViewById(R.id.btnLaunchPortals).setOnClickListener(v -> showLoginBottomSheet());
        
        startAutoScroll();
    }

    private void initViews() {
        rvCampus = findViewById(R.id.miniCampus).findViewById(R.id.rvMiniContent);
        rvFaculty = findViewById(R.id.miniFaculty).findViewById(R.id.rvMiniContent);
        rvAdmin = findViewById(R.id.miniAdmin).findViewById(R.id.rvMiniContent);
        rvSyllabus = findViewById(R.id.miniSyllabus).findViewById(R.id.rvMiniContent);
        rvContactInfo = findViewById(R.id.rvContactInfo);
    }

    private void setupMiniCards() {
        // 1. Campus Mini
        setupMiniHeader(findViewById(R.id.miniCampus), "Explore Campus");
        List<GalleryItem> campusItems = new ArrayList<>();
        campusItems.add(new GalleryItem("Campus", "clg_img"));
        campusItems.add(new GalleryItem("Entrance", "clg_img"));
        rvCampus.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCampus.setAdapter(new GalleryAdapter(campusItems));
        new PagerSnapHelper().attachToRecyclerView(rvCampus);

        // 2. Faculty Mini
        setupMiniHeader(findViewById(R.id.miniFaculty), "Faculty");
        List<Faculty> facultyList = getFacultyData();
        rvFaculty.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFaculty.setAdapter(new HorizontalFacultyAdapter(facultyList));

        // 3. Admin Mini
        setupMiniHeader(findViewById(R.id.miniAdmin), "Administrative");
        List<Faculty> adminList = getAdminData();
        rvAdmin.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvAdmin.setAdapter(new HorizontalFacultyAdapter(adminList));

        // 4. Syllabus Mini
        setupMiniHeader(findViewById(R.id.miniSyllabus), "Syllabus");
        List<GalleryItem> syllabusGraphics = new ArrayList<>();
        syllabusGraphics.add(new GalleryItem("R22", "ic_results"));
        syllabusGraphics.add(new GalleryItem("Calendar", "ic_attendance"));
        rvSyllabus.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSyllabus.setAdapter(new GalleryAdapter(syllabusGraphics));
        
        // Enable click interceptor ONLY for Syllabus card to allow scrolling on others
        View syllabusClicker = findViewById(R.id.miniSyllabus).findViewById(R.id.vClickInterceptor);
        if (syllabusClicker != null) {
            syllabusClicker.setClickable(true);
            syllabusClicker.setFocusable(true);
            syllabusClicker.setBackgroundResource(android.R.drawable.list_selector_background);
            syllabusClicker.setOnClickListener(v -> showSyllabusBottomSheet());
        }
    }

    private void setupMiniHeader(View card, String title) {
        TextView tv = card.findViewById(R.id.tvMiniTitle);
        tv.setText(title);
    }

    private List<Faculty> getFacultyData() {
        List<Faculty> list = new ArrayList<>();
        list.add(new Faculty("Dr. G.R.S Murthy", "H.O.D (CSE)", "murthy.grs@gvpcdpgc.edu.in", "dr_grs_murthy"));
        list.add(new Faculty("Sri R.Kanaka Raju", "Assistant Professor", "rkanakaraju@gvpcdpgc.edu.in", "r_kanaka_raju"));
        list.add(new Faculty("Mr. M. Anil", "Assistant Professor", "anilmeka@gvpcdpgc.edu.in", "anil"));
        return list;
    }

    private List<Faculty> getAdminData() {
        List<Faculty> list = new ArrayList<>();
        list.add(new Faculty("Prof. D. Saritha", "Dean", "sarithad@gvpcdpgc.edu.in", "saritha"));
        list.add(new Faculty("Dr. Bh. Padma", "Professor", "padmabh@gvpcdpgc.edu.in", "padma"));
        return list;
    }

    private void setupContactInfo() {
        List<ContactInfo> infoList = new ArrayList<>();
        infoList.add(new ContactInfo("📍 Address", "Gayatri Vidya Parishad College, Rushikonda, Visakhapatnam-530045.", android.R.drawable.ic_dialog_map));
        infoList.add(new ContactInfo("📧 Email", "principalgvpcdpgca@gmail.com", android.R.drawable.ic_dialog_email));
        infoList.add(new ContactInfo("📞 Contact", "0891-2783722 / 2955084", android.R.drawable.ic_menu_call));
        
        rvContactInfo.setLayoutManager(new LinearLayoutManager(this));
        rvContactInfo.setAdapter(new ContactInfoAdapter(infoList));
    }

    private void showSyllabusBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_syllabus, null);
        dialog.setContentView(view);

        AutoCompleteTextView spinnerYear = view.findViewById(R.id.spinnerYear);
        AutoCompleteTextView spinnerSem = view.findViewById(R.id.spinnerSem);
        ChipGroup chipGroupBranch = view.findViewById(R.id.chipGroupBranch);
        MaterialButton btnDownload = view.findViewById(R.id.btnDownloadSyllabus);
        MaterialButton btnCalendar = view.findViewById(R.id.btnDownloadCalendar);

        String[] years = {"1st Year", "2nd Year", "3rd Year", "4th Year"};
        String[] sems = {"Semester 1", "Semester 2"};

        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));
        spinnerSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sems));

        View.OnClickListener updateVisibility = v -> {
            if (!spinnerYear.getText().toString().isEmpty() && !spinnerSem.getText().toString().isEmpty()) {
                chipGroupBranch.setVisibility(View.VISIBLE);
            }
        };

        spinnerYear.setOnItemClickListener((parent, v1, position, id) -> updateVisibility.onClick(null));
        spinnerSem.setOnItemClickListener((parent, v1, position, id) -> updateVisibility.onClick(null));

        chipGroupBranch.setOnCheckedStateChangeListener((group, checkedIds) -> btnDownload.setEnabled(!checkedIds.isEmpty()));

        btnDownload.setOnClickListener(v -> {
            int checkedId = chipGroupBranch.getCheckedChipId();
            if (checkedId != View.NO_ID) {
                Chip chip = view.findViewById(checkedId);
                String branch = chip.getText().toString();
                String year = spinnerYear.getText().toString();
                String sem = spinnerSem.getText().toString();
                fetchAndOpenUrl("syllabus/" + year + "/" + sem + "/" + branch);
                dialog.dismiss();
            }
        });

        btnCalendar.setOnClickListener(v -> {
            fetchAndOpenUrl("academic_calendar/2025-26");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void fetchAndOpenUrl(String path) {
        dbRef.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !url.isEmpty()) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(MainActivity.this, "File not available yet.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startAutoScroll() {
        Runnable scrollRunnable = new Runnable() {
            @Override
            public void run() {
                autoScroll(rvCampus);
                autoScroll(rvFaculty);
                autoScroll(rvAdmin);
                autoScroll(rvSyllabus);
                autoScrollVertical(rvContactInfo);
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY);
            }
        };
        autoScrollHandler.postDelayed(scrollRunnable, AUTO_SCROLL_DELAY);
    }

    private void autoScroll(RecyclerView rv) {
        if (rv == null || rv.getAdapter() == null || rv.getLayoutManager() == null) return;
        int currentPos = ((LinearLayoutManager) rv.getLayoutManager()).findFirstVisibleItemPosition();
        int nextPos = (currentPos + 1) % rv.getAdapter().getItemCount();
        rv.smoothScrollToPosition(nextPos);
    }

    private void autoScrollVertical(RecyclerView rv) {
        if (rv == null || rv.getAdapter() == null || rv.getLayoutManager() == null) return;
        int currentPos = ((LinearLayoutManager) rv.getLayoutManager()).findFirstVisibleItemPosition();
        rv.smoothScrollToPosition(currentPos + 1);
    }

    private void showLoginBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_login, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        bottomSheetView.findViewById(R.id.bsBtnAdmin).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(MainActivity.this, AdminLoginActivity.class));
        });

        bottomSheetView.findViewById(R.id.bsBtnFaculty).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(MainActivity.this, FacultyLoginActivity.class));
        });

        bottomSheetView.findViewById(R.id.bsBtnStudent).setOnClickListener(v -> {
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