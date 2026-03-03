package com.example.mygvp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.mygvp.admin.AdminLoginActivity;
import com.example.mygvp.faculty.FacultyLoginActivity;
import com.example.mygvp.student.StudentLoginActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private ExtendedFloatingActionButton fabLogin;
    private CardView cardCampusMap, cardFacultyPresence, cardLatestEvents, cardGallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fabLogin = findViewById(R.id.fabLogin);
        cardCampusMap = findViewById(R.id.cardCampusMap);
        cardFacultyPresence = findViewById(R.id.cardFacultyPresence);
        cardLatestEvents = findViewById(R.id.cardLatestEvents);
        cardGallery = findViewById(R.id.cardGallery);

        // Open Bottom Sheet on Click
        fabLogin.setOnClickListener(v -> showLoginBottomSheet());

        // Setup listeners for showcase cards (Toasts for now, implement activities later)
        cardCampusMap.setOnClickListener(v -> Toast.makeText(MainActivity.this, "Campus Map feature coming soon", Toast.LENGTH_SHORT).show());
        cardFacultyPresence.setOnClickListener(v -> Toast.makeText(MainActivity.this, "Faculty Directory feature coming soon", Toast.LENGTH_SHORT).show());
        cardLatestEvents.setOnClickListener(v -> Toast.makeText(MainActivity.this, "Events feature coming soon", Toast.LENGTH_SHORT).show());
        cardGallery.setOnClickListener(v -> Toast.makeText(MainActivity.this, "Gallery feature coming soon", Toast.LENGTH_SHORT).show());
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
}