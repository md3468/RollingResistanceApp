package com.coolapp.assignment1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.Executors;

import data.AppDatabase;
import data.User;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvTitle, tvSwitchMode;
    private boolean isLoginMode = true;
    private SharedPreferences userPrefs;
    private SharedPreferences themePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        themePrefs = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);

        setContentView(R.layout.activity_login);

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

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            recreate();
        });

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvTitle = findViewById(R.id.tv_title);
        tvSwitchMode = findViewById(R.id.tv_switch_mode);

        tvSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            if (isLoginMode) {
                tvTitle.setText(R.string.login_title);
                btnLogin.setText(R.string.login_button);
                tvSwitchMode.setText(R.string.go_to_register_text);
            } else {
                tvTitle.setText(R.string.register_title);
                btnLogin.setText(R.string.register_button);
                tvSwitchMode.setText(R.string.go_to_login_text);
            }
        });

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(this);
                if (isLoginMode) {
                    User userInDb = db.testDao().getUserByName(user);
                    runOnUiThread(() -> {
                        if (userInDb != null && userInDb.password.equals(pass)) {
                            userPrefs.edit().putInt("currentUserId", userInDb.id).apply();
                            userPrefs.edit().putString("currentUser", user).apply();
                            startActivity(new Intent(LoginActivity.this, SelectionActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    User existing = db.testDao().getUserByName(user);
                    runOnUiThread(() -> {
                        if (existing != null) {
                            Toast.makeText(this, R.string.user_already_exists, Toast.LENGTH_SHORT).show();
                        } else {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.testDao().insertUser(new User(user, pass));
                                runOnUiThread(() -> {
                                    Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                                    isLoginMode = true;
                                    tvTitle.setText(R.string.login_title);
                                    btnLogin.setText(R.string.login_button);
                                    tvSwitchMode.setText(R.string.go_to_register_text);
                                });
                            });
                        }
                    });
                }
            });
        });
    }
}
