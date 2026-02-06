package com.coolapp.assignment1;
import android.Manifest; import android.bluetooth.BluetoothAdapter; import android.bluetooth.BluetoothDevice; import android.bluetooth.BluetoothSocket; import android.content.BroadcastReceiver; import android.content.Context; import android.content.Intent; import android.content.IntentFilter; import android.content.SharedPreferences; import android.content.pm.PackageManager; import android.os.Build; import android.os.Bundle; import android.widget.ArrayAdapter; import android.widget.Button; import android.widget.ImageButton; import android.widget.ListView; import android.widget.Toast;
import androidx.activity.EdgeToEdge; import androidx.appcompat.app.AppCompatActivity; import androidx.appcompat.app.AppCompatDelegate; import androidx.core.app.ActivityCompat; import androidx.core.graphics.Insets; import androidx.core.view.ViewCompat; import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.IOException; import java.io.InputStream; import java.io.OutputStream; import java.util.ArrayList; import java.util.UUID;
public class EspActivity extends AppCompatActivity {
    private SharedPreferences themePrefs;
    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    // UI
    private Button btnSearch;
    private Button btnConnect;
    private Button btnProceed;

    // SPP/RFCOMM fields
    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String selectedMac;
    private BluetoothSocket socket;
    private ConnectedThread connectedThread;
    private volatile boolean isConnected = false;
    private volatile boolean awaitingMeasurement = false;

    // Receiver: add devices even if we can't read name (shows "Unknown" + MAC)
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;

                String deviceName = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED) {
                        deviceName = device.getName();
                    }
                } else {
                    deviceName = device.getName();
                }
                String display = ((deviceName != null && !deviceName.isEmpty()) ? deviceName : "Unknown")
                        + "\n" + device.getAddress();

                if (!deviceList.contains(display)) {
                    deviceList.add(display);
                    if (adapter != null) adapter.notifyDataSetChanged();
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
            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode);
                recreate();
            }
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, SelectionActivity.class));
            finish();
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);

        ListView lvDevices = findViewById(R.id.lv_devices);
        lvDevices.setAdapter(adapter);
        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            String display = deviceList.get(position);
            String mac = display.substring(display.lastIndexOf("\n") + 1).trim();
            selectedMac = mac;
            Toast.makeText(this, "Selected: " + mac, Toast.LENGTH_SHORT).show();
            updateButtonsState();
        });

        btnSearch = findViewById(R.id.btn_search);
        btnConnect = findViewById(R.id.btn_connect);
        btnProceed = findViewById(R.id.btn_proceed);

        // Initial state
        btnSearch.setEnabled(true);
        btnConnect.setEnabled(false);
        btnProceed.setEnabled(false);

        btnSearch.setOnClickListener(v -> startBluetoothDiscovery());

        btnConnect.setOnClickListener(v -> {
            if (selectedMac == null || selectedMac.isEmpty()) {
                Toast.makeText(this, "Select a device first", Toast.LENGTH_SHORT).show();
                return;
            }
            btnConnect.setEnabled(false);
            connectToSelectedDevice();
        });

        btnProceed.setOnClickListener(v -> {
            if (!isConnected || connectedThread == null) {
                Toast.makeText(this, "Not connected to ESP", Toast.LENGTH_SHORT).show();
                return;
            }
            // Send command and wait for response (handled in handleReceivedLine)
            awaitingMeasurement = true;
            btnProceed.setEnabled(false);
            sendMessage("START_MEASUREMENT");
        });
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
        disconnect();
    }

    private void startBluetoothDiscovery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, 1);
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, 1);
                return;
            }
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

    private void connectToSelectedDevice() {
        if (bluetoothAdapter == null || selectedMac == null) {
            Toast.makeText(this, "Bluetooth not ready or device not selected", Toast.LENGTH_SHORT).show();
            updateButtonsState();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                updateButtonsState();
                return;
            }
        }

        new Thread(() -> {
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }

                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(selectedMac);

                try {
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                } catch (Exception ignored) {}

                if (socket == null) {
                    try {
                        socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                    } catch (Exception e2) {
                        runOnUiThread(() -> {
                            Toast.makeText(EspActivity.this, "Failed to create socket", Toast.LENGTH_SHORT).show();
                            updateButtonsState();
                        });
                        return;
                    }
                }

                socket.connect();

                runOnUiThread(() -> {
                    isConnected = true;
                    Toast.makeText(EspActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    updateButtonsState();
                });

                connectedThread = new ConnectedThread(socket);
                connectedThread.start();

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(EspActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    isConnected = false;
                    updateButtonsState();
                });
                disconnect();
            }
        }).start();
    }

    private void sendMessage(String text) {
        ConnectedThread ct = connectedThread;
        if (ct != null) {
            ct.write((text + "\n").getBytes());
            runOnUiThread(() -> Toast.makeText(this, "TX: " + text, Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleReceivedLine(String msg) {
        Toast.makeText(EspActivity.this, "Measurement: " + msg, Toast.LENGTH_SHORT).show();

        if (!awaitingMeasurement) {
            return; // Only act on responses when we asked for a measurement
        }

        awaitingMeasurement = false;

        if ("FAIL!".equalsIgnoreCase(msg.trim())) {
            Toast.makeText(EspActivity.this, "Measurement failed. Please retry.", Toast.LENGTH_SHORT).show();
            btnProceed.setEnabled(true);
            return;
        }

        // Success: save and navigate
        getSharedPreferences("ESPData", Context.MODE_PRIVATE)
                .edit()
                .putString("lastMeasurement", msg)
                .apply();

        btnProceed.setEnabled(true);
        startActivity(new Intent(EspActivity.this, MainActivity.class));
    }

    private void disconnect() {
        isConnected = false;
        awaitingMeasurement = false;
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) {}
            socket = null;
        }
        runOnUiThread(this::updateButtonsState);
    }

    private void updateButtonsState() {
        btnSearch.setEnabled(true);
        boolean canConnect = (selectedMac != null && !selectedMac.isEmpty() && !isConnected);
        btnConnect.setEnabled(canConnect);
        btnProceed.setEnabled(isConnected && !awaitingMeasurement);
    }

    private class ConnectedThread extends Thread {
        private final InputStream in;
        private final OutputStream out;
        private volatile boolean running = true;

        ConnectedThread(BluetoothSocket socket) throws IOException {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            StringBuilder line = new StringBuilder();

            while (running) {
                try {
                    bytes = in.read(buffer);
                    if (bytes == -1) break;
                    for (int i = 0; i < bytes; i++) {
                        char c = (char) buffer[i];
                        if (c == '\n' || c == '\r') {
                            if (line.length() > 0) {
                                final String msg = line.toString();
                                line.setLength(0);
                                runOnUiThread(() -> handleReceivedLine(msg));
                            }
                        } else {
                            line.append(c);
                            if (line.length() > 2048) {
                                final String msg = line.toString();
                                line.setLength(0);
                                runOnUiThread(() -> handleReceivedLine(msg));
                            }
                        }
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(EspActivity.this, "Disconnected" , Toast.LENGTH_SHORT).show();
                        isConnected = false;
                        awaitingMeasurement = false;
                        updateButtonsState();
                        btnProceed.setEnabled(true);
                    });
                    break;
                }
            }
            running = false;
        }

        void write(byte[] data) {
            try {
                out.write(data);
                out.flush();
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(EspActivity.this, "Write failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }

        void cancel() {
            running = false;
            try { in.close(); } catch (IOException ignored) {}
            try { out.close(); } catch (IOException ignored) {}
        }
    }
}