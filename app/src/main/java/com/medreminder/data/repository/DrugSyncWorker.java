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
import com.medreminder.data.remote.dto.DrugUpsertRequest;
import com.medreminder.util.NetworkUtils;
import com.medreminder.util.ValidationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Response;

public class DrugSyncWorker extends Worker {

    public DrugSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!NetworkUtils.isOnline(getApplicationContext())) {
            return Result.retry();
        }

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        AppSettingsEntity settings = db.appSettingsDao().getSync();
        UserSessionEntity session = db.userSessionDao().getSessionSync();

        String baseUrl = settings != null ? settings.serverUrl : null;
        String token = session != null ? session.token : null;

        MedReminderApi api = ApiFactory.create(baseUrl, token);
        try {
            if (session != null && session.isLoggedIn && "ADMIN".equalsIgnoreCase(session.role)) {
                syncPendingLocalDrugs(db, api);
            }

            Response<List<DrugDto>> response = api.getDrugs().execute();
            if (!response.isSuccessful() || response.body() == null) {
                return Result.retry();
            }

            List<DrugEntity> entities = mapServerDrugs(db, response.body());
            if (entities.isEmpty()) {
                return Result.retry();
            }

            db.drugDao().insertAll(entities);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void syncPendingLocalDrugs(AppDatabase db, MedReminderApi api) throws Exception {
        List<DrugEntity> locals = db.drugDao().getPendingLocalSync();
        for (DrugEntity localDrug : locals) {
            if (!ValidationUtils.isValidDrugText(localDrug.name, 2, 50)
                    || !ValidationUtils.isValidDrugText(localDrug.form, 2, 50)
                    || !ValidationUtils.isValidDrugText(localDrug.dosage, 1, 50)
                    || !ValidationUtils.isValidDrugText(localDrug.activeSubstance, 1, 50)
                    || !ValidationUtils.isValidDrugText(localDrug.country, 3, 56)
                    || !ValidationUtils.isValidDrugText(localDrug.manufacturer, 2, 50)) {
                continue;
            }

            DrugUpsertRequest request = new DrugUpsertRequest();
            request.name = localDrug.name;
            request.form = localDrug.form;
            request.dosage = localDrug.dosage;
            request.activeSubstance = localDrug.activeSubstance;
            request.country = localDrug.country;
            request.manufacturer = localDrug.manufacturer;

            Response<DrugDto> createResponse = api.createDrug(request).execute();
            if (!createResponse.isSuccessful() || createResponse.body() == null || createResponse.body().id <= 0) {
                continue;
            }

            DrugDto serverDrug = createResponse.body();
            DrugEntity conflict = db.drugDao().findByServerIdSync(serverDrug.id);
            if (conflict != null && conflict.id != localDrug.id) {
                continue;
            }

            localDrug.serverId = serverDrug.id;
            localDrug.updatedAt = System.currentTimeMillis();
            db.drugDao().update(localDrug);
        }
    }

    private List<DrugEntity> mapServerDrugs(AppDatabase db, List<DrugDto> source) {
        long now = System.currentTimeMillis();
        List<DrugEntity> entities = new ArrayList<>();
        Set<String> uniqueForms = new HashSet<>();

        for (DrugDto dto : source) {
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
            DrugEntity existingByServerId = db.drugDao().findByServerIdSync(dto.id);
            if (existingByServerId != null) {
                entity.id = existingByServerId.id;
            } else {
                DrugEntity pendingByName = db.drugDao().findByNameSync(dto.name.trim());
                if (pendingByName != null && pendingByName.serverId <= 0) {
                    entity.id = pendingByName.id;
                }
            }

            entity.serverId = dto.id;
            entity.name = dto.name.trim();
            entity.form = dto.form.trim();
            entity.dosage = dto.dosage.trim();
            entity.activeSubstance = dto.activeSubstance.trim();
            entity.country = dto.country.trim();
            entity.manufacturer = dto.manufacturer.trim();
            entity.updatedAt = now;

            String normalizedForm = entity.form.toLowerCase(Locale.ROOT);
            if (!uniqueForms.contains(normalizedForm) && uniqueForms.size() >= 25) {
                continue;
            }
            uniqueForms.add(normalizedForm);
            entities.add(entity);
        }
        return entities;
    }
}
