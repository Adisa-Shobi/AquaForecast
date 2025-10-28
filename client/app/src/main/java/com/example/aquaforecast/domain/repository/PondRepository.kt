// domain/repository/PondRepository.kt
package com.example.aquaforecast.domain.repository

import com.example.aquaforecast.domain.model.Pond
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for pond configuration and management
 * Provides both low-level operations (save, update, delete) and high-level
 * UI-friendly helper functions for common operations
 */
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
    /**
     * Save or update pond configuration from UI form data
     * This is a convenience method that handles both insert and update operations
     * automatically based on whether a pond already exists
     *
     * If a pond already exists, it will be updated with the new values.
     * If no pond exists, a new one will be created.
     *
     * @param pondName The name of the pond (e.g., "Main Pond A")
     * @param species The species as a string ("Tilapia" or "Catfish")
     * @param initialStockCount The initial number of fish in the pond
     * @param startDate The date when the pond was stocked
     * @return Result.Success with Unit if saved successfully,
     *         Result.Error with message if save failed (e.g., invalid species)
     *
     * @throws IllegalArgumentException if species is not "Tilapia" or "Catfish"
     */
    suspend fun savePondConfig(
        pondName: String,
        species: String,
        initialStockCount: Int,
        startDate: LocalDate
    ): Result<Unit>

    /**
     * Get the current pond configuration
     * Alias for get() with more descriptive name for UI usage
     *
     * @return Result.Success with pond configuration or null if not configured,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getPondConfig(): Result<Pond?>

    /**
     * Observe pond configuration changes in real-time
     * Returns a Flow that emits the current pond configuration whenever it changes
     * Useful for reactive UI updates
     *
     * Emits:
     * - null when no pond is configured
     * - Pond object when configuration exists
     * - Updated Pond object whenever configuration changes
     *
     * @return Flow<Pond?> that emits pond configuration changes
     */
    fun observePondConfig(): Flow<Pond?>

    /**
     * Delete the pond configuration
     * Alias for delete() with more descriptive name for UI usage
     *
     * This is a destructive operation that removes all farm data and predictions.
     * Consider showing a confirmation dialog before calling this method.
     *
     * @return Result.Success with Unit if deleted successfully,
     *         Result.Error with message if deletion failed
     */
    suspend fun deletePondConfig(): Result<Unit>

    /**
     * Get all ponds from the database
     * Returns a list of all configured ponds
     *
     * @return Result.Success with list of ponds (empty if none configured),
     *         Result.Error with message if retrieval failed
     */
    suspend fun getAllPonds(): Result<List<Pond>>

    /**
     * Get a specific pond by its ID
     *
     * @param pondId The ID of the pond to retrieve
     * @return Result.Success with pond or null if not found,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getPondById(pondId: Long): Result<Pond?>

    /**
     * Delete a specific pond by its ID
     *
     * @param pondId The ID of the pond to delete
     * @return Result.Success with Unit if deleted successfully,
     *         Result.Error with message if deletion failed
     */
    suspend fun deletePondById(pondId: Long): Result<Unit>

    /**
     * Get the total count of configured ponds
     *
     * @return Result.Success with count of ponds,
     *         Result.Error with message if count failed
     */
    suspend fun getPondCount(): Result<Int>

    /**
     * Report fish deaths and update stock count
     *
     * @param pondId The ID of the pond
     * @param deathCount The number of fish that died
     * @return Result.Success with Unit if updated successfully,
     *         Result.Error with message if update failed
     */
    suspend fun reportFishDeaths(pondId: Long, deathCount: Int): Result<Unit>

    /**
     * Mark a pond as harvested (readonly)
     *
     * @param pondId The ID of the pond to mark as harvested
     * @return Result.Success with Unit if marked successfully,
     *         Result.Error with message if operation failed
     */
    suspend fun markPondAsHarvested(pondId: Long): Result<Unit>

    /**
     * Mark a pond as not harvested (reverse harvest operation)
     *
     * @param pondId The ID of the pond to mark as not harvested
     * @return Result.Success with Unit if marked successfully,
     *         Result.Error with message if operation failed
     */
    suspend fun markPondAsNotHarvested(pondId: Long): Result<Unit>
}