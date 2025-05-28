package com.chandra.soundscape;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText nameInput, emailInput, passwordInput, confirmPasswordInput;
    CheckBox termsCheckbox;
    Spinner roleSpinner;
    Button registerButton;
    TextView loginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        roleSpinner = findViewById(R.id.role_spinner);
        registerButton = findViewById(R.id.register_button);
        loginLink = findViewById(R.id.login_link);

        // Spinner Role
        String[] roles = {"Admin", "User", "Dokter"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        // Register Button Click
        registerButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString();
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();
            String role = roleSpinner.getSelectedItem().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Harap isi semua field", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Password tidak cocok", Toast.LENGTH_SHORT).show();
            } else if (!termsCheckbox.isChecked()) {
                Toast.makeText(this, "Harap setujui syarat & ketentuan", Toast.LENGTH_SHORT).show();
            } else {
                // Lakukan proses registrasi
                Toast.makeText(this, "Akun terdaftar sebagai " + role, Toast.LENGTH_SHORT).show();
            }
        });

        // Link ke Login
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}

