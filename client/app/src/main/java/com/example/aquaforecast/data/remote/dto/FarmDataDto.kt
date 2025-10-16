package com.example.aquaforecast.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FarmDataDto(
    val temperature: Double,
    val ph: Double,
    val dissolvedOxygen: Double,
    val ammonia: Double,
    val nitrate: Double,
    val turbidity: Double,
    val timestamp: Long,
    val pondId: String
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val message: String,
    val syncedCount: Int
)

@Serializable
data class ModelVersionResponse(
    val version: Int,
    val releaseDate: String,
    val modelSize: Long? = null,
    val checksum: String? = null
)