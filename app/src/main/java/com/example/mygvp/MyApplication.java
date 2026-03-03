package com.example.mygvp;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dwz7e2ov0"); // Replace with your cloud name if different
        config.put("api_key", "731174966675128");    // Replace with your api key if different
        config.put("api_secret", "YOUR_API_SECRET"); // It's better to use unsigned uploads as you are doing
        
        // For unsigned uploads, you only really need the cloud_name
        MediaManager.init(this, config);
    }
}
