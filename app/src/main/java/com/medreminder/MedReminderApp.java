package com.medreminder;

import android.app.Application;

import com.medreminder.data.repository.DrugRepository;
import com.medreminder.data.repository.ReminderRepository;
import com.medreminder.data.repository.SettingsRepository;
import com.medreminder.notification.NotificationHelper;
import com.medreminder.util.ThemeModeManager;
import com.medreminder.util.WorkScheduler;

public class MedReminderApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeModeManager.applyTheme(this);

        NotificationHelper.createChannel(this);

        SettingsRepository settingsRepository = new SettingsRepository(this);
        settingsRepository.ensureDefaults();

        DrugRepository drugRepository = new DrugRepository(this);
        drugRepository.ensureSeedData();

        WorkScheduler.schedulePeriodicDrugSync(this);
        new ReminderRepository(this).rescheduleAllFuture();
    }
}
