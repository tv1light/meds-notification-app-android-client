package com.medreminder.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.medreminder.BuildConfig;
import com.medreminder.data.local.dao.AppSettingsDao;
import com.medreminder.data.local.database.AppDatabase;
import com.medreminder.data.local.entity.AppSettingsEntity;
import com.medreminder.data.local.entity.UserSessionEntity;
import com.medreminder.data.remote.api.ApiFactory;
import com.medreminder.util.AppExecutors;

public class SettingsRepository {
    private final AppDatabase db;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SettingsRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public LiveData<AppSettingsEntity> observe() {
        return db.appSettingsDao().observe();
    }

    public AppSettingsEntity getSync() {
        return db.appSettingsDao().getSync();
    }

    public void ensureDefaults() {
        AppExecutors.io().execute(() -> {
            AppSettingsEntity settings = db.appSettingsDao().getSync();
            if (settings == null) {
                AppSettingsEntity defaults = new AppSettingsEntity();
                defaults.id = 1;
                defaults.serverUrl = BuildConfig.DEFAULT_SERVER_URL;
                defaults.quietModeEnabled = false;
                defaults.quietModeStart = "22:00";
                defaults.quietModeEnd = "07:00";
                db.appSettingsDao().upsert(defaults);
            }

            UserSessionEntity session = db.userSessionDao().getSessionSync();
            if (session == null) {
                UserSessionEntity empty = new UserSessionEntity();
                empty.id = 1;
                empty.isLoggedIn = false;
                empty.serverUrl = BuildConfig.DEFAULT_SERVER_URL;
                db.userSessionDao().upsert(empty);
            }
        });
    }

    public void save(AppSettingsEntity settings, Runnable onDone) {
        AppExecutors.io().execute(() -> {
            settings.id = 1;
            settings.serverUrl = ApiFactory.normalizeBaseUrl(settings.serverUrl);
            db.appSettingsDao().upsert(settings);

            UserSessionEntity session = db.userSessionDao().getSessionSync();
            if (session != null) {
                session.serverUrl = settings.serverUrl;
                db.userSessionDao().upsert(session);
            }

            if (onDone != null) {
                mainHandler.post(onDone);
            }
        });
    }
}
