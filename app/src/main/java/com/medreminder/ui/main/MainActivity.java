package com.medreminder.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.medreminder.R;
import com.medreminder.ui.courses.CoursesFragment;
import com.medreminder.ui.drugs.DrugsFragment;
import com.medreminder.ui.intake_log.IntakeLogFragment;
import com.medreminder.ui.settings.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> { }
        );

        ensureNotificationPermission();

        toolbar = findViewById(R.id.toolbar);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) {
                open(new TodayFragment(), getString(R.string.today));
                return true;
            }
            if (id == R.id.nav_courses) {
                open(new CoursesFragment(), getString(R.string.courses));
                return true;
            }
            if (id == R.id.nav_drugs) {
                open(new DrugsFragment(), getString(R.string.drugs));
                return true;
            }
            if (id == R.id.nav_log) {
                open(new IntakeLogFragment(), getString(R.string.journal));
                return true;
            }
            if (id == R.id.nav_settings) {
                open(new SettingsFragment(), getString(R.string.settings));
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_today);
        }
    }

    private void open(@NonNull Fragment fragment, @NonNull String title) {
        toolbar.setTitle(title);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainContainer, fragment)
                .commit();
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
