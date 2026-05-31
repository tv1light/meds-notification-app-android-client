package com.medreminder.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.medreminder.R;
import com.medreminder.data.repository.SessionRepository;
import com.medreminder.ui.main.MainActivity;
import com.medreminder.util.AppExecutors;

public class LauncherActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        SessionRepository sessionRepository = new SessionRepository(this);
        AppExecutors.io().execute(() -> {
            boolean loggedIn = sessionRepository.isLoggedInSync();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(this, loggedIn ? MainActivity.class : LoginActivity.class);
                startActivity(intent);
                finish();
            }, 300L);
        });
    }
}
