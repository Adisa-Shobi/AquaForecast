package com.example.aquaforecast.domain.repository

import com.example.aquaforecast.domain.model.Pond

interface PondRepository {

    /**
     * Save pond configuration to the database
     * This is typically done during initial setup
     *
     * @param pond The pond configuration containing name, species, stock count, and start date
     * @return Result.Success with the generated pond ID if saved successfully,
     *         Result.Error with message if save failed
     */
    suspend fun save(pond: Pond): Result<Long>

    /**
     * Retrieve the current pond configuration
     * Returns the single configured pond for the current user
     *
     * @return Result.Success with pond configuration or null if not configured,
     *         Result.Error with message if retrieval failed
     */
    suspend fun get(): Result<Pond?>

    /**
     * Update existing pond configuration
     * Used to modify pond details like name, stock count, etc.
     *
     * @param pond The pond with updated information (must have valid ID)
     * @return Result.Success with Unit if updated successfully,
     *         Result.Error with message if update failed
     */
    suspend fun update(pond: Pond): Result<Unit>

    /**
     * Delete the pond configuration and all associated data
     * This is a destructive operation that removes all farm data and predictions
     *
     * @return Result.Success with Unit if deleted successfully,
     *         Result.Error with message if deletion failed
     */
    suspend fun delete(): Result<Unit>

    /**
     * Check if a pond has been configured by the user
     * Used to determine if user should see setup screen or dashboard
     *
     * @return Result.Success with true if pond is configured, false otherwise,
     *         Result.Error with message if check failed
     */
    suspend fun isConfigured(): Result<Boolean>
}