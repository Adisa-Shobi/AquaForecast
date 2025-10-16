package com.example.aquaforecast.data.repository

import com.example.aquaforecast.data.local.dao.FarmDataDao

import com.example.aquaforecast.data.remote.ApiService
import com.example.aquaforecast.data.remote.dto.FarmDataDto
import com.example.aquaforecast.domain.repository.SyncRepository
import kotlinx.coroutines.withContext
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError
import com.example.aquaforecast.domain.repository.asSuccess
import kotlinx.coroutines.Dispatchers
import android.content.Context
import android.util.Log
import java.io.File
import retrofit2.HttpException
import java.io.IOException


/**
 * Implementation of SyncRepository
 * Handles backend synchronization and model updates with explicit HTTP status handling
 */
class SyncRepositoryImpl(
    private val apiService: ApiService,
    private val farmDataDao: FarmDataDao,
    private val context: Context
) : SyncRepository {

    companion object {
        private const val TAG = "SyncRepository"
        private const val MODEL_FILE_NAME = "fish_model.tflite"
        private const val MODEL_BACKUP_NAME = "${MODEL_FILE_NAME}.backup"
        private const val LAST_SYNC_KEY = "last_sync_time"
        private const val MODEL_VERSION_KEY = "model_version"
        private const val SYNC_PREFS = "sync_prefs"
        private const val MODEL_PREFS = "model_prefs"
        private const val BUFFER_SIZE = 8192
    }

    override suspend fun syncData(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Get unsynced data from local database
            val unsyncedEntities = farmDataDao.getUnsyncedData()

            // Early return if nothing to sync
            if (unsyncedEntities.isEmpty()) {
                Log.d(TAG, "No unsynced data to upload")
                return@withContext 0.asSuccess()
            }

            Log.d(TAG, "Syncing ${unsyncedEntities.size} entries")

            // Convert entities to DTOs for API
            val dtos = unsyncedEntities.map { entity ->
                FarmDataDto(
                    temperature = entity.temperature,
                    ph = entity.ph,
                    dissolvedOxygen = entity.dissolvedOxygen,
                    ammonia = entity.ammonia,
                    nitrate = entity.nitrate,
                    turbidity = entity.turbidity,
                    timestamp = entity.timestamp,
                    pondId = entity.pondId
                )
            }

            // Upload to backend
            val response = apiService.syncFarmData(dtos)

            // Check HTTP status
            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    Log.e(TAG, "Sync response body is null")
                    return@withContext "Sync failed: Empty response from server".asError()
                }

                // Check if backend reports success
                if (body.success) {
                    // Mark entries as synced in local database
                    val ids = unsyncedEntities.map { it.id }
                    farmDataDao.markAsSynced(ids)

                    // Save sync timestamp
                    saveSyncTime()

                    Log.d(TAG, "Successfully synced ${body.syncedCount} entries")
                    body.syncedCount.asSuccess()
                } else {
                    Log.e(TAG, "Sync failed: ${body.message}")
                    "Sync failed: ${body.message}".asError()
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "Sync failed: $errorMsg")
                "Sync failed: $errorMsg".asError()
            }
        } catch (e: HttpException) {
            val errorMsg = "HTTP error ${e.code()}: ${e.message()}"
            Log.e(TAG, errorMsg, e)
            errorMsg.asError()
        } catch (e: IOException) {
            val errorMsg = "Network error: ${e.message ?: "Please check your internet connection"}"
            Log.e(TAG, errorMsg, e)
            errorMsg.asError()
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to sync data"
            Log.e(TAG, errorMsg, e)
            errorMsg.asError()
        }
    }

    override suspend fun checkModelUpdate(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for model updates")

            val response = apiService.checkModelVersion()

            // Check HTTP status
            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    Log.e(TAG, "Model version response body is null")
                    return@withContext "Failed to check model version: Empty response".asError()
                }

                val serverVersion = body.version
                val localVersion = getLocalModelVersion()

                val updateAvailable = serverVersion > localVersion

                Log.d(TAG, "Model check: Server v$serverVersion, Local v$localVersion, Update available: $updateAvailable")

                updateAvailable.asSuccess()
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "Model version check failed: $errorMsg")
                "Failed to check model version: $errorMsg".asError()
            }
        } catch (e: HttpException) {
            val errorMsg = "HTTP error ${e.code()}: ${e.message()}"
            Log.e(TAG, errorMsg, e)
            errorMsg.asError()
        } catch (e: IOException) {
            val errorMsg = "Network error: ${e.message ?: "Please check your internet connection"}"
            Log.e(TAG, errorMsg, e)
            errorMsg.asError()
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to check model update"
            Log.e(TAG, errorMsg, e)
            errorMsg.asError()
        }
    }

    override suspend fun downloadModel(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting model download")

            val response = apiService.downloadModel()

            // Check HTTP status
            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    Log.e(TAG, "Model download response body is null")
                    return@withContext "Model download failed: Empty response".asError()
                }

                val modelFile = File(context.filesDir, MODEL_FILE_NAME)
                val backupFile = File(context.filesDir, MODEL_BACKUP_NAME)

                // Create backup of existing model
                if (modelFile.exists()) {
                    Log.d(TAG, "Creating backup of existing model")
                    modelFile.copyTo(backupFile, overwrite = true)
                }

                // Download and write new model
                var bytesWritten = 0L
                body.byteStream().use { inputStream ->
                    modelFile.outputStream().use { outputStream ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesWritten += bytesRead
                        }
                        outputStream.flush()
                    }
                }

                Log.d(TAG, "Downloaded $bytesWritten bytes")

                // Verify file integrity
                if (!modelFile.exists()) {
                    Log.e(TAG, "Model file does not exist after download")
                    restoreBackup()
                    return@withContext "Model download failed: File not created".asError()
                }

                if (modelFile.length() == 0L) {
                    Log.e(TAG, "Downloaded model file is empty")
                    restoreBackup()
                    return@withContext "Model download failed: File is empty".asError()
                }

                // Basic TFLite file validation (check magic bytes)
                val isValidTFLite = modelFile.inputStream().use { input ->
                    val header = ByteArray(4)
                    val bytesRead = input.read(header)
                    bytesRead == 4 // Basic check - could add more validation
                }

                if (!isValidTFLite) {
                    Log.e(TAG, "Downloaded file is not a valid TFLite model")
                    restoreBackup()
                    return@withContext "Model download failed: Invalid model file".asError()
                }

                // Success - update version and cleanup
                incrementModelVersion()
                backupFile.delete()

                Log.d(TAG, "Model downloaded successfully: ${modelFile.absolutePath}")
                modelFile.absolutePath.asSuccess()
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "Model download failed: $errorMsg")
                "Model download failed: $errorMsg".asError()
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error during model download", e)
            restoreBackup()
            "HTTP error ${e.code()}: ${e.message()}".asError()
        } catch (e: IOException) {
            Log.e(TAG, "Network error during model download", e)
            restoreBackup()
            "Network error: ${e.message ?: "Please check your internet connection"}".asError()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during model download", e)
            restoreBackup()
            (e.message ?: "Failed to download model").asError()
        }
    }

    override suspend fun getLastSyncTime(): Result<Long?> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
            val time = prefs.getLong(LAST_SYNC_KEY, -1L)
            val result = if (time == -1L) null else time
            result.asSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last sync time", e)
            (e.message ?: "Failed to get last sync time").asError()
        }
    }

    /**
     * Save current timestamp as last sync time to SharedPreferences
     */
    private fun saveSyncTime() {
        try {
            val prefs = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
            val success = prefs.edit()
                .putLong(LAST_SYNC_KEY, System.currentTimeMillis())
                .commit()

            if (success) {
                Log.d(TAG, "Saved sync time")
            } else {
                Log.w(TAG, "Failed to save sync time")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saving sync time", e)
        }
    }

    /**
     * Get local model version from SharedPreferences
     * @return Model version number, 0 if not set
     */
    private fun getLocalModelVersion(): Int {
        return try {
            val prefs = context.getSharedPreferences(MODEL_PREFS, Context.MODE_PRIVATE)
            prefs.getInt(MODEL_VERSION_KEY, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local model version", e)
            0
        }
    }

    /**
     * Increment model version in SharedPreferences after successful download
     */
    private fun incrementModelVersion() {
        try {
            val prefs = context.getSharedPreferences(MODEL_PREFS, Context.MODE_PRIVATE)
            val currentVersion = prefs.getInt(MODEL_VERSION_KEY, 0)
            val newVersion = currentVersion + 1
            val success = prefs.edit()
                .putInt(MODEL_VERSION_KEY, newVersion)
                .commit()

            if (success) {
                Log.d(TAG, "Model version updated: $currentVersion -> $newVersion")
            } else {
                Log.w(TAG, "Failed to update model version")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception incrementing model version", e)
        }
    }

    /**
     * Restore model backup if it exists
     * Called when model download fails
     */
    private fun restoreBackup() {
        try {
            val backupFile = File(context.filesDir, MODEL_BACKUP_NAME)

            if (backupFile.exists()) {
                val modelFile = File(context.filesDir, MODEL_FILE_NAME)
                backupFile.copyTo(modelFile, overwrite = true)
                backupFile.delete()
                Log.d(TAG, "Restored model backup")
            } else {
                Log.d(TAG, "No backup to restore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore backup", e)
        }
    }
}