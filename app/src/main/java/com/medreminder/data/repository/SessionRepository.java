package com.medreminder.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.medreminder.BuildConfig;
import com.medreminder.R;
import com.medreminder.data.local.database.AppDatabase;
import com.medreminder.data.local.entity.AppSettingsEntity;
import com.medreminder.data.local.entity.LocalProfileEntity;
import com.medreminder.data.local.entity.UserSessionEntity;
import com.medreminder.data.remote.api.ApiFactory;
import com.medreminder.data.remote.api.MedReminderApi;
import com.medreminder.data.remote.dto.AuthResponse;
import com.medreminder.data.remote.dto.LoginRequest;
import com.medreminder.data.remote.dto.RegisterRequest;
import com.medreminder.util.AppExecutors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionRepository {
    private static final String ROLE_LOCAL = "LOCAL_USER";

    private final Context context;
    private final AppDatabase db;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SessionRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
    }

    public LiveData<UserSessionEntity> observeSession() {
        return db.userSessionDao().observeSession();
    }

    public void login(String login, String password, RepositoryCallback<UserSessionEntity> callback) {
        String serverUrl = resolveServerUrlSync();
        MedReminderApi api = ApiFactory.create(serverUrl, null);
        api.login(new LoginRequest(login, password)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    dispatchError(callback, context.getString(R.string.auth_failed));
                    return;
                }
                AuthResponse body = response.body();
                AppExecutors.io().execute(() -> {
                    UserSessionEntity session = upsertSession(
                            body.userId,
                            body.login == null ? login : body.login,
                            body.role == null ? "USER" : body.role,
                            body.token,
                            serverUrl
                    );
                    dispatchSuccess(callback, session);
                });
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                loginLocalInternal(
                        login,
                        password,
                        callback,
                        context.getString(R.string.local_login_fallback_hint),
                        context.getString(R.string.local_auth_failed)
                );
            }
        });
    }

    public void register(String login, String password, RepositoryCallback<UserSessionEntity> callback) {
        String serverUrl = resolveServerUrlSync();
        MedReminderApi api = ApiFactory.create(serverUrl, null);
        api.register(new RegisterRequest(login, password)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    dispatchError(callback, context.getString(R.string.sync_fail));
                    return;
                }
                AuthResponse body = response.body();
                AppExecutors.io().execute(() -> {
                    UserSessionEntity session = upsertSession(
                            body.userId,
                            body.login == null ? login : body.login,
                            body.role == null ? "USER" : body.role,
                            body.token,
                            serverUrl
                    );
                    dispatchSuccess(callback, session);
                });
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                dispatchError(callback, context.getString(R.string.local_login_fallback_hint));
            }
        });
    }

    public void loginLocal(String login, String password, RepositoryCallback<UserSessionEntity> callback) {
        loginLocalInternal(
                login,
                password,
                callback,
                context.getString(R.string.local_auth_failed),
                context.getString(R.string.local_auth_failed)
        );
    }

    public void registerLocal(String login, String password, RepositoryCallback<UserSessionEntity> callback) {
        AppExecutors.io().execute(() -> {
            LocalProfileEntity existing = db.localProfileDao().findByLoginSync(login);
            if (existing != null) {
                dispatchError(callback, context.getString(R.string.local_profile_exists));
                return;
            }

            LocalProfileEntity profile = new LocalProfileEntity();
            profile.login = login;
            profile.password = password;
            profile.createdAt = System.currentTimeMillis();

            try {
                long id = db.localProfileDao().insert(profile);
                UserSessionEntity session = upsertSession(
                        id,
                        login,
                        ROLE_LOCAL,
                        null,
                        resolveServerUrlSync()
                );
                dispatchSuccess(callback, session);
            } catch (Exception e) {
                dispatchError(callback, context.getString(R.string.local_profile_exists));
            }
        });
    }

    public void logout(Runnable onDone) {
        AppExecutors.io().execute(() -> {
            UserSessionEntity existing = db.userSessionDao().getSessionSync();
            UserSessionEntity session = existing == null ? new UserSessionEntity() : existing;
            session.id = 1;
            session.isLoggedIn = false;
            session.token = null;
            if (session.serverUrl == null || session.serverUrl.trim().isEmpty()) {
                session.serverUrl = resolveServerUrlSync();
            }
            db.userSessionDao().upsert(session);
            if (onDone != null) {
                mainHandler.post(onDone);
            }
        });
    }

    public boolean isLoggedInSync() {
        UserSessionEntity session = db.userSessionDao().getSessionSync();
        return session != null && session.isLoggedIn;
    }

    public String resolveServerUrlSync() {
        AppSettingsEntity settings = db.appSettingsDao().getSync();
        if (settings != null && settings.serverUrl != null && !settings.serverUrl.trim().isEmpty()) {
            return ApiFactory.normalizeBaseUrl(settings.serverUrl);
        }
        UserSessionEntity session = db.userSessionDao().getSessionSync();
        if (session != null && session.serverUrl != null && !session.serverUrl.trim().isEmpty()) {
            return ApiFactory.normalizeBaseUrl(session.serverUrl);
        }
        return BuildConfig.DEFAULT_SERVER_URL;
    }

    public String tokenSync() {
        UserSessionEntity session = db.userSessionDao().getSessionSync();
        return session == null ? null : session.token;
    }

    private <T> void dispatchSuccess(RepositoryCallback<T> callback, T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private void dispatchError(RepositoryCallback<?> callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private void loginLocalInternal(String login,
                                    String password,
                                    RepositoryCallback<UserSessionEntity> callback,
                                    String profileNotFoundError,
                                    String wrongPasswordError) {
        AppExecutors.io().execute(() -> {
            LocalProfileEntity profile = db.localProfileDao().findByLoginSync(login);
            if (profile == null) {
                dispatchError(callback, profileNotFoundError);
                return;
            }
            if (!profile.password.equals(password)) {
                dispatchError(callback, wrongPasswordError);
                return;
            }

            UserSessionEntity session = upsertSession(
                    profile.id,
                    profile.login,
                    ROLE_LOCAL,
                    null,
                    resolveServerUrlSync()
            );
            dispatchSuccess(callback, session);
        });
    }

    private UserSessionEntity upsertSession(long userId,
                                            String login,
                                            String role,
                                            String token,
                                            String serverUrl) {
        UserSessionEntity session = new UserSessionEntity();
        session.id = 1;
        session.userId = userId;
        session.login = login;
        session.role = role;
        session.token = token;
        session.isLoggedIn = true;
        session.serverUrl = serverUrl;
        db.userSessionDao().upsert(session);
        return session;
    }
}
