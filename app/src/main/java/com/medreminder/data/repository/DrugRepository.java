package com.medreminder.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.medreminder.R;
import com.medreminder.data.local.dao.DrugDao;
import com.medreminder.data.local.database.AppDatabase;
import com.medreminder.data.local.entity.DrugEntity;
import com.medreminder.data.remote.api.ApiFactory;
import com.medreminder.data.remote.api.MedReminderApi;
import com.medreminder.data.remote.dto.DrugDto;
import com.medreminder.data.remote.dto.HealthResponse;
import com.medreminder.util.AppExecutors;
import com.medreminder.util.SeedDataUtil;
import com.medreminder.util.ValidationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DrugRepository {
    private final Context context;
    private final AppDatabase db;
    private final DrugDao drugDao;
    private final SessionRepository sessionRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DrugRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
        this.drugDao = db.drugDao();
        this.sessionRepository = new SessionRepository(this.context);
    }

    public LiveData<List<DrugEntity>> observe(String query) {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isEmpty()) {
            return drugDao.observeAll();
        }
        return drugDao.search(safeQuery);
    }

    public List<DrugEntity> getAllSync() {
        return drugDao.getAllSync();
    }

    public DrugEntity getByIdSync(long id) {
        return drugDao.getByIdSync(id);
    }

    public void ensureSeedData() {
        AppExecutors.io().execute(() -> {
            if (drugDao.countAll() == 0) {
                List<DrugEntity> seed = SeedDataUtil.loadSeedDrugs(context);
                drugDao.insertAll(seed);
            }
        });
    }

    public void syncDictionary(RepositoryCallback<Integer> callback) {
        String serverUrl = sessionRepository.resolveServerUrlSync();
        String token = sessionRepository.tokenSync();
        MedReminderApi api = ApiFactory.create(serverUrl, token);
        api.getDrugs().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<DrugDto>> call, Response<List<DrugDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    dispatchError(callback, context.getString(R.string.sync_fail));
                    return;
                }
                List<DrugEntity> mapped = mapDrugs(response.body());
                if (mapped.isEmpty()) {
                    dispatchError(callback, context.getString(R.string.sync_fail));
                    return;
                }
                AppExecutors.io().execute(() -> {
                    drugDao.insertAll(mapped);
                    dispatchSuccess(callback, mapped.size());
                });
            }

            @Override
            public void onFailure(Call<List<DrugDto>> call, Throwable t) {
                dispatchError(callback, context.getString(R.string.server_unavailable));
            }
        });
    }

    public void checkConnection(RepositoryCallback<String> callback) {
        String serverUrl = sessionRepository.resolveServerUrlSync();
        MedReminderApi api = ApiFactory.create(serverUrl, sessionRepository.tokenSync());
        api.health().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<HealthResponse> call, Response<HealthResponse> response) {
                if (response.isSuccessful() && response.body() != null && "ok".equalsIgnoreCase(response.body().status)) {
                    dispatchSuccess(callback, context.getString(R.string.connection_ok));
                } else {
                    dispatchError(callback, context.getString(R.string.server_unavailable));
                }
            }

            @Override
            public void onFailure(Call<HealthResponse> call, Throwable t) {
                dispatchError(callback, context.getString(R.string.server_unavailable));
            }
        });
    }

    private List<DrugEntity> mapDrugs(List<DrugDto> dtos) {
        long now = System.currentTimeMillis();
        List<DrugEntity> entities = new ArrayList<>();
        Set<String> uniqueForms = new HashSet<>();
        for (DrugDto dto : dtos) {
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
            String normalizedForm = entity.form.toLowerCase();
            if (!uniqueForms.contains(normalizedForm) && uniqueForms.size() >= 25) {
                continue;
            }
            uniqueForms.add(normalizedForm);
            entity.updatedAt = now;
            entities.add(entity);
        }
        return entities;
    }

    private <T> void dispatchSuccess(RepositoryCallback<T> callback, T value) {
        mainHandler.post(() -> callback.onSuccess(value));
    }

    private void dispatchError(RepositoryCallback<?> callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
