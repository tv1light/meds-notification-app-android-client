package com.medreminder.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.medreminder.data.local.dao.IntakeLogDao;
import com.medreminder.data.local.database.AppDatabase;
import com.medreminder.data.local.entity.IntakeLogWithDrug;
import com.medreminder.util.DateTimeUtils;

import java.util.List;

public class IntakeLogRepository {
    private final IntakeLogDao intakeLogDao;

    public IntakeLogRepository(Context context) {
        this.intakeLogDao = AppDatabase.getInstance(context.getApplicationContext()).intakeLogDao();
    }

    public LiveData<List<IntakeLogWithDrug>> observe(Long dayFilter, String drugQuery) {
        long fromTs = 0L;
        long toTs = 0L;
        if (dayFilter != null && dayFilter > 0) {
            fromTs = DateTimeUtils.startOfDay(dayFilter);
            toTs = DateTimeUtils.endOfDay(dayFilter);
        }
        return intakeLogDao.observeFiltered(fromTs, toTs, drugQuery == null ? "" : drugQuery.trim());
    }
}
