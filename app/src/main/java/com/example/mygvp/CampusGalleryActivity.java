package com.example.mygvp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.Arrays;
import java.util.List;

public class CampusGalleryActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private GallerySliderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_gallery);

        viewPager = findViewById(R.id.viewPagerGallery);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Load your drawables directly. No Firebase needed!
        // Add all your 25 images to this list.
        List<Integer> imageList = Arrays.asList(
                R.drawable.main_gate,
//                R.drawable.library,
                R.drawable.canteen,
                R.drawable.degree_block,
                R.drawable.parking,
                R.drawable.girls_hostel,
                R.drawable.basket_ball_ground
        );

        // Passed 'null' for titles since you probably don't need them in the full gallery
        adapter = new GallerySliderAdapter(this, imageList, null);
        viewPager.setAdapter(adapter);
    }
}