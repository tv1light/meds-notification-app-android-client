package com.medreminder.util;

public final class Constants {
    private Constants() {}

    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_TAKEN = "TAKEN";
    public static final String STATUS_SKIPPED = "SKIPPED";
    public static final String STATUS_POSTPONED = "POSTPONED";

    public static final String SCHEDULE_EXACT_TIMES = "EXACT_TIMES";
    public static final String SCHEDULE_INTERVAL = "INTERVAL";
    public static final String SCHEDULE_COURSE_PAUSE = "COURSE_PAUSE";

    public static final String NOTIFICATION_BANNER = "BANNER";
    public static final String NOTIFICATION_SOUND = "SOUND";
    public static final String NOTIFICATION_VIBRATION = "VIBRATION";

    public static final String NOTIFICATION_CHANNEL_ID = "med_reminder_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "med_reminder";

    public static final String ACTION_REMINDER_TAKEN = "com.medreminder.ACTION_REMINDER_TAKEN";
    public static final String ACTION_REMINDER_SKIPPED = "com.medreminder.ACTION_REMINDER_SKIPPED";
    public static final String ACTION_REMINDER_POSTPONE = "com.medreminder.ACTION_REMINDER_POSTPONE";
    public static final String ACTION_REMINDER_POSTPONE_10 = "com.medreminder.ACTION_REMINDER_POSTPONE_10";

    public static final String EXTRA_REMINDER_ID = "extra_reminder_id";
    public static final String EXTRA_COURSE_ID = "extra_course_id";
    public static final String EXTRA_DRUG_NAME = "extra_drug_name";
    public static final String EXTRA_DOSAGE = "extra_dosage";
    public static final String EXTRA_PLANNED_TIME = "extra_planned_time";

    public static final String WORK_DRUG_SYNC = "work_drug_sync";
}
