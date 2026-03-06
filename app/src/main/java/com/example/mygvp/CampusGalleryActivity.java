package com.example.mygvp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class CampusGalleryActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private List<String> imageList = new ArrayList<>();
    private GallerySliderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_gallery);

        viewPager = findViewById(R.id.viewPagerGallery);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("campus_gallery");
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String imageName = ds.getValue(String.class);
                    if (imageName != null) {
                        imageList.add(imageName);
                    }
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                } else {
                    adapter = new GallerySliderAdapter(CampusGalleryActivity.this, imageList);
                    viewPager.setAdapter(adapter);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
