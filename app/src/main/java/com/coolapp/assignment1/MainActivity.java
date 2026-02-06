package com.coolapp.assignment1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import utils.CalculationHelper;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load calculation constants from SharedPreferences
        CalculationHelper.loadConstants(this);

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
        TextView tvTireHelp = findViewById(R.id.tv_tire_help);
        EditText etPressureBar = findViewById(R.id.et_pressure_bar);
        EditText etTemperatureC = findViewById(R.id.et_temperature_c);
        EditText etI0A = findViewById(R.id.et_I0_A);
        EditText etIloadedA = findViewById(R.id.et_Iloaded_A);
        EditText etMassOnleverarmKg = findViewById(R.id.et_mass_on_lever_arm_kg);

        // Insert ESP measurement (if available) into Iloaded_A
        SharedPreferences espPrefs = getSharedPreferences("ESPData", Context.MODE_PRIVATE);
        String espMeasurement = espPrefs.getString("lastMeasurement", null);
        if (espMeasurement != null && !espMeasurement.trim().isEmpty()) {
            if (!"FAIL!".equalsIgnoreCase(espMeasurement.trim())) {
                String existingLoaded = etIloadedA.getText().toString();
                String combinedLoaded;
                if (existingLoaded == null || existingLoaded.trim().isEmpty()) {
                    combinedLoaded = espMeasurement.trim();
                } else {
                    combinedLoaded = existingLoaded.trim() + " " + espMeasurement.trim();
                }
                etIloadedA.setText(combinedLoaded);
                etIloadedA.setSelection(combinedLoaded.length());

                Toast.makeText(this, "Measurement inserted from ESP", Toast.LENGTH_SHORT).show();
            }
            // Consume it so it won't auto-insert next time
            espPrefs.edit().remove("lastMeasurement").apply();
        }

        // Info-Popup für ETRTO mit klickbarem Link (jetzt über Help-Button)
        tvTireHelp.setOnClickListener(v -> {
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
        });

        CheckBox cbTubeless = findViewById(R.id.cb_tubeless);
        CheckBox cbTempStable = findViewById(R.id.cb_temp_stable);
        CheckBox cbPressureChecked = findViewById(R.id.cb_pressure_checked);

        Button btnEditConstants = findViewById(R.id.btn_edit_constants);
        btnEditConstants.setOnClickListener(v -> showEditConstantsDialog());

        Button btnClearInput = findViewById(R.id.btn_clear_input);

        btnClearInput.setOnClickListener(v -> {
            // Clear EditTexts
            etTireName.setText("");
            etPressureBar.setText("");
            etTemperatureC.setText("");
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

                // Werte aus den EditTexts ziehen (nutzt parseInput für Komma-Support)
                result.tireName = etTireName.getText().toString();
                result.pressureBar = CalculationHelper.parseInput(etPressureBar.getText().toString());
                result.temperatureC = CalculationHelper.parseInput(etTemperatureC.getText().toString());

                // Store raw values (all entered values as string)
                result.idleCurrentAmp = etI0A.getText().toString();
                result.loadCurrentAmp = etIloadedA.getText().toString();

                // Nutze calculateAverage für mehrere Werte
                result.I0A = CalculationHelper.calculateAverage(etI0A.getText().toString());
                result.ILoadedA = CalculationHelper.calculateAverage(etIloadedA.getText().toString());

                result.massKg = CalculationHelper.parseInput(etMassOnleverarmKg.getText().toString());

                // Checkboxen auslesen
                result.isTubeless = cbTubeless.isChecked();
                result.isTempStable = cbTempStable.isChecked();
                result.isPressureChecked = cbPressureChecked.isChecked();

                // Berechnung durchführen (Deine utils-Klasse)
                CalculationHelper.calculateAndFill(result);

                // In Datenbank speichern
                AppDatabase.getDatabase(this).testDao().insertResult(result);

                Toast.makeText(this, "Saved successfully! Crr: " + String.format("%.5f", result.calculatedCrr), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error: Please check if all numeric fields are filled!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditConstantsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_constants);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(40, 40, 40, 20);

        final EditText etG = addConstantRow(mainLayout, "G:", String.valueOf(CalculationHelper.G), "(e.g. 9.81)");
        final EditText etLHang = addConstantRow(mainLayout, "L_Hang:", String.valueOf(CalculationHelper.LEVER_HANG), "(e.g. 0.875)");
        final EditText etLTire = addConstantRow(mainLayout, "L_Tire:", String.valueOf(CalculationHelper.LEVER_TIRE), "(e.g. 0.358)");
        final EditText etVSupply = addConstantRow(mainLayout, "V_Supply:", String.valueOf(CalculationHelper.V_SUPPLY_DEFAULT), "(e.g. 12.0)");
        final EditText etRpm = addConstantRow(mainLayout, "RPM:", String.valueOf(CalculationHelper.MOTOR_SPEED_RPM), "(e.g. 213.0)");

        builder.setView(mainLayout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                double g = CalculationHelper.parseInput(etG.getText().toString());
                double lHang = CalculationHelper.parseInput(etLHang.getText().toString());
                double lTire = CalculationHelper.parseInput(etLTire.getText().toString());
                double vSupply = CalculationHelper.parseInput(etVSupply.getText().toString());
                double rpm = CalculationHelper.parseInput(etRpm.getText().toString());

                CalculationHelper.saveConstants(this, g, lHang, lTire, vSupply, rpm);
                Toast.makeText(this, "Constants saved!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private EditText addConstantRow(LinearLayout parent, String label, String value, String example) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 4, 0, 4);
        row.setLayoutParams(rowParams);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setMinWidth(180); // Ensure consistent starting point for EditTexts
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

        EditText editText = new EditText(this);
        editText.setText(value);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
        editText.setGravity(Gravity.CENTER);
        editText.setPadding(0, 20, 0, 20);

        TextView tvExample = new TextView(this);
        tvExample.setText(example);
        tvExample.setTextSize(12);
        tvExample.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        tvExample.setPadding(10, 0, 0, 0);
        tvExample.setGravity(Gravity.END);

        row.addView(tvLabel);
        row.addView(editText);
        row.addView(tvExample);
        parent.addView(row);

        return editText;
    }
}