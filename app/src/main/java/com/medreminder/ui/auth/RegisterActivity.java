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

public class RegisterActivity extends AppCompatActivity {
    private EditText etLogin;
    private EditText etPassword;
    private EditText etRepeatPassword;
    private Button btnRegister;
    private Button btnRegisterLocal;
    private LinearProgressIndicator progress;

    private SessionRepository sessionRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnRegisterLocal = findViewById(R.id.btnRegisterLocal);
        progress = findViewById(R.id.registerProgress);

        sessionRepository = new SessionRepository(this);

        btnRegister.setOnClickListener(v -> register());
        btnRegisterLocal.setOnClickListener(v -> registerLocal());
    }

    private void register() {
        String login = safe(etLogin.getText());
        String password = safe(etPassword.getText());
        String repeat = safe(etRepeatPassword.getText());

        if (login.isEmpty() || password.isEmpty() || repeat.isEmpty()) {
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
        if (!password.equals(repeat)) {
            toast(getString(R.string.password_mismatch));
            return;
        }

        setLoading(true);
        sessionRepository.register(login, password, new RepositoryCallback<>() {
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

    private void registerLocal() {
        String login = safe(etLogin.getText());
        String password = safe(etPassword.getText());
        String repeat = safe(etRepeatPassword.getText());

        if (login.isEmpty() || password.isEmpty() || repeat.isEmpty()) {
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
        if (!password.equals(repeat)) {
            toast(getString(R.string.password_mismatch));
            return;
        }

        setLoading(true);
        sessionRepository.registerLocal(login, password, new RepositoryCallback<>() {
            @Override
            public void onSuccess(UserSessionEntity value) {
                setLoading(false);
                toast(getString(R.string.local_profile_created));
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
        btnRegister.setEnabled(!loading);
        btnRegisterLocal.setEnabled(!loading);
    }

    private void openMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
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
