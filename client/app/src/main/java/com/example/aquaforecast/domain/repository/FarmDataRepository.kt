package com.example.aquaforecast.domain.repository

import com.example.aquaforecast.domain.model.FarmData


interface FarmDataRepository {

    /**
     * Save a new farm data entry to the database
     *
     * @param farmData The farm data entry to save containing water quality measurements
     * @return Result.Success with the ID of the saved entry, Result.Error with message if failed
     */
    suspend fun save(farmData: FarmData): Result<Long>

    /**
     * Retrieve all farm data entries from the database
     *
     * @return Result.Success with list of all farm data entries ordered by timestamp (newest first),
     *         Result.Error with message if retrieval failed
     */
    suspend fun getAll(): Result<List<FarmData>>

    /**
     * Retrieve farm data entries within a specific date range
     *
     * @param startDate Start timestamp in milliseconds (inclusive)
     * @param endDate End timestamp in milliseconds (inclusive)
     * @return Result.Success with filtered list of farm data entries ordered by timestamp (newest first),
     *         Result.Error with message if retrieval failed
     */
    suspend fun getByDateRange(startDate: Long, endDate: Long): Result<List<FarmData>>

    /**
     * Get the most recent farm data entry for a specific pond
     *
     * @param pondId The unique identifier of the pond
     * @return Result.Success with the latest farm data entry or null if no data exists,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getLatest(pondId: String): Result<FarmData?>

    /**
     * Retrieve recent farm data entries for a specific pond
     *
     * @param pondId The unique identifier of the pond
     * @param limit Maximum number of entries to retrieve (default: 7)
     * @return Result.Success with list of recent entries ordered by timestamp (newest first),
     *         Result.Error with message if retrieval failed
     */
    suspend fun getRecent(pondId: String, limit: Int = 7): Result<List<FarmData>>

    /**
     * Get all farm data entries that have not been synced to the backend server
     * Used for background sync operations
     *
     * @return Result.Success with list of unsynced entries,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getUnsynced(): Result<List<FarmData>>

    /**
     * Mark farm data entries as synced after successful upload to backend
     *
     * @param ids List of farm data entry IDs to mark as synced
     * @return Result.Success with Unit if marked successfully,
     *         Result.Error with message if operation failed
     */
    suspend fun markSynced(ids: List<Long>): Result<Unit>
}