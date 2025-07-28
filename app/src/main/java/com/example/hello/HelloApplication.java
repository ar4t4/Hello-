package com.example.hello;

import android.app.Application;
import com.example.hello.helpers.CloudinaryHelper;

public class HelloApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Cloudinary
        CloudinaryHelper.initCloudinary(this);
    }
} 