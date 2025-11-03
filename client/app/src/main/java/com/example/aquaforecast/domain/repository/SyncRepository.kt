package com.example.aquaforecast.domain.repository

interface SyncRepository {

    /**
     * Synchronize local farm data to backend server
     * Uploads all unsynced farm data entries and marks them as synced on success
     * This is typically called by WorkManager on a scheduled basis
     *
     * @return Result.Success with number of entries synced if successful,
     *         Result.Error with message if sync failed (network error, server error, etc.)
     */
    suspend fun syncData(): Result<Int>

    /**
     * Check if a new ML model version is available from the backend
     * Compares local model version with latest version on server
     *
     * @return Result.Success with true if update is available, false if current model is up-to-date,
     *         Result.Error with message if check failed (network error, server unreachable, etc.)
     */
    suspend fun checkModelUpdate(): Result<Boolean>

    /**
     * Download updated ML model from backend server
     * Fetches the latest .tflite model file and saves it to local storage
     * Should only be called after checkModelUpdate() returns true
     *
     * @return Result.Success with file path to the downloaded model if successful,
     *         Result.Error with message if download failed (network error, insufficient storage, etc.)
     */
    suspend fun downloadModel(): Result<String>

    /**
     * Get the timestamp of the last successful data sync
     * Used to display sync status to user and determine when to sync again
     *
     * @return Result.Success with timestamp in milliseconds or null if never synced,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getLastSyncTime(): Result<Long?>

    /**
     * Get the count of unsynced farm data entries
     * Used to show progress information to the user before syncing
     *
     * @return Result.Success with count of unsynced entries,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getUnsyncedCount(): Result<Int>
}