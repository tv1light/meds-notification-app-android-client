package com.medreminder.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.medreminder.data.local.dao.AppSettingsDao;
import com.medreminder.data.local.dao.DrugDao;
import com.medreminder.data.local.dao.IntakeLogDao;
import com.medreminder.data.local.dao.LocalProfileDao;
import com.medreminder.data.local.dao.ReminderDao;
import com.medreminder.data.local.dao.TherapyCourseDao;
import com.medreminder.data.local.dao.UserSessionDao;
import com.medreminder.data.local.entity.AppSettingsEntity;
import com.medreminder.data.local.entity.DrugEntity;
import com.medreminder.data.local.entity.IntakeLogEntity;
import com.medreminder.data.local.entity.LocalProfileEntity;
import com.medreminder.data.local.entity.ReminderEntity;
import com.medreminder.data.local.entity.TherapyCourseEntity;
import com.medreminder.data.local.entity.UserSessionEntity;

@Database(
        entities = {
                UserSessionEntity.class,
                DrugEntity.class,
                TherapyCourseEntity.class,
                ReminderEntity.class,
                IntakeLogEntity.class,
                AppSettingsEntity.class,
                LocalProfileEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract UserSessionDao userSessionDao();

    public abstract DrugDao drugDao();

    public abstract TherapyCourseDao therapyCourseDao();

    public abstract ReminderDao reminderDao();

    public abstract IntakeLogDao intakeLogDao();

    public abstract AppSettingsDao appSettingsDao();

    public abstract LocalProfileDao localProfileDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "med_reminder.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
