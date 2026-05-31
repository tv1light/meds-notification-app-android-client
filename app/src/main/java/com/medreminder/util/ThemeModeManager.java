package com.medreminder.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public final class ThemeModeManager {
    public static final String MODE_SYSTEM = "SYSTEM";
    public static final String MODE_LIGHT = "LIGHT";
    public static final String MODE_DARK = "DARK";

    private static final String PREFS_NAME = "medreminder_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    private ThemeModeManager() {}

    public static void applyTheme(@NonNull Context context) {
        String mode = getThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode));
    }

    public static void setThemeMode(@NonNull Context context, @NonNull String rawMode) {
        String mode = normalizeMode(rawMode);
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_THEME_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode));
    }

    @NonNull
    public static String getThemeMode(@NonNull Context context) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = preferences.getString(KEY_THEME_MODE, MODE_SYSTEM);
        return normalizeMode(stored);
    }

    private static int toNightMode(@NonNull String mode) {
        if (MODE_LIGHT.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (MODE_DARK.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    @NonNull
    private static String normalizeMode(String rawMode) {
        if (rawMode == null) {
            return MODE_SYSTEM;
        }
        String mode = rawMode.trim().toUpperCase(Locale.ROOT);
        if (MODE_LIGHT.equals(mode) || MODE_DARK.equals(mode) || MODE_SYSTEM.equals(mode)) {
            return mode;
        }
        return MODE_SYSTEM;
    }
}
