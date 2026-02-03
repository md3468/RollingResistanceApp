package com.coolapp.assignment1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

public class EspActivity extends AppCompatActivity {

    private SharedPreferences themePrefs;
    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress();
                    String display = (deviceName != null ? deviceName : "Unknown") + "\n" + deviceHardwareAddress;

                    if (!deviceList.contains(display)) {
                        deviceList.add(display);
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        themePrefs = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        setContentView(R.layout.activity_esp);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        SwitchMaterial darkModeSwitch = findViewById(R.id.switch_mode);
        boolean isDarkModeOn = themePrefs.getBoolean("isDarkModeOn", false);
        darkModeSwitch.setChecked(isDarkModeOn);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themePrefs.edit().putBoolean("isDarkModeOn", isChecked).apply();

            int targetMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

            // Verhindert das Flackern: Nur neustarten, wenn der Modus sich wirklich ändert
            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode);
                recreate();
            }
        });

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, SelectionActivity.class));
            finish();
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);

        ListView lvDevices = findViewById(R.id.lv_devices);
        lvDevices.setAdapter(adapter);

        Button btnConnect = findViewById(R.id.btn_connect);
        Button btnProceed = findViewById(R.id.btn_proceed);

        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            btnProceed.setEnabled(true);
            Toast.makeText(this, "Device selected", Toast.LENGTH_SHORT).show();
        });

        btnConnect.setOnClickListener(v -> startBluetoothDiscovery());

        btnProceed.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    @Override
    protected void onStop() {
        super.onStop();
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void startBluetoothDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
            return;
        }

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            deviceList.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            bluetoothAdapter.startDiscovery();
            Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
        }
    }
}