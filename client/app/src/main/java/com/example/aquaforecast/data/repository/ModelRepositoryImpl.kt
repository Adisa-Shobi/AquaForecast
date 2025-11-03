package com.example.aquaforecast.data.repository

import android.content.Context
import android.util.Log
import com.example.aquaforecast.data.preferences.PreferencesManager
import com.example.aquaforecast.data.remote.ApiService
import com.example.aquaforecast.domain.model.ModelVersion
import com.example.aquaforecast.domain.repository.ModelRepository
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError
import com.example.aquaforecast.domain.repository.asSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ModelRepositoryImpl"

class ModelRepositoryImpl(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ModelRepository {

    companion object {
        private const val MODEL_FILE_NAME = "aqua_forecast_model.tflite"
        private const val MODEL_DIR = "models"
    }

    override suspend fun getCurrentModelVersion(): Result<ModelVersion> = withContext(Dispatchers.IO) {
        try {
            val version = preferencesManager.getModelVersionValue()
            val updatedAt = preferencesManager.getModelUpdatedAtValue() ?: 0L

            ModelVersion(
                version = version,
                lastUpdated = updatedAt
            ).asSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current model version", e)
            "Failed to get model version: ${e.message}".asError()
        }
    }

    override suspend fun checkForModelUpdate(): Result<ModelVersion?> = withContext(Dispatchers.IO) {
        try {
            val currentVersion = preferencesManager.getModelVersionValue()

            // Call backend API to check for latest deployed model
            val response = apiService.getLatestModel()

            if (!response.isSuccessful || response.body() == null) {
                return@withContext "Failed to fetch model info from server".asError()
            }

            val body = response.body()!!
            if (body.success && body.data != null) {
                val latestVersion = body.data.version

                // Check if there's a newer version available
                if (latestVersion != currentVersion) {
                    Log.i(TAG, "New model version available: $latestVersion (current: $currentVersion)")
                    ModelVersion(
                        version = latestVersion,
                        lastUpdated = System.currentTimeMillis()
                    ).asSuccess()
                } else {
                    Log.i(TAG, "No model update available (current version: $currentVersion)")
                    null.asSuccess()
                }
            } else {
                "No model available from server".asError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for model update", e)
            "Failed to check for updates: ${e.message}".asError()
        }
    }

    override suspend fun updateModel(): Result<ModelVersion> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting model update...")

            // Get latest model info from API
            val response = apiService.getLatestModel()

            if (!response.isSuccessful || response.body() == null) {
                return@withContext "Failed to fetch model info from server".asError()
            }

            val body = response.body()!!
            if (!body.success || body.data == null) {
                return@withContext "No model available from server".asError()
            }

            val modelData = body.data
            val modelUrl = modelData.tfliteModelUrl
            val newVersion = modelData.version

            Log.i(TAG, "Downloading model version $newVersion from $modelUrl")

            // Download the model file
            val downloadResponse = apiService.downloadFile(modelUrl)

            if (!downloadResponse.isSuccessful || downloadResponse.body() == null) {
                return@withContext "Failed to download model file".asError()
            }

            // Save to internal storage
            val modelDir = File(context.filesDir, MODEL_DIR)
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }

            val modelFile = File(modelDir, MODEL_FILE_NAME)
            downloadResponse.body()!!.byteStream().use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.i(TAG, "Model downloaded successfully: ${modelFile.absolutePath}")

            // Update preferences with new version
            val now = System.currentTimeMillis()
            preferencesManager.setModelVersion(newVersion, now)

            Log.i(TAG, "Model updated to version $newVersion")

            ModelVersion(
                version = newVersion,
                lastUpdated = now
            ).asSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating model", e)
            "Failed to update model: ${e.message}".asError()
        }
    }
}
