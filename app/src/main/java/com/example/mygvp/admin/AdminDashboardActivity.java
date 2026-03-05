package com.example.mygvp.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mygvp.AdminUploadActivity;
import com.example.mygvp.MainActivity;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AdminDashboardActivity extends AppCompatActivity {
    
    private MaterialButton btnLogout;
    private MaterialCardView cardSystemNotices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        btnLogout = findViewById(R.id.btnLogout);
        cardSystemNotices = findViewById(R.id.cardSystemNotices);

        // We'll use the "Broadcast Notices" card to host our new Upload Portal for now
        cardSystemNotices.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminUploadActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}