package com.medreminder.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.medreminder.data.local.database.AppDatabase;
import com.medreminder.data.local.entity.AppSettingsEntity;
import com.medreminder.data.local.entity.ReminderEntity;
import com.medreminder.data.local.entity.TherapyCourseEntity;
import com.medreminder.util.AppExecutors;
import com.medreminder.util.Constants;
import com.medreminder.util.DateTimeUtils;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long reminderId = intent.getLongExtra(Constants.EXTRA_REMINDER_ID, -1L);
        if (reminderId <= 0) {
            return;
        }

        final String drugNameFromIntent = intent.getStringExtra(Constants.EXTRA_DRUG_NAME);
        final String dosageFromIntent = intent.getStringExtra(Constants.EXTRA_DOSAGE);
        final long plannedTime = intent.getLongExtra(Constants.EXTRA_PLANNED_TIME, System.currentTimeMillis());
        final String notificationType = intent.getStringExtra("extra_notification_type");

        PendingResult pendingResult = goAsync();
        AppExecutors.io().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
                ReminderEntity reminder = db.reminderDao().getByIdSync(reminderId);
                if (reminder == null || !Constants.STATUS_PLANNED.equals(reminder.status)) {
                    return;
                }

                String resolvedDosage = dosageFromIntent;
                long resolvedPlannedTime = plannedTime;
                String resolvedNotificationType = notificationType;

                TherapyCourseEntity course = db.therapyCourseDao().getByIdSync(reminder.courseId);
                if (course != null) {
                    resolvedNotificationType = course.notificationType;
                    resolvedPlannedTime = reminder.plannedDateTime;
                    if (course.dosageText != null && !course.dosageText.trim().isEmpty()) {
                        resolvedDosage = course.dosageText;
                    }
                }

                AppSettingsEntity settings = db.appSettingsDao().getSync();
                boolean quiet = settings != null
                        && settings.quietModeEnabled
                        && DateTimeUtils.isInQuietRange(settings.quietModeStart, settings.quietModeEnd, System.currentTimeMillis());

                String safeDrug = drugNameFromIntent == null ? "Препарат" : drugNameFromIntent;
                String safeDosage = resolvedDosage == null ? "" : resolvedDosage;
                String safeType = resolvedNotificationType == null ? Constants.NOTIFICATION_BANNER : resolvedNotificationType;

                NotificationHelper.showReminderNotification(
                        context,
                        reminderId,
                        safeDrug,
                        safeDosage,
                        resolvedPlannedTime,
                        safeType,
                        quiet
                );
            } finally {
                pendingResult.finish();
            }
        });
    }
}
