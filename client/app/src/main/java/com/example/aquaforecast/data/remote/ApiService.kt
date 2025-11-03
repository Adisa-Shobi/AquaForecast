package com.example.aquaforecast.data.remote

import com.example.aquaforecast.data.remote.dto.FarmDataSyncRequest
import com.example.aquaforecast.data.remote.dto.ModelVersionResponse
import com.example.aquaforecast.data.remote.dto.RegisterRequest
import com.example.aquaforecast.data.remote.dto.RegisterResponse
import com.example.aquaforecast.data.remote.dto.SyncResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {

    /**
     * Register or verify user in the backend system after Firebase authentication
     * POST /api/v1/auth/register
     */
    @POST("api/v1/auth/register")
    suspend fun registerUser(
        @Header("Authorization") authToken: String,
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    /**
     * Sync farm data to backend
     * POST /api/v1/farm-data/sync
     */
    @POST("api/v1/farm-data/sync")
    suspend fun syncFarmData(
        @Header("Authorization") authToken: String,
        @Body request: FarmDataSyncRequest
    ): Response<SyncResponse>

    /**
     * Get latest ML model version
     * GET /api/v1/models/latest
     */
    @GET("api/v1/models/latest")
    suspend fun getLatestModel(): Response<ModelVersionResponse>

    /**
     * Check for model update
     * GET /api/v1/models/check-update
     */
    @GET("api/v1/models/check-update")
    suspend fun checkModelUpdate(
        @Query("current_version") currentVersion: String,
        @Query("app_version") appVersion: String? = null
    ): Response<ModelVersionResponse>

    /**
     * Download a file from a URL (used for downloading model files)
     */
    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>
}
