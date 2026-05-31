package com.medreminder.ui.settings;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.medreminder.R;
import com.medreminder.data.local.entity.AppSettingsEntity;
import com.medreminder.data.repository.DrugRepository;
import com.medreminder.data.repository.RepositoryCallback;
import com.medreminder.data.repository.SessionRepository;
import com.medreminder.data.repository.SettingsRepository;
import com.medreminder.ui.auth.LoginActivity;
import com.medreminder.util.WorkScheduler;

import java.util.Calendar;

public class SettingsFragment extends Fragment {
    private SettingsRepository settingsRepository;
    private DrugRepository drugRepository;
    private SessionRepository sessionRepository;

    private EditText etServerUrl;
    private SwitchMaterial swQuiet;
    private EditText etQuietFrom;
    private EditText etQuietTo;

    private AppSettingsEntity currentSettings;

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsRepository = new SettingsRepository(requireContext());
        drugRepository = new DrugRepository(requireContext());
        sessionRepository = new SessionRepository(requireContext());

        etServerUrl = view.findViewById(R.id.etServerUrl);
        swQuiet = view.findViewById(R.id.swQuietMode);
        etQuietFrom = view.findViewById(R.id.etQuietFrom);
        etQuietTo = view.findViewById(R.id.etQuietTo);

        MaterialButton btnCheck = view.findViewById(R.id.btnCheckConnection);
        MaterialButton btnSync = view.findViewById(R.id.btnSyncDrugs);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        etQuietFrom.setOnClickListener(v -> showTimePicker(etQuietFrom));
        etQuietTo.setOnClickListener(v -> showTimePicker(etQuietTo));

        btnCheck.setOnClickListener(v -> persistSettings(() -> drugRepository.checkConnection(new RepositoryCallback<>() {
            @Override
            public void onSuccess(String value) {
                toast(value);
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        })));

        btnSync.setOnClickListener(v -> persistSettings(() -> {
            WorkScheduler.runOneTimeDrugSync(requireContext());
            drugRepository.syncDictionary(new RepositoryCallback<>() {
                @Override
                public void onSuccess(Integer value) {
                    toast(getString(R.string.sync_ok) + " (" + value + ")");
                }

                @Override
                public void onError(String message) {
                    toast(message);
                }
            });
        }));

        btnLogout.setOnClickListener(v -> showLogoutDialog());

        settingsRepository.observe().observe(getViewLifecycleOwner(), settings -> {
            if (settings == null) {
                return;
            }
            currentSettings = settings;
            etServerUrl.setText(settings.serverUrl);
            swQuiet.setChecked(settings.quietModeEnabled);
            etQuietFrom.setText(settings.quietModeStart);
            etQuietTo.setText(settings.quietModeEnd);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        persistSettings(null);
    }

    private void showTimePicker(EditText target) {
        String value = target.getText() == null ? "22:00" : target.getText().toString();
        int hour = 22;
        int minute = 0;
        String[] split = value.split(":");
        if (split.length == 2) {
            try {
                hour = Integer.parseInt(split[0]);
                minute = Integer.parseInt(split[1]);
            } catch (Exception ignored) {
            }
        }

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (timePicker, h, m) -> target.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m)),
                hour,
                minute,
                true
        );
        dialog.show();
    }

    private void persistSettings(@Nullable Runnable afterSave) {
        AppSettingsEntity entity = currentSettings == null ? new AppSettingsEntity() : currentSettings;
        entity.serverUrl = safe(etServerUrl.getText(), "http://10.0.2.2:8080/");
        entity.quietModeEnabled = swQuiet.isChecked();
        entity.quietModeStart = safe(etQuietFrom.getText(), "22:00");
        entity.quietModeEnd = safe(etQuietTo.getText(), "07:00");
        settingsRepository.save(entity, afterSave);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirm_logout)
                .setPositiveButton(R.string.yes, (dialog, which) -> sessionRepository.logout(() -> {
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                }))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private String safe(CharSequence text, String fallback) {
        String value = text == null ? "" : text.toString().trim();
        return value.isEmpty() ? fallback : value;
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
