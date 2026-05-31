package com.medreminder.util;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.medreminder.R;

public final class StatusUiUtils {
    private StatusUiUtils() {}

    public static int colorForStatus(Context context, String status) {
        if (Constants.STATUS_TAKEN.equals(status)) {
            return ContextCompat.getColor(context, R.color.status_taken);
        }
        if (Constants.STATUS_SKIPPED.equals(status)) {
            return ContextCompat.getColor(context, R.color.status_skipped);
        }
        if (Constants.STATUS_POSTPONED.equals(status)) {
            return ContextCompat.getColor(context, R.color.status_postponed);
        }
        return ContextCompat.getColor(context, R.color.status_planned);
    }

    public static int labelForStatus(String status) {
        if (Constants.STATUS_TAKEN.equals(status)) {
            return R.string.status_taken;
        }
        if (Constants.STATUS_SKIPPED.equals(status)) {
            return R.string.status_skipped;
        }
        if (Constants.STATUS_POSTPONED.equals(status)) {
            return R.string.status_postponed;
        }
        if (Constants.STATUS_PLANNED.equals(status)) {
            return R.string.status_planned;
        }
        return R.string.status_unknown;
    }
}
