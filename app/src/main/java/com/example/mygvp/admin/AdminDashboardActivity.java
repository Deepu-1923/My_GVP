package com.example.mygvp.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mygvp.AdminUploadActivity;
import com.example.mygvp.MainActivity;
import com.example.mygvp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AdminDashboardActivity extends AppCompatActivity {
    
    private MaterialButton btnLogout, btnManageFees;
    private MaterialCardView cardSystemNotices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        btnLogout = findViewById(R.id.btnLogout);
        cardSystemNotices = findViewById(R.id.cardSystemNotices);
        btnManageFees = findViewById(R.id.btnManageFees);

        if (cardSystemNotices != null) {
            cardSystemNotices.setOnClickListener(v -> {
                startActivity(new Intent(AdminDashboardActivity.this, AdminUploadActivity.class));
            });
        }

        if (btnManageFees != null) {
            btnManageFees.setOnClickListener(this::showFeesMenu);
        }

        if (btnLogout != null) {
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

    private void showFeesMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("View Fees Details");
        popup.getMenu().add("Manage Fees Details");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("View Fees Details")) {
                startActivity(new Intent(this, AdminViewFeesActivity.class));
            } else if (title.equals("Manage Fees Details")) {
                startActivity(new Intent(this, AdminManageFeesActivity.class));
            }
            return true;
        });
        popup.show();
    }
}
