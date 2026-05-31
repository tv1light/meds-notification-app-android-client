package com.medreminder.util;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.medreminder.data.repository.DrugSyncWorker;

import java.util.concurrent.TimeUnit;

public final class WorkScheduler {
    private WorkScheduler() {}

    public static void schedulePeriodicDrugSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(DrugSyncWorker.class, 12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(Constants.WORK_DRUG_SYNC, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void runOneTimeDrugSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DrugSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork("manual_drug_sync", ExistingWorkPolicy.REPLACE, request);
    }
}
