package com.example.aquaforecast.data.repository

import android.content.Context
import android.util.Log
import com.example.aquaforecast.data.local.dao.FarmDataDao
import com.example.aquaforecast.data.local.dao.PondDao
import com.example.aquaforecast.data.local.dao.PredictionDao
import com.example.aquaforecast.data.remote.ApiService
import com.example.aquaforecast.data.remote.dto.FarmDataReading
import com.example.aquaforecast.data.remote.dto.FarmDataSyncRequest
import com.example.aquaforecast.data.remote.dto.LocationData
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.SyncRepository
import com.example.aquaforecast.domain.repository.asError
import com.example.aquaforecast.domain.repository.asSuccess
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


/**
 * Implementation of SyncRepository
 * Handles backend synchronization with proper data mapping including:
 * - Water quality parameters
 * - Fish predictions (weight, length)
 * - User verification status
 * - Pond cycle start date
 * - Device location and country
 */
class SyncRepositoryImpl(
    private val apiService: ApiService,
    private val farmDataDao: FarmDataDao,
    private val predictionDao: PredictionDao,
    private val pondDao: PondDao,
    private val firebaseAuth: FirebaseAuth,
    private val context: Context
) : SyncRepository {

    companion object {
        private const val TAG = "SyncRepository"
        private const val LAST_SYNC_KEY = "last_sync_time"
        private const val SYNC_PREFS = "sync_prefs"
        private const val MODEL_VERSION_KEY = "model_version"
        private const val MODEL_PREFS = "model_prefs"
    }

    override suspend fun syncData(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Get Firebase auth token
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No authenticated user")
                return@withContext "Please sign in to sync data".asError()
            }

            val token = try {
                currentUser.getIdToken(false).await().token
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get Firebase token", e)
                return@withContext "Authentication failed. Please sign in again".asError()
            }

            if (token == null) {
                Log.e(TAG, "Firebase token is null")
                return@withContext "Authentication token unavailable".asError()
            }

            // Get unsynced data
            val unsyncedEntities = farmDataDao.getUnsyncedData()

            if (unsyncedEntities.isEmpty()) {
                Log.d(TAG, "No unsynced data to upload")
                return@withContext 0.asSuccess()
            }

            Log.d(TAG, "Syncing ${unsyncedEntities.size} entries")

            // Map entities to DTOs
            val readings = unsyncedEntities.mapNotNull { entity ->
                try {
                    // Get pond data for start date
                    val pond = pondDao.getPondById(entity.pondId)
                    if (pond == null) {
                        Log.w(TAG, "Pond not found for entry ${entity.id}")
                        return@mapNotNull null
                    }

                    // Get prediction data if available
                    val prediction = predictionDao.getPredictionByFarmDataId(entity.id)

                    // Format timestamp to ISO 8601
                    val recordedAt = formatTimestampISO(entity.timestamp)
                    val startDate = formatDateISO(pond.startDate)

                    // Use stored location from entity (captured at data entry time)
                    val latitude = entity.latitude ?: 0.0
                    val longitude = entity.longitude ?: 0.0

                    if (entity.latitude == null || entity.longitude == null) {
                        Log.w(TAG, "Location not available for entry ${entity.id}")
                    }

                    FarmDataReading(
                        temperature = entity.temperature,
                        ph = entity.ph,
                        dissolvedOxygen = entity.dissolvedOxygen,
                        ammonia = entity.ammonia,
                        nitrate = entity.nitrate,
                        turbidity = entity.turbidity,
                        fishWeight = prediction?.predictedWeight,
                        fishLength = prediction?.predictedLength,
                        verified = prediction?.verified ?: false,
                        startDate = startDate,
                        location = LocationData(
                            latitude = latitude,
                            longitude = longitude
                        ),
                        countryCode = null, // Optional field
                        recordedAt = recordedAt
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping entity ${entity.id}", e)
                    null
                }
            }

            if (readings.isEmpty()) {
                Log.w(TAG, "No valid readings to sync after mapping")
                return@withContext "No valid data to sync".asError()
            }

            // Get device ID
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            // Create sync request
            val request = FarmDataSyncRequest(
                deviceId = deviceId,
                readings = readings
            )

            // Upload to backend
            val response = apiService.syncFarmData("Bearer $token", request)

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    Log.e(TAG, "Sync response body is null")
                    return@withContext "Sync failed: Empty response from server".asError()
                }

                if (body.success && body.data != null) {
                    // Mark entries as synced
                    val ids = unsyncedEntities.map { it.id }
                    farmDataDao.markAsSynced(ids)

                    // Save sync timestamp
                    saveSyncTime()

                    Log.d(TAG, "Successfully synced ${body.data.syncedCount} entries")
                    body.data.syncedCount.asSuccess()
                } else {
                    val errorMsg = body.error?.message ?: "Unknown error"
                    Log.e(TAG, "Sync failed: $errorMsg")
                    "Sync failed: $errorMsg".asError()
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

            val localVersion = getLocalModelVersion()
            val response = apiService.checkModelUpdate(localVersion)

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    Log.e(TAG, "Model update response body is null")
                    return@withContext "Failed to check model version: Empty response".asError()
                }

                if (body.success && body.data != null) {
                    val updateAvailable = body.data.isActive
                    Log.d(TAG, "Model update check: ${if (updateAvailable) "Update available" else "Up to date"}")
                    updateAvailable.asSuccess()
                } else {
                    Log.w(TAG, "No model update available")
                    false.asSuccess()
                }
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
            Log.d(TAG, "Model download not yet implemented")
            "Model download not yet implemented".asError()
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to download model"
            Log.e(TAG, errorMsg, e)
            errorMsg.asError()
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

    override suspend fun getUnsyncedCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val unsyncedEntities = farmDataDao.getUnsyncedData()
            unsyncedEntities.size.asSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unsynced count", e)
            (e.message ?: "Failed to get unsynced count").asError()
        }
    }

    /**
     * Format timestamp to ISO 8601 format (YYYY-MM-DDTHH:MM:SSZ)
     */
    private fun formatTimestampISO(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }

    /**
     * Format date to ISO 8601 date format (YYYY-MM-DD)
     */
    private fun formatDateISO(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }

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

    private fun getLocalModelVersion(): String {
        return try {
            val prefs = context.getSharedPreferences(MODEL_PREFS, Context.MODE_PRIVATE)
            prefs.getString(MODEL_VERSION_KEY, "1.0.0") ?: "1.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local model version", e)
            "1.0.0"
        }
    }
}
