package com.medreminder.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.medreminder.R;
import com.medreminder.data.local.dao.DrugDao;
import com.medreminder.data.local.database.AppDatabase;
import com.medreminder.data.local.entity.DrugEntity;
import com.medreminder.data.remote.api.ApiFactory;
import com.medreminder.data.remote.api.MedReminderApi;
import com.medreminder.data.remote.dto.DrugDto;
import com.medreminder.data.remote.dto.DrugUpsertRequest;
import com.medreminder.data.remote.dto.HealthResponse;
import com.medreminder.util.AppExecutors;
import com.medreminder.util.NetworkUtils;
import com.medreminder.util.SeedDataUtil;
import com.medreminder.util.ValidationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Response;

public class DrugRepository {
    private static final String DEFAULT_FORM = "таблетки";
    private static final String DEFAULT_DOSAGE = "не указано";
    private static final String DEFAULT_COUNTRY = "Неизвестно";
    private static final String DEFAULT_MANUFACTURER = "Пользователь";

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

    public void createOrGetCustomDrug(String rawName,
                                      String rawDosageHint,
                                      RepositoryCallback<DrugEntity> callback) {
        AppExecutors.io().execute(() -> {
            String name = normalize(rawName);
            if (!ValidationUtils.isValidDrugText(name, 2, 50)) {
                dispatchError(callback, context.getString(R.string.validation_drug_name));
                return;
            }

            DrugEntity existing = drugDao.findByNameSync(name);
            if (existing != null) {
                dispatchSuccess(callback, existing);
                return;
            }
            String dosageHint = normalize(rawDosageHint);
            if (!ValidationUtils.isValidDrugText(dosageHint, 1, 50)) {
                dosageHint = DEFAULT_DOSAGE;
            }

            DrugEntity drug = buildLocalDrug(
                    name,
                    DEFAULT_FORM,
                    dosageHint,
                    name,
                    DEFAULT_COUNTRY,
                    DEFAULT_MANUFACTURER
            );

            long id = drugDao.insert(drug);
            drug.id = id;
            dispatchSuccess(callback, drug);
        });
    }

    public void addDrugByAdmin(String rawName,
                               String rawForm,
                               String rawDosage,
                               String rawActiveSubstance,
                               String rawCountry,
                               String rawManufacturer,
                               RepositoryCallback<DrugEntity> callback) {
        AppExecutors.io().execute(() -> {
            if (!sessionRepository.isAdminSync()) {
                dispatchError(callback, context.getString(R.string.admin_only_action));
                return;
            }

            DrugEntity draft = buildLocalDrug(
                    rawName,
                    rawForm,
                    rawDosage,
                    rawActiveSubstance,
                    rawCountry,
                    rawManufacturer
            );
            String validationError = validateDrug(draft);
            if (validationError != null) {
                dispatchError(callback, validationError);
                return;
            }

            DrugEntity existing = drugDao.findByNameSync(draft.name);
            if (existing != null) {
                dispatchError(callback, context.getString(R.string.drug_already_exists));
                return;
            }

            long id = drugDao.insert(draft);
            draft.id = id;

            // Best-effort instant push for admin edits.
            if (NetworkUtils.isOnline(context)) {
                syncSingleLocalDrugIfPossible(draft);
            }

            dispatchSuccess(callback, draft);
        });
    }

    public void syncDictionary(RepositoryCallback<Integer> callback) {
        AppExecutors.io().execute(() -> {
            if (!NetworkUtils.isOnline(context)) {
                dispatchError(callback, context.getString(R.string.server_unavailable));
                return;
            }

            String serverUrl = sessionRepository.resolveServerUrlSync();
            String token = sessionRepository.tokenSync();
            MedReminderApi api = ApiFactory.create(serverUrl, token);

            if (sessionRepository.isAdminSync()) {
                try {
                    pushPendingLocalDrugs(api);
                } catch (IOException e) {
                    dispatchError(callback, context.getString(R.string.server_unavailable));
                    return;
                }
            }

            Response<List<DrugDto>> response;
            try {
                response = api.getDrugs().execute();
            } catch (IOException e) {
                dispatchError(callback, context.getString(R.string.server_unavailable));
                return;
            }

            if (!response.isSuccessful() || response.body() == null) {
                dispatchError(callback, mapHttpError(response.code()));
                return;
            }

            List<DrugEntity> mapped = mapDrugs(response.body());
            if (mapped.isEmpty()) {
                dispatchError(callback, context.getString(R.string.sync_fail));
                return;
            }

            drugDao.insertAll(mapped);
            dispatchSuccess(callback, mapped.size());
        });
    }

    public void checkConnection(RepositoryCallback<String> callback) {
        AppExecutors.io().execute(() -> {
            if (!NetworkUtils.isOnline(context)) {
                dispatchError(callback, context.getString(R.string.server_unavailable));
                return;
            }

            String serverUrl = sessionRepository.resolveServerUrlSync();
            MedReminderApi api = ApiFactory.create(serverUrl, sessionRepository.tokenSync());
            try {
                Response<HealthResponse> response = api.health().execute();
                if (response.isSuccessful()
                        && response.body() != null
                        && "ok".equalsIgnoreCase(response.body().status)) {
                    dispatchSuccess(callback, context.getString(R.string.connection_ok));
                } else {
                    dispatchError(callback, mapHttpError(response.code()));
                }
            } catch (IOException e) {
                dispatchError(callback, context.getString(R.string.server_unavailable));
            }
        });
    }

