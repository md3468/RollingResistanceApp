package com.coolapp.assignment1;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences themePrefs = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        boolean isDarkModeOn = themePrefs.getBoolean("isDarkModeOn", false);

        AppCompatDelegate.setDefaultNightMode(
                isDarkModeOn ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
