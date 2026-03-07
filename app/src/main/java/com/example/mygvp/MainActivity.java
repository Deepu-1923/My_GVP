package com.example.mygvp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mygvp.admin.AdminLoginActivity;
import com.example.mygvp.faculty.FacultyLoginActivity;
import com.example.mygvp.student.StudentLoginActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference dbRef;
    private RecyclerView rvContactInfo;
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;

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

        rvContactInfo = findViewById(R.id.rvContactInfo);

        setupDashboard();
        setupContactInfo();

        findViewById(R.id.btnLaunchPortals).setOnClickListener(v -> showLoginBottomSheet());
    }

    private void setupDashboard() {
        // 1. Explore Campus
        View cardCampus = findViewById(R.id.miniCampus);
        setupCard(cardCampus, "Explore\nCampus", android.R.drawable.ic_menu_gallery, R.color.bg_soft_purple);

        List<Integer> campusImages = Arrays.asList(
                R.drawable.main_gate,
                R.drawable.canteen,
                R.drawable.degree_block,
                R.drawable.parking,
                R.drawable.girls_hostel,
                R.drawable.gym_sports_room,
                R.drawable.basket_ball_ground
        );

        List<String> campusTitles = Arrays.asList(
                "Main Gate",
                "College Canteen",
                "Degree Block",
                "Parking Area",
                "Girls Hostel",
                "Gym and Sports",
                "Basketball Ground"
        );

        cardCampus.setOnClickListener(v -> showImageSliderPopup(campusImages, campusTitles));

        // 2. Faculty Directory
        View cardFaculty = findViewById(R.id.miniFaculty);
        setupCard(cardFaculty, "Faculty\nDirectory", android.R.drawable.ic_menu_my_calendar, R.color.bg_soft_blue);
        cardFaculty.setOnClickListener(v -> startActivity(new Intent(this, CampusGalleryActivity.class)));

        // 3. Administrative
        View cardAdmin = findViewById(R.id.miniAdmin);
        setupCard(cardAdmin, "Administrative", android.R.drawable.ic_menu_info_details, R.color.bg_soft_orange);
        cardAdmin.setOnClickListener(v -> Toast.makeText(this, "Admin Services", Toast.LENGTH_SHORT).show());

        // 4. Syllabus & Calendar
        View cardSyllabus = findViewById(R.id.miniSyllabus);
        setupCard(cardSyllabus, "Academic\nResources", android.R.drawable.ic_menu_agenda, R.color.bg_soft_green);
        cardSyllabus.setOnClickListener(v -> showSyllabusBottomSheet());
    }

    private void setupCard(View card, String title, int iconRes, int bgColorRes) {
        TextView tv = card.findViewById(R.id.tvMiniTitle);
        ImageView iv = card.findViewById(R.id.ivMiniIcon);
        View iconBg = card.findViewById(R.id.flIconBg);

        if (tv != null) tv.setText(title);

        if (iv != null) {
            if (iconRes == 0) {
                iv.setVisibility(View.GONE);
            } else {
                iv.setVisibility(View.VISIBLE);
                iv.setImageResource(iconRes);
            }
        }

        if (iconBg != null) iconBg.setBackgroundTintList(getColorStateList(bgColorRes));
    }

    private void setupContactInfo() {
        List<ContactInfo> infoList = new ArrayList<>();
        infoList.add(new ContactInfo("📍 Address", "Gayatri Vidya Parishad College, Rushikonda, Visakhapatnam-530045.", android.R.drawable.ic_dialog_map));
        infoList.add(new ContactInfo("📧 Email", "principalgvpcdpgca@gmail.com", android.R.drawable.ic_dialog_email));
        infoList.add(new ContactInfo("📞 Contact", "0891-2783722 / 2955084", android.R.drawable.ic_menu_call));

        rvContactInfo.setLayoutManager(new LinearLayoutManager(this));
        rvContactInfo.setAdapter(new ContactInfoAdapter(infoList));
    }

    private void showImageSliderPopup(List<Integer> images, List<String> titles) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_image_viewer);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        ViewPager2 viewPager = dialog.findViewById(R.id.vpImageSlider);
        View btnClose = dialog.findViewById(R.id.btnClose);

        GallerySliderAdapter adapter = new GallerySliderAdapter(this, images, titles);
        viewPager.setAdapter(adapter);

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                int nextItem = (viewPager.getCurrentItem() + 1) % images.size();
                viewPager.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(this, 3000);
            }
        };
        sliderHandler.postDelayed(sliderRunnable, 3000);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    sliderHandler.removeCallbacks(sliderRunnable);
                }
            }
        });

        btnClose.setOnClickListener(v -> {
            sliderHandler.removeCallbacks(sliderRunnable);
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> sliderHandler.removeCallbacks(sliderRunnable));

        dialog.show();
    }

    private void showSyllabusBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_syllabus, null);
        dialog.setContentView(view);

        AutoCompleteTextView spinnerYear = view.findViewById(R.id.spinnerYear);
        AutoCompleteTextView spinnerSem = view.findViewById(R.id.spinnerSem);
        AutoCompleteTextView spinnerBranch = view.findViewById(R.id.spinnerBranch);
        MaterialButton btnDownload = view.findViewById(R.id.btnDownloadSyllabus);
        MaterialButton btnCalendar = view.findViewById(R.id.btnDownloadCalendar);

        String[] years = {"1st Year", "2nd Year", "3rd Year", "4th Year"};
        String[] sems = {"Semester 1", "Semester 2"};
        String[] branches = {"CIVIL", "CSE", "CSM", "ECE", "MECH"};

        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years));
        spinnerSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sems));
        spinnerBranch.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, branches));

        Runnable validate = () -> {
            boolean ready = !spinnerYear.getText().toString().isEmpty() &&
                    !spinnerSem.getText().toString().isEmpty() &&
                    !spinnerBranch.getText().toString().isEmpty();
            btnDownload.setEnabled(ready);
        };

        spinnerYear.setOnItemClickListener((parent, v, position, id) -> validate.run());
        spinnerSem.setOnItemClickListener((parent, v, position, id) -> validate.run());
        spinnerBranch.setOnItemClickListener((parent, v, position, id) -> validate.run());

        btnDownload.setOnClickListener(v -> {
            String branch = spinnerBranch.getText().toString();
            String year = spinnerYear.getText().toString();
            String sem = spinnerSem.getText().toString();
            fetchAndOpen("syllabus/" + year + "/" + sem + "/" + branch, "Syllabus");
            dialog.dismiss();
        });

        btnCalendar.setOnClickListener(v -> {
            fetchLatestCalendar();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void fetchLatestCalendar() {
        // Fetch the most recent academic calendar year entry
        dbRef.child("academic_calendar").orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String url = ds.getValue(String.class);
                        openUrl(url, "Academic Calendar (" + ds.getKey() + ")");
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Calendar not uploaded yet.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchAndOpen(String path, String type) {
        dbRef.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !url.isEmpty()) {
                    openUrl(url, type);
                } else {
                    Toast.makeText(MainActivity.this, type + " not available for this selection.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openUrl(String url, String title) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
            Toast.makeText(this, "Opening " + title, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
}
