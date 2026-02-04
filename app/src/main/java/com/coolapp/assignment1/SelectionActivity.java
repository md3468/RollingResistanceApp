package com.coolapp.assignment1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import android.widget.Button;

public class SelectionActivity extends AppCompatActivity {

    private SharedPreferences themePrefs;
    private SharedPreferences userPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_selection);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        themePrefs = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        SwitchMaterial darkModeSwitch = findViewById(R.id.switch_mode);
        boolean isDarkModeOn = themePrefs.getBoolean("isDarkModeOn", false);
        darkModeSwitch.setChecked(isDarkModeOn);

        // Stabilisierte Logik wie in ESP und Main
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themePrefs.edit().putBoolean("isDarkModeOn", isChecked).apply();

            int targetMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode);
                recreate();
            }
        });

        Button btnManual = findViewById(R.id.btn_manual_input);
        Button btnEsp = findViewById(R.id.btn_esp_input);
        Button btnLogout = findViewById(R.id.btn_logout);

        // Sign Out Logik
        btnLogout.setOnClickListener(v -> {
            userPrefs.edit().clear().apply();
            startActivity(new Intent(SelectionActivity.this, LoginActivity.class));
            finish();
        });

        // Wechsel zu Manual Input
        btnManual.setOnClickListener(v -> {
            startActivity(new Intent(SelectionActivity.this, MainActivity.class));
            finish(); // kills the current screen and removes it from the background
        });

        // Wechsel zu ESP Input
        btnEsp.setOnClickListener(v -> {
            startActivity(new Intent(SelectionActivity.this, EspActivity.class));
            finish(); // kills the current screen and removes it from the background
        });
    }
}