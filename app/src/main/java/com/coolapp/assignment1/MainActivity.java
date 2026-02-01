package com.coolapp.assignment1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Edge-to-Edge reactivities
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // 2. Listener für WindowInsets hinzufügen, um Padding anzupassen
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Das Padding des Views anpassen, um die Systemleisten zu berücksichtigen
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        SwitchMaterial darkModeSwitch = findViewById(R.id.switch_mode);

        // Lade die gespeicherte Einstellung
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        boolean isDarkModeOn = sharedPreferences.getBoolean("isDarkModeOn", false);

        // Setze den richtigen Modus beim Start
        if (isDarkModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            darkModeSwitch.setChecked(true);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            darkModeSwitch.setChecked(false);
        }

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putBoolean("isDarkModeOn", true);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putBoolean("isDarkModeOn", false);
            }
            editor.apply();
        });

        // Find views
        EditText etTireName = findViewById(R.id.et_tire_name);
        EditText etPressureBar = findViewById(R.id.et_pressure_bar);
        EditText etTemperatureC = findViewById(R.id.et_temperature_c);
        EditText etSpeedKmh = findViewById(R.id.et_speed_kmh);
        EditText etP0W = findViewById(R.id.et_p0_w);
        EditText etPloadedW = findViewById(R.id.et_ploaded_w);
        EditText etMassOnTireKg = findViewById(R.id.et_mass_on_tire_kg);

        CheckBox cbTubeless = findViewById(R.id.cb_tubeless);
        CheckBox cbTempStable = findViewById(R.id.cb_temp_stable);
        CheckBox cbPressureChecked = findViewById(R.id.cb_pressure_checked);

        Button btnClearInput = findViewById(R.id.btn_clear_input);

        btnClearInput.setOnClickListener(v -> {
            // Clear EditTexts
            etTireName.setText("");
            etPressureBar.setText("");
            etTemperatureC.setText("");
            etSpeedKmh.setText("");
            etP0W.setText("");
            etPloadedW.setText("");
            etMassOnTireKg.setText("");

            // Uncheck CheckBoxes
            cbTubeless.setChecked(false);
            cbTempStable.setChecked(false);
            cbPressureChecked.setChecked(false);
        });
    }
}