    private void pushPendingLocalDrugs(MedReminderApi api) throws IOException {
        List<DrugEntity> locals = drugDao.getPendingLocalSync();
        for (DrugEntity localDrug : locals) {
            syncSingleLocalDrugIfPossible(localDrug, api);
        }
    }

    private void syncSingleLocalDrugIfPossible(@NonNull DrugEntity localDrug) {
        try {
            MedReminderApi api = ApiFactory.create(
                    sessionRepository.resolveServerUrlSync(),
                    sessionRepository.tokenSync()
            );
            syncSingleLocalDrugIfPossible(localDrug, api);
        } catch (IOException ignored) {
            // No-op. Drug will stay pending and can be pushed later via sync.
        }
    }

    private void syncSingleLocalDrugIfPossible(@NonNull DrugEntity localDrug, @NonNull MedReminderApi api) throws IOException {
        if (localDrug.serverId > 0) {
            return;
        }

        DrugUpsertRequest request = toUpsertRequest(localDrug);
        Response<DrugDto> response = api.createDrug(request).execute();
        if (!response.isSuccessful() || response.body() == null || response.body().id <= 0) {
            return;
        }

        DrugDto body = response.body();
        DrugEntity conflict = drugDao.findByServerIdSync(body.id);
        if (conflict != null && conflict.id != localDrug.id) {
            // Keep pending for manual reconciliation to avoid breaking local course links.
            return;
        }

        DrugEntity updated = new DrugEntity();
        updated.id = localDrug.id;
        updated.serverId = body.id;
        updated.name = normalizeOrDefault(body.name, localDrug.name);
        updated.form = normalizeOrDefault(body.form, localDrug.form);
        updated.dosage = normalizeOrDefault(body.dosage, localDrug.dosage);
        updated.activeSubstance = normalizeOrDefault(body.activeSubstance, localDrug.activeSubstance);
        updated.country = normalizeOrDefault(body.country, localDrug.country);
        updated.manufacturer = normalizeOrDefault(body.manufacturer, localDrug.manufacturer);
        updated.updatedAt = System.currentTimeMillis();
        drugDao.update(updated);
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
            DrugEntity existingByServerId = drugDao.findByServerIdSync(dto.id);
            if (existingByServerId != null) {
                entity.id = existingByServerId.id;
            } else {
                DrugEntity pendingByName = drugDao.findByNameSync(dto.name.trim());
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

            String normalizedForm = entity.form.toLowerCase(Locale.ROOT);
            if (!uniqueForms.contains(normalizedForm) && uniqueForms.size() >= 25) {
                continue;
            }
            uniqueForms.add(normalizedForm);
            entity.updatedAt = now;
            entities.add(entity);
        }
        return entities;
    }

    private DrugUpsertRequest toUpsertRequest(DrugEntity drug) {
        DrugUpsertRequest request = new DrugUpsertRequest();
        request.name = drug.name;
        request.form = drug.form;
        request.dosage = drug.dosage;
        request.activeSubstance = drug.activeSubstance;
        request.country = drug.country;
        request.manufacturer = drug.manufacturer;
        return request;
    }

    private DrugEntity buildLocalDrug(String rawName,
                                      String rawForm,
                                      String rawDosage,
                                      String rawActiveSubstance,
                                      String rawCountry,
                                      String rawManufacturer) {
        DrugEntity drug = new DrugEntity();
        drug.serverId = nextLocalServerId();
        drug.name = normalize(rawName);
        drug.form = normalize(rawForm);
        drug.dosage = normalize(rawDosage);
        drug.activeSubstance = normalize(rawActiveSubstance);
        drug.country = normalize(rawCountry);
        drug.manufacturer = normalize(rawManufacturer);
        drug.updatedAt = System.currentTimeMillis();
        return drug;
    }

    private long nextLocalServerId() {
        Long minValue = drugDao.getMinNonPositiveServerId();
        if (minValue == null) {
            return -1L;
        }
        return minValue - 1L;
    }

    private String validateDrug(DrugEntity drug) {
        if (!ValidationUtils.isValidDrugText(drug.name, 2, 50)) {
            return context.getString(R.string.validation_drug_name);
        }
        if (!ValidationUtils.isValidDrugText(drug.form, 2, 50)) {
            return context.getString(R.string.validation_drug_form);
        }
        if (!ValidationUtils.isValidDrugText(drug.dosage, 1, 50)) {
            return context.getString(R.string.validation_drug_dosage);
        }
        if (!ValidationUtils.isValidDrugText(drug.activeSubstance, 1, 50)) {
            return context.getString(R.string.validation_drug_substance);
        }
        if (!ValidationUtils.isValidDrugText(drug.country, 3, 56)) {
            return context.getString(R.string.validation_drug_country);
        }
        if (!ValidationUtils.isValidDrugText(drug.manufacturer, 2, 50)) {
            return context.getString(R.string.validation_drug_manufacturer);
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private String mapHttpError(int code) {
        if (code == 401 || code == 403) {
            return context.getString(R.string.server_auth_required);
        }
        if (code >= 500) {
            return context.getString(R.string.server_unavailable);
        }
        return context.getString(R.string.sync_fail);
    }

    private <T> void dispatchSuccess(RepositoryCallback<T> callback, T value) {
        mainHandler.post(() -> callback.onSuccess(value));
    }

    private void dispatchError(RepositoryCallback<?> callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
