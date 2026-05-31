package com.medreminder.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.medreminder.data.local.dao.DrugDao;
import com.medreminder.data.local.dao.IntakeLogDao;
import com.medreminder.data.local.dao.ReminderDao;
import com.medreminder.data.local.dao.TherapyCourseDao;
import com.medreminder.data.local.database.AppDatabase;
import com.medreminder.data.local.entity.DrugEntity;
import com.medreminder.data.local.entity.IntakeLogEntity;
import com.medreminder.data.local.entity.ReminderEntity;
import com.medreminder.data.local.entity.ReminderWithCourseDrug;
import com.medreminder.data.local.entity.TherapyCourseEntity;
import com.medreminder.notification.ReminderScheduler;
import com.medreminder.util.AppExecutors;
import com.medreminder.util.Constants;
import com.medreminder.util.DateTimeUtils;

import java.util.List;

public class ReminderRepository {
    private static final long TIME_PICKER_GRACE_MS = 59_999L;

    private final Context context;
    private final ReminderDao reminderDao;
    private final TherapyCourseDao courseDao;
    private final DrugDao drugDao;
    private final IntakeLogDao intakeLogDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ReminderRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.reminderDao = db.reminderDao();
        this.courseDao = db.therapyCourseDao();
        this.drugDao = db.drugDao();
        this.intakeLogDao = db.intakeLogDao();
    }

    public LiveData<List<ReminderWithCourseDrug>> observeDay(long dayMillis) {
        return reminderDao.observeForDay(DateTimeUtils.startOfDay(dayMillis), DateTimeUtils.endOfDay(dayMillis));
    }

    public void markTaken(long reminderId, Runnable onDone) {
        applyStatus(reminderId, Constants.STATUS_TAKEN, onDone);
    }

    public void markSkipped(long reminderId, Runnable onDone) {
        applyStatus(reminderId, Constants.STATUS_SKIPPED, onDone);
    }

    public void undoTaken(long reminderId, Runnable onDone) {
        AppExecutors.io().execute(() -> {
            ReminderEntity reminder = reminderDao.getByIdSync(reminderId);
            if (reminder == null || !Constants.STATUS_TAKEN.equals(reminder.status)) {
                finish(onDone);
                return;
            }

            TherapyCourseEntity course = courseDao.getByIdSync(reminder.courseId);
            reminderDao.updateStatus(reminderId, Constants.STATUS_PLANNED, null);

            if (course != null) {
                intakeLogDao.deleteLastByReminderKeys(
                        reminder.courseId,
                        course.drugId,
                        reminder.plannedDateTime,
                        Constants.STATUS_TAKEN
                );

                DrugEntity drug = drugDao.getByIdSync(course.drugId);
                String drugName = drug == null ? "Препарат" : drug.name;
                long now = System.currentTimeMillis();
                long triggerTime = resolveNotificationTriggerTime(reminder.plannedDateTime, course.notificationLeadMinutes, now);
                ReminderScheduler.cancel(context, reminder.id);
                ReminderScheduler.schedule(
                        context,
                        reminder.id,
                        triggerTime,
                        reminder.courseId,
                        drugName,
                        course.dosageText,
                        course.notificationType
                );
            }

            finish(onDone);
        });
    }

    public void postponeByMinutes(long reminderId, int minutes, Runnable onDone) {
        AppExecutors.io().execute(() -> {
            ReminderEntity source = reminderDao.getByIdSync(reminderId);
            if (source == null) {
                finish(onDone);
                return;
            }
            long base = Math.max(System.currentTimeMillis(), source.plannedDateTime);
            postponeToTimeInternal(source, base + minutes * 60L * 1000L, false);
            finish(onDone);
        });
    }

    public void postponeToTime(long reminderId, long newPlannedTime, Runnable onDone) {
        AppExecutors.io().execute(() -> {
            ReminderEntity source = reminderDao.getByIdSync(reminderId);
            if (source == null) {
                finish(onDone);
                return;
            }
            postponeToTimeInternal(source, newPlannedTime, true);
            finish(onDone);
        });
    }

    public void rescheduleAllFuture() {
        AppExecutors.io().execute(() -> {
            List<ReminderEntity> reminders = reminderDao.getByStatusFromTime(Constants.STATUS_PLANNED, System.currentTimeMillis());
            long now = System.currentTimeMillis();
            int count = 0;
            for (ReminderEntity reminder : reminders) {
                if (count >= 500) {
                    break;
                }
                TherapyCourseEntity course = courseDao.getByIdSync(reminder.courseId);
                if (course == null) {
                    continue;
                }
                if (reminder.plannedDateTime < now) {
                    continue;
                }
                DrugEntity drug = drugDao.getByIdSync(course.drugId);
                String drugName = drug == null ? "Препарат" : drug.name;
                long triggerTime = resolveNotificationTriggerTime(reminder.plannedDateTime, course.notificationLeadMinutes, now);
                ReminderScheduler.schedule(
                        context,
                        reminder.id,
                        triggerTime,
                        reminder.courseId,
                        drugName,
                        course.dosageText,
                        course.notificationType
                );
                count++;
            }
        });
    }

    private void applyStatus(long reminderId, String status, Runnable onDone) {
        AppExecutors.io().execute(() -> {
            ReminderEntity reminder = reminderDao.getByIdSync(reminderId);
            if (reminder == null) {
                finish(onDone);
                return;
            }
            if (!Constants.STATUS_PLANNED.equals(reminder.status)) {
                finish(onDone);
                return;
            }

            long now = System.currentTimeMillis();
            reminderDao.updateStatus(reminderId, status, now);
            ReminderScheduler.cancel(context, reminderId);

            TherapyCourseEntity course = courseDao.getByIdSync(reminder.courseId);
            if (course != null) {
                IntakeLogEntity log = new IntakeLogEntity();
                log.courseId = reminder.courseId;
                log.drugId = course.drugId;
                log.plannedDateTime = reminder.plannedDateTime;
                log.actualDateTime = now;
                log.status = status;
                intakeLogDao.insert(log);
            }
            finish(onDone);
        });
    }

    private void postponeToTimeInternal(ReminderEntity source, long newPlannedTime, boolean markTakenIfPast) {
        if (!Constants.STATUS_PLANNED.equals(source.status)) {
            return;
        }

        long now = System.currentTimeMillis();
        ReminderScheduler.cancel(context, source.id);

        TherapyCourseEntity course = courseDao.getByIdSync(source.courseId);
        if (course == null) {
            return;
        }

        if (markTakenIfPast && isPastForTimePicker(newPlannedTime, now)) {
            reminderDao.updateStatus(source.id, Constants.STATUS_TAKEN, now);

            IntakeLogEntity takenLog = new IntakeLogEntity();
            takenLog.courseId = source.courseId;
            takenLog.drugId = course.drugId;
            takenLog.plannedDateTime = source.plannedDateTime;
            takenLog.actualDateTime = now;
            takenLog.status = Constants.STATUS_TAKEN;
            takenLog.comment = "Перенос в прошлое, отмечено как принято";
            intakeLogDao.insert(takenLog);
            return;
        }

        IntakeLogEntity log = new IntakeLogEntity();
        log.courseId = source.courseId;
        log.drugId = course.drugId;
        log.plannedDateTime = source.plannedDateTime;
        log.actualDateTime = now;
        log.status = Constants.STATUS_POSTPONED;
        log.comment = "Перенесено на " + DateTimeUtils.formatDateTime(newPlannedTime);
        intakeLogDao.insert(log);

        // Keep the same reminder row and ID so UI immediately shows updated time
        // instead of leaving an outdated postponed row in today's list.
        reminderDao.reschedule(source.id, newPlannedTime, Constants.STATUS_PLANNED);
        DrugEntity drug = drugDao.getByIdSync(course.drugId);
        String drugName = drug == null ? "Препарат" : drug.name;
        if (newPlannedTime >= now) {
            long triggerTime = resolveNotificationTriggerTime(newPlannedTime, course.notificationLeadMinutes, now);

            ReminderScheduler.schedule(
                    context,
                    source.id,
                    triggerTime,
                    source.courseId,
                    drugName,
                    course.dosageText,
                    course.notificationType
            );
        }
    }

    private boolean isPastForTimePicker(long selectedTime, long nowMillis) {
        // TimePicker returns minute precision, so keep current minute as "not in the past".
        return selectedTime + TIME_PICKER_GRACE_MS < nowMillis;
    }

    private long resolveNotificationTriggerTime(long plannedTime, int leadMinutes, long nowMillis) {
        long triggerTime = plannedTime - Math.max(0, leadMinutes) * 60_000L;
        if (triggerTime < nowMillis) {
            triggerTime = plannedTime;
        }
        if (triggerTime <= nowMillis) {
            triggerTime = nowMillis + 1_000L;
        }
        return triggerTime;
    }

    private void finish(Runnable onDone) {
        if (onDone != null) {
            mainHandler.post(onDone);
        }
    }
}
