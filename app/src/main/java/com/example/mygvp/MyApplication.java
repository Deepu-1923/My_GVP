package com.example.mygvp;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Cloudinary for Unsigned Uploads
        // Only cloud_name is required when using unsigned presets
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dwz7e2ov0");
        
        MediaManager.init(this, config);
    }
}
