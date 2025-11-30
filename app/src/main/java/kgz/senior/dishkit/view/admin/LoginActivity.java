package kgz.senior.dishkit.view.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import kgz.senior.dishkit.R;
import kgz.senior.dishkit.viewmodel.AdminViewModel; // Импорт AdminViewModel

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private Button loginButton;
    private ProgressBar loginProgressBar;

    private AdminViewModel adminViewModel; // Объявляем AdminViewModel

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Инициализация ViewModel
        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        // Проверяем, авторизован ли пользователь уже
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Если пользователь авторизован, сразу переходим в AdminPanelActivity
            startAdminPanelActivity();
            return; // Завершаем onCreate, чтобы не обрабатывать дальше
        }

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Пожалуйста, введите Email и Пароль", Toast.LENGTH_SHORT).show();
                return;
            }

            adminViewModel.login(email, password); // Вызов метода login из ViewModel
        });

        // Подписываемся на LiveData от AdminViewModel
        adminViewModel.isLoading().observe(this, isLoading -> {
            loginProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!isLoading);
            emailEditText.setEnabled(!isLoading);
            passwordEditText.setEnabled(!isLoading);
        });

        adminViewModel.getError().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        adminViewModel.getLoginSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(LoginActivity.this, "Вход выполнен успешно", Toast.LENGTH_SHORT).show();
                startAdminPanelActivity();
            }
        });
    }

    private void startAdminPanelActivity() {
        Intent intent = new Intent(LoginActivity.this, AdminPanelActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}