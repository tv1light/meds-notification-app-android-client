package com.medreminder.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.medreminder.util.Constants;

public final class ReminderScheduler {
    private ReminderScheduler() {}

    public static void schedule(Context context,
                                long reminderId,
                                long triggerAtMillis,
                                long courseId,
                                String drugName,
                                String dosage,
                                String notificationType) {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildPendingIntent(context, reminderId, courseId, triggerAtMillis, drugName, dosage, notificationType);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (SecurityException ignored) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void cancel(Context context, long reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = buildPendingIntent(context, reminderId, 0L, 0L, "", "", "");
        alarmManager.cancel(pendingIntent);
    }

    private static PendingIntent buildPendingIntent(Context context,
                                                    long reminderId,
                                                    long courseId,
                                                    long plannedTime,
                                                    String drugName,
                                                    String dosage,
                                                    String notificationType) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(Constants.EXTRA_REMINDER_ID, reminderId);
        intent.putExtra(Constants.EXTRA_COURSE_ID, courseId);
        intent.putExtra(Constants.EXTRA_PLANNED_TIME, plannedTime);
        intent.putExtra(Constants.EXTRA_DRUG_NAME, drugName);
        intent.putExtra(Constants.EXTRA_DOSAGE, dosage);
        intent.putExtra("extra_notification_type", notificationType);

        return PendingIntent.getBroadcast(
                context,
                (int) reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
