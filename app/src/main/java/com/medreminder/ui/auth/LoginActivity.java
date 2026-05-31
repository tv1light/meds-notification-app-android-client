package com.medreminder.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.medreminder.R;
import com.medreminder.data.local.entity.UserSessionEntity;
import com.medreminder.data.repository.RepositoryCallback;
import com.medreminder.data.repository.SessionRepository;
import com.medreminder.ui.main.MainActivity;
import com.medreminder.util.ValidationUtils;

public class LoginActivity extends AppCompatActivity {
    private EditText etLogin;
    private EditText etPassword;
    private LinearProgressIndicator progress;
    private Button btnLogin;
    private Button btnLocalLogin;

    private SessionRepository sessionRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        progress = findViewById(R.id.loginProgress);
        btnLogin = findViewById(R.id.btnLogin);
        btnLocalLogin = findViewById(R.id.btnLocalLogin);
        Button btnGoRegister = findViewById(R.id.btnGoRegister);

        sessionRepository = new SessionRepository(this);

        btnLogin.setOnClickListener(v -> onLoginClick());
        btnLocalLogin.setOnClickListener(v -> onLocalLoginClick());
        btnGoRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void onLoginClick() {
        String login = safe(etLogin.getText());
        String password = safe(etPassword.getText());

        if (login.isEmpty() || password.isEmpty()) {
            toast(getString(R.string.empty_fields));
            return;
        }
        if (!ValidationUtils.isValidLogin(login)) {
            toast(getString(R.string.validation_login));
            return;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            toast(getString(R.string.validation_password));
            return;
        }

        setLoading(true);
        sessionRepository.login(login, password, new RepositoryCallback<>() {
            @Override
            public void onSuccess(UserSessionEntity value) {
                setLoading(false);
                openMain();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                toast(message);
            }
        });
    }

    private void onLocalLoginClick() {
        String login = safe(etLogin.getText());
        String password = safe(etPassword.getText());

        if (login.isEmpty() || password.isEmpty()) {
            toast(getString(R.string.empty_fields));
            return;
        }
        if (!ValidationUtils.isValidLogin(login)) {
            toast(getString(R.string.validation_login));
            return;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            toast(getString(R.string.validation_password));
            return;
        }

        setLoading(true);
        sessionRepository.loginLocal(login, password, new RepositoryCallback<>() {
            @Override
            public void onSuccess(UserSessionEntity value) {
                setLoading(false);
                openMain();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                toast(message);
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLocalLogin.setEnabled(!loading);
    }

    private void openMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String safe(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
