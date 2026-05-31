package com.medreminder.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.medreminder.data.local.database.AppDatabase;
import com.medreminder.data.local.entity.AppSettingsEntity;
import com.medreminder.data.local.entity.DrugEntity;
import com.medreminder.data.local.entity.UserSessionEntity;
import com.medreminder.data.remote.api.ApiFactory;
import com.medreminder.data.remote.api.MedReminderApi;
import com.medreminder.data.remote.dto.DrugDto;
import com.medreminder.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class DrugSyncWorker extends Worker {

    public DrugSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        AppSettingsEntity settings = db.appSettingsDao().getSync();
        UserSessionEntity session = db.userSessionDao().getSessionSync();

        String baseUrl = settings != null ? settings.serverUrl : null;
        String token = session != null ? session.token : null;

        MedReminderApi api = ApiFactory.create(baseUrl, token);
        try {
            Response<List<DrugDto>> response = api.getDrugs().execute();
            if (!response.isSuccessful() || response.body() == null) {
                return Result.retry();
            }
            List<DrugEntity> entities = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (DrugDto dto : response.body()) {
                if (dto == null) {
                    continue;
                }
                if (!ValidationUtils.isValidDrugText(dto.name, 2, 50)
                        || !ValidationUtils.isValidDrugText(dto.form, 2, 50)
                        || !ValidationUtils.isValidDrugText(dto.dosage, 1, 50)
                        || !ValidationUtils.isValidDrugText(dto.activeSubstance, 1, 50)
                        || !ValidationUtils.isValidDrugText(dto.country, 3, 56)
                        || !ValidationUtils.isValidDrugText(dto.manufacturer, 2, 50)) {
                    continue;
                }
                DrugEntity entity = new DrugEntity();
                entity.serverId = dto.id;
                entity.name = dto.name.trim();
                entity.form = dto.form.trim();
                entity.dosage = dto.dosage.trim();
                entity.activeSubstance = dto.activeSubstance.trim();
                entity.country = dto.country.trim();
                entity.manufacturer = dto.manufacturer.trim();
                entity.updatedAt = now;
                entities.add(entity);
            }
            if (!entities.isEmpty()) {
                db.drugDao().insertAll(entities);
                return Result.success();
            }
            return Result.retry();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
