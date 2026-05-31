package com.medreminder.data.remote.api;

import androidx.annotation.Nullable;

import com.medreminder.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiFactory {
    private ApiFactory() {}

    public static MedReminderApi create(@Nullable String serverUrl, @Nullable String token) {
        // Base URL is user-configurable in Settings screen. Default points to emulator host.
        String baseUrl = normalizeBaseUrl(serverUrl);
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(logging);

        if (token != null && !token.trim().isEmpty()) {
            builder.addInterceptor((Interceptor.Chain chain) -> {
                Request request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(request);
            });
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(MedReminderApi.class);
    }

    public static String normalizeBaseUrl(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return BuildConfig.DEFAULT_SERVER_URL;
        }
        String result = url.trim();
        if (!result.startsWith("http://") && !result.startsWith("https://")) {
            result = "http://" + result;
        }
        if (!result.endsWith("/")) {
            result = result + "/";
        }
        return result;
    }
}
