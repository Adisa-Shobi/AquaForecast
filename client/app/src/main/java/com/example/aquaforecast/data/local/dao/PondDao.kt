package com.example.aquaforecast.data.local.dao

import androidx.room.*
import com.example.aquaforecast.data.local.entity.PondEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for pond configuration
 * Handles all database operations for pond data
 */
@Dao
interface PondDao {

    /**
     * Get the current pond configuration
     * Returns null if no pond is configured
     */
    @Query("SELECT * FROM pond LIMIT 1")
    suspend fun getPond(): PondEntity?

    /**
     * Observe pond configuration changes
     * Emits null if no pond exists, emits updated pond on changes
     */
    @Query("SELECT * FROM pond LIMIT 1")
    fun observePond(): Flow<PondEntity?>

    /**
     * Insert a new pond configuration
     * Replaces existing pond if one exists (due to OnConflictStrategy.REPLACE)
     * Returns the row ID of the inserted pond
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pond: PondEntity): Long

    /**
     * Update existing pond configuration
     * Requires the pond to have a valid ID
     */
    @Update
    suspend fun update(pond: PondEntity)

    /**
     * Delete all pond configurations
     * Since we only support one pond, this effectively resets the pond config
     */
    @Query("DELETE FROM pond")
    suspend fun deleteAll()

    /**
     * Check if a pond configuration exists
     * Returns true if at least one pond record exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM pond LIMIT 1)")
    suspend fun exists(): Boolean

    /**
     * Get pond by ID
     * Useful if we later support multiple ponds
     */
    @Query("SELECT * FROM pond WHERE id = :pondId")
    suspend fun getPondById(pondId: Long): PondEntity?

    /**
     * Delete pond by ID
     * Useful if we later support multiple ponds
     */
    @Query("DELETE FROM pond WHERE id = :pondId")
    suspend fun deleteById(pondId: Long)

    /**
     * Get pond count
     * Returns the total number of ponds in the database
     */
    @Query("SELECT COUNT(*) FROM pond")
    suspend fun getCount(): Int

    /**
     * Update pond name
     * Allows updating just the name without affecting other fields
     */
    @Query("UPDATE pond SET name = :name WHERE id = :pondId")
    suspend fun updateName(pondId: Long, name: String)

    /**
     * Update stock count
     * Allows updating just the stock count without affecting other fields
     */
    @Query("UPDATE pond SET stock_count = :stockCount WHERE id = :pondId")
    suspend fun updateStockCount(pondId: Long, stockCount: Int)

    /**
     * Get pond name
     * Quick way to get just the pond name without loading entire entity
     */
    @Query("SELECT name FROM pond LIMIT 1")
    suspend fun getPondName(): String?

    /**
     * Get pond species
     * Quick way to get just the species without loading entire entity
     */
    @Query("SELECT species FROM pond LIMIT 1")
    suspend fun getPondSpecies(): String?
}