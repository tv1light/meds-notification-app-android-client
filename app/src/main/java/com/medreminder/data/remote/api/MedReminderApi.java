package com.medreminder.data.remote.api;

import com.medreminder.data.remote.dto.AuthResponse;
import com.medreminder.data.remote.dto.DrugDto;
import com.medreminder.data.remote.dto.HealthResponse;
import com.medreminder.data.remote.dto.LoginRequest;
import com.medreminder.data.remote.dto.RegisterRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface MedReminderApi {
    // Endpoints are intentionally simple and can be changed to match backend routes.
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("api/drugs")
    Call<List<DrugDto>> getDrugs();

    @GET("api/health")
    Call<HealthResponse> health();
}
