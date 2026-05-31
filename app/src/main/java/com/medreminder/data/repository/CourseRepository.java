package com.medreminder.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.medreminder.data.local.dao.DrugDao;
import com.medreminder.data.local.dao.ReminderDao;
import com.medreminder.data.local.dao.TherapyCourseDao;
import com.medreminder.data.local.database.AppDatabase;
import com.medreminder.data.local.entity.CourseWithDrug;
import com.medreminder.data.local.entity.DrugEntity;
import com.medreminder.data.local.entity.ReminderEntity;
import com.medreminder.data.local.entity.TherapyCourseEntity;
import com.medreminder.notification.ReminderScheduler;
import com.medreminder.util.AppExecutors;
import com.medreminder.util.Constants;
import com.medreminder.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CourseRepository {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private final Context context;
    private final TherapyCourseDao courseDao;
    private final ReminderDao reminderDao;
    private final DrugDao drugDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CourseRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.courseDao = db.therapyCourseDao();
        this.reminderDao = db.reminderDao();
        this.drugDao = db.drugDao();
    }

    public LiveData<List<CourseWithDrug>> observeCourses(String search, boolean sortByDrugName) {
        return sortByDrugName
                ? courseDao.observeByDrugName(search == null ? "" : search)
                : courseDao.observeByStartDate(search == null ? "" : search);
    }

    public TherapyCourseEntity getByIdSync(long id) {
        return courseDao.getByIdSync(id);
    }

    public void saveCourse(TherapyCourseEntity course, RepositoryCallback<Long> callback) {
        AppExecutors.io().execute(() -> {
            String validationError = validateCourse(course);
            if (validationError != null) {
                dispatchError(callback, validationError);
                return;
            }

            DrugEntity drug = drugDao.getByIdSync(course.drugId);
            if (drug == null) {
                dispatchError(callback, "Не выбран препарат");
                return;
            }

            long now = System.currentTimeMillis();
            if (course.createdAt <= 0) {
                course.createdAt = now;
            }
            course.isActive = course.endDate >= DateTimeUtils.startOfDay(now);

            long courseId;
            if (course.id > 0) {
                List<ReminderEntity> existingReminders = reminderDao.getByCourseIdSync(course.id);
                for (ReminderEntity reminder : existingReminders) {
                    ReminderScheduler.cancel(context, reminder.id);
                }
                reminderDao.deleteByCourseId(course.id);
                courseDao.update(course);
                courseId = course.id;
            } else {
                courseId = courseDao.insert(course);
                course.id = courseId;
            }

            List<ReminderEntity> plannedReminders = generateReminders(course);
            long nowForSchedule = System.currentTimeMillis();
            for (ReminderEntity reminder : plannedReminders) {
                long reminderId = reminderDao.insert(reminder);
                reminder.id = reminderId;
                if (reminder.plannedDateTime < nowForSchedule) {
                    continue;
                }
                long triggerTime = resolveNotificationTriggerTime(
                        reminder.plannedDateTime,
                        course.notificationLeadMinutes,
                        nowForSchedule
                );
                ReminderScheduler.schedule(
                        context,
                        reminderId,
                        triggerTime,
                        course.id,
                        drug.name,
                        course.dosageText,
                        course.notificationType
                );
            }

            dispatchSuccess(callback, courseId);
        });
    }

    public void deleteCourse(long courseId, RepositoryCallback<Boolean> callback) {
        AppExecutors.io().execute(() -> {
            TherapyCourseEntity course = courseDao.getByIdSync(courseId);
            if (course == null) {
                dispatchError(callback, "Курс не найден");
                return;
            }
            List<ReminderEntity> reminders = reminderDao.getByCourseIdSync(courseId);
            for (ReminderEntity reminder : reminders) {
                ReminderScheduler.cancel(context, reminder.id);
            }
            reminderDao.deleteByCourseId(courseId);
            courseDao.delete(course);
            dispatchSuccess(callback, true);
        });
    }

    private String validateCourse(TherapyCourseEntity course) {
        long start = DateTimeUtils.startOfDay(course.startDate);
        long end = DateTimeUtils.startOfDay(course.endDate);
        if (start <= 0 || end <= 0 || end < start) {
            return "Проверьте даты курса";
        }
        long days = ((end - start) / DAY_MS) + 1;
        if (days < 1 || days > 365) {
            return "Длительность курса должна быть от 1 до 365 дней";
        }
        if (course.notificationLeadMinutes < 0 || course.notificationLeadMinutes > 15) {
            return "Опережение уведомления должно быть от 0 до 15 минут";
        }
        if (DateTimeUtils.parseTimesCsv(course.exactTimesCsv).isEmpty()) {
            return "Некорректно задано время приема";
        }

        if (Constants.SCHEDULE_INTERVAL.equals(course.scheduleType)) {
            if (course.intervalMinutes < 5) {
                return "Минимальный интервал - 5 минут";
            }
            int perDay = 1440 / course.intervalMinutes;
            if (perDay > 288) {
                return "Слишком частые напоминания (максимум 288 в сутки)";
            }
        }

        return null;
    }

    private List<ReminderEntity> generateReminders(TherapyCourseEntity course) {
        List<ReminderEntity> reminders = new ArrayList<>();
        long start = DateTimeUtils.startOfDay(course.startDate);
        long end = DateTimeUtils.startOfDay(course.endDate);
        List<Integer> exactTimes = DateTimeUtils.parseTimesCsv(course.exactTimesCsv);
        if (exactTimes.isEmpty()) {
            exactTimes.add(8 * 60);
        }
        Collections.sort(exactTimes);

        for (long day = start; day <= end; day += DAY_MS) {
            if (reminders.size() > 60000) {
                break;
            }

            if (Constants.SCHEDULE_INTERVAL.equals(course.scheduleType)) {
                int interval = Math.max(5, course.intervalMinutes);
                int startMinute = exactTimes.get(0);
                for (int minute = startMinute; minute < 1440; minute += interval) {
                    reminders.add(createReminder(course.id, DateTimeUtils.dateWithMinutes(day, minute)));
                }
            } else {
                for (int minute : exactTimes) {
                    reminders.add(createReminder(course.id, DateTimeUtils.dateWithMinutes(day, minute)));
                }
            }
        }

        return reminders;
    }

    private ReminderEntity createReminder(long courseId, long plannedTime) {
        ReminderEntity reminder = new ReminderEntity();
        reminder.courseId = courseId;
        reminder.plannedDateTime = plannedTime;
        reminder.status = Constants.STATUS_PLANNED;
        reminder.actualDateTime = null;
        reminder.postponedFromReminderId = null;
        return reminder;
    }

    private long resolveNotificationTriggerTime(long plannedTime, int leadMinutes, long nowMillis) {
        long triggerTime = plannedTime - Math.max(0, leadMinutes) * 60_000L;
        if (triggerTime < nowMillis) {
            // If lead window is missed, notify at intake time instead of spamming instantly.
            triggerTime = plannedTime;
        }
        if (triggerTime <= nowMillis) {
            triggerTime = nowMillis + 1_000L;
        }
        return triggerTime;
    }

    private <T> void dispatchSuccess(RepositoryCallback<T> callback, T value) {
        mainHandler.post(() -> callback.onSuccess(value));
    }

    private void dispatchError(RepositoryCallback<?> callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
