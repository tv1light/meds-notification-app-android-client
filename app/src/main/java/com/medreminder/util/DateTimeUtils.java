package com.medreminder.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class DateTimeUtils {
    private static final String DATE_PATTERN = "dd.MM.yyyy";
    private static final String TIME_PATTERN = "HH:mm";
    private static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm";

    private DateTimeUtils() {}

    public static String formatDate(long millis) {
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(new Date(millis));
    }

    public static String formatTime(long millis) {
        return new SimpleDateFormat(TIME_PATTERN, Locale.getDefault()).format(new Date(millis));
    }

    public static String formatDateTime(long millis) {
        return new SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).format(new Date(millis));
    }

    public static long parseDate(String value) {
        try {
            Date date = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).parse(value);
            return date == null ? 0L : date.getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }

    public static int toMinutesOfDay(String hhmm) {
        String[] parts = hhmm.split(":");
        if (parts.length != 2) {
            return -1;
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int min = Integer.parseInt(parts[1].trim());
            if (hour < 0 || hour > 23 || min < 0 || min > 59) {
                return -1;
            }
            return hour * 60 + min;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static List<Integer> parseTimesCsv(String csv) {
        List<Integer> times = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) {
            return times;
        }
        String[] tokens = csv.split(",");
        for (String token : tokens) {
            int minutes = toMinutesOfDay(token.trim());
            if (minutes >= 0) {
                times.add(minutes);
            }
        }
        return times;
    }

    public static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long endOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startOfDay(millis));
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        return calendar.getTimeInMillis();
    }

    public static long dateWithMinutes(long dateMillis, int minutesOfDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startOfDay(dateMillis));
        calendar.set(Calendar.HOUR_OF_DAY, minutesOfDay / 60);
        calendar.set(Calendar.MINUTE, minutesOfDay % 60);
        return calendar.getTimeInMillis();
    }

    public static boolean isInQuietRange(String quietStart, String quietEnd, long nowMillis) {
        int start = toMinutesOfDay(quietStart == null ? "" : quietStart);
        int end = toMinutesOfDay(quietEnd == null ? "" : quietEnd);
        if (start < 0 || end < 0) {
            return false;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        int now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        if (start <= end) {
            return now >= start && now <= end;
        }
        return now >= start || now <= end;
    }
}
