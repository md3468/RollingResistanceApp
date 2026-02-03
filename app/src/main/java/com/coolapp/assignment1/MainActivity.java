package com.coolapp.assignment1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import data.AppDatabase;
import data.TestResult;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        SharedPreferences themePrefs = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        SwitchMaterial darkModeSwitch = findViewById(R.id.switch_mode);
        boolean isDarkModeOn = themePrefs.getBoolean("isDarkModeOn", false);
        darkModeSwitch.setChecked(isDarkModeOn);

        // STABILISIERTE DARK MODE LOGIK
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 1. Speichern
            themePrefs.edit().putBoolean("isDarkModeOn", isChecked).apply();

            // 2. Ziel-Modus festlegen
            int targetMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

            // 3. Sicherheitsabfrage: Nur neustarten, wenn der Modus ungleich dem aktuellen ist
            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode);
                recreate();
            }
        });

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
            etTireName.setText("");
            etPressureBar.setText("");
            etTemperatureC.setText("");
            etSpeedKmh.setText("");
            etP0W.setText("");
            etPloadedW.setText("");
            etMassOnTireKg.setText("");

            cbTubeless.setChecked(false);
            cbTempStable.setChecked(false);
            cbPressureChecked.setChecked(false);
        });

        Button btnSave = findViewById(R.id.btn_save_to_list);

        btnSave.setOnClickListener(v -> {
            try {
                TestResult result = new TestResult();

                SharedPreferences userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                result.userId = userPrefs.getInt("currentUserId", -1);

                result.tireName = etTireName.getText().toString();
                result.pressureBar = Double.parseDouble(etPressureBar.getText().toString());
                result.temperatureC = Double.parseDouble(etTemperatureC.getText().toString());
                result.speedKmh = Double.parseDouble(etSpeedKmh.getText().toString());
                result.p0W = Double.parseDouble(etP0W.getText().toString());
                result.pLoadedW = Double.parseDouble(etPloadedW.getText().toString());
                result.massKg = Double.parseDouble(etMassOnTireKg.getText().toString());

                result.isTubeless = cbTubeless.isChecked();
                result.isTempStable = cbTempStable.isChecked();
                result.isPressureChecked = cbPressureChecked.isChecked();

                utils.CalculationHelper.calculateAndFill(result);

                AppDatabase.getDatabase(this).testDao().insertResult(result);

                Toast.makeText(this, "Saved successfully! Crr: " + String.format("%.5f", result.calculatedCrr), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error: Please check if all numeric fields are filled!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}