package com.example.aquaforecast.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class FarmDataReading(
    val temperature: Double,
    val ph: Double,
    @SerialName("dissolved_oxygen")
    val dissolvedOxygen: Double,
    val ammonia: Double,
    val nitrate: Double,
    val turbidity: Double,
    @SerialName("fish_weight")
    val fishWeight: Double? = null,
    @SerialName("fish_length")
    val fishLength: Double? = null,
    val verified: Boolean = false,
    @SerialName("start_date")
    val startDate: String? = null,
    val location: LocationData,
    @SerialName("country_code")
    val countryCode: String? = null,
    @SerialName("recorded_at")
    val recordedAt: String
)

@Serializable
data class FarmDataSyncRequest(
    @SerialName("device_id")
    val deviceId: String? = null,
    val readings: List<FarmDataReading>
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val data: SyncData? = null,
    val error: ErrorData? = null
)

@Serializable
data class SyncData(
    @SerialName("synced_count")
    val syncedCount: Int,
    @SerialName("failed_count")
    val failedCount: Int,
    @SerialName("sync_id")
    val syncId: String? = null,
    @SerialName("synced_at")
    val syncedAt: String? = null
)

@Serializable
data class ErrorData(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

// Auth DTOs
@Serializable
data class RegisterRequest(
    @SerialName("firebase_uid")
    val firebaseUid: String,
    val email: String
)

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val data: RegisterData? = null,
    val error: ErrorData? = null
)

@Serializable
data class RegisterData(
    @SerialName("user_id")
    val userId: String,
    @SerialName("firebase_uid")
    val firebaseUid: String,
    val email: String,
    @SerialName("created_at")
    val createdAt: String
)
