package com.example.mygvp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mygvp.admin.AdminLoginActivity;
import com.example.mygvp.faculty.FacultyLoginActivity;
import com.example.mygvp.student.StudentLoginActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private ExtendedFloatingActionButton fabLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fabLogin = findViewById(R.id.fabLogin);

        // Open Bottom Sheet on Click
        fabLogin.setOnClickListener(v -> showLoginBottomSheet());

        // Note: You can also bind the GridLayout cards here and add basic
        // Toast messages for the review to show they are "Coming Soon" or active.
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