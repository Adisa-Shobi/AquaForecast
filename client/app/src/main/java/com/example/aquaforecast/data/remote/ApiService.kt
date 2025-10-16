package com.example.aquaforecast.data.remote

import com.example.aquaforecast.data.remote.dto.FarmDataDto
import com.example.aquaforecast.data.remote.dto.ModelVersionResponse
import com.example.aquaforecast.data.remote.dto.SyncResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ApiService {

    /**
     * Upload farm data to backend
     */
    @POST("api/v1/farm-data/sync")
    suspend fun syncFarmData(@Body data: List<FarmDataDto>): Response<SyncResponse>

    /**
     * Check latest model version
     */
    @GET("api/v1/model/version")
    suspend fun checkModelVersion(): Response<ModelVersionResponse>

    /**
     * Download ML model file
     */
    @Streaming
    @GET("api/v1/model/download")
    suspend fun downloadModel(): Response<ResponseBody>
}