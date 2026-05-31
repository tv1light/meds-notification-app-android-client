package com.medreminder.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.medreminder.data.repository.ReminderRepository;
import com.medreminder.util.Constants;

public class ReminderActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        long reminderId = intent.getLongExtra(Constants.EXTRA_REMINDER_ID, -1L);
        if (reminderId <= 0) {
            return;
        }
        String action = intent.getAction();
        PendingResult pendingResult = goAsync();
        ReminderRepository repository = new ReminderRepository(context.getApplicationContext());
        Runnable onDone = () -> {
            NotificationManagerCompat.from(context).cancel((int) reminderId);
            pendingResult.finish();
        };

        if (Constants.ACTION_REMINDER_TAKEN.equals(action)) {
            repository.markTaken(reminderId, onDone);
        } else if (Constants.ACTION_REMINDER_SKIPPED.equals(action)) {
            repository.markSkipped(reminderId, onDone);
        } else if (Constants.ACTION_REMINDER_POSTPONE_10.equals(action)) {
            repository.postponeByMinutes(reminderId, 10, onDone);
        } else {
            pendingResult.finish();
        }
    }
}
