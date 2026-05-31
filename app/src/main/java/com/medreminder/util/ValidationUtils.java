package com.medreminder.util;

import java.util.Locale;

public final class ValidationUtils {
    private static final float MIN_DOSAGE_MG = 0.001f; // 1 mcg
    private static final float MAX_DOSAGE_MG = 100_000f; // 100 g

    private ValidationUtils() {}

    public static boolean isValidLogin(String login) {
        return login != null && login.length() >= 4 && login.length() <= 12;
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8 && password.length() <= 14;
    }

    public static boolean isValidDrugText(String value, int min, int max) {
        return value != null && value.trim().length() >= min && value.trim().length() <= max;
    }

    public static boolean isValidDosageRange(String dosageText) {
        if (dosageText == null) {
            return false;
        }
        String value = dosageText.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return false;
        }
        value = value.replace(",", ".");
        String[] parts = value.split("\\s+");
        if (parts.length < 2) {
            return true; // keep compatibility for free-form dosage like "1 таблетка"
        }
        try {
            float amount = Float.parseFloat(parts[0]);
            String unit = parts[1];
            float mg;
            if ("мкг".equals(unit) || "mcg".equals(unit) || "µg".equals(unit)) {
                mg = amount / 1000f;
            } else if ("мг".equals(unit) || "mg".equals(unit)) {
                mg = amount;
            } else if ("г".equals(unit) || "g".equals(unit)) {
                mg = amount * 1000f;
            } else {
                return true;
            }
            return mg >= MIN_DOSAGE_MG && mg <= MAX_DOSAGE_MG;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
