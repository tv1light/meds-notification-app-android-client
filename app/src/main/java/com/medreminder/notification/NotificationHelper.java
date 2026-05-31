package com.medreminder.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.medreminder.R;
import com.medreminder.ui.main.MainActivity;
import com.medreminder.util.Constants;

public final class NotificationHelper {
    private NotificationHelper() {}

    public static void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_desc));
        manager.createNotificationChannel(channel);
    }

    public static void showReminderNotification(Context context,
                                                long reminderId,
                                                String drugName,
                                                String dosage,
                                                long plannedTime,
                                                String notificationType,
                                                boolean quietMode) {
        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) reminderId,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent takenIntent = actionIntent(context, Constants.ACTION_REMINDER_TAKEN, reminderId);
        PendingIntent skipIntent = actionIntent(context, Constants.ACTION_REMINDER_SKIPPED, reminderId);
        PendingIntent postponeIntent = actionIntent(context, Constants.ACTION_REMINDER_POSTPONE_10, reminderId);

        String text = drugName + " • " + dosage + " • " + com.medreminder.util.DateTimeUtils.formatTime(plannedTime);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(0, context.getString(R.string.action_taken), takenIntent)
                .addAction(0, context.getString(R.string.action_postpone), postponeIntent)
                .addAction(0, context.getString(R.string.action_skip), skipIntent);

        if (!quietMode) {
            if (Constants.NOTIFICATION_SOUND.equals(notificationType)) {
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
            } else if (Constants.NOTIFICATION_VIBRATION.equals(notificationType)) {
                builder.setVibrate(new long[]{0, 250, 150, 250});
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            NotificationManagerCompat.from(context).notify((int) reminderId, builder.build());
        } catch (SecurityException ignored) {
            // Permission can be revoked while app is running. Notification is safely skipped.
        }
    }

    private static PendingIntent actionIntent(Context context, String action, long reminderId) {
        Intent intent = new Intent(context, ReminderActionReceiver.class);
        intent.setAction(action);
        intent.setPackage(context.getPackageName());
        intent.putExtra(Constants.EXTRA_REMINDER_ID, reminderId);
        int actionOffset;
        if (Constants.ACTION_REMINDER_TAKEN.equals(action)) {
            actionOffset = 1;
        } else if (Constants.ACTION_REMINDER_POSTPONE_10.equals(action)) {
            actionOffset = 2;
        } else {
            actionOffset = 3;
        }
        int requestCode = (int) ((reminderId % 1_000_000L) * 10 + actionOffset);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
