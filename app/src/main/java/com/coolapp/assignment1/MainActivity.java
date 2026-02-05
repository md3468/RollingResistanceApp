package com.coolapp.assignment1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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

        // Back Button Logic
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectionActivity.class);
            startActivity(intent);
            finish();
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
        EditText etSpeedrpm = findViewById(R.id.et_motor_speed_rpm);
        EditText etI0A = findViewById(R.id.et_I0_A);
        EditText etIloadedA = findViewById(R.id.et_Iloaded_A);
        EditText etMassOnleverarmKg = findViewById(R.id.et_mass_on_lever_arm_kg);

        // Info-Popup für ETRTO mit klickbarem Link
        etTireName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.etrto_info_title)
                        .setMessage(Html.fromHtml(getString(R.string.etrto_info_text), Html.FROM_HTML_MODE_LEGACY))
                        .setPositiveButton("OK", (d, which) -> d.dismiss())
                        .show();
                
                // Macht den Link klickbar
                TextView messageView = dialog.findViewById(android.R.id.message);
                if (messageView != null) {
                    messageView.setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        });

        CheckBox cbTubeless = findViewById(R.id.cb_tubeless);
        CheckBox cbTempStable = findViewById(R.id.cb_temp_stable);
        CheckBox cbPressureChecked = findViewById(R.id.cb_pressure_checked);

        Button btnClearInput = findViewById(R.id.btn_clear_input);

        btnClearInput.setOnClickListener(v -> {
            // Clear EditTexts
            etTireName.setText("");
            etPressureBar.setText("");
            etTemperatureC.setText("");
            etSpeedrpm.setText("");
            etI0A.setText("");
            etIloadedA.setText("");
            etMassOnleverarmKg.setText("");

            // Uncheck CheckBoxes
            cbTubeless.setChecked(false);
            cbTempStable.setChecked(false);
            cbPressureChecked.setChecked(false);
        });

        Button btnGoToList = findViewById(R.id.btn_go_to_list);
        btnGoToList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListActivity.class);
            startActivity(intent);
        });

        Button btnSave = findViewById(R.id.btn_save_to_list);

        btnSave.setOnClickListener(v -> {
            try {
                // Daten-Objekt erstellen
                TestResult result = new TestResult();

                // User-ID aus den Prefs holen (vom Login)
                SharedPreferences userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                result.userId = userPrefs.getInt("currentUserId", -1);

                // Werte aus den EditTexts ziehen
                result.tireName = etTireName.getText().toString();
                result.pressureBar = Double.parseDouble(etPressureBar.getText().toString());
                result.temperatureC = Double.parseDouble(etTemperatureC.getText().toString());
                result.etSpeedrpm = Double.parseDouble(etSpeedrpm.getText().toString());
                result.I0A = Double.parseDouble(etI0A.getText().toString());
                result.ILoadedA = Double.parseDouble(etIloadedA.getText().toString());
                result.massKg = Double.parseDouble(etMassOnleverarmKg.getText().toString());

                // Checkboxen auslesen
                result.isTubeless = cbTubeless.isChecked();
                result.isTempStable = cbTempStable.isChecked();
                result.isPressureChecked = cbPressureChecked.isChecked();

                // Berechnung durchführen (Deine utils-Klasse)
                utils.CalculationHelper.calculateAndFill(result);

                // In Datenbank speichern
                AppDatabase.getDatabase(this).testDao().insertResult(result);

                Toast.makeText(this, "Saved successfully! Crr: " + String.format("%.5f", result.calculatedCrr), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error: Please check if all numeric fields are filled!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
