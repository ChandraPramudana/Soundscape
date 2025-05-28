package com.chandra.soundscape;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Spinner roleSpinner;
    CheckBox rememberMe;
    Button loginButton;
    TextView registerLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        roleSpinner = findViewById(R.id.role_spinner);
        loginButton = findViewById(R.id.login_button);
        registerLink = findViewById(R.id.register_link);

        // Spinner Role
        String[] roles = {"Admin", "User", "Dokter"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        // Login Button Click
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            String role = roleSpinner.getSelectedItem().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password harus diisi", Toast.LENGTH_SHORT).show();
            } else if (role.equals("Pilih Role")) {
                Toast.makeText(this, "Silakan pilih role terlebih dahulu", Toast.LENGTH_SHORT).show();
            } else {
                // Lanjutkan proses login
                Toast.makeText(this, "Login sebagai " + role, Toast.LENGTH_SHORT).show();
            }
        });

        // Link ke Register
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
