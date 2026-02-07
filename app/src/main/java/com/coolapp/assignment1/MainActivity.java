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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

import java.util.List;

import data.AppDatabase;
import data.TestResult;
import utils.CalculationHelper;

public class MainActivity extends AppCompatActivity {

    // Global deklariert, damit alle Methoden darauf zugreifen können
    private AppDatabase db;
    private AutoCompleteTextView etTireName;
    private EditText etPressureBar, etTemperatureC, etI0A, etIloadedA, etMassOnleverarmKg;
    private CheckBox cbTubeless, cbTempStable, cbPressureChecked;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalculationHelper.loadConstants(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Datenbank initialisieren
        db = AppDatabase.getDatabase(this);

        // UI Elemente finden
        initViews();

        // Autofill für Reifennamen einrichten
        setupTireNameAutoFill();

        // Insets / Padding Logik
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        // Dark Mode Logic
        setupDarkMode();

        // Info-Popup
        findViewById(R.id.tv_tire_help).setOnClickListener(v -> showEtrtoInfo());

        // Button Listeners
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SelectionActivity.class));
            finish();
        });

        findViewById(R.id.btn_edit_constants).setOnClickListener(v -> showEditConstantsDialog());

        findViewById(R.id.btn_clear_input).setOnClickListener(v -> clearAllInputs());

        findViewById(R.id.btn_go_to_list).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ListActivity.class));
        });

        findViewById(R.id.btn_save_to_list).setOnClickListener(v -> saveResult());

        // ESP Messung einfügen
        checkEspData();
    }

    private void initViews() {
        etTireName = findViewById(R.id.et_tire_name);
        etPressureBar = findViewById(R.id.et_pressure_bar);
        etTemperatureC = findViewById(R.id.et_temperature_c);
        etI0A = findViewById(R.id.et_I0_A);
        etIloadedA = findViewById(R.id.et_Iloaded_A);
        etMassOnleverarmKg = findViewById(R.id.et_mass_on_lever_arm_kg);
        cbTubeless = findViewById(R.id.cb_tubeless);
        cbTempStable = findViewById(R.id.cb_temp_stable);
        cbPressureChecked = findViewById(R.id.cb_pressure_checked);
    }

    /**
     * Lädt alle bisherigen Reifennamen aus der DB und setzt sie als Vorschläge
     */
    private void setupTireNameAutoFill() {
        new Thread(() -> {
            List<String> existingTires = db.testDao().getAllUniqueTireNames();
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        existingTires
                );
                etTireName.setAdapter(adapter);
            });
        }).start();
    }

    private void saveResult() {
        try {
            TestResult result = new TestResult();
            SharedPreferences userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            result.userId = userPrefs.getInt("currentUserId", -1);

            result.tireName = etTireName.getText().toString();
            result.pressureBar = CalculationHelper.parseInput(etPressureBar.getText().toString());
            result.temperatureC = CalculationHelper.parseInput(etTemperatureC.getText().toString());
            result.idleCurrentAmp = etI0A.getText().toString();
            result.loadCurrentAmp = etIloadedA.getText().toString();
            result.I0A = CalculationHelper.calculateAverage(etI0A.getText().toString());
            result.ILoadedA = CalculationHelper.calculateAverage(etIloadedA.getText().toString());
            result.massKg = CalculationHelper.parseInput(etMassOnleverarmKg.getText().toString());
            result.isTubeless = cbTubeless.isChecked();
            result.isTempStable = cbTempStable.isChecked();
            result.isPressureChecked = cbPressureChecked.isChecked();

            CalculationHelper.calculateAndFill(result);

            new Thread(() -> {
                db.testDao().insertResult(result);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Saved! Crr: " + String.format("%.5f", result.calculatedCrr), Toast.LENGTH_LONG).show();
                    // Liste aktualisieren, falls ein neuer Name dazu kam
                    setupTireNameAutoFill();
                });
            }).start();

        } catch (Exception e) {
            Toast.makeText(this, "Error: Check numeric fields!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDarkMode() {
        SwitchMaterial darkModeSwitch = findViewById(R.id.switch_mode);
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        boolean isDarkModeOn = sharedPreferences.getBoolean("isDarkModeOn", false);

        AppCompatDelegate.setDefaultNightMode(isDarkModeOn ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        darkModeSwitch.setChecked(isDarkModeOn);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("isDarkModeOn", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private void showEtrtoInfo() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.etrto_info_title)
                .setMessage(Html.fromHtml(getString(R.string.etrto_info_text), Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK", (d, which) -> d.dismiss())
                .show();
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) messageView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void checkEspData() {
        SharedPreferences espPrefs = getSharedPreferences("ESPData", Context.MODE_PRIVATE);
        String espMeasurement = espPrefs.getString("lastMeasurement", null);
        if (espMeasurement != null && !espMeasurement.trim().isEmpty() && !"FAIL!".equalsIgnoreCase(espMeasurement)) {
            String combined = etIloadedA.getText().toString().trim() + " " + espMeasurement.trim();
            etIloadedA.setText(combined.trim());
            espPrefs.edit().remove("lastMeasurement").apply();
            Toast.makeText(this, "ESP Data inserted", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAllInputs() {
        etTireName.setText("");
        etPressureBar.setText("");
        etTemperatureC.setText("");
        etI0A.setText("");
        etIloadedA.setText("");
        etMassOnleverarmKg.setText("");
        cbTubeless.setChecked(false);
        cbTempStable.setChecked(false);
        cbPressureChecked.setChecked(false);
    }

    private void showEditConstantsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_constants);
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(40, 40, 40, 20);

        final EditText etG = addConstantRow(mainLayout, "Gravity:", String.valueOf(CalculationHelper.G), "m/s²", "9.81");
        final EditText etLHang = addConstantRow(mainLayout, "Hang Length:", String.valueOf(CalculationHelper.LEVER_HANG), "m", "0.875");
        final EditText etLTire = addConstantRow(mainLayout, "Tire Length:", String.valueOf(CalculationHelper.LEVER_TIRE), "m", "0.358");
        final EditText etVSupply = addConstantRow(mainLayout, "Voltage:", String.valueOf(CalculationHelper.V_SUPPLY_DEFAULT), "V", "12.0");
        final EditText etRpm = addConstantRow(mainLayout, "Motor Speed:", String.valueOf(CalculationHelper.MOTOR_SPEED_RPM), "RPM", "213.0");

        builder.setView(mainLayout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                CalculationHelper.saveConstants(this,
                        CalculationHelper.parseInput(etG.getText().toString()),
                        CalculationHelper.parseInput(etLHang.getText().toString()),
                        CalculationHelper.parseInput(etLTire.getText().toString()),
                        CalculationHelper.parseInput(etVSupply.getText().toString()),
                        CalculationHelper.parseInput(etRpm.getText().toString()));
                Toast.makeText(this, "Constants saved!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { Toast.makeText(this, "Invalid input!", Toast.LENGTH_SHORT).show(); }
        });
        builder.setNegativeButton("Cancel", null).show();
    }

    private EditText addConstantRow(LinearLayout parent, String label, String value, String unit, String hint) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 8, 0, 8);
        row.setLayoutParams(rowParams);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

        EditText editText = new EditText(this);
        editText.setText(value);
        editText.setHint(hint);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

        TextView tvUnit = new TextView(this);
        tvUnit.setText(unit);
        tvUnit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.6f));

        row.addView(tvLabel); row.addView(editText); row.addView(tvUnit);
        parent.addView(row);
        return editText;
    }
}