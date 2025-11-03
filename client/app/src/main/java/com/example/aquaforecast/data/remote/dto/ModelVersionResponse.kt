package com.example.aquaforecast.data.remote.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ModelVersionResponse(
    val success: Boolean,
    val data: ModelVersionData? = null,
    val message: String? = null
)

@Serializable
data class ModelVersionData(
    @SerialName("model_id") val modelId: String,
    val version: String,
    @SerialName("tflite_model_url") val tfliteModelUrl: String,
    @SerialName("tflite_size_bytes") val tfliteSizeBytes: Long? = null,
    @SerialName("keras_model_url") val kerasModelUrl: String? = null,
    @SerialName("keras_size_bytes") val kerasSizeBytes: Long? = null,
    @SerialName("base_model_id") val baseModelId: String? = null,
    @SerialName("base_model_version") val baseModelVersion: String? = null,
    @SerialName("preprocessing_config") val preprocessingConfig: JsonElement? = null,
    @SerialName("model_config") val modelConfig: JsonElement? = null,
    @SerialName("training_data_count") val trainingDataCount: Int? = null,
    @SerialName("training_duration_seconds") val trainingDurationSeconds: Int? = null,
    val metrics: JsonElement? = null,
    val status: String? = null,
    @SerialName("is_deployed") val isDeployed: Boolean,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("min_app_version") val minAppVersion: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("deployed_at") val deployedAt: String? = null,
    val notes: String? = null
)
